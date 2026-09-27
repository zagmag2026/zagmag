import core from "./phase14bg-settings-whatsapp.js";
import { getSessionUser, hasStaffPermission } from "./auth.ts";

const REPORT_TYPES = new Set([
  "OPERATIONS_OVERVIEW",
  "BOOKINGS",
  "UPCOMING_BOOKINGS",
  "CANCELLED_BOOKINGS",
  "PICKUPS",
  "MISSED_PICKUPS",
  "RETURNS",
  "OVERDUE",
  "AVAILABILITY",
  "CURRENTLY_OUT",
  "ITEM_UTILIZATION",
  "LOW_USE_ITEMS",
  "ITEM_HISTORY",
  "CATEGORY_STOCK",
  "CUSTOMER_HISTORY",
  "ACTIVE_RENTALS",
  "FREQUENT_CUSTOMERS",
  "NEW_RETURNING_CUSTOMERS",
  "CUSTOMER_EXCEPTIONS",
  "STAFF_ACTIVITY",
  "WHATSAPP_ACTIVITY",
  "AUDIT_REPORT",
  "BILLING_OVERVIEW",
  "BILLS_REPORT",
  "PENDING_BALANCE",
  "FULL_AMOUNT_RECEIVED",
  "EXCEPTIONS"
]);

const DATE_BASES = new Set([
  "BOOKING_DATE",
  "PICKUP_DATE",
  "RETURN_DATE",
  "ACTIVITY_DATE",
  "AVAILABILITY_DATE",
  "BILL_DATE",
  "DUE_DATE"
]);

const GROUPINGS = new Set(["NONE", "DATE", "CATEGORY", "ITEM", "CUSTOMER", "STAFF", "STATUS"]);
const SORTS = new Set(["NEWEST", "OLDEST", "NAME", "QUANTITY", "DUE_ASC", "DUE_DESC"]);
const MAX_MUTATION_BODY_BYTES = 256 * 1024;

async function mutationBodyTooLarge(request) {
  if (!["POST", "PUT", "PATCH"].includes(request.method)) return false;
  if (!new URL(request.url).pathname.startsWith("/api/")) return false;
  const declared = Number(request.headers.get("content-length") || "0");
  if (Number.isFinite(declared) && declared > MAX_MUTATION_BODY_BYTES) return true;
  const body = request.clone().body;
  if (!body) return false;
  const reader = body.getReader();
  let total = 0;
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) return false;
      total += value?.byteLength || 0;
      if (total > MAX_MUTATION_BODY_BYTES) {
        await reader.cancel();
        return true;
      }
    }
  } finally {
    reader.releaseLock();
  }
}

const STATUSES = new Set([
  "",
  "RESERVED",
  "BOOKED",
  "PART_PICKUP",
  "FULL_PICKUP",
  "PART_RETURN",
  "FULL_RETURN",
  "MISSED_PICKUP",
  "OVERDUE",
  "CANCELLED",
  "PREPARED",
  "FAILED",
  "CREATE",
  "UPDATE",
  "DELETE",
  "ARCHIVE",
  "RESTORE",
  "PENDING",
  "FULL_AMOUNT_RECEIVED",
  "DRAFT",
  "FINAL"
]);

const OWNER_ONLY_REPORT_TYPES = new Set([
  "AUDIT_REPORT",
  "BILLING_OVERVIEW",
  "BILLS_REPORT",
  "PENDING_BALANCE",
  "FULL_AMOUNT_RECEIVED"
]);

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

function text(value, max = 300) {
  return String(value ?? "").trim().slice(0, max);
}

function boolValue(value) {
  return value === true || value === 1 || value === "1" || String(value).toLowerCase() === "true";
}

function validDate(value) {
  const raw = String(value || "");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) return false;
  const date = new Date(raw + "T00:00:00Z");
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === raw;
}

function businessToday() {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(new Date());
  const byType = Object.fromEntries(parts.map(part => [part.type, part.value]));
  return byType.year + "-" + byType.month + "-" + byType.day;
}

function addIsoDays(value, days) {
  const date = new Date(value + "T00:00:00Z");
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function istDayStartUtc(value) {
  return new Date(value + "T00:00:00+05:30").toISOString().slice(0, 19).replace("T", " ");
}

function like(value) {
  return "%" + value + "%";
}

function safeCustomFilters(raw) {
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return {};
    const result = {};
    Object.entries(parsed).slice(0, 20).forEach(([key, value]) => {
      const safeKey = text(key, 100);
      const safeValue = text(value, 160);
      if (safeKey && safeValue) result[safeKey] = safeValue;
    });
    return result;
  } catch {
    return {};
  }
}

function parseFilters(request) {
  const url = new URL(request.url);
  const typeRaw = text(url.searchParams.get("type"), 60).toUpperCase();
  const dateBasisRaw = text(url.searchParams.get("dateBasis"), 40).toUpperCase();
  const groupingRaw = text(url.searchParams.get("grouping"), 40).toUpperCase();
  const sortRaw = text(url.searchParams.get("sort"), 40).toUpperCase();
  const statusRaw = normalizeStatus(text(url.searchParams.get("status"), 40).toUpperCase());
  const exportAll = boolValue(url.searchParams.get("export"));
  const requestedPageSize = Number(url.searchParams.get("pageSize") || "10");
  return {
    type: REPORT_TYPES.has(typeRaw) ? typeRaw : "OPERATIONS_OVERVIEW",
    dateBasis: DATE_BASES.has(dateBasisRaw) ? dateBasisRaw : "BOOKING_DATE",
    grouping: GROUPINGS.has(groupingRaw) ? groupingRaw : "NONE",
    sort: SORTS.has(sortRaw) ? sortRaw : "NEWEST",
    status: STATUSES.has(statusRaw) ? statusRaw : "",
    page: exportAll ? 1 : Math.max(1, Number(url.searchParams.get("page") || "1") || 1),
    pageSize: exportAll
      ? 10000
      : Math.min(50, Math.max(1, Number.isFinite(requestedPageSize) ? requestedPageSize : 10)),
    exportAll,
    fromDate: validDate(url.searchParams.get("fromDate")) ? url.searchParams.get("fromDate") : "",
    toDate: validDate(url.searchParams.get("toDate")) ? url.searchParams.get("toDate") : "",
    categoryId: text(url.searchParams.get("categoryId"), 100),
    itemSearch: text(url.searchParams.get("itemSearch"), 160),
    customerSearch: text(url.searchParams.get("customerSearch"), 160),
    staffUserId: text(url.searchParams.get("staffUserId"), 100),
    search: text(url.searchParams.get("search"), 160),
    datePreset: text(url.searchParams.get("datePreset"), 40).toUpperCase(),
    dashboardFilter: text(url.searchParams.get("dashboardFilter"), 40).toUpperCase(),
    customFilters: safeCustomFilters(url.searchParams.get("customFilters"))
  };
}

function normalizeStatus(value) {
  const legacy = {
    PARTIALLY_GIVEN: "PART_PICKUP",
    GIVEN: "FULL_PICKUP",
    PARTIALLY_RETURNED: "PART_RETURN",
    RETURNED: "FULL_RETURN"
  };
  return legacy[value] || value;
}

function paging(filters, total) {
  const pages = Math.max(1, Math.ceil(total / filters.pageSize));
  return { page: filters.page, pageSize: filters.pageSize, total, pages };
}

function summaryItem(label, value) {
  return { label, value: String(value ?? 0) };
}

function appliedFilters(filters) {
  return {
    type: filters.type,
    dateBasis: filters.dateBasis,
    datePreset: filters.datePreset,
    fromDate: filters.fromDate,
    toDate: filters.toDate,
    categoryId: filters.categoryId,
    itemSearch: filters.itemSearch,
    customerSearch: filters.customerSearch,
    status: filters.status,
    staffUserId: filters.staffUserId,
    grouping: filters.grouping,
    sort: filters.sort,
    search: filters.search,
    dashboardFilter: filters.dashboardFilter,
    customFilters: filters.customFilters
  };
}

function bookingBaseCte(today) {
  return [
    "WITH qty AS (",
    "  SELECT booking_id,",
    "         COALESCE(SUM(booked_qty),0) original_booked_qty,",
    "         COALESCE(SUM(booked_qty-closed_qty),0) active_booked_qty,",
    "         COALESCE(SUM(given_qty),0) given_qty,",
    "         COALESCE(SUM(returned_qty),0) returned_qty,",
    "         COALESCE(SUM(CASE WHEN booked_qty-closed_qty>given_qty THEN booked_qty-closed_qty-given_qty ELSE 0 END),0) pending_pickup_qty,",
    "         COALESCE(SUM(CASE WHEN given_qty>returned_qty THEN given_qty-returned_qty ELSE 0 END),0) pending_return_qty",
    "  FROM booking_items GROUP BY booking_id",
    "), booking_base AS (",
    "  SELECT b.id,b.customer_id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,",
    "         b.status AS raw_status,b.confirmation_state,b.created_by_user_id,b.created_at,b.updated_at,",
    "         cu.name AS customer_name,cu.mobile AS customer_mobile,u.name AS staff_name,",
    "         COALESCE(q.original_booked_qty,0) original_booked_qty,",
    "         COALESCE(q.active_booked_qty,0) active_booked_qty,",
    "         COALESCE(q.given_qty,0) given_qty,COALESCE(q.returned_qty,0) returned_qty,",
    "         COALESCE(q.pending_pickup_qty,0) pending_pickup_qty,COALESCE(q.pending_return_qty,0) pending_return_qty,",
    "         CASE",
    "           WHEN b.status='CANCELLED' THEN 'CANCELLED'",
    "           WHEN b.confirmation_state='RESERVED' AND COALESCE(q.given_qty,0)=0 THEN 'RESERVED'",
    "           WHEN COALESCE(q.pending_return_qty,0)>0 AND b.return_date<'" + today + "' THEN 'OVERDUE'",
    "           WHEN b.confirmation_state='BOOKED' AND COALESCE(q.pending_pickup_qty,0)>0 AND b.pickup_date<'" + today + "' THEN 'MISSED_PICKUP'",
    "           WHEN COALESCE(q.pending_return_qty,0)>0 AND COALESCE(q.returned_qty,0)>0 THEN 'PART_RETURN'",
    "           WHEN COALESCE(q.pending_pickup_qty,0)>0 AND COALESCE(q.given_qty,0)>0 THEN 'PART_PICKUP'",
    "           WHEN COALESCE(q.pending_pickup_qty,0)>0 THEN 'BOOKED'",
    "           WHEN COALESCE(q.pending_return_qty,0)>0 THEN 'FULL_PICKUP'",
    "           WHEN COALESCE(q.given_qty,0)>0 AND COALESCE(q.returned_qty,0)>=COALESCE(q.given_qty,0) THEN 'FULL_RETURN'",
    "           ELSE 'BOOKED'",
    "         END AS status",
    "  FROM bookings b",
    "  JOIN customers cu ON cu.id=b.customer_id",
    "  LEFT JOIN users u ON u.id=b.created_by_user_id",
    "  LEFT JOIN qty q ON q.booking_id=b.id",
    ")"
  ].join("\n");
}

