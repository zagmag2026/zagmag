import core from "./phase14e.js";
import { getSessionUser } from "./auth.ts";

const PUBLIC_SHELL_MARKER = "worker-r1";
const BUSINESS_TZ = "Asia/Kolkata";
const IST_OFFSET_MINUTES = 330;
const STRICT_DATE_PATHS = new Set([
  "POST /api/admin/bookings",
  "POST /api/admin/bookings/availability",
  "POST /api/public/availability"
]);

function isStrictDateRequest(request, url) {
  if (STRICT_DATE_PATHS.has(`${request.method} ${url.pathname}`)) return true;
  return request.method === "PUT" && /^\/api\/admin\/bookings\/[^/]+$/.test(url.pathname);
}

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
    return new Intl.DateTimeFormat("en-CA", {
      timeZone: BUSINESS_TZ,
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    }).format(new Date());
  } catch {
    return new Date().toISOString().slice(0, 10);
  }
}

function validDateOnly(value) {
  const raw = String(value || "");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) return false;
  const date = new Date(raw + "T00:00:00Z");
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === raw;
}

function nextDateOnly(value) {
  if (!validDateOnly(value)) return "";
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  date.setUTCDate(date.getUTCDate() + 1);
  return date.toISOString().slice(0, 10);
}

function businessDayStartUtcSql(value) {
  if (!validDateOnly(value)) return "";
  const [year, month, day] = value.split("-").map(Number);
  const utcMs = Date.UTC(year, month - 1, day, 0, 0, 0) - IST_OFFSET_MINUTES * 60 * 1000;
  return new Date(utcMs).toISOString().replace("T", " ").slice(0, 19);
}

function parseDisplayPreferences(value) {
  let raw = {};
  try { raw = typeof value === "string" && value ? JSON.parse(value) : (value || {}); } catch {}
  return {
    defaultLanguage: raw?.defaultLanguage === "EN" ? "EN" : "GU",
    dateFormat: raw?.dateFormat === "YYYY-MM-DD" ? "YYYY-MM-DD" : "DD-MM-YYYY",
    timeZone: BUSINESS_TZ
  };
}

async function readDisplayPreferences(env) {
  const row = await env.DB.prepare(`SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1`).first();
  return parseDisplayPreferences(row?.value_json);
}

async function enhanceAuthResponse(request, env, ctx) {
  const response = await core.fetch(request, env, ctx);
  if (!response.ok) return response;
  let data;
  try { data = await response.clone().json(); } catch { return response; }
  if (!data || data.ok === false || !data.user) return response;
  data.displayPreferences = await readDisplayPreferences(env);
  const headers = new Headers(response.headers);
  headers.delete("content-length");
  headers.set("cache-control", "no-store");
  return new Response(JSON.stringify(data), {
    status: response.status,
    statusText: response.statusText,
    headers
  });
}

