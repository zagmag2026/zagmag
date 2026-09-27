import core from "./phase14bc-screen10-settings.js";
import { getSessionUser, hasStaffPermission } from "./auth.ts";

const REPORT_TYPES = new Set([
  "BOOKINGS",
  "PICKUPS",
  "RETURNS",
  "OVERDUE",
  "ITEM_HISTORY",
  "CUSTOMER_HISTORY",
  "CATEGORY_STOCK",
  "STAFF_ACTIVITY"
]);
const DATE_BASES = new Set(["BOOKING_DATE", "PICKUP_DATE", "RETURN_DATE", "ACTIVITY_DATE"]);
const GROUPINGS = new Set(["NONE", "DATE", "CATEGORY", "ITEM", "CUSTOMER", "STAFF", "STATUS"]);
const SORTS = new Set(["NEWEST", "OLDEST", "NAME", "QUANTITY"]);
const STATUSES = new Set([
  "",
  "RESERVED",
  "BOOKED",
  "PARTIALLY_GIVEN",
  "GIVEN",
  "PARTIALLY_RETURNED",
  "RETURNED",
  "CANCELLED"
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
  return /^\d{4}-\d{2}-\d{2}$/.test(String(value || ""));
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

function like(value) {
  return `%${value}%`;
}

function parseFilters(request) {
  const url = new URL(request.url);
  const typeRaw = text(url.searchParams.get("type"), 40).toUpperCase();
  const dateBasisRaw = text(url.searchParams.get("dateBasis"), 40).toUpperCase();
  const groupingRaw = text(url.searchParams.get("grouping"), 40).toUpperCase();
  const sortRaw = text(url.searchParams.get("sort"), 40).toUpperCase();
  const statusRaw = text(url.searchParams.get("status"), 40).toUpperCase();
  const exportAll = boolValue(url.searchParams.get("export"));
  const requestedPageSize = Number(url.searchParams.get("pageSize") || "10");
  return {
    type: REPORT_TYPES.has(typeRaw) ? typeRaw : "BOOKINGS",
    dateBasis: DATE_BASES.has(dateBasisRaw) ? dateBasisRaw : "BOOKING_DATE",
    grouping: GROUPINGS.has(groupingRaw) ? groupingRaw : "NONE",
    sort: SORTS.has(sortRaw) ? sortRaw : "NEWEST",
    status: STATUSES.has(statusRaw) ? statusRaw : "",
    page: exportAll ? 1 : Math.max(1, Number(url.searchParams.get("page") || "1") || 1),
    pageSize: exportAll ? 10000 : Math.min(50, Math.max(1, Number.isFinite(requestedPageSize) ? requestedPageSize : 10)),
    exportAll,
    fromDate: validDate(url.searchParams.get("fromDate")) ? url.searchParams.get("fromDate") : "",
    toDate: validDate(url.searchParams.get("toDate")) ? url.searchParams.get("toDate") : "",
    categoryId: text(url.searchParams.get("categoryId"), 100),
    itemSearch: text(url.searchParams.get("itemSearch"), 160),
    customerSearch: text(url.searchParams.get("customerSearch"), 160),
    staffUserId: text(url.searchParams.get("staffUserId"), 100),
    search: text(url.searchParams.get("search"), 160),
    datePreset: text(url.searchParams.get("datePreset"), 40).toUpperCase()
  };
}

function paging(filters, total) {
  const pages = Math.max(1, Math.ceil(total / filters.pageSize));
  return { page: filters.page, pageSize: filters.pageSize, total, pages };
}

function statusClause(filters, alias, where, binds) {
  if (!filters.status) return;
  if (filters.status === "RESERVED") {
    where.push(`${alias}.status='BOOKED' AND ${alias}.confirmation_state='RESERVED'`);
  } else if (filters.status === "BOOKED") {
    where.push(`${alias}.status='BOOKED' AND ${alias}.confirmation_state='BOOKED'`);
  } else {
    where.push(`${alias}.status=?`);
    binds.push(filters.status);
  }
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
    search: filters.search
  };
}

async function executePaged(env, filters, countStmt, summaryStmt, rowsStmt, title, columns, summaryMapper) {
  const results = await env.DB.batch([countStmt, summaryStmt, rowsStmt]);
  const total = Number(results[0]?.results?.[0]?.count || 0);
  if (filters.exportAll && total > 10000) {
    return {
      error: apiJson({
        ok: false,
        error: "EXPORT_TOO_LARGE",
        message: "This report has more than 10,000 rows. Narrow the filters before creating the PDF."
      }, 422)
    };
  }
  return {
    data: {
      ok: true,
      type: filters.type,
      title,
      columns,
      rows: results[2]?.results || [],
      summary: summaryMapper(results[1]?.results?.[0] || {}),
      pagination: paging(filters, total),
      appliedFilters: appliedFilters(filters)
    }
  };
}

function bookingDateColumn(filters) {
  if (filters.dateBasis === "PICKUP_DATE") return "b.pickup_date";
  if (filters.dateBasis === "RETURN_DATE") return "b.return_date";
  return "b.booking_date";
}

async function bookingsReport(env, filters) {
  const where = ["1=1"];
  const binds = [];
  const dateColumn = bookingDateColumn(filters);
  if (filters.fromDate) { where.push(`${dateColumn}>=?`); binds.push(filters.fromDate); }
  if (filters.toDate) { where.push(`${dateColumn}<=?`); binds.push(filters.toDate); }
  statusClause(filters, "b", where, binds);
  if (filters.customerSearch) {
    where.push("(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.categoryId) {
    where.push("EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND ix.category_id=?)");
    binds.push(filters.categoryId);
  }
  if (filters.itemSearch) {
    where.push("EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?)))");
    binds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  if (filters.staffUserId) {
    where.push("b.created_by_user_id=?");
    binds.push(filters.staffUserId);
  }
  if (filters.search) {
    const q = like(filters.search);
    where.push("(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)");
    binds.push(q, q, q);
  }
  const clause = where.join(" AND ");
  const qtyCte = `WITH qty AS (
    SELECT booking_id,COALESCE(SUM(booked_qty),0) booked_qty,COALESCE(SUM(given_qty),0) given_qty,
           COALESCE(SUM(returned_qty),0) returned_qty
    FROM booking_items GROUP BY booking_id
  )`;
  const groupOrder = filters.grouping === "DATE" ? `${dateColumn} ASC,`
    : filters.grouping === "CUSTOMER" ? "cu.name COLLATE NOCASE ASC,"
    : filters.grouping === "STAFF" ? "u.name COLLATE NOCASE ASC,"
    : filters.grouping === "STATUS" ? "b.status ASC,"
    : "";
  const sortOrder = filters.sort === "OLDEST" ? "b.booking_date ASC,b.created_at ASC"
    : filters.sort === "NAME" ? "cu.name COLLATE NOCASE ASC,b.booking_date DESC"
    : filters.sort === "QUANTITY" ? "COALESCE(q.booked_qty,0) DESC,b.booking_date DESC"
    : "b.booking_date DESC,b.created_at DESC";

  const countStmt = env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings b JOIN customers cu ON cu.id=b.customer_id WHERE ${clause}`).bind(...binds);
  const summaryStmt = env.DB.prepare(`${qtyCte}
    SELECT COUNT(*) total_bookings,COALESCE(SUM(q.booked_qty),0) total_qty,
           COALESCE(SUM(q.given_qty),0) picked_qty,COALESCE(SUM(q.returned_qty),0) returned_qty
    FROM bookings b JOIN customers cu ON cu.id=b.customer_id
    LEFT JOIN qty q ON q.booking_id=b.id WHERE ${clause}`).bind(...binds);
  const rowsStmt = env.DB.prepare(`${qtyCte}
    SELECT b.id,b.customer_id AS customer_id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,
           CASE WHEN b.status='BOOKED' AND b.confirmation_state='RESERVED' THEN 'RESERVED' ELSE b.status END AS status,
           cu.name AS customer_name,cu.mobile AS customer_mobile,u.name AS staff_name,
           COALESCE(q.booked_qty,0) AS booked_qty,COALESCE(q.given_qty,0) AS given_qty,
           COALESCE(q.returned_qty,0) AS returned_qty,
           COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ')
                     FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items
    FROM bookings b JOIN customers cu ON cu.id=b.customer_id
    LEFT JOIN users u ON u.id=b.created_by_user_id LEFT JOIN qty q ON q.booking_id=b.id
    WHERE ${clause}
    ORDER BY ${groupOrder}${sortOrder}
    LIMIT ? OFFSET ?`).bind(...binds, filters.pageSize, (filters.page - 1) * filters.pageSize);

  return executePaged(
    env, filters, countStmt, summaryStmt, rowsStmt, "Bookings",
    [
      { key: "booking_no", label: "Booking No" },
      { key: "customer_name", label: "Customer" },
      { key: "status", label: "Status" },
      { key: "booking_date", label: "Booking Date" },
      { key: "pickup_date", label: "Pickup Date" },
      { key: "return_date", label: "Return Date" },
      { key: "booked_qty", label: "Qty" },
      { key: "staff_name", label: "Booked By" }
    ],
    row => [
      summaryItem("Total Bookings", row.total_bookings),
      summaryItem("Total Qty", row.total_qty),
      summaryItem("Picked Qty", row.picked_qty),
      summaryItem("Returned Qty", row.returned_qty)
    ]
  );
}

function activityWhere(filters, kind) {
  const isPickup = kind === "PICKUPS";
  const eventAlias = isPickup ? "pe" : "re";
  const itemAlias = isPickup ? "pei" : "rei";
  const dateField = isPickup ? "pe.pickup_at" : "re.return_at";
  const staffField = isPickup ? "pe.handled_by_user_id" : "re.received_by_user_id";
  const qtyField = isPickup ? "pei.qty_given" : "rei.qty_returned";
  const where = ["1=1"];
  const binds = [];
  if (filters.fromDate) { where.push(`substr(${dateField},1,10)>=?`); binds.push(filters.fromDate); }
  if (filters.toDate) { where.push(`substr(${dateField},1,10)<=?`); binds.push(filters.toDate); }
  statusClause(filters, "b", where, binds);
  if (filters.categoryId) { where.push("i.category_id=?"); binds.push(filters.categoryId); }
  if (filters.itemSearch) {
    where.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))");
    binds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  if (filters.customerSearch) {
    where.push("(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.staffUserId) { where.push(`${staffField}=?`); binds.push(filters.staffUserId); }
  if (filters.search) {
    const q = like(filters.search);
    where.push("(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ? OR LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?) OR LOWER(u.name) LIKE LOWER(?))");
    binds.push(q, q, q, q, q, q);
  }
  return { isPickup, eventAlias, itemAlias, dateField, staffField, qtyField, clause: where.join(" AND "), binds };
}

async function activityReport(env, filters, kind) {
  const f = activityWhere(filters, kind);
  const eventTable = f.isPickup ? "pickup_events" : "return_events";
  const eventItemTable = f.isPickup ? "pickup_event_items" : "return_event_items";
  const eventFk = f.isPickup ? "pickup_event_id" : "return_event_id";
  const groupOrder = filters.grouping === "DATE" ? `${f.dateField} ASC,`
    : filters.grouping === "CATEGORY" ? "c.name COLLATE NOCASE ASC,"
    : filters.grouping === "ITEM" ? "i.item_name COLLATE NOCASE ASC,"
    : filters.grouping === "CUSTOMER" ? "cu.name COLLATE NOCASE ASC,"
    : filters.grouping === "STAFF" ? "u.name COLLATE NOCASE ASC,"
    : filters.grouping === "STATUS" ? "b.status ASC,"
    : "";
  const sortOrder = filters.sort === "OLDEST" ? `${f.dateField} ASC`
    : filters.sort === "NAME" ? "cu.name COLLATE NOCASE ASC"
    : filters.sort === "QUANTITY" ? `${f.qtyField} DESC`
    : `${f.dateField} DESC`;

  const commonFrom = `FROM ${eventTable} ${f.eventAlias}
    JOIN ${eventItemTable} ${f.itemAlias} ON ${f.itemAlias}.${eventFk}=${f.eventAlias}.id
    JOIN booking_items bi ON bi.id=${f.itemAlias}.booking_item_id
    JOIN bookings b ON b.id=${f.eventAlias}.booking_id
    JOIN customers cu ON cu.id=b.customer_id
    JOIN items i ON i.id=bi.item_id
    JOIN categories c ON c.id=i.category_id
    LEFT JOIN users u ON u.id=${f.staffField}`;

  const countStmt = env.DB.prepare(`SELECT COUNT(*) AS count ${commonFrom} WHERE ${f.clause}`).bind(...f.binds);
  const summaryStmt = env.DB.prepare(`SELECT COUNT(DISTINCT ${f.eventAlias}.id) total_events,
      COUNT(DISTINCT b.id) total_bookings,COALESCE(SUM(${f.qtyField}),0) total_qty
      ${commonFrom} WHERE ${f.clause}`).bind(...f.binds);
  const rowsStmt = env.DB.prepare(`SELECT ${f.eventAlias}.id AS event_id,b.id AS booking_id,b.customer_id AS customer_id,b.booking_no,
      cu.name AS customer_name,cu.mobile AS customer_mobile,i.item_code,i.item_name,c.name AS category_name,
      ${f.qtyField} AS quantity,${f.dateField} AS activity_at,u.name AS staff_name,
      CASE WHEN b.status='BOOKED' AND b.confirmation_state='RESERVED' THEN 'RESERVED' ELSE b.status END AS status,
      CASE WHEN ${f.eventAlias}.correction_of_event_id IS NULL THEN 0 ELSE 1 END AS is_correction
      ${commonFrom} WHERE ${f.clause}
      ORDER BY ${groupOrder}${sortOrder}
      LIMIT ? OFFSET ?`).bind(...f.binds, filters.pageSize, (filters.page - 1) * filters.pageSize);

  const title = f.isPickup ? "Pickups" : "Returns";
  return executePaged(
    env, filters, countStmt, summaryStmt, rowsStmt, title,
    [
      { key: "booking_no", label: "Booking No" },
      { key: "customer_name", label: "Customer" },
      { key: "item_name", label: "Item" },
      { key: "category_name", label: "Category" },
      { key: "quantity", label: f.isPickup ? "Picked Qty" : "Returned Qty" },
      { key: "activity_at", label: f.isPickup ? "Pickup Time" : "Return Time" },
      { key: "staff_name", label: f.isPickup ? "Given By" : "Received By" }
    ],
    row => [
      summaryItem(f.isPickup ? "Total Pickups" : "Total Returns", row.total_events),
      summaryItem("Bookings", row.total_bookings),
      summaryItem(f.isPickup ? "Picked Qty" : "Returned Qty", row.total_qty)
    ]
  );
}

async function overdueReport(env, filters) {
  const today = businessToday();
  const where = ["b.status<>'CANCELLED'", "bi.given_qty>bi.returned_qty", "b.return_date<?"];
  const binds = [today];
  if (filters.fromDate) { where.push("b.return_date>=?"); binds.push(filters.fromDate); }
  if (filters.toDate) { where.push("b.return_date<=?"); binds.push(filters.toDate); }
  statusClause(filters, "b", where, binds);
  if (filters.categoryId) { where.push("i.category_id=?"); binds.push(filters.categoryId); }
  if (filters.itemSearch) {
    where.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))");
    binds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  if (filters.customerSearch) {
    where.push("(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.staffUserId) { where.push("b.created_by_user_id=?"); binds.push(filters.staffUserId); }
  if (filters.search) {
    const q = like(filters.search);
    where.push("(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ? OR LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))");
    binds.push(q, q, q, q, q);
  }
  const clause = where.join(" AND ");
  const groupOrder = filters.grouping === "DATE" ? "b.return_date ASC,"
    : filters.grouping === "CATEGORY" ? "c.name COLLATE NOCASE ASC,"
    : filters.grouping === "ITEM" ? "i.item_name COLLATE NOCASE ASC,"
    : filters.grouping === "CUSTOMER" ? "cu.name COLLATE NOCASE ASC,"
    : filters.grouping === "STAFF" ? "u.name COLLATE NOCASE ASC,"
    : filters.grouping === "STATUS" ? "b.status ASC,"
    : "";
  const sortOrder = filters.sort === "OLDEST" ? "b.return_date DESC"
    : filters.sort === "NAME" ? "cu.name COLLATE NOCASE ASC"
    : filters.sort === "QUANTITY" ? "(bi.given_qty-bi.returned_qty) DESC"
    : "b.return_date ASC";

  const commonFrom = `FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id
    JOIN customers cu ON cu.id=b.customer_id JOIN items i ON i.id=bi.item_id
    JOIN categories c ON c.id=i.category_id LEFT JOIN users u ON u.id=b.created_by_user_id`;
  const countStmt = env.DB.prepare(`SELECT COUNT(*) AS count ${commonFrom} WHERE ${clause}`).bind(...binds);
  const summaryStmt = env.DB.prepare(`SELECT COUNT(DISTINCT b.id) total_bookings,
      COALESCE(SUM(bi.given_qty-bi.returned_qty),0) pending_qty,
      COALESCE(MAX(CAST(julianday(?) - julianday(b.return_date) AS INTEGER)),0) max_overdue_days
      ${commonFrom} WHERE ${clause}`).bind(today, ...binds);
  const rowsStmt = env.DB.prepare(`SELECT b.id AS booking_id,b.customer_id AS customer_id,b.booking_no,cu.name AS customer_name,cu.mobile AS customer_mobile,
      i.item_code,i.item_name,c.name AS category_name,(bi.given_qty-bi.returned_qty) AS pending_qty,
      b.return_date,CAST(julianday(?) - julianday(b.return_date) AS INTEGER) AS overdue_days,
      u.name AS staff_name,b.status
      ${commonFrom} WHERE ${clause}
      ORDER BY ${groupOrder}${sortOrder}
      LIMIT ? OFFSET ?`).bind(today, ...binds, filters.pageSize, (filters.page - 1) * filters.pageSize);
  return executePaged(
    env, filters, countStmt, summaryStmt, rowsStmt, "Overdue Returns",
    [
      { key: "booking_no", label: "Booking No" },
      { key: "customer_name", label: "Customer" },
      { key: "customer_mobile", label: "Mobile" },
      { key: "item_name", label: "Item" },
      { key: "pending_qty", label: "Pending Qty" },
      { key: "return_date", label: "Return Date" },
      { key: "overdue_days", label: "Overdue Days" }
    ],
    row => [
      summaryItem("Overdue Bookings", row.total_bookings),
      summaryItem("Pending Qty", row.pending_qty),
      summaryItem("Max Overdue Days", row.max_overdue_days)
    ]
  );
}

function historyInner(filters) {
  const where = ["1=1"];
  const binds = [];
  if (filters.fromDate) { where.push("b.booking_date>=?"); binds.push(filters.fromDate); }
  if (filters.toDate) { where.push("b.booking_date<=?"); binds.push(filters.toDate); }
  statusClause(filters, "b", where, binds);
  if (filters.customerSearch) {
    where.push("(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)");
    binds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.staffUserId) { where.push("b.created_by_user_id=?"); binds.push(filters.staffUserId); }
  return { clause: where.join(" AND "), binds };
}

async function itemHistoryReport(env, filters) {
  const outer = ["i.archived_at IS NULL"];
  const outerBinds = [];
  if (filters.categoryId) { outer.push("i.category_id=?"); outerBinds.push(filters.categoryId); }
  if (filters.itemSearch) {
    outer.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))");
    outerBinds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  if (filters.search) {
    const q = like(filters.search);
    outer.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?))");
    outerBinds.push(q, q, q);
  }
  const inner = historyInner(filters);
  const innerWhere = inner.clause;
  const outerWhere = outer.join(" AND ");
  const requireMatch = Boolean(filters.fromDate || filters.toDate || filters.status || filters.customerSearch || filters.staffUserId);
  const cte = `WITH agg AS (
    SELECT bi.item_id,COUNT(DISTINCT b.id) total_bookings,
           COALESCE(SUM(bi.booked_qty),0) booked_qty,COALESCE(SUM(bi.given_qty),0) given_qty,
           COALESCE(SUM(bi.returned_qty),0) returned_qty,
           COALESCE(SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) pending_qty,
           MAX(b.booking_date) last_booking_date
    FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN customers cu ON cu.id=b.customer_id
    WHERE ${innerWhere} GROUP BY bi.item_id
  )`;
  const common = `FROM items i JOIN categories c ON c.id=i.category_id LEFT JOIN agg a ON a.item_id=i.id
    WHERE ${outerWhere}${requireMatch ? " AND a.item_id IS NOT NULL" : ""}`;
  const allBinds = [...inner.binds, ...outerBinds];
  const groupOrder = filters.grouping === "CATEGORY" ? "c.name COLLATE NOCASE ASC," : "";
  const sortOrder = filters.sort === "NAME" ? "i.item_name COLLATE NOCASE ASC"
    : filters.sort === "OLDEST" ? "COALESCE(a.last_booking_date,'') ASC"
    : filters.sort === "QUANTITY" ? "COALESCE(a.booked_qty,0) DESC"
    : "COALESCE(a.last_booking_date,'') DESC,i.item_name COLLATE NOCASE";
  const countStmt = env.DB.prepare(`${cte} SELECT COUNT(*) AS count ${common}`).bind(...allBinds);
  const summaryStmt = env.DB.prepare(`${cte} SELECT COUNT(*) total_items,
      COALESCE(SUM(a.total_bookings),0) total_bookings,COALESCE(SUM(a.booked_qty),0) booked_qty,
      COALESCE(SUM(a.given_qty),0) given_qty,COALESCE(SUM(a.returned_qty),0) returned_qty ${common}`).bind(...allBinds);
  const rowsStmt = env.DB.prepare(`${cte} SELECT i.item_code,i.item_name,c.name AS category_name,i.total_quantity,
      COALESCE(a.total_bookings,0) total_bookings,COALESCE(a.booked_qty,0) booked_qty,
      COALESCE(a.given_qty,0) given_qty,COALESCE(a.returned_qty,0) returned_qty,
      COALESCE(a.pending_qty,0) pending_qty,a.last_booking_date ${common}
      ORDER BY ${groupOrder}${sortOrder} LIMIT ? OFFSET ?`).bind(...allBinds, filters.pageSize, (filters.page - 1) * filters.pageSize);
  return executePaged(
    env, filters, countStmt, summaryStmt, rowsStmt, "Item-wise History",
    [
      { key: "item_code", label: "Item Code" },
      { key: "item_name", label: "Item" },
      { key: "category_name", label: "Category" },
      { key: "total_bookings", label: "Bookings" },
      { key: "booked_qty", label: "Booked Qty" },
      { key: "given_qty", label: "Picked Qty" },
      { key: "returned_qty", label: "Returned Qty" },
      { key: "last_booking_date", label: "Last Activity" }
    ],
    row => [
      summaryItem("Items", row.total_items),
      summaryItem("Bookings", row.total_bookings),
      summaryItem("Booked Qty", row.booked_qty),
      summaryItem("Picked Qty", row.given_qty),
      summaryItem("Returned Qty", row.returned_qty)
    ]
  );
}

async function customerHistoryReport(env, filters) {
  const outer = ["cu.archived_at IS NULL"];
  const outerBinds = [];
  if (filters.customerSearch) {
    outer.push("(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)");
    outerBinds.push(like(filters.customerSearch), like(filters.customerSearch));
  }
  if (filters.search) {
    const q = like(filters.search);
    outer.push("(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)");
    outerBinds.push(q, q);
  }
  const innerWhere = ["1=1"];
  const innerBinds = [];
  if (filters.fromDate) { innerWhere.push("b.booking_date>=?"); innerBinds.push(filters.fromDate); }
  if (filters.toDate) { innerWhere.push("b.booking_date<=?"); innerBinds.push(filters.toDate); }
  statusClause(filters, "b", innerWhere, innerBinds);
  if (filters.categoryId) {
    innerWhere.push("EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND ix.category_id=?)");
    innerBinds.push(filters.categoryId);
  }
  if (filters.itemSearch) {
    innerWhere.push("EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?)))");
    innerBinds.push(like(filters.itemSearch), like(filters.itemSearch));
  }
  if (filters.staffUserId) { innerWhere.push("b.created_by_user_id=?"); innerBinds.push(filters.staffUserId); }
  const requireMatch = Boolean(filters.fromDate || filters.toDate || filters.status || filters.categoryId || filters.itemSearch || filters.staffUserId);
  const cte = `WITH agg AS (
    SELECT b.customer_id,COUNT(DISTINCT b.id) total_bookings,
           COUNT(DISTINCT CASE WHEN b.status='CANCELLED' THEN b.id END) cancelled_bookings,
           COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' THEN bi.booked_qty ELSE 0 END),0) booked_qty,
           COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' THEN bi.given_qty ELSE 0 END),0) given_qty,
           COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' THEN bi.returned_qty ELSE 0 END),0) returned_qty,
           MAX(b.booking_date) last_booking_date
    FROM bookings b LEFT JOIN booking_items bi ON bi.booking_id=b.id
    WHERE ${innerWhere.join(" AND ")} GROUP BY b.customer_id
  )`;
  const common = `FROM customers cu LEFT JOIN agg a ON a.customer_id=cu.id
    WHERE ${outer.join(" AND ")}${requireMatch ? " AND a.customer_id IS NOT NULL" : ""}`;
  const allBinds = [...innerBinds, ...outerBinds];
  const sortOrder = filters.sort === "NAME" ? "cu.name COLLATE NOCASE ASC"
    : filters.sort === "OLDEST" ? "COALESCE(a.last_booking_date,'') ASC"
    : filters.sort === "QUANTITY" ? "COALESCE(a.booked_qty,0) DESC"
    : "COALESCE(a.last_booking_date,'') DESC,cu.name COLLATE NOCASE";
  const countStmt = env.DB.prepare(`${cte} SELECT COUNT(*) AS count ${common}`).bind(...allBinds);
  const summaryStmt = env.DB.prepare(`${cte} SELECT COUNT(*) total_customers,
      COALESCE(SUM(a.total_bookings),0) total_bookings,COALESCE(SUM(a.booked_qty),0) booked_qty ${common}`).bind(...allBinds);
  const rowsStmt = env.DB.prepare(`${cte} SELECT cu.id AS customer_id,cu.name AS customer_name,cu.mobile AS customer_mobile,
      COALESCE(a.total_bookings,0) total_bookings,COALESCE(a.cancelled_bookings,0) cancelled_bookings,
      COALESCE(a.booked_qty,0) booked_qty,COALESCE(a.given_qty,0) given_qty,
      COALESCE(a.returned_qty,0) returned_qty,a.last_booking_date ${common}
      ORDER BY ${sortOrder} LIMIT ? OFFSET ?`).bind(...allBinds, filters.pageSize, (filters.page - 1) * filters.pageSize);
  return executePaged(
    env, filters, countStmt, summaryStmt, rowsStmt, "Customer-wise History",
    [
      { key: "customer_name", label: "Customer" },
      { key: "customer_mobile", label: "Mobile" },
      { key: "total_bookings", label: "Bookings" },
      { key: "booked_qty", label: "Booked Qty" },
      { key: "given_qty", label: "Picked Qty" },
      { key: "returned_qty", label: "Returned Qty" },
      { key: "last_booking_date", label: "Last Activity" }
    ],
    row => [
      summaryItem("Customers", row.total_customers),
      summaryItem("Bookings", row.total_bookings),
      summaryItem("Booked Qty", row.booked_qty)
    ]
  );
}

async function categoryStockReport(env, filters) {
  const where = ["c.is_active=1"];
  const binds = [];
  if (filters.categoryId) { where.push("c.id=?"); binds.push(filters.categoryId); }
  if (filters.search) { where.push("LOWER(c.name) LIKE LOWER(?)"); binds.push(like(filters.search)); }
  const clause = where.join(" AND ");
  const cte = `WITH item_rollup AS (
      SELECT i.category_id,COUNT(*) total_items,COALESCE(SUM(i.total_quantity),0) total_quantity
      FROM items i WHERE i.archived_at IS NULL AND i.is_active=1 GROUP BY i.category_id
    ), flow AS (
      SELECT i.category_id,
        COALESCE(SUM(CASE WHEN b.status NOT IN ('CANCELLED','RETURNED') AND bi.booked_qty-bi.closed_qty>bi.given_qty THEN bi.booked_qty-bi.closed_qty-bi.given_qty ELSE 0 END),0) booked_qty,
        COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) given_qty
      FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN items i ON i.id=bi.item_id GROUP BY i.category_id
    )`;
  const common = `FROM categories c LEFT JOIN item_rollup ir ON ir.category_id=c.id LEFT JOIN flow f ON f.category_id=c.id WHERE ${clause}`;
  const sortOrder = filters.sort === "QUANTITY" ? "COALESCE(ir.total_quantity,0) DESC"
    : filters.sort === "OLDEST" ? "c.display_order DESC,c.name COLLATE NOCASE"
    : "c.display_order ASC,c.name COLLATE NOCASE";
  const countStmt = env.DB.prepare(`${cte} SELECT COUNT(*) AS count ${common}`).bind(...binds);
  const summaryStmt = env.DB.prepare(`${cte} SELECT COUNT(*) total_categories,
      COALESCE(SUM(ir.total_items),0) total_items,COALESCE(SUM(ir.total_quantity),0) total_qty,
      COALESCE(SUM(f.booked_qty),0) booked_qty,COALESCE(SUM(f.given_qty),0) given_qty,
      COALESCE(SUM(MAX(0,COALESCE(ir.total_quantity,0)-COALESCE(f.given_qty,0))),0) available_qty ${common}`).bind(...binds);
  const rowsStmt = env.DB.prepare(`${cte} SELECT c.name AS category_name,COALESCE(ir.total_items,0) total_items,
      COALESCE(ir.total_quantity,0) total_qty,COALESCE(f.booked_qty,0) booked_qty,
      COALESCE(f.given_qty,0) given_qty,MAX(0,COALESCE(ir.total_quantity,0)-COALESCE(f.given_qty,0)) available_qty
      ${common} ORDER BY ${sortOrder} LIMIT ? OFFSET ?`).bind(...binds, filters.pageSize, (filters.page - 1) * filters.pageSize);
  return executePaged(
    env, filters, countStmt, summaryStmt, rowsStmt, "Category Stock",
    [
      { key: "category_name", label: "Category" },
      { key: "total_items", label: "Items" },
      { key: "total_qty", label: "Total Qty" },
      { key: "available_qty", label: "Available Qty" },
      { key: "booked_qty", label: "Booked Qty" },
      { key: "given_qty", label: "Given Qty" }
    ],
    row => [
      summaryItem("Categories", row.total_categories),
      summaryItem("Items", row.total_items),
      summaryItem("Total Qty", row.total_qty),
      summaryItem("Available Qty", row.available_qty),
      summaryItem("Booked Qty", row.booked_qty),
      summaryItem("Given Qty", row.given_qty)
    ]
  );
}

async function staffActivityReport(env, filters) {
  const userWhere = ["u.is_active=1", "u.archived_at IS NULL", "u.role IN ('OWNER','STAFF')"];
  const userBinds = [];
  if (filters.staffUserId) { userWhere.push("u.id=?"); userBinds.push(filters.staffUserId); }
  if (filters.search) {
    userWhere.push("(LOWER(u.name) LIKE LOWER(?) OR u.mobile LIKE ?)");
    userBinds.push(like(filters.search), like(filters.search));
  }

  const bookingWhere = ["1=1"];
  const bookingBinds = [];
  if (filters.fromDate) { bookingWhere.push("b.booking_date>=?"); bookingBinds.push(filters.fromDate); }
  if (filters.toDate) { bookingWhere.push("b.booking_date<=?"); bookingBinds.push(filters.toDate); }

  const pickupWhere = ["1=1"];
  const pickupBinds = [];
  if (filters.fromDate) { pickupWhere.push("substr(pe.pickup_at,1,10)>=?"); pickupBinds.push(filters.fromDate); }
  if (filters.toDate) { pickupWhere.push("substr(pe.pickup_at,1,10)<=?"); pickupBinds.push(filters.toDate); }

  const returnWhere = ["1=1"];
  const returnBinds = [];
  if (filters.fromDate) { returnWhere.push("substr(re.return_at,1,10)>=?"); returnBinds.push(filters.fromDate); }
  if (filters.toDate) { returnWhere.push("substr(re.return_at,1,10)<=?"); returnBinds.push(filters.toDate); }

  const cte = `WITH booking_activity AS (
      SELECT b.created_by_user_id user_id,COUNT(*) bookings
      FROM bookings b WHERE ${bookingWhere.join(" AND ")} GROUP BY b.created_by_user_id
    ), pickup_activity AS (
      SELECT pe.handled_by_user_id user_id,COUNT(DISTINCT pe.id) pickups,COALESCE(SUM(pei.qty_given),0) pickup_qty
      FROM pickup_events pe JOIN pickup_event_items pei ON pei.pickup_event_id=pe.id
      WHERE ${pickupWhere.join(" AND ")} GROUP BY pe.handled_by_user_id
    ), return_activity AS (
      SELECT re.received_by_user_id user_id,COUNT(DISTINCT re.id) returns,COALESCE(SUM(rei.qty_returned),0) return_qty
      FROM return_events re JOIN return_event_items rei ON rei.return_event_id=re.id
      WHERE ${returnWhere.join(" AND ")} GROUP BY re.received_by_user_id
    )`;
  const allBinds = [...bookingBinds, ...pickupBinds, ...returnBinds, ...userBinds];
  const common = `FROM users u LEFT JOIN booking_activity ba ON ba.user_id=u.id
      LEFT JOIN pickup_activity pa ON pa.user_id=u.id LEFT JOIN return_activity ra ON ra.user_id=u.id
      WHERE ${userWhere.join(" AND ")}`;
  const sortOrder = filters.sort === "OLDEST" ? "u.created_at ASC"
    : filters.sort === "NAME" ? "u.name COLLATE NOCASE ASC"
    : filters.sort === "QUANTITY" ? "(COALESCE(ba.bookings,0)+COALESCE(pa.pickups,0)+COALESCE(ra.returns,0)) DESC"
    : "total_activities DESC,u.name COLLATE NOCASE";
  const countStmt = env.DB.prepare(`${cte} SELECT COUNT(*) AS count ${common}`).bind(...allBinds);
  const summaryStmt = env.DB.prepare(`${cte} SELECT COUNT(*) total_staff,
      COALESCE(SUM(ba.bookings),0) bookings,COALESCE(SUM(pa.pickups),0) pickups,
      COALESCE(SUM(ra.returns),0) returns,
      COALESCE(SUM(COALESCE(ba.bookings,0)+COALESCE(pa.pickups,0)+COALESCE(ra.returns,0)),0) total_activities
      ${common}`).bind(...allBinds);
  const rowsStmt = env.DB.prepare(`${cte} SELECT u.id AS staff_id,u.name AS staff_name,u.role,
      COALESCE(ba.bookings,0) bookings,COALESCE(pa.pickups,0) pickups,COALESCE(pa.pickup_qty,0) pickup_qty,
      COALESCE(ra.returns,0) returns,COALESCE(ra.return_qty,0) return_qty,
      (COALESCE(ba.bookings,0)+COALESCE(pa.pickups,0)+COALESCE(ra.returns,0)) total_activities
      ${common} ORDER BY ${sortOrder} LIMIT ? OFFSET ?`).bind(...allBinds, filters.pageSize, (filters.page - 1) * filters.pageSize);
  return executePaged(
    env, filters, countStmt, summaryStmt, rowsStmt, "Staff Activity",
    [
      { key: "staff_name", label: "Staff" },
      { key: "role", label: "Role" },
      { key: "bookings", label: "Bookings" },
      { key: "pickups", label: "Pickups" },
      { key: "returns", label: "Returns" },
      { key: "total_activities", label: "Total Activities" }
    ],
    row => [
      summaryItem("Users", row.total_staff),
      summaryItem("Bookings", row.bookings),
      summaryItem("Pickups", row.pickups),
      summaryItem("Returns", row.returns),
      summaryItem("Activities", row.total_activities)
    ]
  );
}

async function runReport(env, filters) {
  if (filters.type === "PICKUPS") return activityReport(env, filters, "PICKUPS");
  if (filters.type === "RETURNS") return activityReport(env, filters, "RETURNS");
  if (filters.type === "OVERDUE") return overdueReport(env, filters);
  if (filters.type === "ITEM_HISTORY") return itemHistoryReport(env, filters);
  if (filters.type === "CUSTOMER_HISTORY") return customerHistoryReport(env, filters);
  if (filters.type === "CATEGORY_STOCK") return categoryStockReport(env, filters);
  if (filters.type === "STAFF_ACTIVITY") return staffActivityReport(env, filters);
  return bookingsReport(env, filters);
}

function safePresetConfig(value) {
  const raw = value && typeof value === "object" && !Array.isArray(value) ? value : {};
  const type = REPORT_TYPES.has(text(raw.type, 40).toUpperCase()) ? text(raw.type, 40).toUpperCase() : "BOOKINGS";
  const dateBasis = DATE_BASES.has(text(raw.dateBasis, 40).toUpperCase()) ? text(raw.dateBasis, 40).toUpperCase() : "BOOKING_DATE";
  const grouping = GROUPINGS.has(text(raw.grouping, 40).toUpperCase()) ? text(raw.grouping, 40).toUpperCase() : "NONE";
  const sort = SORTS.has(text(raw.sort, 40).toUpperCase()) ? text(raw.sort, 40).toUpperCase() : "NEWEST";
  const status = STATUSES.has(text(raw.status, 40).toUpperCase()) ? text(raw.status, 40).toUpperCase() : "";
  return {
    type,
    dateBasis,
    datePreset: text(raw.datePreset, 40).toUpperCase(),
    fromDate: validDate(raw.fromDate) ? String(raw.fromDate) : "",
    toDate: validDate(raw.toDate) ? String(raw.toDate) : "",
    categoryId: text(raw.categoryId, 100),
    itemSearch: text(raw.itemSearch, 160),
    customerSearch: text(raw.customerSearch, 160),
    status,
    staffUserId: text(raw.staffUserId, 100),
    grouping,
    sort,
    search: text(raw.search, 160)
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

async function bootstrap(env, user) {
  const [categoriesResult, usersResult, presetsResult] = await env.DB.batch([
    env.DB.prepare("SELECT id,name FROM categories WHERE is_active=1 ORDER BY display_order,name COLLATE NOCASE"),
    env.DB.prepare("SELECT id,name,role FROM users WHERE is_active=1 AND archived_at IS NULL AND role IN ('OWNER','STAFF') ORDER BY name COLLATE NOCASE"),
    env.DB.prepare(`SELECT rp.id,rp.owner_user_id,rp.name,rp.visibility,rp.config_json,rp.updated_at,u.name AS owner_name
      FROM report_presets rp JOIN users u ON u.id=rp.owner_user_id
      WHERE rp.owner_user_id=? OR rp.visibility='SHARED'
      ORDER BY CASE WHEN rp.owner_user_id=? THEN 0 ELSE 1 END,rp.updated_at DESC,rp.name COLLATE NOCASE`).bind(user.id, user.id)
  ]);
  const presets = (presetsResult.results || []).map(row => {
    let config = {};
    try { config = JSON.parse(String(row.config_json || "{}")); } catch { config = {}; }
    return {
      id: row.id,
      ownerUserId: row.owner_user_id,
      ownerName: row.owner_name,
      name: row.name,
      visibility: row.visibility,
      config,
      updatedAt: row.updated_at,
      canEdit: String(row.owner_user_id) === user.id || (user.role === "OWNER" && row.visibility === "SHARED")
    };
  });
  return {
    ok: true,
    today: businessToday(),
    categories: categoriesResult.results || [],
    staff: usersResult.results || [],
    statuses: ["RESERVED","BOOKED","PARTIALLY_GIVEN","GIVEN","PARTIALLY_RETURNED","RETURNED","CANCELLED"],
    presets
  };
}

async function savePreset(request, env, user, presetId = "") {
  const body = await readJson(request);
  if (!body) return apiJson({ ok: false, error: "VALIDATION", message: "A valid preset is required." }, 400);
  const name = text(body.name, 80);
  if (!name) return apiJson({ ok: false, error: "VALIDATION", message: "Preset name is required." }, 400);
  const config = safePresetConfig(body.config);
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

  await env.DB.batch([
    env.DB.prepare("UPDATE report_presets SET name=?,visibility=?,config_json=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")
      .bind(name, visibility, JSON.stringify(config), presetId),
    env.DB.prepare("INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)")
      .bind(
        crypto.randomUUID(), user.id, "UPDATE", "REPORT_PRESET", presetId,
        JSON.stringify({ name: existing.name, visibility: existing.visibility, config: JSON.parse(String(existing.config_json || "{}")) }),
        JSON.stringify({ name, visibility, config })
      )
  ]);
  return apiJson({ ok: true, id: presetId, message: "Report preset updated." });
}

async function deletePreset(env, user, presetId) {
  const existing = await env.DB.prepare("SELECT * FROM report_presets WHERE id=? LIMIT 1").bind(presetId).first();
  if (!existing) return apiJson({ ok: false, error: "NOT_FOUND", message: "Report preset not found." }, 404);
  const canEdit = String(existing.owner_user_id) === user.id || (user.role === "OWNER" && existing.visibility === "SHARED");
  if (!canEdit) return apiJson({ ok: false, error: "FORBIDDEN", message: "You cannot delete this report preset." }, 403);
  await env.DB.batch([
    env.DB.prepare("DELETE FROM report_presets WHERE id=?").bind(presetId),
    env.DB.prepare("INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json) VALUES(?,?,?,?,?,?)")
      .bind(crypto.randomUUID(), user.id, "DELETE", "REPORT_PRESET", presetId, JSON.stringify(existing))
  ]);
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

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const isReportGenerator = url.pathname === "/api/admin/report-generator" || url.pathname === "/api/admin/report-generator/bootstrap";
    const presetMatch = url.pathname.match(/^\/api\/admin\/report-presets(?:\/([^/]+))?$/);

    if (isReportGenerator || presetMatch) {
      const auth = await requireReportsUser(request, env);
      if (auth.response) return auth.response;
      const user = auth.user;

      if (url.pathname === "/api/admin/report-generator/bootstrap" && request.method === "GET") {
        return apiJson(await bootstrap(env, user));
      }

      if (url.pathname === "/api/admin/report-generator" && request.method === "GET") {
        const result = await runReport(env, parseFilters(request));
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

    return core.fetch(request, env, ctx);
  }
};
