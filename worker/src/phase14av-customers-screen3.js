import core from "./phase14au-dashboard-screen2.js";
import { getSessionUser } from "./auth.ts";

function apiJson(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
      "referrer-policy": "no-referrer"
    }
  });
}

function isAdmin(user) {
  return user && String(user.role || "").toUpperCase() !== "STAFF";
}

function text(value, max = 500) {
  return String(value ?? "").trim().slice(0, max);
}

function mobile10(value) {
  const digits = String(value || "").replace(/\D/g, "");
  if (digits.length === 12 && digits.startsWith("91")) return digits.slice(2);
  return digits;
}

async function readBody(request) {
  try {
    const body = await request.json();
    return body && typeof body === "object" && !Array.isArray(body) ? body : null;
  } catch {
    return null;
  }
}

async function requireUser(request, env) {
  return await getSessionUser(env, request);
}

function auditStatement(env, userId, action, recordId, oldValue, newValue) {
  return env.DB.prepare(`
    INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json)
    VALUES (?,?,?,?,?,?,?)
  `).bind(
    crypto.randomUUID(),
    userId || null,
    action,
    "CUSTOMER",
    recordId || null,
    oldValue === undefined ? null : JSON.stringify(oldValue),
    newValue === undefined ? null : JSON.stringify(newValue)
  );
}

async function writeAudit(env, userId, action, recordId, oldValue, newValue) {
  await auditStatement(env, userId, action, recordId, oldValue, newValue).run();
}

function displayStatus(status, confirmationState) {
  const raw = String(status || "").toUpperCase();
  const confirmation = String(confirmationState || "BOOKED").toUpperCase();
  if (raw === "BOOKED" && confirmation === "RESERVED") return "RESERVED";
  if (raw === "BOOKED") return "BOOKED";
  if (raw === "PARTIALLY_GIVEN") return "PART_PICKUP";
  if (raw === "GIVEN") return "FULL_PICKUP";
  if (raw === "PARTIALLY_RETURNED") return "PART_RETURN";
  if (raw === "RETURNED") return "FULL_RETURN";
  if (raw === "CANCELLED") return "CANCELLED";
  return raw || "BOOKED";
}

function businessToday() {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(new Date());
  const byType = Object.fromEntries(parts.map(part => [part.type, part.value]));
  return `${byType.year}-${byType.month}-${byType.day}`;
}

function sortSql(sort) {
  switch (String(sort || "NAME_ASC").toUpperCase()) {
    case "NAME_DESC": return "c.name COLLATE NOCASE DESC, c.created_at DESC";
    case "NEWEST": return "c.created_at DESC, c.name COLLATE NOCASE ASC";
    case "OLDEST": return "c.created_at ASC, c.name COLLATE NOCASE ASC";
    default: return "c.name COLLATE NOCASE ASC, c.created_at DESC";
  }
}

function bookingSortSql(sort) {
  switch (String(sort || "NEWEST").toUpperCase()) {
    case "OLDEST": return "b.created_at ASC, b.id ASC";
    case "PICKUP_ASC": return "b.pickup_date ASC, b.created_at DESC";
    case "PICKUP_DESC": return "b.pickup_date DESC, b.created_at DESC";
    case "RETURN_ASC": return "b.return_date ASC, b.created_at DESC";
    case "RETURN_DESC": return "b.return_date DESC, b.created_at DESC";
    default: return "b.created_at DESC, b.id DESC";
  }
}

