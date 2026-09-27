import core from "./phase14ak-partial-return.js";

const BOOKING_PATH = "/api/admin/bookings";
const PUBLIC_CATALOG_PATH = "/api/public/catalog";

function wrappedStatement(native, sql, args = []) {
  const metadata = { native, sql: String(sql || ""), args };
  return {
    __phase14al: metadata,
    bind(...values) { return wrappedStatement(native.bind(...values), sql, values); },
    first(...values) { return native.first(...values); },
    run(...values) { return native.run(...values); },
    all(...values) { return native.all(...values); },
    raw(...values) { return native.raw(...values); }
  };
}

function statementMetadata(statement) {
  return statement?.__phase14al || { native: statement, sql: "", args: [] };
}

function sequentialBookingNo(businessDate, value) {
  return `BK-${String(businessDate).replace(/-/g, "")}-${String(value).padStart(3, "0")}`;
}

function parseDisplayPreferences(value) {
  let raw = {};
  try { raw = typeof value === "string" && value ? JSON.parse(value) : (value || {}); } catch {}
  return {
    defaultLanguage: raw?.defaultLanguage === "EN" ? "EN" : "GU",
    dateFormat: raw?.dateFormat === "YYYY-MM-DD" ? "YYYY-MM-DD" : "DD-MM-YYYY"
  };
}

