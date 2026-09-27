import core from "./phase14at-booking-lifecycle.js";
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

function monthBounds(today) {
  const [year, month] = today.split("-").map(Number);
  const start = `${year}-${String(month).padStart(2, "0")}-01`;
  const next = month === 12
    ? `${year + 1}-01-01`
    : `${year}-${String(month + 1).padStart(2, "0")}-01`;
  return { start, next };
}

function parseJsonArray(value) {
  if (Array.isArray(value)) return value;
  if (typeof value !== "string" || !value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

async function dashboardScreen2(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
  if (!hasStaffPermission(user, "DASHBOARD")) {
    return apiJson({ ok: false, error: "FORBIDDEN", message: "Dashboard access is not enabled for this account." }, 403);
  }

  const today = businessToday();
  const month = monthBounds(today);
  const paymentStatusSql = bookingPaymentStatusSql("b");

  // One bundled query owns all 34 fixed KPI values plus dynamic Category-wise Inventory.
  // No queue/detail rows are loaded by Dashboard.
  const row = await env.DB.prepare(`
    WITH booking_rollup AS (
      SELECT
        b.id,
        b.customer_id,
        b.booking_date,
        b.pickup_date,
        b.return_date,
        b.status,
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
        i.id AS item_id,
        i.category_id,
        i.total_quantity,
        COALESCE(SUM(CASE
          WHEN b.id IS NOT NULL
            AND b.status<>'CANCELLED'
            AND (bi.booked_qty-COALESCE(bi.closed_qty,0))>bi.given_qty
          THEN (bi.booked_qty-COALESCE(bi.closed_qty,0))-bi.given_qty
          ELSE 0 END),0) AS pickup_pending_qty,
        COALESCE(SUM(CASE
          WHEN b.id IS NOT NULL
            AND b.status<>'CANCELLED'
            AND bi.given_qty>bi.returned_qty
          THEN bi.given_qty-bi.returned_qty
          ELSE 0 END),0) AS currently_out_qty
      FROM items i
      LEFT JOIN booking_items bi ON bi.item_id=i.id
      LEFT JOIN bookings b ON b.id=bi.booking_id
      WHERE i.archived_at IS NULL AND i.is_active=1
      GROUP BY i.id
    ),
    item_rollup AS (
      SELECT
        item_id,
        category_id,
        total_quantity,
        pickup_pending_qty,
        currently_out_qty,
        MAX(0,total_quantity-pickup_pending_qty-currently_out_qty) AS available_now
      FROM item_flow
    ),
    category_rollup AS (
      SELECT
        c.id AS category_id,
        c.name AS category_name,
        c.display_order,
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
          WHEN br.status<>'CANCELLED'
            AND br.booking_date>='${month.start}'
            AND br.booking_date<'${month.next}'
          THEN 1 ELSE 0 END),0) AS month_bookings,
        MAX(CASE
          WHEN br.status<>'CANCELLED' AND br.pending_return_qty>0
          THEN 1 ELSE 0 END) AS active_rental,
        MAX(CASE
          WHEN br.status<>'CANCELLED' AND (
            (br.confirmation_state<>'RESERVED' AND br.pending_pickup_qty>0 AND br.pickup_date<'${today}')
            OR (br.pending_return_qty>0 AND br.return_date<'${today}')
          )
          THEN 1 ELSE 0 END) AS has_exception
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
        WHERE status<>'CANCELLED' AND confirmation_state='RESERVED'),0) AS reserved_orders,
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
        WHERE month_bookings>0 AND first_booking_date>='${month.start}' AND first_booking_date<'${month.next}'),0) AS new_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup
        WHERE month_bookings>0 AND first_booking_date<'${month.start}'),0) AS returning_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup WHERE total_bookings>=2),0) AS frequent_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup WHERE active_rental=1),0) AS active_rental_customers,
      COALESCE((SELECT COUNT(*) FROM customer_rollup WHERE has_exception=1),0) AS customer_exceptions,

      COALESCE((
        SELECT json_group_array(json_object(
          'categoryId',category_id,
          'categoryName',category_name,
          'items',items,
          'totalQty',total_qty,
          'available',available,
          'pickupPending',pickup_pending,
          'currentlyOut',currently_out
        ))
        FROM (
          SELECT * FROM category_rollup
          ORDER BY display_order ASC,category_name COLLATE NOCASE ASC
        )
      ),'[]') AS category_inventory_json,

      COALESCE((SELECT COUNT(*) FROM booking_rollup
        WHERE booking_date='${today}' AND status<>'CANCELLED'),0) AS today_bookings_count
  `).first();

  if (!row) {
    return apiJson({ ok: false, error: "DASHBOARD_UNAVAILABLE", message: "Unable to load dashboard." }, 500);
  }

  const kpis = {
    todayOverview: {
      todayPickups: Number(row.today_pickups || 0),
      todayReturns: Number(row.today_returns || 0),
      missedPickups: Number(row.missed_pickups || 0),
      overdueReturns: Number(row.overdue_returns || 0)
    },
    bookingStatus: {
      reserved: Number(row.reserved_orders || 0),
      booked: Number(row.booked_orders || 0),
      partPickedUp: Number(row.part_picked_up_orders || 0),
      fullPickedUp: Number(row.full_picked_up_orders || 0),
      partReturn: Number(row.part_return_orders || 0),
      fullReturned: Number(row.full_returned_orders || 0),
      cancelled: Number(row.cancelled_orders || 0),
      activeRentalOrders: Number(row.active_rental_orders || 0)
    },
    paymentBilling: {
      pendingPayment: Number(row.pending_payment || 0),
      partPayment: Number(row.part_payment || 0),
      fullPayment: Number(row.full_payment || 0),
      draftBills: Number(row.draft_bills || 0),
      finalBills: Number(row.final_bills || 0),
      billsToday: Number(row.bills_today || 0),
      pendingBalance: Number(row.pending_balance || 0),
      totalReceived: Number(row.total_received || 0)
    },
    inventory: {
      availableNow: Number(row.available_now || 0),
      pickupPendingQty: Number(row.pickup_pending_qty || 0),
      currentlyOutQty: Number(row.currently_out_qty || 0),
      totalQuantity: Number(row.total_quantity || 0),
      totalItems: Number(row.total_items || 0),
      categories: Number(row.categories || 0),
      lowStock: Number(row.low_stock || 0),
      unavailable: Number(row.unavailable || 0)
    },
    customers: {
      totalCustomers: Number(row.total_customers || 0),
      newCustomers: Number(row.new_customers || 0),
      returningCustomers: Number(row.returning_customers || 0),
      frequentCustomers: Number(row.frequent_customers || 0),
      activeRentalCustomers: Number(row.active_rental_customers || 0),
      customerExceptions: Number(row.customer_exceptions || 0)
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

  return apiJson({
    ok: true,
    today,
    kpis: visibleKpis,
    categoryInventory: access.items ? parseJsonArray(row.category_inventory_json) : [],
    access,

    // Compatibility aliases retained while old consumers/tests are retired.
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
      todayBookings: Number(row.today_bookings_count || 0),
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

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/api/admin/dashboard") {
      return dashboardScreen2(request, env);
    }
    return core.fetch(request, env, ctx);
  }
};