async function listCustomers(request, env, user) {
  const url = new URL(request.url);
  const archived = url.searchParams.get("archived") === "1";
  if (archived && !isAdmin(user)) {
    return apiJson({ ok: false, error: "FORBIDDEN", message: "You do not have permission for archived customers." }, 403);
  }

  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(10, Math.max(1, Number(url.searchParams.get("pageSize") || "10") || 10));
  const search = text(url.searchParams.get("search"), 120);
  const pin = text(url.searchParams.get("pin"), 100);
  const where = ["c.permanently_deleted_at IS NULL", archived ? "c.archived_at IS NOT NULL" : "c.archived_at IS NULL"];
  const binds = [];

  if (search) {
    const q = `%${search}%`;
    where.push("(LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR COALESCE(c.alternate_mobile,'') LIKE ?)");
    binds.push(q, q, q);
  }

  const clause = where.join(" AND ");
  const order = sortSql(url.searchParams.get("sort"));
  const offset = (page - 1) * pageSize;

  const [summaryResult, countResult, rowsResult] = await env.DB.batch([
    env.DB.prepare(`
      SELECT COUNT(*) AS total_customers,
             COALESCE(SUM(CASE WHEN archived_at IS NULL THEN 1 ELSE 0 END),0) AS active_customers,
             COALESCE(SUM(CASE WHEN archived_at IS NOT NULL THEN 1 ELSE 0 END),0) AS archived_customers
      FROM customers
      WHERE permanently_deleted_at IS NULL
    `),
    env.DB.prepare(`SELECT COUNT(*) AS count FROM customers c WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`
      SELECT c.id,c.name,c.mobile,c.alternate_mobile,c.address,c.archived_at,c.created_at,c.updated_at,
             (SELECT COUNT(*) FROM bookings b WHERE b.customer_id=c.id) AS total_bookings,
             (SELECT COUNT(*) FROM bookings b WHERE b.customer_id=c.id AND b.status NOT IN ('CANCELLED','RETURNED')) AS active_bookings,
             COALESCE((
               SELECT json_group_array(json_object(
                 'id',x.id,
                 'booking_no',x.booking_no,
                 'status',x.status,
                 'display_status',x.display_status,
                 'pickup_date',x.pickup_date,
                 'return_date',x.return_date
               ))
               FROM (
                 SELECT b.id,b.booking_no,b.status,b.pickup_date,b.return_date,
                        CASE
                          WHEN b.status='BOOKED' AND COALESCE(b.confirmation_state,'BOOKED')='RESERVED' THEN 'RESERVED'
                          WHEN b.status='BOOKED' THEN 'BOOKED'
                          WHEN b.status='PARTIALLY_GIVEN' THEN 'PART_PICKUP'
                          WHEN b.status='GIVEN' THEN 'FULL_PICKUP'
                          WHEN b.status='PARTIALLY_RETURNED' THEN 'PART_RETURN'
                          WHEN b.status='RETURNED' THEN 'FULL_RETURN'
                          WHEN b.status='CANCELLED' THEN 'CANCELLED'
                          ELSE b.status END AS display_status
                 FROM bookings b
                 WHERE b.customer_id=c.id AND b.status NOT IN ('CANCELLED','RETURNED')
                 ORDER BY b.pickup_date ASC,b.created_at DESC
                 LIMIT 20
               ) x
             ),'[]') AS active_booking_options
      FROM customers c
      WHERE ${clause}
      ORDER BY CASE WHEN ?<>'' AND c.id=? THEN 0 ELSE 1 END, ${order}
      LIMIT ? OFFSET ?
    `).bind(...binds, pin, pin, pageSize, offset)
  ]);

  const summary = summaryResult?.results?.[0] || {};
  const total = Number(countResult?.results?.[0]?.count || 0);
  const customers = (rowsResult?.results || []).map(row => ({
    ...row,
    total_bookings: Number(row.total_bookings || 0),
    active_bookings: Number(row.active_bookings || 0),
    active_booking_options: (() => {
      try { return JSON.parse(String(row.active_booking_options || "[]")); }
      catch { return []; }
    })()
  }));

  return apiJson({
    ok: true,
    summary: {
      totalCustomers: Number(summary.total_customers || 0),
      activeCustomers: Number(summary.active_customers || 0),
      archivedCustomers: Number(summary.archived_customers || 0)
    },
    customers,
    pagination: {
      page,
      pageSize,
      total,
      totalPages: Math.max(1, Math.ceil(total / pageSize))
    }
  });
}

function validateCustomer(body) {
  if (!body) return { error: "Invalid request." };
  const name = text(body.name, 120);
  const mobile = mobile10(body.mobile);
  const alternateMobile = mobile10(body.alternateMobile);
  const address = text(body.address, 500);
  if (!name) return { error: "Name is required." };
  if (!/^\d{10}$/.test(mobile)) return { error: "Enter a valid 10-digit mobile number." };
  if (alternateMobile && !/^\d{10}$/.test(alternateMobile)) return { error: "Enter a valid 10-digit alternative mobile number." };
  return { name, mobile, alternateMobile, address };
}

async function hasDuplicatePrimary(env, mobile, excludeId = "") {
  const row = await env.DB.prepare(`
    SELECT id FROM customers WHERE mobile=? AND (?='' OR id<>?) LIMIT 1
  `).bind(mobile, excludeId, excludeId).first();
  return !!row;
}

async function createCustomer(request, env, user) {
  const input = validateCustomer(await readBody(request));
  if (input.error) return apiJson({ ok: false, error: "VALIDATION", message: input.error }, 400);
  if (await hasDuplicatePrimary(env, input.mobile)) {
    return apiJson({ ok: false, error: "DUPLICATE_MOBILE", message: "This primary mobile number is already registered." }, 409);
  }

  const id = crypto.randomUUID();
  await env.DB.batch([
    env.DB.prepare(`
      INSERT INTO customers (id,name,mobile,alternate_mobile,address,notes,is_active,archived_at)
      VALUES (?,?,?,?,?,NULL,1,NULL)
    `).bind(id, input.name, input.mobile, input.alternateMobile || null, input.address || null),
    env.DB.prepare(`
      INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json)
      VALUES (?,?,?,?,?,?)
    `).bind(crypto.randomUUID(), user.id, "CREATE", "CUSTOMER", id, JSON.stringify(input))
  ]);

  return apiJson({ ok: true, id, message: "Customer created successfully." }, 201);
}

async function updateCustomer(request, env, user, id) {
  const old = await env.DB.prepare(`SELECT * FROM customers WHERE id=?`).bind(id).first();
  if (!old) return apiJson({ ok: false, error: "NOT_FOUND", message: "Customer not found." }, 404);
  if (old.archived_at) return apiJson({ ok: false, error: "CUSTOMER_ARCHIVED", message: "Restore the customer before editing." }, 409);

  const body = await readBody(request);
  const input = validateCustomer(body);
  if (input.error) return apiJson({ ok: false, error: "VALIDATION", message: input.error }, 400);
  if (await hasDuplicatePrimary(env, input.mobile, id)) {
    return apiJson({ ok: false, error: "DUPLICATE_MOBILE", message: "This primary mobile number is already registered." }, 409);
  }

  const expectedUpdatedAt = String(body?.expectedUpdatedAt || "").trim();
  const nextUpdatedAt = new Date().toISOString();
  const result = await env.DB.batch([
    expectedUpdatedAt
      ? env.DB.prepare(`UPDATE customers SET name=?,mobile=?,alternate_mobile=?,address=?,updated_at=? WHERE id=? AND updated_at=? AND archived_at IS NULL`)
          .bind(input.name, input.mobile, input.alternateMobile || null, input.address || null, nextUpdatedAt, id, expectedUpdatedAt)
      : env.DB.prepare(`UPDATE customers SET name=?,mobile=?,alternate_mobile=?,address=?,updated_at=? WHERE id=? AND archived_at IS NULL`)
          .bind(input.name, input.mobile, input.alternateMobile || null, input.address || null, nextUpdatedAt, id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
      SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM customers WHERE id=? AND updated_at=?)`)
      .bind(crypto.randomUUID(), user.id, "UPDATE", "CUSTOMER", id, JSON.stringify(old), JSON.stringify(input), id, nextUpdatedAt)
  ]);
  if (expectedUpdatedAt && Number(result?.[0]?.meta?.changes || 0) === 0) {
    return apiJson({ ok: false, error: "STALE_WRITE", message: "This customer changed on another device. Refresh before saving again." }, 409);
  }
  return apiJson({ ok: true, id, updatedAt: nextUpdatedAt, message: "Customer updated successfully." });
}

