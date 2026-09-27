import core from "./phase14bd-screen8-reports.js";
import { getSessionUser, hasStaffPermission } from "./auth.ts";
import { bookingPaymentStatusSql } from "./payment-classifier.js";

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

function safeInt(value) {
  const n = Number(value || 0);
  return Number.isFinite(n) ? Math.trunc(n) : 0;
}

function displayStatus(row) {
  const raw = String(row.status || "").toUpperCase();
  const confirmation = String(row.confirmation_state || "BOOKED").toUpperCase();
  const picked = safeInt(row.given_qty);
  const returned = safeInt(row.returned_qty);
  const pickupPending = safeInt(row.pickup_pending ?? row.remaining_to_give);
  const returnPending = safeInt(row.return_pending ?? row.pending_qty);

  if (raw === "CANCELLED") return "CANCELLED";
  if (confirmation === "RESERVED" && picked === 0) return "RESERVED";
  if (returnPending > 0 && returned > 0) return "PART_RETURN";
  if (pickupPending > 0 && picked > 0) return "PART_PICKUP";
  if (pickupPending > 0) return "BOOKED";
  if (returnPending > 0) return "FULL_PICKUP";
  if (picked > 0) return "FULL_RETURN";
  return "BOOKED";
}

function bookingSortSql(sort) {
  switch (String(sort || "NEWEST").toUpperCase()) {
    case "OLDEST": return "b.created_at ASC,b.id ASC";
    case "PICKUP_ASC": return "b.pickup_date ASC,b.created_at DESC";
    case "PICKUP_DESC": return "b.pickup_date DESC,b.created_at DESC";
    case "RETURN_ASC": return "b.return_date ASC,b.created_at DESC";
    case "RETURN_DESC": return "b.return_date DESC,b.created_at DESC";
    default: return "b.created_at DESC,b.id DESC";
  }
}

async function requireSession(request, env) {
  const user = await getSessionUser(env, request);
  return user || null;
}

async function requirePermission(request, env, permission) {
  const user = await requireSession(request, env);
  if (!user) {
    return { response: apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401) };
  }
  if (!hasStaffPermission(user, permission)) {
    return { response: apiJson({ ok: false, error: "FORBIDDEN", message: "This module is not enabled for your Staff account." }, 403) };
  }
  return { user };
}