function itemBaseCte(today) {
  return [
    "WITH item_base AS (",
    "  SELECT b.id AS booking_id,b.customer_id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,",
    "         b.status AS raw_status,b.confirmation_state,b.created_by_user_id,b.created_at,b.updated_at,",
    "         cu.name AS customer_name,cu.mobile AS customer_mobile,u.name AS staff_name,",
    "         (SELECT pu.name FROM pickup_event_items pei JOIN pickup_events pe ON pe.id=pei.pickup_event_id",
    "          LEFT JOIN users pu ON pu.id=pe.handled_by_user_id WHERE pei.booking_item_id=bi.id",
    "          ORDER BY pe.pickup_at DESC,pe.created_at DESC LIMIT 1) AS pickup_by,",
    "         (SELECT ru.name FROM return_event_items rei JOIN return_events re ON re.id=rei.return_event_id",
    "          LEFT JOIN users ru ON ru.id=re.received_by_user_id WHERE rei.booking_item_id=bi.id",
    "          ORDER BY re.return_at DESC,re.created_at DESC LIMIT 1) AS return_by,",
    "         bi.id AS booking_item_id,bi.item_id,i.item_code,i.item_name,i.category_id,c.name AS category_name,",
    "         bi.booked_qty,bi.closed_qty,(bi.booked_qty-bi.closed_qty) AS active_booked_qty,",
    "         bi.given_qty,bi.returned_qty,",
    "         CASE WHEN bi.booked_qty-bi.closed_qty>bi.given_qty THEN bi.booked_qty-bi.closed_qty-bi.given_qty ELSE 0 END AS pending_pickup_qty,",
    "         CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END AS pending_return_qty,",
    "         CASE",
    "           WHEN b.status='CANCELLED' THEN 'CANCELLED'",
    "           WHEN b.confirmation_state='RESERVED' AND bi.given_qty=0 THEN 'RESERVED'",
    "           WHEN bi.given_qty>bi.returned_qty AND b.return_date<'" + today + "' THEN 'OVERDUE'",
    "           WHEN b.confirmation_state='BOOKED' AND bi.booked_qty-bi.closed_qty>bi.given_qty AND b.pickup_date<'" + today + "' THEN 'MISSED_PICKUP'",
    "           WHEN bi.given_qty>bi.returned_qty AND bi.returned_qty>0 THEN 'PART_RETURN'",
    "           WHEN bi.booked_qty-bi.closed_qty>bi.given_qty AND bi.given_qty>0 THEN 'PART_PICKUP'",
    "           WHEN bi.booked_qty-bi.closed_qty>bi.given_qty THEN 'BOOKED'",
    "           WHEN bi.given_qty>bi.returned_qty THEN 'FULL_PICKUP'",
    "           WHEN bi.given_qty>0 AND bi.returned_qty>=bi.given_qty THEN 'FULL_RETURN'",
    "           ELSE 'BOOKED'",
    "         END AS status",
    "  FROM booking_items bi",
    "  JOIN bookings b ON b.id=bi.booking_id",
    "  JOIN customers cu ON cu.id=b.customer_id",
    "  JOIN items i ON i.id=bi.item_id",
    "  JOIN categories c ON c.id=i.category_id",
    "  LEFT JOIN users u ON u.id=b.created_by_user_id",
    ")"
  ].join("\n");
}

function addDateRange(filters, fieldSql, where, binds) {
  if (filters.fromDate) {
    where.push(fieldSql + ">=?");
    binds.push(filters.fromDate);
  }
  if (filters.toDate) {
    where.push(fieldSql + "<=?");
    binds.push(filters.toDate);
  }
}

function addCustomItemFilters(filters, itemIdSql, where, binds) {
  Object.entries(filters.customFilters || {}).forEach(([fieldId, rawValue]) => {
    const values = String(rawValue || "")
      .split(",")
      .map(item => text(item, 160))
      .filter(Boolean);
    values.forEach(value => {
      where.push(
        "EXISTS (SELECT 1 FROM item_field_values ifv " +
        "JOIN category_fields cf ON cf.id=ifv.category_field_id " +
        "WHERE ifv.item_id=" + itemIdSql + " AND ifv.category_field_id=? AND (" +
        "(cf.field_type='TEXT' AND LOWER(COALESCE(ifv.value_text,'')) LIKE LOWER(?)) OR " +
        "(cf.field_type='MULTI_SELECT' AND (',' || LOWER(COALESCE(ifv.value_text,'')) || ',') LIKE LOWER(?)) OR " +
        "(cf.field_type='NUMBER' AND CAST(COALESCE(ifv.value_text,'0') AS REAL)=CAST(? AS REAL)) OR " +
        "(cf.field_type IN ('DROPDOWN','YES_NO','DATE') AND LOWER(COALESCE(ifv.value_text,''))=LOWER(?))" +
        "))"
      );
      binds.push(fieldId, like(value), "%," + value + ",%", value, value);
    });
  });
}

