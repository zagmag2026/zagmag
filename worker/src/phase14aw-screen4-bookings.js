import core from "./phase14av-customers-screen3.js";
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

function text(value, max = 500) {
  return String(value ?? "").trim().slice(0, max);
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

async function listBookingsScreen4(request, env) {
  const url = new URL(request.url);
  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(10, Math.max(1, Number(url.searchParams.get("pageSize") || "10") || 10));
  const search = text(url.searchParams.get("search"), 120);
  const status = text(url.searchParams.get("status"), 40).toUpperCase();
  const view = text(url.searchParams.get("view"), 40).toUpperCase() || "ALL";
  const sort = text(url.searchParams.get("sort"), 40).toUpperCase() || "NEWEST";
  const today = businessToday();
  const paymentStatusSql = bookingPaymentStatusSql("b");
  const where = ["c.archived_at IS NULL"];
  const binds = [];

  if (search) {
    const q = `%${search}%`;
    where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR COALESCE(c.alternate_mobile,'') LIKE ? OR EXISTS (
      SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id
      WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))
    ))`);
    binds.push(q, q, q, q, q, q);
  }

  const pendingPickupExists = `EXISTS (
    SELECT 1 FROM booking_items bp
    WHERE bp.booking_id=b.id
      AND (bp.booked_qty-COALESCE(bp.closed_qty,0))>bp.given_qty
  )`;
  const pendingReturnExists = `EXISTS (
    SELECT 1 FROM booking_items br
    WHERE br.booking_id=b.id AND br.given_qty>br.returned_qty
  )`;

  if (view === "RESERVED") {
    where.push("b.status='BOOKED' AND COALESCE(b.confirmation_state,'BOOKED')='RESERVED'");
  } else if (view === "BOOKED") {
    where.push(`b.status<>'CANCELLED' AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED'
      AND ${pendingPickupExists}
      AND NOT EXISTS (SELECT 1 FROM booking_items bg WHERE bg.booking_id=b.id AND bg.given_qty>0)`);
  } else if (view === "PART_PICKUP") {
    where.push(`b.status<>'CANCELLED' AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED'
      AND ${pendingPickupExists}
      AND EXISTS (SELECT 1 FROM booking_items bg WHERE bg.booking_id=b.id AND bg.given_qty>0)
      AND NOT (
        ${pendingReturnExists}
        AND EXISTS (SELECT 1 FROM booking_items rr WHERE rr.booking_id=b.id AND rr.returned_qty>0)
      )`);
  } else if (view === "FULL_PICKUP") {
    where.push(`b.status<>'CANCELLED' AND NOT (${pendingPickupExists}) AND ${pendingReturnExists}
      AND NOT EXISTS (SELECT 1 FROM booking_items rr WHERE rr.booking_id=b.id AND rr.returned_qty>0)`);
  } else if (view === "PART_RETURN") {
    where.push(`b.status<>'CANCELLED' AND ${pendingReturnExists}
      AND EXISTS (SELECT 1 FROM booking_items rr WHERE rr.booking_id=b.id AND rr.returned_qty>0)`);
  } else if (view === "FULL_RETURN") {
    where.push(`b.status<>'CANCELLED' AND NOT (${pendingPickupExists}) AND NOT (${pendingReturnExists})
      AND EXISTS (SELECT 1 FROM booking_items bg WHERE bg.booking_id=b.id AND bg.given_qty>0)`);
  } else if (view === "TODAY_PICKUP") {
    where.push(`b.pickup_date=? AND b.status<>'CANCELLED'
      AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED' AND ${pendingPickupExists}`);
    binds.push(today);
  } else if (view === "TODAY_RETURN") {
    where.push(`b.return_date=? AND b.status<>'CANCELLED' AND ${pendingReturnExists}`);
    binds.push(today);
  } else if (view === "MISSED_PICKUP") {
    where.push(`b.pickup_date<? AND b.status<>'CANCELLED'
      AND COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED' AND ${pendingPickupExists}`);
    binds.push(today);
  } else if (view === "ACTIVE_RENTAL") {
    where.push(`b.status<>'CANCELLED' AND ${pendingReturnExists}`);
  } else if (view === "OVERDUE") {
    where.push(`b.return_date < ? AND b.status<>'CANCELLED' AND ${pendingReturnExists}`);
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
             COALESCE(b.advance_amount,0) AS advance_amount,
             (SELECT pb.id FROM bills pb WHERE pb.booking_id=b.id ORDER BY CASE WHEN pb.status<>'CANCELLED' THEN 0 ELSE 1 END,pb.created_at DESC LIMIT 1) AS bill_id,
             ${paymentStatusSql} AS payment_status,
             c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,c.address AS customer_address,c.alternate_mobile AS customer_alternate_mobile,u.name AS booked_by,
             COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items_summary,
             COALESCE((SELECT COUNT(*) FROM booking_items bic WHERE bic.booking_id=b.id),0) AS item_count,
             COALESCE((
               SELECT json_group_array(json_object(
                 'item_code',p.item_code,
                 'item_name',p.item_name,
                 'category_name',p.category_name,
                 'image_url',p.image_url,
                 'quantity',p.booked_qty,
                 'given_qty',p.given_qty,
                 'returned_qty',p.returned_qty
               ))
               FROM (
                 SELECT i.item_code,i.item_name,c2.name AS category_name,bi.booked_qty,bi.given_qty,bi.returned_qty,
                        (SELECT im.image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order ASC LIMIT 1) AS image_url
                 FROM booking_items bi
                 JOIN items i ON i.id=bi.item_id
                 JOIN categories c2 ON c2.id=i.category_id
                 WHERE bi.booking_id=b.id
                 ORDER BY i.item_name COLLATE NOCASE ASC
                 LIMIT 30
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

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname === "/api/admin/bookings" && request.method === "GET") {
      const user = await getSessionUser(env, request);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      if (!hasStaffPermission(user, "BOOKINGS")) {
        return apiJson({ ok: false, error: "FORBIDDEN", message: "Bookings access is not enabled for this account." }, 403);
      }
      return listBookingsScreen4(request, env);
    }
    return core.fetch(request, env, ctx);
  }
};