async function archiveCustomer(env, user, id) {
  if (!isAdmin(user)) return apiJson({ ok: false, error: "FORBIDDEN", message: "You do not have permission for this action." }, 403);
  const old = await env.DB.prepare(`SELECT * FROM customers WHERE id=?`).bind(id).first();
  if (!old) return apiJson({ ok: false, error: "NOT_FOUND", message: "Customer not found." }, 404);
  if (old.archived_at) return apiJson({ ok: true, id, message: "Customer is already archived." });

  const active = await env.DB.prepare(`
    SELECT COUNT(*) AS count FROM bookings
    WHERE customer_id=? AND status NOT IN ('CANCELLED','RETURNED')
  `).bind(id).first();
  if (Number(active?.count || 0) > 0) {
    return apiJson({
      ok: false,
      error: "ACTIVE_BOOKINGS",
      message: "Cancel or complete all active bookings before deleting this customer."
    }, 409);
  }

  await env.DB.batch([
    env.DB.prepare(`
      UPDATE customers SET archived_at=CURRENT_TIMESTAMP,is_active=0,updated_at=CURRENT_TIMESTAMP WHERE id=?
    `).bind(id),
    auditStatement(env, user.id, "ARCHIVE", id, old, {
      archived: true,
      bookingHistoryPreserved: true
    })
  ]);
  return apiJson({ ok: true, id, message: "Customer deleted successfully." });
}