function addItemFilters(filters, alias, where, binds) {
  if (filters.categoryId) {
    where.push(alias + ".category_id=?");
    binds.push(filters.categoryId);
  }
  if (filters.itemSearch) {
    where.push("(LOWER(" + alias + ".item_code) LIKE LOWER(?) OR LOWER(" + alias + ".item_name) LIKE LOWER(?))");
    binds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  addCustomItemFilters(filters, alias + ".item_id", where, binds);
}

function addItemBaseCommonFilters(filters, where, binds) {
  addItemFilters(filters, "ib", where, binds);
  if (filters.customerSearch) {
    where.push("(LOWER(ib.customer_name) LIKE LOWER(?) OR ib.customer_mobile LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.staffUserId) {
    where.push("ib.created_by_user_id=?");
    binds.push(filters.staffUserId);
  }
  if (filters.status) {
    where.push("ib.status=?");
    binds.push(filters.status);
  }
  if (filters.search) {
    const q = like(filters.search);
    where.push(
      "(LOWER(ib.booking_no) LIKE LOWER(?) OR LOWER(ib.customer_name) LIKE LOWER(?) OR ib.customer_mobile LIKE ? " +
      "OR LOWER(ib.item_code) LIKE LOWER(?) OR LOWER(ib.item_name) LIKE LOWER(?) OR LOWER(ib.category_name) LIKE LOWER(?))"
    );
    binds.push(q, q, q, q, q, q);
  }
}

function bookingItemExists(filters, bookingAlias, where, binds) {
  const conditions = [];
  const localBinds = [];
  if (filters.categoryId) {
    conditions.push("ix.category_id=?");
    localBinds.push(filters.categoryId);
  }
  if (filters.itemSearch) {
    conditions.push("(LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))");
    localBinds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  Object.entries(filters.customFilters || {}).forEach(([fieldId, rawValue]) => {
    const values = String(rawValue || "")
      .split(",")
      .map(item => text(item, 160))
      .filter(Boolean);
    values.forEach(value => {
      conditions.push(
        "EXISTS (SELECT 1 FROM item_field_values ifv " +
        "JOIN category_fields cf ON cf.id=ifv.category_field_id " +
        "WHERE ifv.item_id=ix.id AND ifv.category_field_id=? AND (" +
        "(cf.field_type='TEXT' AND LOWER(COALESCE(ifv.value_text,'')) LIKE LOWER(?)) OR " +
        "(cf.field_type='MULTI_SELECT' AND (',' || LOWER(COALESCE(ifv.value_text,'')) || ',') LIKE LOWER(?)) OR " +
        "(cf.field_type='NUMBER' AND CAST(COALESCE(ifv.value_text,'0') AS REAL)=CAST(? AS REAL)) OR " +
        "(cf.field_type IN ('DROPDOWN','YES_NO','DATE') AND LOWER(COALESCE(ifv.value_text,''))=LOWER(?))" +
        "))"
      );
      localBinds.push(fieldId, like(value), "%," + value + ",%", value, value);
    });
  });
  if (!conditions.length) return;
  where.push(
    "EXISTS (SELECT 1 FROM booking_items bix JOIN items ix ON ix.id=bix.item_id WHERE bix.booking_id=" +
    bookingAlias + ".id AND " + conditions.join(" AND ") + ")"
  );
  binds.push(...localBinds);
}

function addBookingBaseCommonFilters(filters, where, binds) {
  bookingItemExists(filters, "bb", where, binds);
  if (filters.customerSearch) {
    where.push("(LOWER(bb.customer_name) LIKE LOWER(?) OR bb.customer_mobile LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.staffUserId) {
    where.push("bb.created_by_user_id=?");
    binds.push(filters.staffUserId);
  }
  if (filters.status) {
    where.push("bb.status=?");
    binds.push(filters.status);
  }
  if (filters.search) {
    const q = like(filters.search);
    where.push("(LOWER(bb.booking_no) LIKE LOWER(?) OR LOWER(bb.customer_name) LIKE LOWER(?) OR bb.customer_mobile LIKE ?)");
    binds.push(q, q, q);
  }
}

function groupOrder(filters, alias, dateField) {
  switch (filters.grouping) {
    case "DATE": return dateField ? dateField + " ASC," : "";
    case "CATEGORY": return alias + ".category_name COLLATE NOCASE ASC,";
    case "ITEM": return alias + ".item_name COLLATE NOCASE ASC,";
    case "CUSTOMER": return alias + ".customer_name COLLATE NOCASE ASC,";
    case "STAFF": return alias + ".staff_name COLLATE NOCASE ASC,";
    case "STATUS": return alias + ".status ASC,";
    default: return "";
  }
}

function itemSortOrder(filters, alias, defaultField) {
  if (filters.sort === "OLDEST") return defaultField + " ASC," + alias + ".booking_no ASC";
  if (filters.sort === "NAME") return alias + ".item_name COLLATE NOCASE ASC," + defaultField + " DESC";
  if (filters.sort === "QUANTITY") return alias + ".active_booked_qty DESC," + defaultField + " DESC";
  if (filters.sort === "DUE_ASC") return alias + ".return_date ASC," + alias + ".pickup_date ASC";
  if (filters.sort === "DUE_DESC") return alias + ".return_date DESC," + alias + ".pickup_date DESC";
  return defaultField + " DESC," + alias + ".booking_no DESC";
}

function withWindowTotal(rowsSql) {
  const marker = "\nSELECT ";
  const index = rowsSql.lastIndexOf(marker);
  if (index >= 0) {
    return rowsSql.slice(0, index + 1) +
      "SELECT COUNT(*) OVER() AS __total," +
      rowsSql.slice(index + marker.length);
  }
  const leading = rowsSql.length - rowsSql.trimStart().length;
  if (rowsSql.slice(leading).startsWith("SELECT ")) {
    return rowsSql.slice(0, leading) +
      "SELECT COUNT(*) OVER() AS __total," +
      rowsSql.slice(leading + "SELECT ".length);
  }
  throw new Error("Unable to add report window total.");
}

async function executePaged(
  env,
  filters,
  countSql,
  countBinds,
  summarySql,
  summaryBinds,
  rowsSql,
  rowsBinds,
  title,
  columns,
  summaryMapper
) {
  const results = await env.DB.batch([
    env.DB.prepare(summarySql).bind(...summaryBinds),
    env.DB.prepare(withWindowTotal(rowsSql)).bind(...rowsBinds, filters.pageSize, (filters.page - 1) * filters.pageSize)
  ]);
  const rawRows = results[1]?.results || [];
  let total = Number(rawRows[0]?.__total || 0);
  if (!rawRows.length && filters.page > 1) {
    const fallback = await env.DB.prepare(countSql).bind(...countBinds).first();
    total = Number(fallback?.count || 0);
  }
  if (filters.exportAll && total > 10000) {
    return {
      error: apiJson({
        ok: false,
        error: "EXPORT_TOO_LARGE",
        message: "This report has more than 10,000 rows. Narrow the filters before creating the PDF."
      }, 422)
    };
  }
  const rows = rawRows.map(row => {
    const clean = { ...row };
    delete clean.__total;
    return clean;
  });
  return {
    data: {
      ok: true,
      type: filters.type,
      title,
      columns,
      rows,
      summary: summaryMapper(results[0]?.results?.[0] || {}),
      pagination: paging(filters, total),
      appliedFilters: appliedFilters(filters)
    }
  };
}

function itemReportMeta(type) {
  const meta = {
    BOOKINGS: {
      title: "Bookings",
      dateField: "ib.booking_date",
      extra: [],
      columns: [
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["item_name", "Item"],
        ["category_name", "Category"], ["status", "Status"], ["booking_date", "Booking Date"],
        ["pickup_date", "Pickup Date"], ["return_date", "Return Date"], ["active_booked_qty", "Booked Qty"],
        ["given_qty", "Picked Qty"], ["returned_qty", "Returned Qty"], ["staff_name", "Booked By"]
      ]
    },
    UPCOMING_BOOKINGS: {
      title: "Upcoming Bookings",
      dateField: "ib.pickup_date",
      extra: ["ib.raw_status<>'CANCELLED'", "ib.pending_pickup_qty>0", "ib.pickup_date>=__TODAY__"],
      columns: [
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["item_name", "Item"],
        ["category_name", "Category"], ["pickup_date", "Pickup Date"], ["pending_pickup_qty", "Pending Pickup"],
        ["status", "Status"], ["return_date", "Return Date"]
      ]
    },
    CANCELLED_BOOKINGS: {
      title: "Cancelled Bookings",
      dateField: "ib.booking_date",
      extra: ["ib.raw_status='CANCELLED'"],
      columns: [
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["item_name", "Item"],
        ["category_name", "Category"], ["booking_date", "Booking Date"], ["booked_qty", "Booked Qty"],
        ["status", "Status"], ["staff_name", "Booked By"]
      ]
    },
    PICKUPS: {
      title: "Pickup Report",
      dateField: "ib.pickup_date",
      extra: ["ib.raw_status<>'CANCELLED'", "ib.confirmation_state='BOOKED'", "(ib.given_qty>0 OR ib.pending_pickup_qty>0)"],
      columns: [
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["item_name", "Item"],
        ["category_name", "Category"], ["pickup_date", "Pickup Date"], ["given_qty", "Picked Qty"],
        ["pending_pickup_qty", "Pending Pickup"], ["status", "Status"], ["pickup_by", "Given By"]
      ]
    },
    MISSED_PICKUPS: {
      title: "Missed Pickup",
      dateField: "ib.pickup_date",
      extra: ["ib.raw_status<>'CANCELLED'", "ib.confirmation_state='BOOKED'", "ib.pending_pickup_qty>0", "ib.pickup_date<__TODAY__"],
      columns: [
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["customer_mobile", "Mobile"],
        ["item_name", "Item"], ["pending_pickup_qty", "Pending Pickup"], ["pickup_date", "Pickup Date"],
        ["days_late", "Days Missed"], ["status", "Status"]
      ]
    },
    RETURNS: {
      title: "Return Report",
      dateField: "ib.return_date",
      extra: ["ib.raw_status<>'CANCELLED'", "ib.given_qty>0"],
      columns: [
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["item_name", "Item"],
        ["category_name", "Category"], ["return_date", "Return Date"], ["returned_qty", "Returned Qty"],
        ["pending_return_qty", "Pending Return"], ["status", "Status"], ["return_by", "Received By"]
      ]
    },
    OVERDUE: {
      title: "Overdue Return",
      dateField: "ib.return_date",
      extra: ["ib.raw_status<>'CANCELLED'", "ib.pending_return_qty>0", "ib.return_date<__TODAY__"],
      columns: [
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["customer_mobile", "Mobile"],
        ["item_name", "Item"], ["pending_return_qty", "Pending Return"], ["return_date", "Return Date"],
        ["days_late", "Overdue Days"], ["status", "Status"]
      ]
    },
    CURRENTLY_OUT: {
      title: "Currently Out",
      dateField: "ib.return_date",
      extra: ["ib.raw_status<>'CANCELLED'", "ib.pending_return_qty>0"],
      columns: [
        ["customer_name", "Customer"], ["booking_no", "Booking No"], ["item_name", "Item"],
        ["category_name", "Category"], ["pending_return_qty", "Out Qty"], ["return_date", "Return Date"],
        ["status", "Status"]
      ]
    },
    ACTIVE_RENTALS: {
      title: "Active Rentals",
      dateField: "ib.return_date",
      extra: ["ib.raw_status<>'CANCELLED'", "ib.pending_return_qty>0"],
      columns: [
        ["customer_name", "Customer"], ["booking_no", "Booking No"], ["item_name", "Item"],
        ["pending_return_qty", "Active Qty"], ["return_date", "Due Return"], ["status", "Status"]
      ]
    },
    CUSTOMER_HISTORY: {
      title: "Customer Rental History",
      dateField: "ib.booking_date",
      extra: [],
      columns: [
        ["customer_name", "Customer"], ["customer_mobile", "Mobile"], ["booking_no", "Booking No"],
        ["item_name", "Item"], ["category_name", "Category"], ["booking_date", "Booking Date"],
        ["pickup_date", "Pickup Date"], ["return_date", "Return Date"], ["booked_qty", "Booked Qty"],
        ["given_qty", "Picked Qty"], ["returned_qty", "Returned Qty"], ["status", "Status"]
      ]
    },
    ITEM_HISTORY: {
      title: "Item Rental History",
      dateField: "ib.booking_date",
      extra: [],
      columns: [
        ["item_name", "Item"], ["item_code", "Item Code"], ["category_name", "Category"],
        ["booking_no", "Booking No"], ["customer_name", "Customer"], ["booking_date", "Booking Date"],
        ["pickup_date", "Pickup Date"], ["return_date", "Return Date"], ["booked_qty", "Booked Qty"],
        ["given_qty", "Picked Qty"], ["returned_qty", "Returned Qty"], ["status", "Status"]
      ]
    },
    EXCEPTIONS: {
      title: "Exception Report",
      dateField: "due_date",
      extra: ["ib.status IN ('MISSED_PICKUP','OVERDUE')"],
      columns: [
        ["exception_type", "Exception"], ["customer_name", "Customer"], ["customer_mobile", "Mobile"],
        ["booking_no", "Booking No"], ["item_name", "Item"], ["due_date", "Due Date"],
        ["pending_qty", "Pending Qty"], ["days_late", "Days Late"]
      ]
    }
  };
  return meta[type] || meta.BOOKINGS;
}

async function itemOperationalReport(env, filters) {
  const today = businessToday();
  const meta = itemReportMeta(filters.type);
  const where = ["1=1"];
  const binds = [];
  for (const raw of meta.extra) where.push(raw.replaceAll("__TODAY__", "'" + today + "'"));
  addItemBaseCommonFilters(filters, where, binds);

  let dateField = meta.dateField;
  if (filters.type === "EXCEPTIONS") {
    dateField = "(CASE WHEN ib.status='OVERDUE' THEN ib.return_date ELSE ib.pickup_date END)";
  } else if (filters.type === "BOOKINGS") {
    dateField = filters.dateBasis === "PICKUP_DATE"
      ? "ib.pickup_date"
      : filters.dateBasis === "RETURN_DATE"
        ? "ib.return_date"
        : "ib.booking_date";
  }
  if (!["CURRENTLY_OUT", "ACTIVE_RENTALS"].includes(filters.type)) {
    addDateRange(filters, dateField, where, binds);
  }

  const cte = itemBaseCte(today);
  const clause = where.join(" AND ");
  const summarySql = cte + "\nSELECT COUNT(DISTINCT ib.booking_id) AS bookings,COUNT(*) AS item_rows," +
    "COALESCE(SUM(ib.booked_qty),0) AS original_booked_qty,COALESCE(SUM(ib.active_booked_qty),0) AS booked_qty," +
    "COALESCE(SUM(ib.given_qty),0) AS picked_qty," +
    "COALESCE(SUM(ib.returned_qty),0) AS returned_qty,COALESCE(SUM(ib.pending_pickup_qty),0) AS pending_pickup_qty," +
    "COALESCE(SUM(ib.pending_return_qty),0) AS pending_return_qty FROM item_base ib WHERE " + clause;

  let rowSelect = "SELECT ib.*";
  if (["MISSED_PICKUPS", "OVERDUE"].includes(filters.type)) {
    const due = filters.type === "OVERDUE" ? "ib.return_date" : "ib.pickup_date";
    rowSelect += ",CAST(julianday('" + today + "')-julianday(" + due + ") AS INTEGER) AS days_late";
  } else if (filters.type === "EXCEPTIONS") {
    rowSelect += ",ib.status AS exception_type," +
      "CASE WHEN ib.status='OVERDUE' THEN ib.return_date ELSE ib.pickup_date END AS due_date," +
      "CASE WHEN ib.status='OVERDUE' THEN ib.pending_return_qty ELSE ib.pending_pickup_qty END AS pending_qty," +
      "CAST(julianday('" + today + "')-julianday(CASE WHEN ib.status='OVERDUE' THEN ib.return_date ELSE ib.pickup_date END) AS INTEGER) AS days_late";
  }

  const group = filters.type === "EXCEPTIONS" && filters.grouping === "DATE"
    ? "due_date ASC,"
    : groupOrder(filters, "ib", dateField);
  const sort = filters.type === "EXCEPTIONS"
    ? (filters.sort === "DUE_DESC" ? "due_date DESC" : "due_date ASC")
    : itemSortOrder(filters, "ib", dateField);

  const countSql = cte + "\nSELECT COUNT(*) AS count FROM item_base ib WHERE " + clause;
  const rowsSql = cte + "\n" + rowSelect + " FROM item_base ib WHERE " + clause +
    " ORDER BY " + group + sort + " LIMIT ? OFFSET ?";

  const summaryMapper = row => {
    const base = [
      summaryItem("Bookings", row.bookings),
      summaryItem("Items", row.item_rows)
    ];
    if (filters.type === "MISSED_PICKUPS") {
      return base.concat([summaryItem("Pending Pickup", row.pending_pickup_qty)]);
    }
    if (["OVERDUE", "CURRENTLY_OUT", "ACTIVE_RENTALS"].includes(filters.type)) {
      return base.concat([summaryItem("Pending Return", row.pending_return_qty)]);
    }
    if (filters.type === "PICKUPS") {
      return base.concat([
        summaryItem("Picked Qty", row.picked_qty),
        summaryItem("Pending Pickup", row.pending_pickup_qty)
      ]);
    }
    if (filters.type === "RETURNS") {
      return base.concat([
        summaryItem("Returned Qty", row.returned_qty),
        summaryItem("Pending Return", row.pending_return_qty)
      ]);
    }
    if (filters.type === "EXCEPTIONS") {
      return base.concat([
        summaryItem("Pending Pickup", row.pending_pickup_qty),
        summaryItem("Pending Return", row.pending_return_qty)
      ]);
    }
    if (["CANCELLED_BOOKINGS", "ITEM_HISTORY", "CUSTOMER_HISTORY"].includes(filters.type)) {
      return base.concat([
        summaryItem("Booked Qty", row.original_booked_qty),
        summaryItem("Picked Qty", row.picked_qty),
        summaryItem("Returned Qty", row.returned_qty)
      ]);
    }
    return base.concat([
      summaryItem("Booked Qty", row.booked_qty),
      summaryItem("Picked Qty", row.picked_qty),
      summaryItem("Returned Qty", row.returned_qty)
    ]);
  };

  return executePaged(
    env,
    filters,
    countSql,
    binds,
    summarySql,
    binds,
    rowsSql,
    binds,
    meta.title,
    meta.columns.map(([key, label]) => ({ key, label })),
    summaryMapper
  );
}

async function operationsOverview(env, filters) {
  const today = businessToday();
  const where = ["1=1"];
  const binds = [];
  addBookingBaseCommonFilters(filters, where, binds);
  const clause = where.join(" AND ");
  const cte = bookingBaseCte(today);
  const from = filters.fromDate || today;
  const to = filters.toDate || today;

  const queueCondition = [
    "bb.status IN ('MISSED_PICKUP','OVERDUE')",
    "(bb.confirmation_state='BOOKED' AND bb.pending_pickup_qty>0 AND bb.pickup_date BETWEEN ? AND ?)",
    "(bb.pending_return_qty>0 AND bb.return_date BETWEEN ? AND ?)"
  ].join(" OR ");
  const queueBinds = [from, to, from, to, ...binds];

  const summarySql = cte + "\nSELECT " +
    "SUM(CASE WHEN bb.booking_date BETWEEN ? AND ? THEN 1 ELSE 0 END) AS bookings," +
    "SUM(CASE WHEN bb.confirmation_state='BOOKED' AND bb.pending_pickup_qty>0 AND bb.pickup_date BETWEEN ? AND ? THEN 1 ELSE 0 END) AS pickup_pending," +
    "SUM(CASE WHEN bb.pending_return_qty>0 AND bb.return_date BETWEEN ? AND ? THEN 1 ELSE 0 END) AS return_pending," +
    "SUM(CASE WHEN bb.status='MISSED_PICKUP' THEN 1 ELSE 0 END) AS missed_pickup," +
    "SUM(CASE WHEN bb.status='OVERDUE' THEN 1 ELSE 0 END) AS overdue," +
    "SUM(CASE WHEN bb.status='FULL_RETURN' AND bb.return_date BETWEEN ? AND ? THEN 1 ELSE 0 END) AS completed " +
    "FROM booking_base bb WHERE " + clause;
  const summaryBinds = [from, to, from, to, from, to, from, to, ...binds];

  const countSql = cte + "\nSELECT COUNT(*) AS count FROM booking_base bb WHERE (" + queueCondition + ") AND " + clause;
  const rowsSql = cte + "\nSELECT bb.booking_no,bb.customer_name,bb.customer_mobile,bb.status," +
    "CASE WHEN bb.status='OVERDUE' THEN 'OVERDUE' WHEN bb.status='MISSED_PICKUP' THEN 'MISSED_PICKUP' " +
    "WHEN bb.pending_return_qty>0 THEN 'RETURN_PENDING' ELSE 'PICKUP_PENDING' END AS action," +
    "CASE WHEN bb.status='OVERDUE' OR bb.pending_return_qty>0 THEN bb.return_date ELSE bb.pickup_date END AS due_date," +
    "CASE WHEN bb.status='OVERDUE' OR bb.pending_return_qty>0 THEN bb.pending_return_qty ELSE bb.pending_pickup_qty END AS pending_qty," +
    "bb.staff_name FROM booking_base bb WHERE (" + queueCondition + ") AND " + clause +
    " ORDER BY CASE WHEN bb.status='OVERDUE' THEN 1 WHEN bb.status='MISSED_PICKUP' THEN 2 WHEN bb.pending_return_qty>0 THEN 3 ELSE 4 END," +
    "due_date ASC LIMIT ? OFFSET ?";

  return executePaged(
    env,
    filters,
    countSql,
    queueBinds,
    summarySql,
    summaryBinds,
    rowsSql,
    queueBinds,
    "Operations Overview",
    [
      { key: "action", label: "Action" },
      { key: "customer_name", label: "Customer" },
      { key: "booking_no", label: "Booking No" },
      { key: "due_date", label: "Due Date" },
      { key: "pending_qty", label: "Pending Qty" },
      { key: "status", label: "Status" }
    ],
    row => [
      summaryItem("Bookings", row.bookings),
      summaryItem("Pickup Pending", row.pickup_pending),
      summaryItem("Return Pending", row.return_pending),
      summaryItem("Missed Pickup", row.missed_pickup),
      summaryItem("Overdue Return", row.overdue),
      summaryItem("Completed", row.completed)
    ]
  );
}

async function availabilityReport(env, filters) {
  const today = businessToday();
  const from = filters.fromDate || today;
  const to = filters.toDate || from;
  const dashboardFilter = String(filters.dashboardFilter || "").toUpperCase();
  const dashboardMode = ["AVAILABLE_NOW", "PICKUP_PENDING", "LOW_STOCK", "UNAVAILABLE"].includes(dashboardFilter);
  const itemWhere = ["i.archived_at IS NULL", "i.is_active=1", "c.is_active=1"];
  const itemBinds = [];
  if (filters.categoryId) {
    itemWhere.push("i.category_id=?");
    itemBinds.push(filters.categoryId);
  }
  if (filters.itemSearch) {
    itemWhere.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))");
    itemBinds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  if (filters.search) {
    const q = like(filters.search);
    itemWhere.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?))");
    itemBinds.push(q, q, q);
  }
  addCustomItemFilters(filters, "i.id", itemWhere, itemBinds);

  const dashboardAvailableExpr = "MAX(0,a.total_quantity-a.dashboard_pickup_pending_qty-a.dashboard_out_qty)";
  const regularAvailableExpr = "MAX(0,a.total_quantity-a.committed_qty)";
  const availableExpr = dashboardMode ? dashboardAvailableExpr : regularAvailableExpr;
  const cte = [
    "WITH availability AS (",
    " SELECT i.id AS item_id,i.item_code,i.item_name,i.category_id,c.name AS category_name,i.total_quantity,",
    "        COALESCE(SUM(CASE",
    "          WHEN b.id IS NULL OR b.status='CANCELLED' THEN 0",
    "          WHEN b.status<>'RETURNED' AND b.pickup_date<=? AND b.return_date>=? THEN bi.booked_qty-bi.closed_qty",
    "          WHEN b.return_date<? AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty",
    "          ELSE 0 END),0) AS committed_qty,",
    "        COALESCE(SUM(CASE WHEN b.id IS NOT NULL AND b.status<>'CANCELLED'",
    "          AND (bi.booked_qty-COALESCE(bi.closed_qty,0))>bi.given_qty",
    "          THEN (bi.booked_qty-COALESCE(bi.closed_qty,0))-bi.given_qty ELSE 0 END),0) AS dashboard_pickup_pending_qty,",
    "        COALESCE(SUM(CASE WHEN b.id IS NOT NULL AND b.status<>'CANCELLED' AND bi.given_qty>bi.returned_qty",
    "          THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS dashboard_out_qty",
    " FROM items i JOIN categories c ON c.id=i.category_id",
    " LEFT JOIN booking_items bi ON bi.item_id=i.id",
    " LEFT JOIN bookings b ON b.id=bi.booking_id",
    " WHERE " + itemWhere.join(" AND "),
    " GROUP BY i.id",
    "), final AS (",
    " SELECT a.*," + availableExpr + " AS available_qty,",
    "        CASE WHEN " + availableExpr + "<=0 THEN 'FULLY_BOOKED'",
    "             WHEN " + availableExpr + "<a.total_quantity THEN 'PARTIAL'",
    "             ELSE 'AVAILABLE' END AS availability_status",
    " FROM availability a",
    ")"
  ].join("\n");
  const prefixBinds = [to, from, from, ...itemBinds];

  let dashboardClause = "";
  if (dashboardFilter === "AVAILABLE_NOW") dashboardClause = " WHERE f.available_qty>0";
  else if (dashboardFilter === "PICKUP_PENDING") dashboardClause = " WHERE f.dashboard_pickup_pending_qty>0";
  else if (dashboardFilter === "LOW_STOCK") dashboardClause = " WHERE f.available_qty BETWEEN 1 AND 5";
  else if (dashboardFilter === "UNAVAILABLE") dashboardClause = " WHERE f.available_qty<=0";

  const countSql = cte + "\nSELECT COUNT(*) AS count FROM final f" + dashboardClause;
  const summarySql = cte + "\nSELECT COUNT(*) AS items,COALESCE(SUM(total_quantity),0) total_qty," +
    "COALESCE(SUM(available_qty),0) available_qty,COALESCE(SUM(committed_qty),0) committed_qty," +
    "COALESCE(SUM(CASE WHEN availability_status='FULLY_BOOKED' THEN 1 ELSE 0 END),0) fully_booked FROM final f" + dashboardClause;
  let order = "f.item_name COLLATE NOCASE ASC";
  if (filters.sort === "QUANTITY") order = "f.available_qty DESC,f.item_name COLLATE NOCASE";
  if (filters.sort === "OLDEST") order = "f.item_code ASC";
  if (filters.sort === "NEWEST") order = "f.item_code DESC";

  const rowsSql = cte + "\nSELECT f.* FROM final f" + dashboardClause + " ORDER BY " + order + " LIMIT ? OFFSET ?";
  return executePaged(
    env, filters,
    countSql, prefixBinds,
    summarySql, prefixBinds,
    rowsSql, prefixBinds,
    "Availability",
    [
      { key: "item_name", label: "Item" },
      { key: "item_code", label: "Item Code" },
      { key: "category_name", label: "Category" },
      { key: "availability_status", label: "Status" },
      { key: "total_quantity", label: "Total Qty" },
      { key: "committed_qty", label: "Committed Qty" },
      { key: "available_qty", label: "Available Qty" }
    ],
    row => [
      summaryItem("Items", row.items),
      summaryItem("Total Qty", row.total_qty),
      summaryItem("Available Qty", row.available_qty),
      summaryItem("Committed Qty", row.committed_qty),
      summaryItem("Fully Booked", row.fully_booked)
    ]
  );
}

async function utilizationReport(env, filters, lowUse) {
  const itemWhere = ["i.archived_at IS NULL", "i.is_active=1", "c.is_active=1"];
  const itemBinds = [];
  if (filters.categoryId) {
    itemWhere.push("i.category_id=?");
    itemBinds.push(filters.categoryId);
  }
  if (filters.itemSearch) {
    itemWhere.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))");
    itemBinds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  if (filters.search) {
    const q = like(filters.search);
    itemWhere.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?))");
    itemBinds.push(q, q, q);
  }
  addCustomItemFilters(filters, "i.id", itemWhere, itemBinds);
  const from = filters.fromDate || "0000-01-01";
  const to = filters.toDate || "9999-12-31";

  const cte = [
    "WITH usage AS (",
    " SELECT i.id AS item_id,i.item_code,i.item_name,i.category_id,c.name AS category_name,i.total_quantity,",
    "        COUNT(DISTINCT CASE WHEN b.status<>'CANCELLED' AND b.booking_date BETWEEN ? AND ? THEN b.id END) AS rental_cycles,",
    "        COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND b.booking_date BETWEEN ? AND ? THEN bi.booked_qty-bi.closed_qty ELSE 0 END),0) AS booked_qty,",
    "        COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND b.booking_date BETWEEN ? AND ? THEN bi.given_qty ELSE 0 END),0) AS issued_qty,",
    "        COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS current_out_qty,",
    "        MAX(CASE WHEN b.status<>'CANCELLED' AND b.booking_date BETWEEN ? AND ? THEN b.booking_date ELSE NULL END) AS last_booking_date",
    " FROM items i JOIN categories c ON c.id=i.category_id",
    " LEFT JOIN booking_items bi ON bi.item_id=i.id",
    " LEFT JOIN bookings b ON b.id=bi.booking_id",
    " WHERE " + itemWhere.join(" AND "),
    " GROUP BY i.id",
    ")"
  ].join("\n");
  const prefixBinds = [from, to, from, to, from, to, from, to, ...itemBinds];
  const filterClause = lowUse ? " WHERE u.rental_cycles<=1" : "";
  const countSql = cte + "\nSELECT COUNT(*) AS count FROM usage u" + filterClause;
  const summarySql = cte + "\nSELECT COUNT(*) AS items,COALESCE(SUM(rental_cycles),0) rental_cycles," +
    "COALESCE(SUM(issued_qty),0) issued_qty,COALESCE(SUM(current_out_qty),0) current_out_qty FROM usage u" + filterClause;

  let order = lowUse ? "u.rental_cycles ASC,u.item_name COLLATE NOCASE" : "u.rental_cycles DESC,u.item_name COLLATE NOCASE";
  if (filters.sort === "NAME") order = "u.item_name COLLATE NOCASE ASC";
  if (filters.sort === "QUANTITY") order = "u.issued_qty DESC,u.item_name COLLATE NOCASE";
  if (filters.sort === "OLDEST") order = "COALESCE(u.last_booking_date,'') ASC,u.item_name COLLATE NOCASE";
  if (filters.sort === "NEWEST" && !lowUse) order = "COALESCE(u.last_booking_date,'') DESC,u.item_name COLLATE NOCASE";

  const rowsSql = cte + "\nSELECT u.*,MAX(0,u.total_quantity-u.current_out_qty) AS available_now FROM usage u" +
    filterClause + " ORDER BY " + order + " LIMIT ? OFFSET ?";

  return executePaged(
    env, filters,
    countSql, prefixBinds,
    summarySql, prefixBinds,
    rowsSql, prefixBinds,
    lowUse ? "Low-use / Idle Items" : "Item Utilization",
    [
      { key: "item_name", label: "Item" },
      { key: "item_code", label: "Item Code" },
      { key: "category_name", label: "Category" },
      { key: "rental_cycles", label: "Rental Cycles" },
      { key: "issued_qty", label: "Issued Qty" },
      { key: "current_out_qty", label: "Currently Out" },
      { key: "available_now", label: "Available Now" },
      { key: "last_booking_date", label: "Last Rental" }
    ],
    row => [
      summaryItem("Items", row.items),
      summaryItem("Rental Cycles", row.rental_cycles),
      summaryItem("Issued Qty", row.issued_qty),
      summaryItem("Currently Out", row.current_out_qty)
    ]
  );
}

