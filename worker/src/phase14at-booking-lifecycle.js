import core from "./phase14ar-bilingual-branding.js";
import { getSessionUser } from "./auth.ts";
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

function lifecycleStatus(status, confirmationState) {
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

async function confirmationMap(env, ids) {
  const unique = [...new Set((ids || []).map(value => String(value || "").trim()).filter(Boolean))];
  if (!unique.length) return new Map();
  const placeholders = unique.map(() => "?").join(",");
  const rows = await env.DB.prepare(`SELECT id,confirmation_state FROM bookings WHERE id IN (${placeholders})`)
    .bind(...unique).all();
  return new Map((rows.results || []).map(row => [String(row.id), String(row.confirmation_state || "BOOKED")]));
}

async function paymentStatusMap(env, ids) {
  const unique = [...new Set((ids || []).map(value => String(value || "").trim()).filter(Boolean))];
  if (!unique.length) return new Map();
  const placeholders = unique.map(() => "?").join(",");
  const paymentStatusSql = bookingPaymentStatusSql("b");
  const rows = await env.DB.prepare(`
    SELECT b.id, ${paymentStatusSql} AS payment_status
    FROM bookings b
    WHERE b.id IN (${placeholders})
  `).bind(...unique).all();
  return new Map((rows.results || []).map(row => [String(row.id), row.payment_status == null ? null : String(row.payment_status)]));
}

async function paymentTimelineForBooking(env, bookingId) {
  const rows = await env.DB.prepare(`
    SELECT al.id,al.action,al.created_at,al.old_value_json,al.new_value_json,u.name AS user_name
    FROM audit_logs al
    JOIN bills bl ON bl.id=al.record_id
    LEFT JOIN users u ON u.id=al.user_id
    WHERE al.module='BILL' AND bl.booking_id=?
    ORDER BY al.created_at ASC,al.id ASC
  `).bind(bookingId).all();
  return (rows.results || []).map(row => ({
    id:String(row.id || ""),
    action:`PAYMENT_${String(row.action || "UPDATE").toUpperCase()}`,
    created_at:String(row.created_at || ""),
    user_name:row.user_name == null ? null : String(row.user_name),
    old_value_json:row.old_value_json == null ? null : String(row.old_value_json),
    new_value_json:row.new_value_json == null ? null : String(row.new_value_json),
    items:[]
  }));
}

function bookingObjects(data) {
  const objects = [];
  if (data?.booking && typeof data.booking === "object") objects.push(data.booking);
  if (Array.isArray(data?.bookings)) objects.push(...data.bookings);
  for (const key of ["missedPickups", "todayBookings", "todayPickups", "todayReturns", "overdueReturns"]) {
    if (Array.isArray(data?.[key])) objects.push(...data[key]);
  }
  return objects.filter(row => row && typeof row === "object" && row.id);
}

async function augmentBookingResponse(response, env) {
  if (!response.ok || !(response.headers.get("content-type") || "").includes("application/json")) return response;
  let data;
  try { data = await response.clone().json(); } catch { return response; }
  const objects = bookingObjects(data);
  if (!objects.length) return response;
  const ids = objects.map(row => row.id);
  const [states, payments] = await Promise.all([
    confirmationMap(env, ids),
    paymentStatusMap(env, ids)
  ]);
  for (const row of objects) {
    const confirmation = states.get(String(row.id)) || "BOOKED";
    row.confirmation_state = confirmation;
    row.display_status = lifecycleStatus(row.status, confirmation);
    row.payment_status = payments.get(String(row.id)) ?? null;
  }
  if (data?.booking?.id && Array.isArray(data.timeline)) {
    const paymentEvents = await paymentTimelineForBooking(env, String(data.booking.id));
    if (paymentEvents.length) data.timeline = [...data.timeline, ...paymentEvents];
  }
  const headers = new Headers(response.headers);
  headers.delete("content-length");
  headers.set("cache-control", "no-store");
  return new Response(JSON.stringify(data), { status: response.status, statusText: response.statusText, headers });
}

async function requireSession(request, env) {
  const user = await getSessionUser(env, request);
  return user || null;
}

async function confirmBooking(request, env, bookingId) {
  const user = await requireSession(request, env);
  if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
  const booking = await env.DB.prepare(`SELECT id,status,confirmation_state FROM bookings WHERE id=?`).bind(bookingId).first();
  if (!booking) return apiJson({ ok: false, error: "NOT_FOUND", message: "Booking not found." }, 404);
  if (String(booking.status) !== "BOOKED") {
    return apiJson({ ok: false, error: "BOOKING_LOCKED", message: "Booking cannot be confirmed after pickup has started or after it is closed." }, 409);
  }
  if (String(booking.confirmation_state || "BOOKED") === "BOOKED") {
    return apiJson({ ok: true, duplicate: true, id: bookingId, confirmationState: "BOOKED", displayStatus: "BOOKED", message: "Booking is already confirmed." });
  }
  const confirmedAt = new Date().toISOString();
  const result = await env.DB.batch([
    env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json)
      SELECT ?,?,?,?,?,?,? WHERE EXISTS (
        SELECT 1 FROM bookings WHERE id=? AND status='BOOKED' AND confirmation_state='RESERVED'
      )`).bind(
        crypto.randomUUID(), user.id, "CONFIRM_BOOKING", "BOOKING", bookingId,
        JSON.stringify({ confirmationState: "RESERVED" }), JSON.stringify({ confirmationState: "BOOKED" }), bookingId
      ),
    env.DB.prepare(`UPDATE bookings SET confirmation_state='BOOKED',updated_at=? WHERE id=? AND status='BOOKED' AND confirmation_state='RESERVED'`)
      .bind(confirmedAt, bookingId)
  ]);
  if (Number(result?.[1]?.meta?.changes || 0) === 0) {
    const current = await env.DB.prepare(`SELECT status,confirmation_state FROM bookings WHERE id=?`).bind(bookingId).first();
    if (String(current?.status || "") === "BOOKED" && String(current?.confirmation_state || "BOOKED") === "BOOKED") {
      return apiJson({ ok: true, duplicate: true, id: bookingId, confirmationState: "BOOKED", displayStatus: "BOOKED", message: "Booking is already confirmed." });
    }
    return apiJson({ ok: false, error: "BOOKING_LOCKED", message: "Booking cannot be confirmed after pickup has started or after it is closed." }, 409);
  }
  return apiJson({ ok: true, id: bookingId, confirmationState: "BOOKED", displayStatus: "BOOKED", message: "Booking confirmed." });
}

async function createWithConfirmation(request, env, ctx) {
  let body = null;
  try { body = await request.clone().json(); } catch {}
  const requested = String(body?.confirmationState || "BOOKED").trim().toUpperCase();
  if (!new Set(["RESERVED", "BOOKED"]).has(requested)) {
    return apiJson({ ok: false, error: "VALIDATION", message: "Invalid booking confirmation state." }, 400);
  }

  // Core create persists confirmation_state in the same D1 batch as the booking/items/audit.
  // This removes the former BOOKED -> RESERVED second-phase partial-commit window.
  const response = await core.fetch(request, env, ctx);
  if (!response.ok || !(response.headers.get("content-type") || "").includes("application/json")) return response;
  let data;
  try { data = await response.clone().json(); } catch { return response; }
  if (!data?.id) return response;

  const row = await env.DB.prepare(`SELECT status,confirmation_state FROM bookings WHERE id=?`).bind(String(data.id)).first();
  const confirmation = String(row?.confirmation_state || data.confirmationState || requested || "BOOKED");
  data.confirmationState = confirmation;
  data.displayStatus = lifecycleStatus(row?.status, confirmation);
  const headers = new Headers(response.headers);
  headers.delete("content-length");
  headers.set("cache-control", "no-store");
  return new Response(JSON.stringify(data), { status: response.status, statusText: response.statusText, headers });
}

async function reservedPickupGuard(request, env) {
  const match = new URL(request.url).pathname.match(/^\/api\/admin\/pickups\/([^/]+)$/);
  if (!match || request.method !== "POST") return null;
  const bookingId = decodeURIComponent(match[1]);
  const row = await env.DB.prepare(`SELECT confirmation_state FROM bookings WHERE id=?`).bind(bookingId).first();
  if (row && String(row.confirmation_state || "BOOKED") === "RESERVED") {
    return apiJson({ ok: false, error: "BOOKING_NOT_CONFIRMED", message: "Confirm the booking before pickup." }, 409);
  }
  return null;
}

// Chain marker for cumulative hardening: return core.fetch(request, env, ctx);
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const confirmMatch = url.pathname.match(/^\/api\/admin\/bookings\/([^/]+)\/confirm$/);
    if (confirmMatch && request.method === "POST") {
      return confirmBooking(request, env, decodeURIComponent(confirmMatch[1]));
    }

    if (url.pathname === "/api/admin/bookings" && request.method === "POST") {
      return createWithConfirmation(request, env, ctx);
    }

    const blockedPickup = await reservedPickupGuard(request, env);
    if (blockedPickup) return blockedPickup;

    const response = await core.fetch(request, env, ctx);
    if (
      request.method === "GET" &&
      (url.pathname === "/api/admin/dashboard" ||
       url.pathname === "/api/admin/bookings" ||
       url.pathname === "/api/admin/bookings/bootstrap" ||
       /^\/api\/admin\/bookings\/[^/]+$/.test(url.pathname))
    ) {
      return augmentBookingResponse(response, env);
    }
    return response;
  }
};