async function restoreCustomer(env, user, id) {
  if (!isAdmin(user)) return apiJson({ ok: false, error: "FORBIDDEN", message: "You do not have permission for this action." }, 403);
  const old = await env.DB.prepare(`SELECT * FROM customers WHERE id=?`).bind(id).first();
  if (!old) return apiJson({ ok: false, error: "NOT_FOUND", message: "Customer not found." }, 404);
  if (!old.archived_at) return apiJson({ ok: true, id, message: "Customer is already active." });

  await env.DB.batch([
    env.DB.prepare(`
      UPDATE customers SET archived_at=NULL,is_active=1,updated_at=CURRENT_TIMESTAMP WHERE id=?
    `).bind(id),
    auditStatement(env, user.id, "RESTORE", id, old, {
      archived: false,
      bookingHistoryPreserved: true
    })
  ]);
  return apiJson({ ok: true, id, message: "Customer restored successfully." });
}

async function permanentDelete(env, user, id) {
  if (!isAdmin(user)) return apiJson({ ok: false, error: "FORBIDDEN", message: "You do not have permission for this action." }, 403);
  const old = await env.DB.prepare(`SELECT * FROM customers WHERE id=? AND permanently_deleted_at IS NULL`).bind(id).first();
  if (!old) return apiJson({ ok: false, error: "NOT_FOUND", message: "Customer not found." }, 404);
  if (!old.archived_at) return apiJson({ ok: false, error: "ARCHIVE_FIRST", message: "Delete/archive the customer before permanent deletion." }, 409);

  const active = await env.DB.prepare(`
    SELECT COUNT(*) AS count FROM bookings
    WHERE customer_id=? AND status NOT IN ('CANCELLED','RETURNED')
  `).bind(id).first();
  if (Number(active?.count || 0) > 0) {
    return apiJson({ ok: false, error: "ACTIVE_BOOKINGS", message: "Active bookings must be cancelled or completed first." }, 409);
  }

  const bookingCountRow = await env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings WHERE customer_id=?`).bind(id).first();
  const bookingCount = Number(bookingCountRow?.count || 0);
  const billCountRow = await env.DB.prepare(`SELECT COUNT(*) AS count FROM bills WHERE customer_id=?`).bind(id).first();
  const billCount = Number(billCountRow?.count || 0);
  const deletedAt = new Date().toISOString();
  const deletedMobile = `deleted-${String(id).replace(/[^a-zA-Z0-9]/g, "").slice(0, 24)}`;

  await env.DB.batch([
    // Financial history is authoritative. Keep every Bill/Bill Item snapshot, but
    // detach live Order references before operational booking history is removed.
    env.DB.prepare(`
      UPDATE bills
      SET booking_no_snapshot=COALESCE(
            booking_no_snapshot,
            (SELECT booking_no FROM bookings WHERE id=bills.booking_id)
          ),
          booking_id=NULL,
          updated_at=?
      WHERE customer_id=?
    `).bind(deletedAt,id),
    env.DB.prepare(`UPDATE whatsapp_activity_logs
      SET customer_name_snapshot=COALESCE(customer_name_snapshot,?),
          customer_mobile_snapshot=COALESCE(customer_mobile_snapshot,?),
          booking_no_snapshot=COALESCE(booking_no_snapshot,(SELECT booking_no FROM bookings WHERE id=whatsapp_activity_logs.booking_id)),
          item_name_snapshot=COALESCE(item_name_snapshot,(SELECT item_name FROM items WHERE id=whatsapp_activity_logs.item_id)),
          customer_id=NULL
      WHERE customer_id=?`).bind(String(old.name || "Deleted customer"),String(old.mobile || ""),id),
    env.DB.prepare(`UPDATE audit_logs
      SET old_value_json='{"redacted":true,"customerDeleted":true}',
          new_value_json='{"redacted":true,"customerDeleted":true}'
      WHERE module='CUSTOMER' AND record_id=?`).bind(id),
    env.DB.prepare(`UPDATE pickup_events SET correction_of_event_id=NULL WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?)`).bind(id),
    env.DB.prepare(`UPDATE return_events SET correction_of_event_id=NULL WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?)`).bind(id),
    env.DB.prepare(`DELETE FROM pickup_event_items WHERE pickup_event_id IN (SELECT id FROM pickup_events WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?) OR given_to_customer_id=?)`).bind(id, id),
    env.DB.prepare(`DELETE FROM return_event_items WHERE return_event_id IN (SELECT id FROM return_events WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?))`).bind(id),
    env.DB.prepare(`DELETE FROM booking_close_event_items WHERE close_event_id IN (SELECT id FROM booking_close_events WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?)) OR booking_item_id IN (SELECT id FROM booking_items WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?))`).bind(id, id),
    env.DB.prepare(`DELETE FROM pickup_events WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?) OR given_to_customer_id=?`).bind(id, id),
    env.DB.prepare(`DELETE FROM return_events WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?)`).bind(id),
    env.DB.prepare(`DELETE FROM booking_close_events WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?)`).bind(id),
    env.DB.prepare(`DELETE FROM booking_items WHERE booking_id IN (SELECT id FROM bookings WHERE customer_id=?)`).bind(id),
    env.DB.prepare(`DELETE FROM bookings WHERE customer_id=?`).bind(id),
    // Keep a hidden tombstone row only to satisfy historical Bill FK integrity.
    // Live customer PII is scrubbed; Bill snapshots keep the finance record.
    env.DB.prepare(`
      UPDATE customers
      SET name='Deleted Customer',mobile=?,alternate_mobile=NULL,address=NULL,notes=NULL,
          is_active=0,archived_at=COALESCE(archived_at,?),permanently_deleted_at=?,updated_at=?
      WHERE id=?
    `).bind(deletedMobile,deletedAt,deletedAt,deletedAt,id),
    auditStatement(env, user.id, "DELETE_PERMANENT", id, { customerId: id, redacted: true }, {
      deleted: true,
      redacted: true,
      bookingsDeleted: bookingCount,
      billsPreserved: billCount,
      financialHistoryPreserved: true
    })
  ]);

  return apiJson({ ok: true, id, message: "Customer permanently deleted. Billing history was preserved." });
}

async function listVisibleBookings(request, env) {
  const url = new URL(request.url);
  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(10, Math.max(1, Number(url.searchParams.get("pageSize") || "10") || 10));
  const search = text(url.searchParams.get("search"), 120);
  const status = text(url.searchParams.get("status"), 40).toUpperCase();
  const view = text(url.searchParams.get("view"), 40).toUpperCase() || "ALL";
  const sort = text(url.searchParams.get("sort"), 40).toUpperCase() || "NEWEST";
  const today = businessToday();
  const where = ["c.archived_at IS NULL"];
  const binds = [];

  if (search) {
    const q = `%${search}%`;
    where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR EXISTS (
      SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id
      WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))
    ))`);
    binds.push(q, q, q, q, q);
  }

  if (view === "RESERVED") {
    where.push("b.status='BOOKED' AND COALESCE(b.confirmation_state,'BOOKED')='RESERVED'");
  } else if (view === "BOOKED") {
    where.push("b.status='BOOKED' AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED'");
  } else if (view === "PICKED_UP") {
    where.push("b.status IN ('PARTIALLY_GIVEN','GIVEN')");
  } else if (view === "RETURNED") {
    where.push("b.status IN ('PARTIALLY_RETURNED','RETURNED')");
  } else if (view === "UPCOMING") {
    // Backward-compatible alias retained for older Android builds.
    where.push("b.status='BOOKED'");
  } else if (view === "OVERDUE") {
    // Backward-compatible server filter; Overdue is no longer a lifecycle tab.
    where.push(`b.return_date < ? AND b.status IN ('PARTIALLY_GIVEN','GIVEN','PARTIALLY_RETURNED') AND EXISTS (
      SELECT 1 FROM booking_items bo WHERE bo.booking_id=b.id AND bo.given_qty>bo.returned_qty
    )`);
    binds.push(today);
  }

  if (status === "RESERVED") {
    where.push("b.status='BOOKED' AND COALESCE(b.confirmation_state,'BOOKED')='RESERVED'");
  } else if (status === "BOOKED") {
    where.push("b.status='BOOKED' AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED'");
  } else if (status) {
    where.push("b.status=?");
    binds.push(status);
  }

  const clause = where.join(" AND ");
  const order = bookingSortSql(sort);
  const offset = (page - 1) * pageSize;
  const [countResult, rowsResult] = await env.DB.batch([
    env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`
      SELECT b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,b.confirmation_state,b.notes,b.created_at,b.updated_at,
             c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,u.name AS booked_by,
             COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items_summary,
             COALESCE((SELECT COUNT(*) FROM booking_items bic WHERE bic.booking_id=b.id),0) AS item_count,
             COALESCE((
               SELECT json_group_array(json_object(
                 'item_name',p.item_name,
                 'image_url',p.image_url,
                 'quantity',p.booked_qty
               ))
               FROM (
                 SELECT i.item_name,bi.booked_qty,
                        (SELECT im.image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order ASC LIMIT 1) AS image_url
                 FROM booking_items bi
                 JOIN items i ON i.id=bi.item_id
                 WHERE bi.booking_id=b.id
                 ORDER BY i.item_name COLLATE NOCASE ASC
                 LIMIT 2
               ) p
             ),'[]') AS item_previews,
             COALESCE((SELECT SUM(bi.booked_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS booked_qty,
             COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS given_qty,
             COALESCE((SELECT SUM(bi.returned_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS returned_qty
      FROM bookings b
      JOIN customers c ON c.id=b.customer_id
      LEFT JOIN users u ON u.id=b.created_by_user_id
      WHERE ${clause}
      ORDER BY ${order}
      LIMIT ? OFFSET ?
    `).bind(...binds, pageSize, offset)
  ]);

  const total = Number(countResult?.results?.[0]?.count || 0);
  const bookings = (rowsResult?.results || []).map(row => ({
    ...row,
    display_status: displayStatus(row.status, row.confirmation_state),
    item_count: Number(row.item_count || 0),
    item_previews: (() => {
      try { return JSON.parse(String(row.item_previews || "[]")); }
      catch { return []; }
    })()
  }));
  return apiJson({
    ok: true,
    bookings,
    pagination: { page, pageSize, total, totalPages: Math.max(1, Math.ceil(total / pageSize)) }
  });
}