async function categorySummaryReport(env, filters) {
  const today = businessToday();
  const where = ["c.is_active=1"];
  const binds = [];
  if (filters.categoryId) {
    where.push("c.id=?");
    binds.push(filters.categoryId);
  }
  if (filters.search) {
    where.push("LOWER(c.name) LIKE LOWER(?)");
    binds.push(like(filters.search));
  }

  const cte = [
    "WITH item_rollup AS (",
    " SELECT i.category_id,COUNT(*) AS total_items,COALESCE(SUM(i.total_quantity),0) AS total_quantity",
    " FROM items i WHERE i.archived_at IS NULL AND i.is_active=1 GROUP BY i.category_id",
    "), flow AS (",
    " SELECT i.category_id,",
    "   COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS currently_out,",
    "   COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND b.confirmation_state='BOOKED' AND bi.booked_qty-bi.closed_qty>bi.given_qty",
    "     THEN bi.booked_qty-bi.closed_qty-bi.given_qty ELSE 0 END),0) AS pending_pickup,",
    "   COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND b.pickup_date>='" + today + "' AND bi.booked_qty-bi.closed_qty>0",
    "     THEN bi.booked_qty-bi.closed_qty ELSE 0 END),0) AS upcoming_reserved",
    " FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN items i ON i.id=bi.item_id",
    " GROUP BY i.category_id",
    ")"
  ].join("\n");
  const common = " FROM categories c LEFT JOIN item_rollup ir ON ir.category_id=c.id LEFT JOIN flow f ON f.category_id=c.id WHERE " + where.join(" AND ");
  const countSql = cte + "\nSELECT COUNT(*) AS count" + common;
  const summarySql = cte + "\nSELECT COUNT(*) AS categories,COALESCE(SUM(ir.total_items),0) total_items," +
    "COALESCE(SUM(ir.total_quantity),0) total_qty,COALESCE(SUM(f.currently_out),0) currently_out," +
    "COALESCE(SUM(f.pending_pickup),0) pending_pickup" + common;
  let order = "c.display_order ASC,c.name COLLATE NOCASE";
  if (filters.sort === "NAME") order = "c.name COLLATE NOCASE ASC";
  if (filters.sort === "QUANTITY") order = "COALESCE(ir.total_quantity,0) DESC,c.name COLLATE NOCASE";
  const rowsSql = cte + "\nSELECT c.name AS category_name,COALESCE(ir.total_items,0) total_items," +
    "COALESCE(ir.total_quantity,0) total_qty,MAX(0,COALESCE(ir.total_quantity,0)-COALESCE(f.currently_out,0)) available_now," +
    "COALESCE(f.currently_out,0) currently_out,COALESCE(f.pending_pickup,0) pending_pickup," +
    "COALESCE(f.upcoming_reserved,0) upcoming_reserved" + common +
    " ORDER BY " + order + " LIMIT ? OFFSET ?";

  return executePaged(
    env, filters,
    countSql, binds,
    summarySql, binds,
    rowsSql, binds,
    "Category Summary",
    [
      { key: "category_name", label: "Category" },
      { key: "total_items", label: "Items" },
      { key: "total_qty", label: "Total Qty" },
      { key: "available_now", label: "Available Now" },
      { key: "currently_out", label: "Currently Out" },
      { key: "pending_pickup", label: "Pickup Pending" },
      { key: "upcoming_reserved", label: "Upcoming Reserved" }
    ],
    row => [
      summaryItem("Categories", row.categories),
      summaryItem("Items", row.total_items),
      summaryItem("Total Qty", row.total_qty),
      summaryItem("Currently Out", row.currently_out),
      summaryItem("Pickup Pending", row.pending_pickup)
    ]
  );
}