function createDatabaseProxy(realDb, state, mode) {
  async function runBatch(statements) {
    const entries = statements.map(statementMetadata);

    if (mode === "booking") {
      const bookingIndex = entries.findIndex(entry =>
        /INSERT\s+INTO\s+bookings\s*\(id,booking_no,customer_id,booking_date,pickup_date,return_date,status,(?:confirmation_state,advance_amount,)?notes,created_by_user_id,request_key\)/i.test(entry.sql)
      );
      if (bookingIndex >= 0) {
        const original = entries[bookingIndex];
        const modernShape = /confirmation_state,advance_amount/i.test(original.sql);
        let id, customerId, bookingDate, pickupDate, returnDate, confirmationState, advanceAmount, notes, createdByUserId, requestKey;
        if (modernShape) {
          [id, , customerId, bookingDate, pickupDate, returnDate, confirmationState, advanceAmount, notes, createdByUserId, requestKey] = original.args;
        } else {
          [id, , customerId, bookingDate, pickupDate, returnDate, notes, createdByUserId, requestKey] = original.args;
          confirmationState = "BOOKED";
          advanceAmount = 0;
        }
        if (!/^\d{4}-\d{2}-\d{2}$/.test(String(bookingDate || ""))) throw new Error("INVALID_BOOKING_SEQUENCE_DATE");

        const sequenceStatement = realDb.prepare(`
          INSERT INTO booking_daily_sequences (business_date,sequence_value,updated_at)
          VALUES (?,1,CURRENT_TIMESTAMP)
          ON CONFLICT(business_date) DO UPDATE SET
            sequence_value=booking_daily_sequences.sequence_value+1,
            updated_at=CURRENT_TIMESTAMP
          RETURNING sequence_value
        `).bind(bookingDate);

        const bookingStatement = modernShape
          ? realDb.prepare(`
              INSERT INTO bookings (
                id,booking_no,customer_id,booking_date,pickup_date,return_date,status,
                confirmation_state,advance_amount,notes,created_by_user_id,request_key
              )
              SELECT ?,
                'BK-' || REPLACE(?, '-', '') || '-' || printf('%03d', sequence_value),
                ?,?,?,?,'BOOKED',?,?,?,?,?
              FROM booking_daily_sequences
              WHERE business_date=?
            `).bind(
              id, bookingDate, customerId, bookingDate, pickupDate, returnDate,
              confirmationState, advanceAmount, notes, createdByUserId, requestKey, bookingDate
            )
          : realDb.prepare(`
              INSERT INTO bookings (id,booking_no,customer_id,booking_date,pickup_date,return_date,status,notes,created_by_user_id,request_key)
              SELECT ?,
                'BK-' || REPLACE(?, '-', '') || '-' || printf('%03d', sequence_value),
                ?,?,?,?,'BOOKED',?,?,?
              FROM booking_daily_sequences
              WHERE business_date=?
            `).bind(id, bookingDate, customerId, bookingDate, pickupDate, returnDate, notes, createdByUserId, requestKey, bookingDate);

        const nativeStatements = entries.map(entry => entry.native);
        nativeStatements[bookingIndex] = bookingStatement;

        const auditIndex = entries.findIndex(entry =>
          /INSERT\s+INTO\s+audit_logs/i.test(entry.sql) &&
          entry.args?.[2] === "CREATE" && entry.args?.[3] === "BOOKING" && String(entry.args?.[4] || "") === String(id)
        );
        if (auditIndex >= 0) {
          const auditArgs = entries[auditIndex].args;
          nativeStatements[auditIndex] = realDb.prepare(`
            INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json)
            VALUES (?,?,?,?,?,json_set(COALESCE(?, '{}'), '$.bookingNo', (SELECT booking_no FROM bookings WHERE id=?)))
          `).bind(auditArgs[0], auditArgs[1], auditArgs[2], auditArgs[3], auditArgs[4], auditArgs[5], id);
        }

        const results = await realDb.batch([sequenceStatement, ...nativeStatements]);
        const allocated = Number(results?.[0]?.results?.[0]?.sequence_value || 0);
        if (Number.isInteger(allocated) && allocated > 0) {
          state.bookingNo = sequentialBookingNo(bookingDate, allocated);
          state.bookingId = String(id || "");
        }
        return results.slice(1);
      }
    }

    const results = await realDb.batch(entries.map(entry => entry.native));
    if (mode === "catalog") {
      const settingsIndex = entries.findIndex(entry =>
        /SELECT\s+value_json\s+FROM\s+settings\s+WHERE\s+key=['"]site_settings['"]\s+LIMIT\s+1/i.test(entry.sql)
      );
      if (settingsIndex >= 0) {
        const value = results?.[settingsIndex]?.results?.[0]?.value_json;
        state.displayPreferences = parseDisplayPreferences(value);
      }
    }
    return results;
  }

  return new Proxy(realDb, {
    get(target, property, receiver) {
      if (property === "prepare") return sql => wrappedStatement(target.prepare(sql), sql);
      if (property === "batch") return runBatch;
      const value = Reflect.get(target, property, receiver);
      return typeof value === "function" ? value.bind(target) : value;
    }
  });
}

function proxiedEnv(env, state, mode) {
  const database = createDatabaseProxy(env.DB, state, mode);
  return new Proxy(env, {
    get(target, property, receiver) {
      if (property === "DB") return database;
      return Reflect.get(target, property, receiver);
    }
  });
}

async function rewriteJsonResponse(response, mutator) {
  if (!response.ok || !(response.headers.get("content-type") || "").includes("application/json")) return response;
  let data;
  try { data = await response.clone().json(); } catch { return response; }
  const changed = await mutator(data);
  if (!changed) return response;
  const headers = new Headers(response.headers);
  headers.delete("content-length");
  return new Response(JSON.stringify(data), { status: response.status, statusText: response.statusText, headers });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === "POST" && url.pathname === BOOKING_PATH) {
      const state = {};
      const response = await core.fetch(request, proxiedEnv(env, state, "booking"), ctx);
      return rewriteJsonResponse(response, async data => {
        if (response.status !== 201 || data?.ok !== true || !data?.id) return false;
        let actual = state.bookingNo || "";
        if (!actual) {
          const row = await env.DB.prepare(`SELECT booking_no FROM bookings WHERE id=? LIMIT 1`).bind(String(data.id)).first();
          actual = row?.booking_no ? String(row.booking_no) : "";
        }
        if (!/^BK-\d{8}-\d{3,}$/.test(actual)) {
          data.bookingNo = "";
          return true;
        }
        data.bookingNo = actual;
        return true;
      });
    }

    if (request.method === "GET" && url.pathname === PUBLIC_CATALOG_PATH) {
      const state = {};
      const response = await core.fetch(request, proxiedEnv(env, state, "catalog"), ctx);
      return rewriteJsonResponse(response, async data => {
        if (!data?.shop || !state.displayPreferences) return false;
        data.shop.defaultLanguage = state.displayPreferences.defaultLanguage;
        data.shop.dateFormat = state.displayPreferences.dateFormat;
        return true;
      });
    }

    return core.fetch(request, env, ctx);
  }
};
