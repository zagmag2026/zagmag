import core from "./phase14o.js";
import { getSessionUser } from "./auth.ts";

const BUSINESS_TZ = "Asia/Kolkata";

function allowedOrigin(request, env) {
  const origin = request.headers.get("origin");
  if (!origin) return null;
  const self = new URL(request.url).origin;
  if (origin === self) return origin;
  const allowed = String(env.ALLOWED_ORIGINS || "").split(",").map(v => v.trim()).filter(Boolean);
  return allowed.includes(origin) ? origin : null;
}

function secureJson(request, env, data, init = {}) {
  const headers = new Headers({
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "referrer-policy": "no-referrer"
  });
  const origin = allowedOrigin(request, env);
  if (origin) {
    headers.set("access-control-allow-origin", origin);
    headers.set("access-control-allow-credentials", "true");
    headers.set("vary", "Origin");
  }
  return new Response(JSON.stringify(data), { ...init, headers });
}

function businessToday() {
  try {
    return new Intl.DateTimeFormat("en-CA", { timeZone: BUSINESS_TZ, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
  } catch {
    return new Date().toISOString().slice(0, 10);
  }
}

async function listPartiallyReturned(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return secureJson(request, env, { ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, { status: 401 });

  const url = new URL(request.url);
  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(50, Math.max(5, Number(url.searchParams.get("pageSize") || "20") || 20));
  const search = (url.searchParams.get("search") || "").trim();
  const today = businessToday();
  const pendingExpr = `COALESCE((SELECT SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=b.id),0)`;
  const givenExpr = `COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0)`;
  const where = [`b.status='PARTIALLY_RETURNED'`, `${givenExpr}>0`, `${pendingExpr}>0`];
  const binds = [];
  if (search) {
    const q = `%${search}%`;
    where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))))`);
    binds.push(q, q, q, q, q);
  }
  const clause = where.join(" AND ");
  const rows = await env.DB.prepare(`SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.created_at,c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,
    COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.given_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id AND bi.given_qty>0),'') AS items_summary,
    ${givenExpr} AS given_qty,
    COALESCE((SELECT SUM(bi.returned_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS returned_qty,
    ${pendingExpr} AS pending_qty,
    (SELECT MAX(re.return_at) FROM return_events re WHERE re.booking_id=b.id) AS last_return_at,
    CASE WHEN b.return_date < ? AND ${pendingExpr}>0 THEN CAST(julianday(?) - julianday(b.return_date) AS INTEGER) ELSE 0 END AS overdue_days,
    COUNT(*) OVER() AS partial_total
    FROM bookings b JOIN customers c ON c.id=b.customer_id
    WHERE ${clause}
    ORDER BY CASE WHEN b.return_date<? AND ${pendingExpr}>0 THEN 0 ELSE 1 END,b.return_date ASC,b.created_at DESC
    LIMIT ? OFFSET ?`)
    .bind(today, today, ...binds, today, pageSize, (page - 1) * pageSize).all();

  const raw = rows.results || [];
  const total = raw.length ? Number(raw[0].partial_total || 0) : 0;
  const returns = raw.map(row => {
    const { partial_total, ...rest } = row;
    return rest;
  });
  return secureJson(request, env, {
    ok: true,
    returns,
    pagination: { page, pageSize, total, pages: Math.max(1, Math.ceil(total / pageSize)) },
    view: "partial"
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/api/admin/returns" && url.searchParams.get("view") === "partial") {
      return listPartiallyReturned(request, env);
    }
    return core.fetch(request, env, ctx);
  }
};