async function bookingAvailabilityForScreen4(request, env) {
  const body = await readBody(request);
  if (!body) return apiJson({ ok: false, error: "INVALID_JSON", message: "Invalid request." }, 400);

  const pickupDate = text(body.pickupDate, 10);
  const returnDate = text(body.returnDate, 10);
  const excludeBookingId = text(body.excludeBookingId, 100) || null;
  const validDate = /^\d{4}-\d{2}-\d{2}$/;
  const itemIds = Array.isArray(body.itemIds)
    ? [...new Set(body.itemIds.map(value => text(value, 100)).filter(Boolean))].slice(0, 300)
    : [];

  if (!validDate.test(pickupDate) || !validDate.test(returnDate) || pickupDate >= returnDate || itemIds.length === 0) {
    return apiJson({
      ok: false,
      error: "VALIDATION",
      message: "Valid Pickup Date, later Return Date and selected items are required."
    }, 400);
  }

  const placeholders = itemIds.map(() => "?").join(",");
  const rowsResult = await env.DB.prepare(`
    SELECT i.id,i.item_code,i.item_name,i.total_quantity,
           COALESCE(SUM(CASE
             WHEN b.pickup_date <= ? AND b.return_date >= ? THEN bi.booked_qty
             WHEN b.return_date < ? AND bi.given_qty > bi.returned_qty THEN bi.given_qty-bi.returned_qty
             ELSE 0 END),0) AS reserved_qty
      FROM items i
      LEFT JOIN booking_items bi ON bi.item_id=i.id
      LEFT JOIN bookings b ON b.id=bi.booking_id
       AND b.status NOT IN ('CANCELLED','RETURNED')
       AND (? IS NULL OR b.id <> ?)
     WHERE i.id IN (${placeholders})
       AND i.archived_at IS NULL
       AND i.is_active=1
     GROUP BY i.id
  `).bind(returnDate, pickupDate, pickupDate, excludeBookingId, excludeBookingId, ...itemIds).all();

  const availability = (rowsResult.results || []).map(row => {
    const totalQuantity = Number(row.total_quantity || 0);
    const reservedQuantity = Number(row.reserved_qty || 0);
    return {
      itemId: String(row.id || ""),
      itemCode: String(row.item_code || ""),
      itemName: String(row.item_name || ""),
      totalQuantity,
      reservedQuantity,
      availableQuantity: Math.max(0, totalQuantity - reservedQuantity)
    };
  });

  return apiJson({ ok: true, availability });
}