function parseItemJson(value) {
  try {
    const parsed = JSON.parse(String(value || "[]"));
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

async function listBookings(request, env) {
  const auth = await requirePermission(request, env, "BOOKINGS");
  if (auth.response) return auth.response;

  const url = new URL(request.url);
  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(10, Math.max(1, Number(url.searchParams.get("pageSize") || "10") || 10));
  const search = String(url.searchParams.get("search") || "").trim().slice(0, 120);
  const view = String(url.searchParams.get("view") || "ALL").trim().toUpperCase();
  const sort = String(url.searchParams.get("sort") || "NEWEST").trim().toUpperCase();
  const today = businessToday();
  const paymentStatusSql = bookingPaymentStatusSql("b");
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
    where.push("b.status<>'CANCELLED' AND COALESCE(b.confirmation_state,'BOOKED')='RESERVED' AND q.given_qty=0");
  } else if (view === "BOOKED") {
    where.push("b.status<>'CANCELLED' AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED' AND q.given_qty=0 AND q.pickup_pending>0");
  } else if (view === "PART_PICKUP" || view === "PART_PICKED_UP") {
    where.push("b.status<>'CANCELLED' AND q.pickup_pending>0 AND q.given_qty>0 AND NOT (q.return_pending>0 AND q.returned_qty>0)");
  } else if (view === "FULL_PICKUP" || view === "FULL_PICKED_UP") {
    where.push("b.status<>'CANCELLED' AND q.pickup_pending=0 AND q.return_pending>0 AND q.returned_qty=0");
  } else if (view === "PART_RETURN") {
    where.push("b.status<>'CANCELLED' AND q.return_pending>0 AND q.returned_qty>0");
  } else if (view === "FULL_RETURN" || view === "FULL_RETURNED") {
    where.push("b.status<>'CANCELLED' AND q.pickup_pending=0 AND q.return_pending=0 AND q.given_qty>0");
  } else if (view === "TODAY_PICKUP") {
    where.push("b.status<>'CANCELLED' AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED' AND b.pickup_date=? AND q.pickup_pending>0");
    binds.push(today);
  } else if (view === "TODAY_RETURN") {
    where.push("b.status<>'CANCELLED' AND b.return_date=? AND q.return_pending>0");
    binds.push(today);
  } else if (view === "MISSED_PICKUP") {
    where.push("b.status<>'CANCELLED' AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED' AND b.pickup_date<? AND q.pickup_pending>0");
    binds.push(today);
  } else if (view === "ACTIVE_RENTAL") {
    where.push("b.status<>'CANCELLED' AND q.return_pending>0");
  } else if (view === "OVERDUE") {
    where.push("b.status<>'CANCELLED' AND b.return_date<? AND q.return_pending>0");
    binds.push(today);
  } else if (view === "CANCELLED") {
    where.push("b.status='CANCELLED'");
  } else if (view === "PAYMENT_PENDING") {
    where.push(`b.status<>'CANCELLED' AND (${paymentStatusSql})='PENDING'`);
  } else if (view === "PAYMENT_PART") {
    where.push(`b.status<>'CANCELLED' AND (${paymentStatusSql})='PART_RECEIVED'`);
  } else if (view === "PAYMENT_FULL") {
    where.push(`b.status<>'CANCELLED' AND (${paymentStatusSql})='FULL_AMOUNT_RECEIVED'`);
  }

  const clause = where.join(" AND ");
  const withQty = `
    WITH q AS (
      SELECT booking_id,
             COALESCE(SUM(booked_qty),0) AS original_booked_qty,
             COALESCE(SUM(booked_qty-COALESCE(closed_qty,0)),0) AS active_booked_qty,
             COALESCE(SUM(given_qty),0) AS given_qty,
             COALESCE(SUM(returned_qty),0) AS returned_qty,
             COALESCE(SUM(COALESCE(closed_qty,0)),0) AS closed_qty,
             COALESCE(SUM(CASE
               WHEN booked_qty-COALESCE(closed_qty,0)>given_qty
               THEN booked_qty-COALESCE(closed_qty,0)-given_qty ELSE 0 END),0) AS pickup_pending,
             COALESCE(SUM(CASE WHEN given_qty>returned_qty THEN given_qty-returned_qty ELSE 0 END),0) AS return_pending
      FROM booking_items GROUP BY booking_id
    )
  `;

  const rows = await env.DB.prepare(`${withQty}
    SELECT b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,b.confirmation_state,b.notes,b.created_at,b.updated_at,
           COUNT(*) OVER() AS __total,
           ${paymentStatusSql} AS payment_status,
           c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,
           COALESCE(c.address,'') AS customer_address,u.name AS booked_by,
           q.original_booked_qty AS booked_qty,q.active_booked_qty,q.given_qty,q.returned_qty,q.closed_qty,q.pickup_pending,q.return_pending,
           COALESCE((SELECT COUNT(*) FROM booking_items bic WHERE bic.booking_id=b.id),0) AS item_count,
           COALESCE((
             SELECT json_group_array(json_object(
               'item_code',x.item_code,'item_name',x.item_name,'category_name',x.category_name,
               'image_url',x.image_url,'images_json',x.images_json,'quantity',x.original_qty,
               'given_qty',x.given_qty,'returned_qty',x.returned_qty
             ))
             FROM (
               SELECT i.item_code,i.item_name,c2.name AS category_name,bi.booked_qty AS original_qty,
                      bi.given_qty,bi.returned_qty,
                      (SELECT im.image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order ASC LIMIT 1) AS image_url,
                      COALESCE((SELECT json_group_array(image_url) FROM (
                        SELECT image_url FROM item_images im2 WHERE im2.item_id=i.id
                        ORDER BY im2.is_primary DESC,im2.display_order ASC LIMIT 8
                      )),'[]') AS images_json
               FROM booking_items bi
               JOIN items i ON i.id=bi.item_id
               JOIN categories c2 ON c2.id=i.category_id
               WHERE bi.booking_id=b.id
               ORDER BY i.item_name COLLATE NOCASE
               LIMIT 30
             ) x
           ),'[]') AS item_previews,
           COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ')
             FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items_summary
    FROM bookings b
    JOIN customers c ON c.id=b.customer_id
    LEFT JOIN users u ON u.id=b.created_by_user_id
    JOIN q ON q.booking_id=b.id
    WHERE ${clause}
    ORDER BY ${bookingSortSql(sort)}
    LIMIT ? OFFSET ?`).bind(...binds, pageSize, (page - 1) * pageSize).all();

  const rawRows = rows.results || [];
  const total = rawRows.length ? safeInt(rawRows[0].__total) : 0;
  const bookings = rawRows.map(row => {
    const normalized = {
      ...row,
      display_status: displayStatus(row),
      item_count: safeInt(row.item_count),
      item_previews: parseItemJson(row.item_previews)
    };
    delete normalized.__total;
    return normalized;
  });
  return apiJson({ ok: true, bookings, pagination: { page, pageSize, total, totalPages: Math.max(1, Math.ceil(total / pageSize)) } });
}

async function dashboard(request, env) {
  const auth = await requirePermission(request, env, "DASHBOARD");
  if (auth.response) return auth.response;
  const user = auth.user;
  const today = businessToday();
  const [year, monthNumber] = today.split("-").map(Number);
  const monthStart = `${year}-${String(monthNumber).padStart(2, "0")}-01`;
  const nextMonth = monthNumber === 12
    ? `${year + 1}-01-01`
    : `${year}-${String(monthNumber + 1).padStart(2, "0")}-01`;
  const paymentStatusSql = bookingPaymentStatusSql("b");

  // One bundled query owns all 34 fixed KPIs plus dynamic Category-wise Inventory.
  // Dashboard does not load per-booking queue/card payloads.
  const row = await env.DB.prepare(`
    WITH booking_rollup AS (
      SELECT
        b.id,b.customer_id,b.booking_date,b.pickup_date,b.return_date,b.status,
        COALESCE(b.confirmation_state,'BOOKED') AS confirmation_state,
        ${paymentStatusSql} AS payment_status,
        COALESCE(SUM(bi.booked_qty),0) AS original_booked_qty,
        COALESCE(SUM(bi.booked_qty-COALESCE(bi.closed_qty,0)),0) AS active_booked_qty,
        COALESCE(SUM(bi.given_qty),0) AS given_qty,
        COALESCE(SUM(bi.returned_qty),0) AS returned_qty,
        COALESCE(SUM(CASE
          WHEN (bi.booked_qty-COALESCE(bi.closed_qty,0))>bi.given_qty
          THEN (bi.booked_qty-COALESCE(bi.closed_qty,0))-bi.given_qty ELSE 0 END),0) AS pending_pickup_qty,
        COALESCE(SUM(CASE
          WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS pending_return_qty
      FROM bookings b
      LEFT JOIN booking_items bi ON bi.booking_id=b.id
      GROUP BY b.id
    ),
    item_flow AS (
      SELECT
        i.id AS item_id,i.category_id,i.total_quantity,
        COALESCE(SUM(CASE
          WHEN b.id IS NOT NULL AND b.status<>'CANCELLED'
            AND (bi.booked_qty-COALESCE(bi.closed_qty,0))>bi.given_qty
          THEN (bi.booked_qty-COALESCE(bi.closed_qty,0))-bi.given_qty ELSE 0 END),0) AS pickup_pending_qty,
        COALESCE(SUM(CASE
          WHEN b.id IS NOT NULL AND b.status<>'CANCELLED' AND bi.given_qty>bi.returned_qty
          THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS currently_out_qty
      FROM items i
      LEFT JOIN booking_items bi ON bi.item_id=i.id
      LEFT JOIN bookings b ON b.id=bi.booking_id
      WHERE i.archived_at IS NULL AND i.is_active=1
      GROUP BY i.id
    ),
    item_rollup AS (
      SELECT item_id,category_id,total_quantity,pickup_pending_qty,currently_out_qty,
             MAX(0,total_quantity-pickup_pending_qty-currently_out_qty) AS available_now
      FROM item_flow
    ),
    category_rollup AS (
      SELECT
        c.id AS category_id,c.name AS category_name,c.display_order,
        COUNT(ir.item_id) AS items,
        COALESCE(SUM(ir.total_quantity),0) AS total_qty,
        COALESCE(SUM(ir.available_now),0) AS available,
        COALESCE(SUM(ir.pickup_pending_qty),0) AS pickup_pending,
        COALESCE(SUM(ir.currently_out_qty),0) AS currently_out
      FROM categories c
      LEFT JOIN item_rollup ir ON ir.category_id=c.id
      WHERE c.is_active=1
      GROUP BY c.id
    ),
    customer_rollup AS (
      SELECT
        c.id AS customer_id,
        COALESCE(SUM(CASE WHEN br.status<>'CANCELLED' THEN 1 ELSE 0 END),0) AS total_bookings,
        MIN(CASE WHEN br.status<>'CANCELLED' THEN br.booking_date END) AS first_booking_date,
        COALESCE(SUM(CASE
          WHEN br.status<>'CANCELLED' AND br.booking_date>='${monthStart}' AND br.booking_date<'${nextMonth}'
          THEN 1 ELSE 0 END),0) AS month_bookings,
        MAX(CASE WHEN br.status<>'CANCELLED' AND br.pending_return_qty>0 THEN 1 ELSE 0 END) AS active_rental,
        MAX(CASE
          WHEN br.status<>'CANCELLED' AND (
            (br.confirmation_state<>'RESERVED' AND br.pending_pickup_qty>0 AND br.pickup_date<'${today}')
            OR (br.pending_return_qty>0 AND br.return_date<'${today}')
          ) THEN 1 ELSE 0 END) AS has_exception
      FROM customers c
      LEFT JOIN booking_rollup br ON br.customer_id=c.id
      WHERE c.archived_at IS NULL
      GROUP BY c.id
    ),
    bill_rollup AS (
      SELECT
        COALESCE(SUM(CASE WHEN status='DRAFT' THEN 1 ELSE 0 END),0) AS draft_bills,
        COALESCE(SUM(CASE WHEN status='FINAL' THEN 1 ELSE 0 END),0) AS final_bills,
        COALESCE(SUM(CASE WHEN status<>'CANCELLED' AND bill_date='${today}' THEN 1 ELSE 0 END),0) AS bills_today,
        COALESCE(SUM(CASE WHEN status<>'CANCELLED' THEN MAX(net_amount-COALESCE(received_amount,0),0) ELSE 0 END),0) AS pending_balance,
        COALESCE(SUM(CASE WHEN status<>'CANCELLED' THEN COALESCE(received_amount,0) ELSE 0 END),0) AS total_received
      FROM bills
    )
    SELECT
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND confirmation_state<>'RESERVED'
          AND pickup_date='${today}' AND pending_pickup_qty>0),0) AS today_pickups,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND return_date='${today}' AND pending_return_qty>0),0) AS today_returns,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND confirmation_state<>'RESERVED'
          AND pickup_date<'${today}' AND pending_pickup_qty>0),0) AS missed_pickups,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND return_date<'${today}' AND pending_return_qty>0),0) AS overdue_returns,

      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND confirmation_state='RESERVED' AND given_qty=0),0) AS reserved_orders,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND confirmation_state<>'RESERVED'
          AND pending_pickup_qty>0 AND given_qty=0),0) AS booked_orders,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND confirmation_state<>'RESERVED'
          AND pending_pickup_qty>0 AND given_qty>0
          AND NOT (pending_return_qty>0 AND returned_qty>0)),0) AS part_picked_up_orders,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND pending_pickup_qty=0
          AND pending_return_qty>0 AND returned_qty=0),0) AS full_picked_up_orders,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND pending_return_qty>0 AND returned_qty>0),0) AS part_return_orders,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND given_qty>0
          AND pending_pickup_qty=0 AND pending_return_qty=0),0) AS full_returned_orders,
      COALESCE((SELECT COUNT(*) FROM booking_rollup WHERE status='CANCELLED'),0) AS cancelled_orders,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND pending_return_qty>0),0) AS active_rental_orders,

      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND payment_status='PENDING'),0) AS pending_payment,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND payment_status='PART_RECEIVED'),0) AS part_payment,
      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE status<>'CANCELLED' AND payment_status='FULL_AMOUNT_RECEIVED'),0) AS full_payment,
      COALESCE((SELECT draft_bills FROM bill_rollup),0) AS draft_bills,
      COALESCE((SELECT final_bills FROM bill_rollup),0) AS final_bills,
      COALESCE((SELECT bills_today FROM bill_rollup),0) AS bills_today,
      COALESCE((SELECT pending_balance FROM bill_rollup),0) AS pending_balance,
      COALESCE((SELECT total_received FROM bill_rollup),0) AS total_received,

      COALESCE((SELECT SUM(available_now) FROM item_rollup),0) AS available_now,
      COALESCE((SELECT SUM(pickup_pending_qty) FROM item_rollup),0) AS pickup_pending_qty,
      COALESCE((SELECT SUM(currently_out_qty) FROM item_rollup),0) AS currently_out_qty,
      COALESCE((SELECT SUM(total_quantity) FROM item_rollup),0) AS total_quantity,
      COALESCE((SELECT COUNT(*) FROM item_rollup),0) AS total_items,
      COALESCE((SELECT COUNT(*) FROM categories WHERE is_active=1),0) AS categories,
      COALESCE((SELECT COUNT(*) FROM item_rollup WHERE available_now BETWEEN 1 AND 5),0) AS low_stock,
      COALESCE((SELECT COUNT(*) FROM item_rollup WHERE available_now<=0),0) AS unavailable,

      COALESCE((SELECT COUNT(*) FROM customer_rollup),0) AS total_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup
        WHERE month_bookings>0 AND first_booking_date>='${monthStart}' AND first_booking_date<'${nextMonth}'),0) AS new_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup
        WHERE month_bookings>0 AND first_booking_date<'${monthStart}'),0) AS returning_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup WHERE total_bookings>=2),0) AS frequent_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup WHERE active_rental=1),0) AS active_rental_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup WHERE has_exception=1),0) AS customer_exceptions,

      COALESCE((
        SELECT json_group_array(json_object(
          'categoryId',category_id,'categoryName',category_name,'items',items,'totalQty',total_qty,
          'available',available,'pickupPending',pickup_pending,'currentlyOut',currently_out
        ))
        FROM (
          SELECT * FROM category_rollup ORDER BY display_order ASC,category_name COLLATE NOCASE ASC
        )
      ),'[]') AS category_inventory_json,

      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE booking_date='${today}' AND status<>'CANCELLED'),0) AS today_bookings_count
  `).first();

  if (!row) return apiJson({ ok: false, error: "DASHBOARD_UNAVAILABLE", message: "Unable to load dashboard." }, 500);

  const kpis = {
    todayOverview: {
      todayPickups: safeInt(row.today_pickups),
      todayReturns: safeInt(row.today_returns),
      missedPickups: safeInt(row.missed_pickups),
      overdueReturns: safeInt(row.overdue_returns)
    },
    bookingStatus: {
      reserved: safeInt(row.reserved_orders),
      booked: safeInt(row.booked_orders),
      partPickedUp: safeInt(row.part_picked_up_orders),
      fullPickedUp: safeInt(row.full_picked_up_orders),
      partReturn: safeInt(row.part_return_orders),
      fullReturned: safeInt(row.full_returned_orders),
      cancelled: safeInt(row.cancelled_orders),
      activeRentalOrders: safeInt(row.active_rental_orders)
    },
    paymentBilling: {
      pendingPayment: safeInt(row.pending_payment),
      partPayment: safeInt(row.part_payment),
      fullPayment: safeInt(row.full_payment),
      draftBills: safeInt(row.draft_bills),
      finalBills: safeInt(row.final_bills),
      billsToday: safeInt(row.bills_today),
      pendingBalance: safeInt(row.pending_balance),
      totalReceived: safeInt(row.total_received)
    },
    inventory: {
      availableNow: safeInt(row.available_now),
      pickupPendingQty: safeInt(row.pickup_pending_qty),
      currentlyOutQty: safeInt(row.currently_out_qty),
      totalQuantity: safeInt(row.total_quantity),
      totalItems: safeInt(row.total_items),
      categories: safeInt(row.categories),
      lowStock: safeInt(row.low_stock),
      unavailable: safeInt(row.unavailable)
    },
    customers: {
      totalCustomers: safeInt(row.total_customers),
      newCustomers: safeInt(row.new_customers),
      returningCustomers: safeInt(row.returning_customers),
      frequentCustomers: safeInt(row.frequent_customers),
      activeRentalCustomers: safeInt(row.active_rental_customers),
      customerExceptions: safeInt(row.customer_exceptions)
    }
  };

  const access = {
    bookings: hasStaffPermission(user, "BOOKINGS"),
    billing: hasStaffPermission(user, "BOOKINGS"),
    items: hasStaffPermission(user, "ITEMS"),
    customers: hasStaffPermission(user, "CUSTOMERS"),
    reports: hasStaffPermission(user, "REPORTS")
  };
  const visibleKpis = {
    todayOverview: access.bookings ? kpis.todayOverview : {
      todayPickups: 0, todayReturns: 0, missedPickups: 0, overdueReturns: 0
    },
    bookingStatus: access.bookings ? kpis.bookingStatus : {
      reserved: 0, booked: 0, partPickedUp: 0, fullPickedUp: 0,
      partReturn: 0, fullReturned: 0, cancelled: 0, activeRentalOrders: 0
    },
    paymentBilling: access.billing ? {
      ...kpis.paymentBilling,
      billsToday: user.role === "OWNER" ? kpis.paymentBilling.billsToday : 0,
      pendingBalance: user.role === "OWNER" ? kpis.paymentBilling.pendingBalance : 0,
      totalReceived: user.role === "OWNER" ? kpis.paymentBilling.totalReceived : 0
    } : {
      pendingPayment: 0, partPayment: 0, fullPayment: 0, draftBills: 0,
      finalBills: 0, billsToday: 0, pendingBalance: 0, totalReceived: 0
    },
    inventory: access.items ? kpis.inventory : {
      availableNow: 0, pickupPendingQty: 0, currentlyOutQty: 0, totalQuantity: 0,
      totalItems: 0, categories: 0, lowStock: 0, unavailable: 0
    },
    customers: access.customers ? kpis.customers : {
      totalCustomers: 0, newCustomers: 0, returningCustomers: 0,
      frequentCustomers: 0, activeRentalCustomers: 0, customerExceptions: 0
    }
  };

  let categoryInventory = [];
  try {
    const parsed = JSON.parse(String(row.category_inventory_json || "[]"));
    if (Array.isArray(parsed)) categoryInventory = parsed;
  } catch {}

  return apiJson({
    ok: true,
    today,
    kpis: visibleKpis,
    categoryInventory: access.items ? categoryInventory : [],
    access,
    summary: {
      totalCategories: visibleKpis.inventory.categories,
      totalItems: visibleKpis.inventory.totalItems,
      totalQuantity: visibleKpis.inventory.totalQuantity,
      availableQuantity: visibleKpis.inventory.availableNow,
      bookedQuantity: visibleKpis.inventory.pickupPendingQty,
      missedPickups: visibleKpis.todayOverview.missedPickups,
      givenQuantity: visibleKpis.inventory.currentlyOutQty,
      overdueReturns: visibleKpis.todayOverview.overdueReturns
    },
    sectionCounts: {
      missedPickups: visibleKpis.todayOverview.missedPickups,
      todayBookings: safeInt(row.today_bookings_count),
      todayPickups: visibleKpis.todayOverview.todayPickups,
      todayReturns: visibleKpis.todayOverview.todayReturns,
      overdueReturns: visibleKpis.todayOverview.overdueReturns
    },
    missedPickups: [],
    todayBookings: [],
    todayPickups: [],
    todayReturns: [],
    overdueReturns: []
  });
}

async function augmentBookingDetail(request, env, ctx, bookingId) {
  const auth = await requirePermission(request, env, "BOOKINGS");
  if (auth.response) return auth.response;

  const response = await core.fetch(request, env, ctx);
  if (!response.ok || !(response.headers.get("content-type") || "").includes("application/json")) return response;
  let data;
  try { data = await response.clone().json(); } catch { return response; }
  if (!data?.booking || !Array.isArray(data?.items)) return response;

  const [qtyRows, pickupRows, returnRows, closeRows] = await env.DB.batch([
    env.DB.prepare(`SELECT id,booked_qty,given_qty,returned_qty,closed_qty FROM booking_items WHERE booking_id=?`).bind(bookingId),
    env.DB.prepare(`
      SELECT pe.id AS event_id,i.item_name,pei.qty_given AS quantity
      FROM pickup_events pe
      JOIN pickup_event_items pei ON pei.pickup_event_id=pe.id
      JOIN booking_items bi ON bi.id=pei.booking_item_id
      JOIN items i ON i.id=bi.item_id
      WHERE pe.booking_id=? ORDER BY pe.created_at,i.item_name COLLATE NOCASE`).bind(bookingId),
    env.DB.prepare(`
      SELECT re.id AS event_id,i.item_name,rei.qty_returned AS quantity
      FROM return_events re
      JOIN return_event_items rei ON rei.return_event_id=re.id
      JOIN booking_items bi ON bi.id=rei.booking_item_id
      JOIN items i ON i.id=bi.item_id
      WHERE re.booking_id=? ORDER BY re.created_at,i.item_name COLLATE NOCASE`).bind(bookingId),
    env.DB.prepare(`
      SELECT ce.id AS event_id,i.item_name,cei.qty_closed AS quantity
      FROM booking_close_events ce
      JOIN booking_close_event_items cei ON cei.close_event_id=ce.id
      JOIN booking_items bi ON bi.id=cei.booking_item_id
      JOIN items i ON i.id=bi.item_id
      WHERE ce.booking_id=? ORDER BY ce.created_at,i.item_name COLLATE NOCASE`).bind(bookingId)
  ]);

  const qtyMap = new Map((qtyRows.results || []).map(row => [String(row.id), row]));
  data.items = data.items.map(item => {
    const q = qtyMap.get(String(item.booking_item_id));
    if (!q) return item;
    const originalBooked = safeInt(q.booked_qty);
    const closed = safeInt(q.closed_qty);
    const activeBooked = Math.max(0, originalBooked - closed);
    return {
      ...item,
      booked_qty: activeBooked,
      original_booked_qty: originalBooked,
      closed_qty: closed,
      given_qty: safeInt(q.given_qty),
      returned_qty: safeInt(q.returned_qty)
    };
  });

  const totals = data.items.reduce((acc, item) => {
    acc.booked += safeInt(item.original_booked_qty);
    acc.active += safeInt(item.booked_qty);
    acc.given += safeInt(item.given_qty);
    acc.returned += safeInt(item.returned_qty);
    acc.closed += safeInt(item.closed_qty);
    return acc;
  }, { booked: 0, active: 0, given: 0, returned: 0, closed: 0 });

  data.booking = {
    ...data.booking,
    booked_qty: totals.booked,
    active_booked_qty: totals.active,
    given_qty: totals.given,
    returned_qty: totals.returned,
    closed_qty: totals.closed,
    pickup_pending: Math.max(0, totals.active - totals.given),
    return_pending: Math.max(0, totals.given - totals.returned)
  };
  data.booking.display_status = displayStatus(data.booking);

  const eventItems = new Map();
  const append = (rows, kind) => {
    for (const row of rows.results || []) {
      const id = String(row.event_id);
      const list = eventItems.get(id) || [];
      list.push({ item_name: String(row.item_name || ""), quantity: safeInt(row.quantity), kind });
      eventItems.set(id, list);
    }
  };
  append(pickupRows, "PICKUP");
  append(returnRows, "RETURN");
  append(closeRows, "CLOSE");

  data.timeline = (Array.isArray(data.timeline) ? data.timeline : []).map(event => {
    let eventId = "";
    try { eventId = String(JSON.parse(String(event.new_value_json || "{}")).eventId || ""); } catch {}
    return { ...event, items: eventItems.get(eventId) || [] };
  });

  const headers = new Headers(response.headers);
  headers.delete("content-length");
  headers.set("cache-control", "no-store");
  return new Response(JSON.stringify(data), { status: response.status, statusText: response.statusText, headers });
}

function parseActionLines(body, kind) {
  if (!Array.isArray(body?.items) || body.items.length < 1 || body.items.length > 30) return null;
  const result = [];
  const seen = new Set();
  for (const raw of body.items) {
    const bookingItemId = String(raw?.bookingItemId || "").trim();
    const quantity = Number(raw?.quantity);
    if (!bookingItemId || !Number.isInteger(quantity) || quantity <= 0 || quantity > 100000 || seen.has(bookingItemId)) return null;
    seen.add(bookingItemId);
    result.push({
      bookingItemId,
      quantity,
      conditionNote: kind === "RETURN" ? String(raw?.conditionNote || "").trim().slice(0, 500) : ""
    });
  }
  return result;
}

function movementStatus(rows, projectedGiven, projectedReturned) {
  let pickupPending = 0, returnPending = 0, totalReturned = 0, totalGiven = 0;
  for (const row of rows) {
    const id = String(row.id);
    const booked = Math.max(0, safeInt(row.booked_qty) - safeInt(row.closed_qty));
    const given = projectedGiven?.has(id) ? safeInt(projectedGiven.get(id)) : safeInt(row.given_qty);
    const returned = projectedReturned?.has(id) ? safeInt(projectedReturned.get(id)) : safeInt(row.returned_qty);
    pickupPending += Math.max(0, booked - given);
    returnPending += Math.max(0, given - returned);
    totalReturned += returned;
    totalGiven += given;
  }
  if (returnPending > 0 && totalReturned > 0) return "PARTIALLY_RETURNED";
  if (pickupPending > 0 && totalGiven > 0) return "PARTIALLY_GIVEN";
  if (pickupPending > 0) return "BOOKED";
  if (returnPending > 0) return "GIVEN";
  if (totalGiven > 0) return "RETURNED";
  return "BOOKED";
}

async function savePickup(request, env, bookingId) {
  const auth = await requirePermission(request, env, "PICKUPS");
  if (auth.response) return auth.response;
  const user = auth.user;
  let body;
  try { body = await request.clone().json(); } catch { return apiJson({ ok: false, error: "INVALID_JSON", message: "Invalid request body." }, 400); }
  const requestKey = String(body?.requestKey || "").trim();
  const notes = String(body?.notes || "").trim().slice(0, 500);
  const lines = parseActionLines(body, "PICKUP");
  if (requestKey.length < 8 || requestKey.length > 120 || !lines) {
    return apiJson({ ok: false, error: "VALIDATION", message: "A valid request key and at least one positive Pickup quantity are required." }, 400);
  }

  const duplicate = await env.DB.prepare("SELECT id FROM pickup_events WHERE request_key=?").bind(requestKey).first();
  if (duplicate) return apiJson({ ok: true, duplicate: true, eventId: duplicate.id, message: "Pickup was already saved." });

  const booking = await env.DB.prepare("SELECT id,customer_id,status,confirmation_state FROM bookings WHERE id=?").bind(bookingId).first();
  if (!booking) return apiJson({ ok: false, error: "NOT_FOUND", message: "Booking not found." }, 404);
  if (String(booking.status) === "CANCELLED") return apiJson({ ok: false, error: "PICKUP_LOCKED", message: "Cancelled booking cannot be picked up." }, 409);
  if (String(booking.confirmation_state || "BOOKED") === "RESERVED") {
    return apiJson({ ok: false, error: "BOOKING_NOT_CONFIRMED", message: "Confirm the booking before pickup." }, 409);
  }

  const itemRows = (await env.DB.prepare("SELECT id,booked_qty,given_qty,returned_qty,closed_qty FROM booking_items WHERE booking_id=?").bind(bookingId).all()).results || [];
  const byId = new Map(itemRows.map(row => [String(row.id), row]));
  for (const line of lines) {
    const row = byId.get(line.bookingItemId);
    if (!row) return apiJson({ ok: false, error: "INVALID_ITEM", message: "One pickup item does not belong to this booking." }, 400);
    const remaining = safeInt(row.booked_qty) - safeInt(row.closed_qty) - safeInt(row.given_qty);
    if (line.quantity > remaining) return apiJson({ ok: false, error: "QTY_EXCEEDS_REMAINING", message: `Pickup quantity exceeds remaining quantity (${remaining}).` }, 409);
  }

  const projectedGiven = new Map(itemRows.map(row => [String(row.id), safeInt(row.given_qty)]));
  for (const line of lines) projectedGiven.set(line.bookingItemId, safeInt(projectedGiven.get(line.bookingItemId)) + line.quantity);
  const pickupPendingAfter = itemRows.reduce((sum, row) => {
    const id = String(row.id);
    return sum + Math.max(0, safeInt(row.booked_qty) - safeInt(row.closed_qty) - safeInt(projectedGiven.get(id)));
  }, 0);
  const pickupEventStatus = pickupPendingAfter > 0 ? "PARTIALLY_GIVEN" : "GIVEN";
  const eventId = crypto.randomUUID();
  const pickupAt = new Date().toISOString();
  const statements = [
    env.DB.prepare("INSERT INTO pickup_events (id,booking_id,given_to_customer_id,handled_by_user_id,pickup_at,notes,request_key) VALUES (?,?,?,?,?,?,?)")
      .bind(eventId, bookingId, booking.customer_id, user.id, pickupAt, notes || null, requestKey)
  ];
  for (const line of lines) {
    statements.push(env.DB.prepare("UPDATE booking_items SET given_qty=given_qty+?,updated_at=CURRENT_TIMESTAMP WHERE id=?").bind(line.quantity, line.bookingItemId));
    statements.push(env.DB.prepare("INSERT INTO pickup_event_items (id,pickup_event_id,booking_item_id,qty_given) VALUES (?,?,?,?)")
      .bind(crypto.randomUUID(), eventId, line.bookingItemId, line.quantity));
  }
  statements.push(env.DB.prepare("INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json) VALUES (?,?,?,?,?,?)")
    .bind(crypto.randomUUID(), user.id, "PICKUP", "BOOKING", bookingId, JSON.stringify({
      eventId, pickupAt, items: lines, movementStatus: pickupEventStatus, status: pickupEventStatus, notes: notes || null
    })));

  try {
    await env.DB.batch(statements);
  } catch (error) {
    const exists = await env.DB.prepare("SELECT id FROM pickup_events WHERE request_key=?").bind(requestKey).first();
    if (exists) return apiJson({ ok: true, duplicate: true, eventId: exists.id, message: "Pickup was already saved." });
    return apiJson({ ok: false, error: "PICKUP_CONFLICT", message: "Pickup quantities changed while saving. Refresh and try again." }, 409);
  }

  const saved = await env.DB.prepare("SELECT status FROM bookings WHERE id=?").bind(bookingId).first();
  const status = String(saved?.status || pickupEventStatus);
  return apiJson({ ok: true, eventId, status, message: pickupEventStatus === "GIVEN" ? "Pickup completed." : "Partial pickup saved." }, 201);
}

async function saveReturn(request, env, bookingId) {
  const auth = await requirePermission(request, env, "RETURNS");
  if (auth.response) return auth.response;
  const user = auth.user;
  let body;
  try { body = await request.clone().json(); } catch { return apiJson({ ok: false, error: "INVALID_JSON", message: "Invalid request body." }, 400); }
  const requestKey = String(body?.requestKey || "").trim();
  const notes = String(body?.notes || "").trim().slice(0, 500);
  const pendingPickupAction = String(body?.pendingPickupAction || "").trim().toUpperCase();
  const lines = parseActionLines(body, "RETURN");
  if (requestKey.length < 8 || requestKey.length > 120 || !lines) {
    return apiJson({ ok: false, error: "VALIDATION", message: "A valid request key and at least one positive Return quantity are required." }, 400);
  }

  const duplicate = await env.DB.prepare("SELECT id FROM return_events WHERE request_key=?").bind(requestKey).first();
  if (duplicate) return apiJson({ ok: true, duplicate: true, eventId: duplicate.id, message: "Return was already saved." });

  const booking = await env.DB.prepare("SELECT id,status FROM bookings WHERE id=?").bind(bookingId).first();
  if (!booking) return apiJson({ ok: false, error: "NOT_FOUND", message: "Booking not found." }, 404);
  if (String(booking.status) === "CANCELLED") return apiJson({ ok: false, error: "RETURN_LOCKED", message: "Cancelled booking cannot be returned." }, 409);

  const itemRows = (await env.DB.prepare("SELECT id,booked_qty,given_qty,returned_qty,closed_qty FROM booking_items WHERE booking_id=?").bind(bookingId).all()).results || [];
  const pickupPending = itemRows.reduce((sum, row) => sum + Math.max(0, safeInt(row.booked_qty) - safeInt(row.closed_qty) - safeInt(row.given_qty)), 0);
  if (pickupPending > 0 && !["KEEP_OPEN", "CLOSE_REMAINING"].includes(pendingPickupAction)) {
    return apiJson({
      ok: false,
      error: "PENDING_PICKUP_DECISION_REQUIRED",
      message: "Choose whether remaining unpicked items should stay open or be closed before saving the return."
    }, 409);
  }

  const byId = new Map(itemRows.map(row => [String(row.id), row]));
  for (const line of lines) {
    const row = byId.get(line.bookingItemId);
    if (!row) return apiJson({ ok: false, error: "INVALID_ITEM", message: "One return item does not belong to this booking." }, 400);
    const pending = safeInt(row.given_qty) - safeInt(row.returned_qty);
    if (line.quantity > pending) return apiJson({ ok: false, error: "QTY_EXCEEDS_PENDING", message: `Return quantity exceeds pending quantity (${pending}).` }, 409);
  }

  const projectedRows = itemRows.map(row => ({ ...row }));
  const closeLines = [];
  if (pickupPending > 0 && pendingPickupAction === "CLOSE_REMAINING") {
    for (const row of projectedRows) {
      const closeQty = Math.max(0, safeInt(row.booked_qty) - safeInt(row.closed_qty) - safeInt(row.given_qty));
      if (closeQty > 0) {
        closeLines.push({ bookingItemId: String(row.id), quantity: closeQty });
        row.closed_qty = safeInt(row.closed_qty) + closeQty;
      }
    }
  }

  const projectedReturned = new Map(projectedRows.map(row => [String(row.id), safeInt(row.returned_qty)]));
  for (const line of lines) projectedReturned.set(line.bookingItemId, safeInt(projectedReturned.get(line.bookingItemId)) + line.quantity);
  const moveStatus = movementStatus(projectedRows, null, projectedReturned);
  const returnEventStatus = moveStatus === "RETURNED" ? "RETURNED" : "PARTIALLY_RETURNED";
  const eventId = crypto.randomUUID();
  const returnAt = new Date().toISOString();
  const statements = [];
  let closeEventId = null;

  if (closeLines.length) {
    closeEventId = crypto.randomUUID();
    statements.push(env.DB.prepare("INSERT INTO booking_close_events (id,booking_id,handled_by_user_id,closed_at,request_key,notes) VALUES (?,?,?,?,?,?)")
      .bind(closeEventId, bookingId, user.id, returnAt, `${requestKey}:close`, "Customer does not need remaining unpicked items."));
    for (const line of closeLines) {
      statements.push(env.DB.prepare("UPDATE booking_items SET closed_qty=closed_qty+?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND booked_qty-closed_qty>given_qty")
        .bind(line.quantity, line.bookingItemId));
      statements.push(env.DB.prepare("INSERT INTO booking_close_event_items (id,close_event_id,booking_item_id,qty_closed) VALUES (?,?,?,?)")
        .bind(crypto.randomUUID(), closeEventId, line.bookingItemId, line.quantity));
    }
    statements.push(env.DB.prepare("INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json) VALUES (?,?,?,?,?,?)")
      .bind(crypto.randomUUID(), user.id, "CLOSE_REMAINING_ITEMS", "BOOKING", bookingId, JSON.stringify({
        eventId: closeEventId, closedAt: returnAt, items: closeLines, reason: "Customer does not need remaining items"
      })));
  }

  statements.push(env.DB.prepare("INSERT INTO return_events (id,booking_id,received_by_user_id,return_at,notes,request_key) VALUES (?,?,?,?,?,?)")
    .bind(eventId, bookingId, user.id, returnAt, notes || null, requestKey));
  for (const line of lines) {
    statements.push(env.DB.prepare("UPDATE booking_items SET returned_qty=returned_qty+?,updated_at=CURRENT_TIMESTAMP WHERE id=?")
      .bind(line.quantity, line.bookingItemId));
    statements.push(env.DB.prepare("INSERT INTO return_event_items (id,return_event_id,booking_item_id,qty_returned,condition_note) VALUES (?,?,?,?,?)")
      .bind(crypto.randomUUID(), eventId, line.bookingItemId, line.quantity, line.conditionNote || null));
  }
  statements.push(env.DB.prepare("INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json) VALUES (?,?,?,?,?,?)")
    .bind(crypto.randomUUID(), user.id, "RETURN", "BOOKING", bookingId, JSON.stringify({
      eventId, returnAt, items: lines, pendingPickupAction: pickupPending > 0 ? pendingPickupAction : null,
      movementStatus: returnEventStatus, status: returnEventStatus, notes: notes || null
    })));

  try {
    await env.DB.batch(statements);
  } catch (error) {
    const exists = await env.DB.prepare("SELECT id FROM return_events WHERE request_key=?").bind(requestKey).first();
    if (exists) return apiJson({ ok: true, duplicate: true, eventId: exists.id, message: "Return was already saved." });
    return apiJson({ ok: false, error: "RETURN_CONFLICT", message: "Return quantities changed while saving. Refresh and try again." }, 409);
  }

  const saved = await env.DB.prepare("SELECT status FROM bookings WHERE id=?").bind(bookingId).first();
  const status = String(saved?.status || moveStatus);
  const message = moveStatus === "RETURNED"
    ? (closeLines.length ? "Return completed and remaining unpicked items were closed." : "Return completed.")
    : (closeLines.length ? "Partial return saved and remaining unpicked items were closed." : "Partial return saved.");
  return apiJson({ ok: true, eventId, closeEventId, status, message }, 201);
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/api/admin/dashboard") {
      return dashboard(request, env);
    }
    if (request.method === "GET" && url.pathname === "/api/admin/bookings") {
      return listBookings(request, env);
    }

    const bookingDetailMatch = url.pathname.match(/^\/api\/admin\/bookings\/([^/]+)$/);
    if (request.method === "GET" && bookingDetailMatch) {
      return augmentBookingDetail(request, env, ctx, decodeURIComponent(bookingDetailMatch[1]));
    }

    const pickupMatch = url.pathname.match(/^\/api\/admin\/pickups\/([^/]+)$/);
    if (request.method === "POST" && pickupMatch) {
      return savePickup(request, env, decodeURIComponent(pickupMatch[1]));
    }

    const returnMatch = url.pathname.match(/^\/api\/admin\/returns\/([^/]+)$/);
    if (request.method === "POST" && returnMatch) {
      return saveReturn(request, env, decodeURIComponent(returnMatch[1]));
    }

    return core.fetch(request, env, ctx);
  }
};