async function customerAggregateReport(env, filters) {
  const today = businessToday();
  const from = filters.fromDate || "0000-01-01";
  const to = filters.toDate || "9999-12-31";
  const baseWhere = ["bb.booking_date BETWEEN ? AND ?"];
  const baseBinds = [from, to];
  bookingItemExists(filters, "bb", baseWhere, baseBinds);
  if (filters.customerSearch) {
    baseWhere.push("(LOWER(bb.customer_name) LIKE LOWER(?) OR bb.customer_mobile LIKE ?)");
    baseBinds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.staffUserId) {
    baseWhere.push("bb.created_by_user_id=?");
    baseBinds.push(filters.staffUserId);
  }
  if (filters.search) {
    const q = like(filters.search);
    baseWhere.push("(LOWER(bb.customer_name) LIKE LOWER(?) OR bb.customer_mobile LIKE ?)");
    baseBinds.push(q, q);
  }

  const cte = bookingBaseCte(today) + "\n, filtered AS (" +
    " SELECT bb.* FROM booking_base bb WHERE " + baseWhere.join(" AND ") +
    "), customer_rollup AS (" +
    " SELECT customer_id,MAX(customer_name) AS customer_name,MAX(customer_mobile) AS customer_mobile," +
    " COUNT(*) AS total_bookings,COALESCE(SUM(active_booked_qty),0) AS booked_qty," +
    " COALESCE(SUM(given_qty),0) AS picked_qty,COALESCE(SUM(returned_qty),0) AS returned_qty," +
    " COALESCE(SUM(pending_return_qty),0) AS active_qty," +
    " SUM(CASE WHEN status='OVERDUE' THEN 1 ELSE 0 END) AS overdue_bookings," +
    " SUM(CASE WHEN status='MISSED_PICKUP' THEN 1 ELSE 0 END) AS missed_pickups," +
    " MAX(booking_date) AS last_booking_date" +
    " FROM filtered GROUP BY customer_id" +
    ")";

  let extra = "";
  if (filters.type === "FREQUENT_CUSTOMERS") extra = " WHERE cr.total_bookings>=2";
  if (filters.type === "NEW_RETURNING_CUSTOMERS" && filters.dashboardFilter === "NEW") {
    extra = " WHERE (SELECT MIN(b0.booking_date) FROM bookings b0 WHERE b0.customer_id=cr.customer_id AND b0.status<>'CANCELLED')>=?";
  } else if (filters.type === "NEW_RETURNING_CUSTOMERS" && filters.dashboardFilter === "RETURNING") {
    extra = " WHERE (SELECT MIN(b0.booking_date) FROM bookings b0 WHERE b0.customer_id=cr.customer_id AND b0.status<>'CANCELLED')<?";
  }
  const extraBinds = filters.type === "NEW_RETURNING_CUSTOMERS" && ["NEW", "RETURNING"].includes(filters.dashboardFilter)
    ? [...baseBinds, from]
    : baseBinds;
  const countSql = cte + "\nSELECT COUNT(*) AS count FROM customer_rollup cr" + extra;
  const summarySql = cte + "\nSELECT COUNT(*) AS customers,COALESCE(SUM(total_bookings),0) bookings," +
    "COALESCE(SUM(active_qty),0) active_qty,COALESCE(SUM(overdue_bookings),0) overdue_bookings FROM customer_rollup cr" + extra;

  let select = "SELECT cr.*";
  let title = "Customer Rental History";
  let columns = [
    { key: "customer_name", label: "Customer" },
    { key: "customer_mobile", label: "Mobile" },
    { key: "total_bookings", label: "Bookings" },
    { key: "booked_qty", label: "Booked Qty" },
    { key: "picked_qty", label: "Picked Qty" },
    { key: "returned_qty", label: "Returned Qty" },
    { key: "active_qty", label: "Active Qty" },
    { key: "last_booking_date", label: "Last Rental" }
  ];

  if (filters.type === "FREQUENT_CUSTOMERS") {
    title = "Frequent Customers";
    columns = [
      { key: "customer_name", label: "Customer" },
      { key: "customer_mobile", label: "Mobile" },
      { key: "total_bookings", label: "Bookings" },
      { key: "active_qty", label: "Active Qty" },
      { key: "last_booking_date", label: "Last Rental" }
    ];
  } else if (filters.type === "NEW_RETURNING_CUSTOMERS") {
    title = "New vs Returning Customers";
    select += ",CASE WHEN (SELECT MIN(b0.booking_date) FROM bookings b0 WHERE b0.customer_id=cr.customer_id AND b0.status<>'CANCELLED')>=?" +
      " THEN 'NEW' ELSE 'RETURNING' END AS customer_type";
    columns = [
      { key: "customer_name", label: "Customer" },
      { key: "customer_type", label: "Type" },
      { key: "total_bookings", label: "Bookings" },
      { key: "last_booking_date", label: "Last Rental" },
      { key: "active_qty", label: "Active Qty" }
    ];
  }

  let order = "cr.last_booking_date DESC,cr.customer_name COLLATE NOCASE";
  if (filters.type === "FREQUENT_CUSTOMERS") order = "cr.total_bookings DESC,cr.last_booking_date DESC";
  if (filters.sort === "NAME") order = "cr.customer_name COLLATE NOCASE ASC";
  if (filters.sort === "OLDEST") order = "cr.last_booking_date ASC,cr.customer_name COLLATE NOCASE";
  if (filters.sort === "QUANTITY") order = "cr.total_bookings DESC,cr.customer_name COLLATE NOCASE";

  const rowBinds = filters.type === "NEW_RETURNING_CUSTOMERS"
    ? [...extraBinds, from]
    : extraBinds;
  const rowsSql = cte + "\n" + select + " FROM customer_rollup cr" + extra +
    " ORDER BY " + order + " LIMIT ? OFFSET ?";

  return executePaged(
    env, filters,
    countSql, extraBinds,
    summarySql, extraBinds,
    rowsSql, rowBinds,
    title,
    columns,
    row => [
      summaryItem("Customers", row.customers),
      summaryItem("Bookings", row.bookings),
      summaryItem("Active Qty", row.active_qty),
      summaryItem("Overdue", row.overdue_bookings)
    ]
  );
}