function fillTemplate(template, values) {
  return String(template || "").replace(/\{([a-z0-9_]+)\}/gi, (_, key) => String(values[key] ?? ""));
}

async function currentShopName(env) {
  const row = await env.DB.prepare(`SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1`).first();
  let settings = {};
  try { settings = JSON.parse(String(row?.value_json || "{}")); } catch {}
  return text(settings.shopNameGu || settings.shopName || env.APP_NAME || "ઝગમગ ડ્રેસીસ", 160);
}

async function composeWhatsApp(request, env, customerId) {
  const customer = await env.DB.prepare(`SELECT id,name,mobile FROM customers WHERE id=?`).bind(customerId).first();
  if (!customer) return apiJson({ ok: false, error: "NOT_FOUND", message: "Customer not found." }, 404);

  const url = new URL(request.url);
  const bookingId = text(url.searchParams.get("bookingId"), 100);
  let booking = null;
  if (bookingId) {
    booking = await env.DB.prepare(`
      SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.confirmation_state,
             COALESCE(SUM(CASE WHEN bi.booked_qty>bi.given_qty THEN bi.booked_qty-bi.given_qty ELSE 0 END),0) AS pending_pickup_qty,
             COALESCE(SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS pending_return_qty
      FROM bookings b
      LEFT JOIN booking_items bi ON bi.booking_id=b.id
      WHERE b.id=? AND b.customer_id=?
      GROUP BY b.id
    `).bind(bookingId, customerId).first();
    if (!booking) return apiJson({ ok: false, error: "BOOKING_NOT_FOUND", message: "Booking not found for this customer." }, 404);
  }

  const today = businessToday();
  let templateKey = "general_inquiry";
  let overdueDays = 0;
  if (booking) {
    const pendingPickup = Number(booking.pending_pickup_qty || 0);
    const pendingReturn = Number(booking.pending_return_qty || 0);
    if (pendingReturn > 0 && String(booking.return_date || "") < today) {
      templateKey = "overdue_reminder";
      overdueDays = Math.max(1, Math.floor((Date.parse(`${today}T00:00:00Z`) - Date.parse(`${booking.return_date}T00:00:00Z`)) / 86400000));
    } else if (pendingReturn > 0) {
      templateKey = "return_reminder";
    } else if (pendingPickup > 0 && String(booking.pickup_date || "") < today) {
      templateKey = "missed_pickup_reminder";
    } else if (pendingPickup > 0) {
      templateKey = "pickup_reminder";
    } else {
      templateKey = "booking_confirmation";
    }
  }

  const template = await env.DB.prepare(`
    SELECT template_key,message_text,message_gu,message_en
    FROM whatsapp_templates
    WHERE template_key=? AND is_active=1
    LIMIT 1
  `).bind(templateKey).first();
  if (!template) return apiJson({ ok: false, error: "TEMPLATE_MISSING", message: "WhatsApp template is not configured." }, 409);

  const values = {
    customer_name: customer.name || "",
    booking_no: booking?.booking_no || "",
    pickup_date: booking?.pickup_date || "",
    return_date: booking?.return_date || "",
    pending_qty: Number(booking?.pending_return_qty || booking?.pending_pickup_qty || 0),
    overdue_days: overdueDays,
    shop_name: await currentShopName(env)
  };

  const gu = fillTemplate(template.message_gu || template.message_text, values).trim();
  const en = fillTemplate(template.message_en || "", values).trim();
  const message = [gu, en].filter(Boolean).join("\n\n");
  return apiJson({ ok: true, mobile: customer.mobile, templateKey, message });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname === "/api/admin/customers" && request.method === "GET") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return listCustomers(request, env, user);
    }

    if (url.pathname === "/api/admin/customers" && request.method === "POST") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return createCustomer(request, env, user);
    }

    const customerMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)$/);
    if (customerMatch && request.method === "PUT") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return updateCustomer(request, env, user, decodeURIComponent(customerMatch[1]));
    }
    if (customerMatch && request.method === "DELETE") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return permanentDelete(env, user, decodeURIComponent(customerMatch[1]));
    }

    const actionMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/(archive|restore)$/);
    if (actionMatch && request.method === "POST") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      const id = decodeURIComponent(actionMatch[1]);
      return actionMatch[2] === "archive"
        ? archiveCustomer(env, user, id)
        : restoreCustomer(env, user, id);
    }

    const whatsappMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/whatsapp$/);
    if (whatsappMatch && request.method === "GET") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return composeWhatsApp(request, env, decodeURIComponent(whatsappMatch[1]));
    }

    if (url.pathname === "/api/admin/bookings/availability" && request.method === "POST") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return bookingAvailabilityForScreen4(request, env);
    }

    if (url.pathname === "/api/admin/bookings" && request.method === "GET") {
      const user = await requireUser(request, env);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return listVisibleBookings(request, env);
    }

    return core.fetch(request, env, ctx);
  }
};