async function listAuditLogsBusinessTime(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return secureJson(request, env, { ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, { status: 401 });
  if (user.role !== "OWNER" && user.role !== "ADMIN") return secureJson(request, env, { ok: false, error: "FORBIDDEN", message: "You do not have permission for this action." }, { status: 403 });

  const url = new URL(request.url);
  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(50, Math.max(10, Number(url.searchParams.get("pageSize") || "20") || 20));
  const module = (url.searchParams.get("module") || "").trim();
  const action = (url.searchParams.get("action") || "").trim();
  const userId = (url.searchParams.get("userId") || "").trim();
  const search = (url.searchParams.get("search") || "").trim();
  const fromDate = (url.searchParams.get("fromDate") || "").trim();
  const toDate = (url.searchParams.get("toDate") || "").trim();

  const where = ["1=1"];
  const binds = [];
  if (module) { where.push("a.module=?"); binds.push(module); }
  if (action) { where.push("a.action=?"); binds.push(action); }
  if (userId) { where.push("a.user_id=?"); binds.push(userId); }
  if (validDateOnly(fromDate)) {
    where.push("a.created_at>=?");
    binds.push(businessDayStartUtcSql(fromDate));
  }
  if (validDateOnly(toDate)) {
    where.push("a.created_at<?");
    binds.push(businessDayStartUtcSql(nextDateOnly(toDate)));
  }
  if (search) {
    where.push(`(LOWER(COALESCE(u.name,'')) LIKE LOWER(?) OR LOWER(COALESCE(a.record_id,'')) LIKE LOWER(?) OR LOWER(COALESCE(a.old_value_json,'')) LIKE LOWER(?) OR LOWER(COALESCE(a.new_value_json,'')) LIKE LOWER(?))`);
    const q = `%${search}%`;
    binds.push(q, q, q, q);
  }

  const clause = where.join(" AND ");
  const [countRes, rowsRes] = await env.DB.batch([
    env.DB.prepare(`SELECT COUNT(*) AS count FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`SELECT a.id,a.user_id,a.action,a.module,a.record_id,a.old_value_json,a.new_value_json,a.created_at,u.name AS user_name,u.role AS user_role
      FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id WHERE ${clause} ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?`).bind(...binds, pageSize, (page - 1) * pageSize)
  ]);
  const total = Number(countRes.results?.[0]?.count || 0);
  return secureJson(request, env, {
    ok: true,
    logs: rowsRes.results || [],
    pagination: { page, pageSize, total, pages: Math.max(1, Math.ceil(total / pageSize)) },
    displayTimeZone: BUSINESS_TZ
  });
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

async function missedPickupData(env, options = {}) {
  const today = businessToday();
  const page = Math.max(1, Number(options.page || 1) || 1);
  const pageSize = Math.min(50, Math.max(1, Number(options.pageSize || 20) || 20));
  const search = String(options.search || "").trim();
  const where = ["b.status IN ('BOOKED','PARTIALLY_GIVEN')", "b.pickup_date < ?"];
  const binds = [today];
  if (search) {
    const q = `%${search}%`;
    where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR EXISTS (
      SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id
      WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))
    ))`);
    binds.push(q, q, q, q, q);
  }
  const remainingExpr = `COALESCE((SELECT SUM(CASE WHEN bi.booked_qty>bi.given_qty THEN bi.booked_qty-bi.given_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=b.id),0)`;
  const rows = await env.DB.prepare(`
    WITH eligible AS (
      SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.created_at,
        c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,
        COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items_summary,
        COALESCE((SELECT SUM(bi.booked_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS booked_qty,
        COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS given_qty,
        ${remainingExpr} AS remaining_qty,
        COALESCE((SELECT MAX(pe.pickup_at) FROM pickup_events pe WHERE pe.booking_id=b.id),NULL) AS last_pickup_at,
        COALESCE((SELECT json_group_array(json_object(
          'booking_item_id',x.booking_item_id,'item_id',x.item_id,'item_code',x.item_code,'item_name',x.item_name,
          'category_name',x.category_name,'booked_qty',x.booked_qty,'given_qty',x.given_qty,'returned_qty',x.returned_qty,
          'remaining_to_give',x.remaining_to_give,'image_url',x.image_url
        )) FROM (
          SELECT bi.id AS booking_item_id,bi.item_id,i.item_code,i.item_name,cat.name AS category_name,
            bi.booked_qty,bi.given_qty,bi.returned_qty,MAX(0,bi.booked_qty-bi.given_qty) AS remaining_to_give,
            (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order LIMIT 1) AS image_url
          FROM booking_items bi JOIN items i ON i.id=bi.item_id JOIN categories cat ON cat.id=i.category_id
          WHERE bi.booking_id=b.id ORDER BY i.item_name COLLATE NOCASE
        ) x),'[]') AS item_lines_json
      FROM bookings b JOIN customers c ON c.id=b.customer_id
      WHERE ${where.join(" AND ")}
    )
    SELECT *,COUNT(*) OVER() AS missed_total,
      CAST(julianday(?) - julianday(pickup_date) AS INTEGER) AS missed_days
    FROM eligible
    WHERE remaining_qty>0
    ORDER BY pickup_date ASC,created_at ASC
    LIMIT ? OFFSET ?
  `).bind(...binds, today, pageSize, (page - 1) * pageSize).all();
  const raw = rows.results || [];
  const total = Number(raw[0]?.missed_total || 0);
  const pickups = raw.map(row => {
    const { missed_total, item_lines_json, ...rest } = row;
    return {
      ...rest,
      booked_qty: Number(rest.booked_qty || 0),
      given_qty: Number(rest.given_qty || 0),
      remaining_qty: Number(rest.remaining_qty || 0),
      missed_days: Math.max(1, Number(rest.missed_days || 0)),
      item_lines: parseJsonArray(item_lines_json).map(line => ({
        ...line,
        booked_qty: Number(line.booked_qty || 0),
        given_qty: Number(line.given_qty || 0),
        returned_qty: Number(line.returned_qty || 0),
        remaining_to_give: Number(line.remaining_to_give || 0)
      }))
    };
  });
  return { pickups, total, today, page, pageSize, totalPages: Math.max(1, Math.ceil(total / pageSize)) };
}

async function missedPickupList(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return secureJson(request, env, { ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, { status: 401 });
  const url = new URL(request.url);
  const data = await missedPickupData(env, {
    page: url.searchParams.get("page"),
    pageSize: url.searchParams.get("pageSize"),
    search: url.searchParams.get("search")
  });
  return secureJson(request, env, {
    ok: true,
    pickups: data.pickups,
    pagination: { page: data.page, pageSize: data.pageSize, total: data.total, totalPages: data.totalPages },
    today: data.today,
    view: "missed"
  });
}

async function enhanceDashboard(request, env, ctx) {
  const response = await core.fetch(request, env, ctx);
  if (!response.ok) return response;
  let data;
  try { data = await response.clone().json(); } catch { return response; }
  if (!data || data.ok === false) return response;
  const missed = await missedPickupData(env, { page: 1, pageSize: 8, search: "" });
  data.summary = { ...(data.summary || {}), missedPickups: missed.total };
  data.missedPickups = missed.pickups;
  const headers = new Headers(response.headers);
  headers.delete("content-length");
  headers.set("cache-control", "no-store");
  return new Response(JSON.stringify(data), { status: response.status, statusText: response.statusText, headers });
}

function dateRangeError(request, env) {
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
  return new Response(JSON.stringify({
    ok: false,
    error: "INVALID_RENTAL_DATE_RANGE",
    message: "Return Date must be at least one day after Pickup Date."
  }), { status: 400, headers });
}

async function strictDateValidation(request) {
  const contentType = (request.headers.get("content-type") || "").toLowerCase();
  if (!contentType.includes("application/json")) return null;
  try {
    const body = await request.clone().json();
    const pickupDate = typeof body?.pickupDate === "string" ? body.pickupDate.trim() : "";
    const returnDate = typeof body?.returnDate === "string" ? body.returnDate.trim() : "";
    if (!pickupDate || !returnDate) return null;
    if (!validDateOnly(pickupDate) || !validDateOnly(returnDate)) {
      return { error: "INVALID_RENTAL_DATE", message: "Select valid Pickup and Return dates." };
    }
    if (pickupDate < businessToday()) {
      return { error: "PAST_PICKUP_DATE", message: "Past pickup dates are not allowed." };
    }
    if (returnDate <= pickupDate) {
      return { error: "INVALID_RENTAL_DATE_RANGE", message: "Return Date must be at least one day after Pickup Date." };
    }
    return null;
  } catch {
    return null;
  }
}

async function hasInvalidStrictRange(request) {
  const validation = await strictDateValidation(request);
  return validation?.error === "INVALID_RENTAL_DATE_RANGE";
}

async function servePublicShell(request, env) {
  if (!env.ASSETS || typeof env.ASSETS.fetch !== "function") return null;
  const sourceUrl = new URL(request.url);
  const assetUrl = new URL("/index.html", sourceUrl.origin);
  const assetRequest = new Request(assetUrl.toString(), {
    method: request.method === "HEAD" ? "HEAD" : "GET",
    headers: { accept: request.headers.get("accept") || "text/html" }
  });
  const assetResponse = await env.ASSETS.fetch(assetRequest);
  const headers = new Headers(assetResponse.headers);
  headers.set("cache-control", "no-store, no-cache, must-revalidate, max-age=0");
  headers.set("cdn-cache-control", "no-store");
  headers.set("cloudflare-cdn-cache-control", "no-store");
  headers.set("pragma", "no-cache");
  headers.set("expires", "0");
  headers.set("x-zhagmag-shell", PUBLIC_SHELL_MARKER);
  headers.delete("etag");
  return new Response(request.method === "HEAD" ? null : assetResponse.body, {
    status: assetResponse.status,
    statusText: assetResponse.statusText,
    headers
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/api/admin/audit-logs") {
      return listAuditLogsBusinessTime(request, env);
    }
    if ((request.method === "GET" && url.pathname === "/api/auth/me") || (request.method === "POST" && url.pathname === "/api/auth/login")) {
      return enhanceAuthResponse(request, env, ctx);
    }
    if (isStrictDateRequest(request, url)) {
      const validation = await strictDateValidation(request);
      if (validation) return secureJson(request, env, { ok: false, ...validation }, { status: 400 });
    }
    if (request.method === "GET" && url.pathname === "/api/admin/pickups" && url.searchParams.get("view") === "missed") {
      return missedPickupList(request, env);
    }
    if (request.method === "GET" && url.pathname === "/api/admin/dashboard") {
      return enhanceDashboard(request, env, ctx);
    }
    if ((request.method === "GET" || request.method === "HEAD") && (url.pathname === "/" || url.pathname === "/index.html")) {
      const shell = await servePublicShell(request, env);
      if (shell) return shell;
    }
    return core.fetch(request, env, ctx);
  }
};