async function customerExceptionsReport(env, filters) {
  const today = businessToday();
  const where = ["bb.status IN ('MISSED_PICKUP','OVERDUE')"];
  const binds = [];
  bookingItemExists(filters, "bb", where, binds);
  if (filters.customerSearch) {
    where.push("(LOWER(bb.customer_name) LIKE LOWER(?) OR bb.customer_mobile LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.search) {
    const q = like(filters.search);
    where.push("(LOWER(bb.customer_name) LIKE LOWER(?) OR bb.customer_mobile LIKE ?)");
    binds.push(q, q);
  }
  const cte = bookingBaseCte(today) + "\n, exception_rollup AS (" +
    " SELECT customer_id,MAX(customer_name) customer_name,MAX(customer_mobile) customer_mobile," +
    " SUM(CASE WHEN status='MISSED_PICKUP' THEN 1 ELSE 0 END) missed_pickups," +
    " SUM(CASE WHEN status='OVERDUE' THEN 1 ELSE 0 END) overdue_returns," +
    " SUM(CASE WHEN status='MISSED_PICKUP' THEN pending_pickup_qty ELSE 0 END) pending_pickup_qty," +
    " SUM(CASE WHEN status='OVERDUE' THEN pending_return_qty ELSE 0 END) pending_return_qty," +
    " CASE WHEN SUM(CASE WHEN status='OVERDUE' THEN 1 ELSE 0 END)>0 THEN 'OVERDUE' ELSE 'MISSED_PICKUP' END AS status" +
    " FROM booking_base bb WHERE " + where.join(" AND ") + " GROUP BY customer_id)";
  const countSql = cte + "\nSELECT COUNT(*) AS count FROM exception_rollup er";
  const summarySql = cte + "\nSELECT COUNT(*) customers,COALESCE(SUM(missed_pickups),0) missed_pickups," +
    "COALESCE(SUM(overdue_returns),0) overdue_returns,COALESCE(SUM(pending_pickup_qty),0) pending_pickup_qty," +
    "COALESCE(SUM(pending_return_qty),0) pending_return_qty FROM exception_rollup er";
  let order = "er.overdue_returns DESC,er.missed_pickups DESC,er.customer_name COLLATE NOCASE";
  if (filters.sort === "NAME") order = "er.customer_name COLLATE NOCASE ASC";
  const rowsSql = cte + "\nSELECT er.* FROM exception_rollup er ORDER BY " + order + " LIMIT ? OFFSET ?";

  return executePaged(
    env, filters,
    countSql, binds,
    summarySql, binds,
    rowsSql, binds,
    "Customer Exceptions",
    [
      { key: "customer_name", label: "Customer" },
      { key: "customer_mobile", label: "Mobile" },
      { key: "status", label: "Priority" },
      { key: "missed_pickups", label: "Missed Pickup" },
      { key: "overdue_returns", label: "Overdue Return" },
      { key: "pending_pickup_qty", label: "Pending Pickup Qty" },
      { key: "pending_return_qty", label: "Pending Return Qty" }
    ],
    row => [
      summaryItem("Customers", row.customers),
      summaryItem("Missed Pickup", row.missed_pickups),
      summaryItem("Overdue Return", row.overdue_returns),
      summaryItem("Pending Pickup Qty", row.pending_pickup_qty),
      summaryItem("Pending Return Qty", row.pending_return_qty)
    ]
  );
}

async function staffActivityReport(env, filters) {
  const userWhere = ["u.role IN ('OWNER','STAFF')"];
  const userBinds = [];
  if (filters.staffUserId) {
    userWhere.push("u.id=?");
    userBinds.push(filters.staffUserId);
  }
  if (filters.search) {
    userWhere.push("(LOWER(u.name) LIKE LOWER(?) OR u.mobile LIKE ?)");
    userBinds.push(like(filters.search), like(filters.search));
  }
  const from = filters.fromDate || "0000-01-01";
  const to = filters.toDate || "9999-12-31";
  const fromTimestamp = filters.fromDate ? istDayStartUtc(filters.fromDate) : "0000-01-01 00:00:00";
  const toTimestampExclusive = filters.toDate ? istDayStartUtc(addIsoDays(filters.toDate, 1)) : "9999-12-31 23:59:59";

  const activityExpression = "(COALESCE(ba.bookings,0)+COALESCE(pa.pickups,0)+COALESCE(ra.returns,0))";
  const cte = [
    "WITH booking_activity AS (",
    " SELECT b.created_by_user_id user_id,COUNT(*) bookings",
    " FROM bookings b WHERE b.booking_date BETWEEN ? AND ? GROUP BY b.created_by_user_id",
    "), pickup_activity AS (",
    " SELECT pe.handled_by_user_id user_id,COUNT(DISTINCT pe.id) pickups,COALESCE(SUM(pei.qty_given),0) pickup_qty",
    " FROM pickup_events pe JOIN pickup_event_items pei ON pei.pickup_event_id=pe.id",
    " WHERE pe.pickup_at>=? AND pe.pickup_at<? GROUP BY pe.handled_by_user_id",
    "), return_activity AS (",
    " SELECT re.received_by_user_id user_id,COUNT(DISTINCT re.id) returns,COALESCE(SUM(rei.qty_returned),0) return_qty",
    " FROM return_events re JOIN return_event_items rei ON rei.return_event_id=re.id",
    " WHERE re.return_at>=? AND re.return_at<? GROUP BY re.received_by_user_id",
    "), base AS (",
    " SELECT u.id staff_id,u.name staff_name,u.role,u.created_at,",
    "   COALESCE(ba.bookings,0) bookings,COALESCE(pa.pickups,0) pickups,COALESCE(pa.pickup_qty,0) pickup_qty,",
    "   COALESCE(ra.returns,0) returns,COALESCE(ra.return_qty,0) return_qty," + activityExpression + " activities",
    " FROM users u LEFT JOIN booking_activity ba ON ba.user_id=u.id",
    " LEFT JOIN pickup_activity pa ON pa.user_id=u.id LEFT JOIN return_activity ra ON ra.user_id=u.id",
    " WHERE " + userWhere.join(" AND "),
    "   AND ((u.is_active=1 AND u.archived_at IS NULL) OR " + activityExpression + ">0)",
    ")"
  ].join("\n");

  let order = "activities DESC,staff_name COLLATE NOCASE";
  if (filters.sort === "NAME") order = "staff_name COLLATE NOCASE ASC";
  if (filters.sort === "OLDEST") order = "created_at ASC";
  const sql = cte + "\nSELECT staff_id,staff_name,role,bookings,pickups,pickup_qty,returns,return_qty,activities," +
    "COUNT(*) OVER() AS __users,COALESCE(SUM(bookings) OVER(),0) AS __bookings," +
    "COALESCE(SUM(pickups) OVER(),0) AS __pickups,COALESCE(SUM(returns) OVER(),0) AS __returns," +
    "COALESCE(SUM(activities) OVER(),0) AS __activities FROM base ORDER BY " + order + " LIMIT ? OFFSET ?";
  const binds = [from,to,fromTimestamp,toTimestampExclusive,fromTimestamp,toTimestampExclusive,...userBinds,filters.pageSize,(filters.page-1)*filters.pageSize];
  const result = await env.DB.prepare(sql).bind(...binds).all();
  const rawRows = result.results || [];
  const first = rawRows[0] || {};
  const total = Number(first.__users || 0);
  if (filters.exportAll && total > 10000) {
    return { error: apiJson({ ok:false,error:"EXPORT_TOO_LARGE",message:"This report has more than 10,000 rows. Narrow the filters before creating the PDF." },422) };
  }
  const rows = rawRows.map(row => {
    const clean = { ...row };
    delete clean.__users; delete clean.__bookings; delete clean.__pickups; delete clean.__returns; delete clean.__activities; delete clean.created_at;
    return clean;
  });
  return {
    data: {
      ok:true,
      type:filters.type,
      title:"Staff Activity",
      columns:[
        { key:"staff_name",label:"Staff" },{ key:"role",label:"Role" },{ key:"bookings",label:"Bookings" },
        { key:"pickups",label:"Pickups" },{ key:"pickup_qty",label:"Pickup Qty" },{ key:"returns",label:"Returns" },
        { key:"return_qty",label:"Return Qty" },{ key:"activities",label:"Activities" }
      ],
      rows,
      summary:[
        summaryItem("Users",first.__users || 0),summaryItem("Bookings",first.__bookings || 0),
        summaryItem("Pickups",first.__pickups || 0),summaryItem("Returns",first.__returns || 0),
        summaryItem("Activities",first.__activities || 0)
      ],
      pagination:paging(filters,total),
      appliedFilters:appliedFilters(filters)
    }
  };
}

async function whatsappActivityReport(env, filters) {
  const where = ["1=1"];
  const binds = [];
  if (filters.fromDate) {
    where.push("w.created_at>=?");
    binds.push(istDayStartUtc(filters.fromDate));
  }
  if (filters.toDate) {
    where.push("w.created_at<?");
    binds.push(istDayStartUtc(addIsoDays(filters.toDate, 1)));
  }
  if (filters.customerSearch) {
    where.push("(LOWER(COALESCE(c.name,w.customer_name_snapshot,'')) LIKE LOWER(?) OR COALESCE(c.mobile,w.customer_mobile_snapshot,'') LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.staffUserId) {
    where.push("w.user_id=?");
    binds.push(filters.staffUserId);
  }
  if (filters.status === "PREPARED" || filters.status === "FAILED") {
    where.push("w.outcome=?");
    binds.push(filters.status);
  }
  if (filters.search) {
    const q = like(filters.search);
    where.push("(LOWER(COALESCE(c.name,w.customer_name_snapshot,'')) LIKE LOWER(?) OR LOWER(COALESCE(b.booking_no,w.booking_no_snapshot,'')) LIKE LOWER(?) " +
      "OR LOWER(COALESCE(bl.bill_no,w.bill_no_snapshot,'')) LIKE LOWER(?) OR LOWER(COALESCE(w.context,'')) LIKE LOWER(?) OR LOWER(COALESCE(w.error_message,'')) LIKE LOWER(?))");
    binds.push(q, q, q, q, q);
  }
  const clause = where.join(" AND ");
  const common = " FROM whatsapp_activity_logs w LEFT JOIN users u ON u.id=w.user_id" +
    " LEFT JOIN customers c ON c.id=w.customer_id LEFT JOIN bookings b ON b.id=w.booking_id" +
    " LEFT JOIN bills bl ON bl.id=w.bill_id";
  const countSql = "SELECT COUNT(*) AS count" + common + " WHERE " + clause;
  const summarySql = "SELECT COUNT(*) total,COALESCE(SUM(CASE WHEN w.outcome='PREPARED' THEN 1 ELSE 0 END),0) prepared," +
    "COALESCE(SUM(CASE WHEN w.outcome='FAILED' THEN 1 ELSE 0 END),0) failed" + common + " WHERE " + clause;
  let order = "w.created_at DESC";
  if (filters.sort === "OLDEST") order = "w.created_at ASC";
  if (filters.sort === "NAME") order = "COALESCE(c.name,w.customer_name_snapshot,'') COLLATE NOCASE ASC,w.created_at DESC";
  const rowsSql = "SELECT datetime(w.created_at,'+5 hours','+30 minutes') AS activity_at,u.name AS staff_name," +
    "COALESCE(c.name,w.customer_name_snapshot) AS customer_name,COALESCE(c.mobile,w.customer_mobile_snapshot) AS customer_mobile," +
    "COALESCE(b.booking_no,w.booking_no_snapshot) AS booking_no,COALESCE(bl.bill_no,w.bill_no_snapshot) AS bill_no,w.outcome,w.status_group,w.context,w.template_count,w.error_code,w.error_message" +
    common + " WHERE " + clause + " ORDER BY " + order + " LIMIT ? OFFSET ?";

  return executePaged(
    env, filters,
    countSql, binds,
    summarySql, binds,
    rowsSql, binds,
    "WhatsApp Activity",
    [
      { key: "activity_at", label: "Date / Time" },
      { key: "outcome", label: "Outcome" },
      { key: "customer_name", label: "Customer" },
      { key: "booking_no", label: "Booking No" },
      { key: "bill_no", label: "Bill No" },
      { key: "staff_name", label: "User" },
      { key: "status_group", label: "Status Group" },
      { key: "context", label: "Context" },
      { key: "template_count", label: "Templates" },
      { key: "error_message", label: "Failure Reason" }
    ],
    row => [
      summaryItem("Attempts", row.total),
      summaryItem("Prepared", row.prepared),
      summaryItem("Failed", row.failed)
    ]
  );
}

async function auditReport(env, filters) {
  const where = ["1=1"];
  const binds = [];
  if (filters.fromDate) {
    where.push("a.created_at>=?");
    binds.push(istDayStartUtc(filters.fromDate));
  }
  if (filters.toDate) {
    where.push("a.created_at<?");
    binds.push(istDayStartUtc(addIsoDays(filters.toDate, 1)));
  }
  if (filters.staffUserId) {
    where.push("a.user_id=?");
    binds.push(filters.staffUserId);
  }
  if (["CREATE", "UPDATE", "DELETE", "ARCHIVE", "RESTORE", "FINALIZE", "PAYMENT_UPDATE", "CANCEL"].includes(filters.status)) {
    where.push("UPPER(a.action)=?");
    binds.push(filters.status);
  }
  if (filters.search) {
    const q = like(filters.search);
    where.push("(LOWER(COALESCE(a.module,'')) LIKE LOWER(?) OR LOWER(COALESCE(a.action,'')) LIKE LOWER(?) " +
      "OR LOWER(COALESCE(a.record_id,'')) LIKE LOWER(?) OR LOWER(COALESCE(u.name,'')) LIKE LOWER(?))");
    binds.push(q, q, q, q);
  }
  const clause = where.join(" AND ");
  const common = " FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id";
  const countSql = "SELECT COUNT(*) AS count" + common + " WHERE " + clause;
  const summarySql = "SELECT COUNT(*) total," +
    "COALESCE(SUM(CASE WHEN UPPER(a.action)='CREATE' THEN 1 ELSE 0 END),0) created," +
    "COALESCE(SUM(CASE WHEN UPPER(a.action)='UPDATE' THEN 1 ELSE 0 END),0) updated," +
    "COALESCE(SUM(CASE WHEN UPPER(a.action)='DELETE' THEN 1 ELSE 0 END),0) deleted" +
    common + " WHERE " + clause;
  let order = "a.created_at DESC";
  if (filters.sort === "OLDEST") order = "a.created_at ASC";
  if (filters.sort === "NAME") order = "u.name COLLATE NOCASE ASC,a.created_at DESC";
  const rowsSql = "SELECT datetime(a.created_at,'+5 hours','+30 minutes') AS activity_at,u.name AS staff_name,a.module,a.action AS status,a.record_id" +
    common + " WHERE " + clause + " ORDER BY " + order + " LIMIT ? OFFSET ?";

  return executePaged(
    env, filters,
    countSql, binds,
    summarySql, binds,
    rowsSql, binds,
    "Audit Report",
    [
      { key: "activity_at", label: "Date / Time" },
      { key: "staff_name", label: "User" },
      { key: "module", label: "Module" },
      { key: "status", label: "Action" },
      { key: "record_id", label: "Record" }
    ],
    row => [
      summaryItem("Actions", row.total),
      summaryItem("Created", row.created),
      summaryItem("Updated", row.updated),
      summaryItem("Deleted", row.deleted)
    ]
  );
}

async function runReport(env, filters, user) {
  if (filters.type === "OPERATIONS_OVERVIEW") return operationsOverview(env, filters);
  if (filters.type === "AVAILABILITY") return availabilityReport(env, filters);
  if (filters.type === "ITEM_UTILIZATION") return utilizationReport(env, filters, false);
  if (filters.type === "LOW_USE_ITEMS") return utilizationReport(env, filters, true);
  if (filters.type === "CATEGORY_STOCK") return categorySummaryReport(env, filters);
  if (["FREQUENT_CUSTOMERS", "NEW_RETURNING_CUSTOMERS"].includes(filters.type)) {
    return customerAggregateReport(env, filters);
  }
  if (filters.type === "CUSTOMER_EXCEPTIONS") return customerExceptionsReport(env, filters);
  if (filters.type === "STAFF_ACTIVITY") return staffActivityReport(env, filters);
  if (filters.type === "WHATSAPP_ACTIVITY") return whatsappActivityReport(env, filters);
  if (filters.type === "AUDIT_REPORT") {
    if (user.role !== "OWNER") {
      return { error: apiJson({ ok: false, error: "FORBIDDEN", message: "Audit Report is Owner-only." }, 403) };
    }
    return auditReport(env, filters);
  }
  return itemOperationalReport(env, filters);
}

function safePresetConfig(value) {
  const raw = value && typeof value === "object" && !Array.isArray(value) ? value : {};
  const rawType = text(raw.type, 60).toUpperCase();
  const type = REPORT_TYPES.has(rawType) ? rawType : "OPERATIONS_OVERVIEW";
  const rawDateBasis = text(raw.dateBasis, 40).toUpperCase();
  const dateBasis = DATE_BASES.has(rawDateBasis) ? rawDateBasis : "BOOKING_DATE";
  const rawGrouping = text(raw.grouping, 40).toUpperCase();
  const grouping = GROUPINGS.has(rawGrouping) ? rawGrouping : "NONE";
  const rawSort = text(raw.sort, 40).toUpperCase();
  const sort = SORTS.has(rawSort) ? rawSort : "NEWEST";
  const status = normalizeStatus(text(raw.status, 40).toUpperCase());
  const customFilters = {};
  const rawCustom = raw.customFilters && typeof raw.customFilters === "object" && !Array.isArray(raw.customFilters)
    ? raw.customFilters
    : {};
  Object.entries(rawCustom).slice(0, 20).forEach(([key, item]) => {
    const safeKey = text(key, 100);
    const safeValue = text(item, 160);
    if (safeKey && safeValue) customFilters[safeKey] = safeValue;
  });
  return {
    type,
    dateBasis,
    datePreset: text(raw.datePreset, 40).toUpperCase(),
    fromDate: validDate(raw.fromDate) ? String(raw.fromDate) : "",
    toDate: validDate(raw.toDate) ? String(raw.toDate) : "",
    categoryId: text(raw.categoryId, 100),
    itemSearch: text(raw.itemSearch, 160),
    customerSearch: text(raw.customerSearch, 160),
    status: STATUSES.has(status) ? status : "",
    staffUserId: text(raw.staffUserId, 100),
    grouping,
    sort,
    search: text(raw.search, 160),
    dashboardFilter: "",
    customFilters
  };
}

async function readJson(request) {
  if (!(request.headers.get("content-type") || "").toLowerCase().includes("application/json")) return null;
  try {
    const value = await request.json();
    return value && typeof value === "object" && !Array.isArray(value) ? value : null;
  } catch {
    return null;
  }
}

function parseFieldOptions(value) {
  try {
    const parsed = JSON.parse(String(value || "[]"));
    return Array.isArray(parsed) ? parsed.map(item => text(item, 100)).filter(Boolean) : [];
  } catch {
    return [];
  }
}

async function bootstrap(env, user) {
  const results = await env.DB.batch([
    env.DB.prepare("SELECT id,name FROM categories WHERE is_active=1 ORDER BY display_order,name COLLATE NOCASE"),
    env.DB.prepare(
      "SELECT id,category_id,field_name,field_type,options_json FROM category_fields " +
      "WHERE is_active=1 ORDER BY category_id,display_order,field_name COLLATE NOCASE"
    ),
    env.DB.prepare(
      "SELECT id,name,role FROM users WHERE is_active=1 AND archived_at IS NULL " +
      "AND role IN ('OWNER','STAFF') ORDER BY name COLLATE NOCASE"
    ),
    env.DB.prepare(
      "SELECT rp.id,rp.owner_user_id,rp.name,rp.visibility,rp.config_json,rp.updated_at,u.name AS owner_name " +
      "FROM report_presets rp JOIN users u ON u.id=rp.owner_user_id " +
      "WHERE rp.owner_user_id=? OR rp.visibility='SHARED' " +
      "ORDER BY CASE WHEN rp.owner_user_id=? THEN 0 ELSE 1 END,rp.updated_at DESC,rp.name COLLATE NOCASE"
    ).bind(user.id, user.id)
  ]);

  const categoryRows = results[0]?.results || [];
  const fieldRows = results[1]?.results || [];
  const staffRows = results[2]?.results || [];
  const validCategoryIds = new Set(categoryRows.map(row => String(row.id)));
  const validStaffIds = new Set(staffRows.map(row => String(row.id)));
  const fieldCategoryById = new Map(fieldRows.map(row => [String(row.id), String(row.category_id)]));

  const presets = (results[3]?.results || []).flatMap(row => {
    let config = {};
    try { config = safePresetConfig(JSON.parse(String(row.config_json || "{}"))); } catch { config = safePresetConfig({}); }
    if (user.role !== "OWNER" && OWNER_ONLY_REPORT_TYPES.has(config.type)) return [];

    if (config.categoryId && !validCategoryIds.has(config.categoryId)) {
      config = { ...config, categoryId: "", customFilters: {} };
    } else if (config.categoryId) {
      config = {
        ...config,
        customFilters: Object.fromEntries(
          Object.entries(config.customFilters || {}).filter(([fieldId]) =>
            fieldCategoryById.get(String(fieldId)) === config.categoryId
          )
        )
      };
    } else if (Object.keys(config.customFilters || {}).length) {
      config = { ...config, customFilters: {} };
    }
    if (config.staffUserId && !validStaffIds.has(config.staffUserId)) {
      config = { ...config, staffUserId: "" };
    }

    return [{
      id: row.id,
      ownerUserId: row.owner_user_id,
      ownerName: row.owner_name,
      name: row.name,
      visibility: row.visibility,
      config,
      updatedAt: row.updated_at,
      canEdit: String(row.owner_user_id) === user.id || (user.role === "OWNER" && row.visibility === "SHARED")
    }];
  });

  return {
    ok: true,
    today: businessToday(),
    categories: categoryRows,
    categoryFields: fieldRows.map(row => ({
      id: row.id,
      categoryId: row.category_id,
      name: row.field_name,
      type: row.field_type,
      options: parseFieldOptions(row.options_json)
    })),
    staff: staffRows,
    statuses: [
      "RESERVED", "BOOKED", "PART_PICKUP", "FULL_PICKUP", "PART_RETURN",
      "FULL_RETURN", "MISSED_PICKUP", "OVERDUE", "CANCELLED",
      "PREPARED", "FAILED", "CREATE", "UPDATE", "DELETE", "ARCHIVE", "RESTORE", "FINALIZE", "PAYMENT_UPDATE", "CANCEL",
      "PENDING", "FULL_AMOUNT_RECEIVED", "DRAFT", "FINAL"
    ],
    presets
  };
}

async function savePreset(request, env, user, presetId = "") {
  const body = await readJson(request);
  if (!body) return apiJson({ ok: false, error: "VALIDATION", message: "A valid preset is required." }, 400);
  const name = text(body.name, 80);
  if (!name) return apiJson({ ok: false, error: "VALIDATION", message: "Preset name is required." }, 400);
  const config = safePresetConfig(body.config);
  if (OWNER_ONLY_REPORT_TYPES.has(config.type) && user.role !== "OWNER") {
    return apiJson({ ok: false, error: "FORBIDDEN", message: "This Report is Owner-only." }, 403);
  }
  let visibility = text(body.visibility, 20).toUpperCase() === "SHARED" ? "SHARED" : "MY";
  if (user.role !== "OWNER") visibility = "MY";

  if (!presetId) {
    const id = crypto.randomUUID();
    await env.DB.batch([
      env.DB.prepare("INSERT INTO report_presets(id,owner_user_id,name,visibility,config_json) VALUES(?,?,?,?,?)")
        .bind(id, user.id, name, visibility, JSON.stringify(config)),
      env.DB.prepare("INSERT INTO audit_logs(id,user_id,action,module,record_id,new_value_json) VALUES(?,?,?,?,?,?)")
        .bind(crypto.randomUUID(), user.id, "CREATE", "REPORT_PRESET", id, JSON.stringify({ name, visibility, config }))
    ]);
    return apiJson({ ok: true, id, message: "Report preset saved." });
  }

  const existing = await env.DB.prepare("SELECT * FROM report_presets WHERE id=? LIMIT 1").bind(presetId).first();
  if (!existing) return apiJson({ ok: false, error: "NOT_FOUND", message: "Report preset not found." }, 404);
  const canEdit = String(existing.owner_user_id) === user.id || (user.role === "OWNER" && existing.visibility === "SHARED");
  if (!canEdit) return apiJson({ ok: false, error: "FORBIDDEN", message: "You cannot edit this report preset." }, 403);

  let oldConfig = {};
  try { oldConfig = JSON.parse(String(existing.config_json || "{}")); } catch { oldConfig = {}; }
  const expectedUpdatedAt = text(body.expectedUpdatedAt, 80);
  const nextUpdatedAt = new Date().toISOString();
  const accessSql = "(owner_user_id=? OR (?='OWNER' AND visibility='SHARED'))";
  const result = await env.DB.batch([
    expectedUpdatedAt
      ? env.DB.prepare("UPDATE report_presets SET name=?,visibility=?,config_json=?,updated_at=? WHERE id=? AND updated_at=? AND " + accessSql)
          .bind(name,visibility,JSON.stringify(config),nextUpdatedAt,presetId,expectedUpdatedAt,user.id,user.role)
      : env.DB.prepare("UPDATE report_presets SET name=?,visibility=?,config_json=?,updated_at=? WHERE id=? AND " + accessSql)
          .bind(name,visibility,JSON.stringify(config),nextUpdatedAt,presetId,user.id,user.role),
    env.DB.prepare("INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) " +
      "SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM report_presets WHERE id=? AND updated_at=?)")
      .bind(
        crypto.randomUUID(), user.id, "UPDATE", "REPORT_PRESET", presetId,
        JSON.stringify({ name: existing.name, visibility: existing.visibility, config: oldConfig }),
        JSON.stringify({ name, visibility, config }), presetId, nextUpdatedAt
      )
  ]);
  if (Number(result?.[0]?.meta?.changes || 0) === 0) {
    return apiJson({ ok:false,error:"STALE_WRITE",message:"This saved preset changed or is no longer editable. Refresh before saving again." },409);
  }
  return apiJson({ ok: true, id: presetId, updatedAt:nextUpdatedAt, message: "Report preset updated." });
}

async function deletePreset(env, user, presetId) {
  const existing = await env.DB.prepare("SELECT * FROM report_presets WHERE id=? LIMIT 1").bind(presetId).first();
  if (!existing) return apiJson({ ok: false, error: "NOT_FOUND", message: "Report preset not found." }, 404);
  const canEdit = String(existing.owner_user_id) === user.id || (user.role === "OWNER" && existing.visibility === "SHARED");
  if (!canEdit) return apiJson({ ok: false, error: "FORBIDDEN", message: "You cannot delete this report preset." }, 403);
  const accessSql = "(owner_user_id=? OR (?='OWNER' AND visibility='SHARED'))";
  const result = await env.DB.batch([
    env.DB.prepare("INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json) " +
      "SELECT ?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM report_presets WHERE id=? AND " + accessSql + ")")
      .bind(crypto.randomUUID(),user.id,"DELETE","REPORT_PRESET",presetId,JSON.stringify(existing),presetId,user.id,user.role),
    env.DB.prepare("DELETE FROM report_presets WHERE id=? AND " + accessSql)
      .bind(presetId,user.id,user.role)
  ]);
  if (Number(result?.[1]?.meta?.changes || 0) === 0) {
    return apiJson({ ok:false,error:"STALE_WRITE",message:"This saved preset changed or is no longer editable." },409);
  }
  return apiJson({ ok: true, message: "Report preset deleted." });
}

async function requireReportsUser(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return { response: apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401) };
  if (!hasStaffPermission(user, "REPORTS")) {
    return { response: apiJson({ ok: false, error: "FORBIDDEN", message: "Reports are not enabled for your Staff account." }, 403) };
  }
  return { user };
}

async function recordWhatsAppActivity(request, env, user, response) {
  try {
    const url = new URL(request.url);
    const match = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/whatsapp$/);
    if (!match || !user) return;
    let data = {};
    try { data = await response.clone().json(); } catch { data = {}; }
    const ok = response.ok && data?.ok !== false;
    const templates = Array.isArray(data?.templates) ? data.templates : [];
    const templateCount = templates.length || (data?.message ? 1 : 0);
    const customerId = decodeURIComponent(match[1]);
    const bookingId = text(url.searchParams.get("bookingId"), 100) || null;
    const itemId = text(url.searchParams.get("itemId"), 100) || null;
    await env.DB.prepare(
      "INSERT INTO whatsapp_activity_logs(" +
      "id,user_id,customer_id,booking_id,item_id,context,status_group,outcome,template_count,error_code,error_message," +
      "customer_name_snapshot,customer_mobile_snapshot,booking_no_snapshot,item_name_snapshot" +
      ") VALUES(?,?,?,?,?,?,?,?,?,?,?," +
      "(SELECT name FROM customers WHERE id=?),(SELECT mobile FROM customers WHERE id=?)," +
      "(SELECT booking_no FROM bookings WHERE id=?),(SELECT item_name FROM items WHERE id=?))"
    ).bind(
      crypto.randomUUID(),
      user.id,
      customerId,
      bookingId,
      itemId,
      text(url.searchParams.get("context"), 80) || text(data?.linkedAction, 80) || text(url.searchParams.get("mode"), 30),
      text(data?.statusGroup, 80) || null,
      ok ? "PREPARED" : "FAILED",
      templateCount,
      ok ? null : text(data?.error, 80),
      ok ? null : text(data?.message, 500),
      customerId, customerId, bookingId, itemId
    ).run();
  } catch {
    // Reporting telemetry must never break the WhatsApp action itself.
  }
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (await mutationBodyTooLarge(request)) {
      return apiJson({ ok: false, error: "REQUEST_TOO_LARGE", message: "Request body is too large." }, 413);
    }
    const isReportGenerator =
      url.pathname === "/api/admin/report-generator" ||
      url.pathname === "/api/admin/report-generator/bootstrap";
    const presetMatch = url.pathname.match(/^\/api\/admin\/report-presets(?:\/([^/]+))?$/);

    if (isReportGenerator || presetMatch) {
      const auth = await requireReportsUser(request, env);
      if (auth.response) return auth.response;
      const user = auth.user;

      if (url.pathname === "/api/admin/report-generator/bootstrap" && request.method === "GET") {
        return apiJson(await bootstrap(env, user));
      }

      if (url.pathname === "/api/admin/report-generator" && request.method === "GET") {
        const result = await runReport(env, parseFilters(request), user);
        return result.error || apiJson(result.data);
      }

      if (presetMatch && !presetMatch[1] && request.method === "POST") {
        return savePreset(request, env, user);
      }
      if (presetMatch && presetMatch[1] && request.method === "PUT") {
        return savePreset(request, env, user, decodeURIComponent(presetMatch[1]));
      }
      if (presetMatch && presetMatch[1] && request.method === "DELETE") {
        return deletePreset(env, user, decodeURIComponent(presetMatch[1]));
      }
      return apiJson({ ok: false, error: "METHOD_NOT_ALLOWED", message: "Method not allowed." }, 405);
    }

    const whatsappMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/whatsapp$/);
    if (whatsappMatch && request.method === "GET") {
      const response = await core.fetch(request, env, ctx);
      const user = await getSessionUser(env, request);
      if (user && ctx?.waitUntil) {
        ctx.waitUntil(recordWhatsAppActivity(request, env, user, response));
      } else if (user) {
        await recordWhatsAppActivity(request, env, user, response);
      }
      return response;
    }

    return core.fetch(request, env, ctx);
  }
};
