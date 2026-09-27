import core from "./phase14al-sequential-weekday.js";

const SETTINGS_SELECT = /SELECT\s+value_json(?:\s*,\s*updated_at)?\s+FROM\s+settings\s+WHERE\s+key=['"]site_settings['"]/i;
const SETTINGS_WRITE = /INSERT\s+INTO\s+settings\s*\([^)]*key[^)]*value_json/i;
const CACHE_TTL_MS = 6 * 60 * 60 * 1000;
const ADMIN_SHELL_MARKER = "worker-ar-admin-r1";
let cachedSettings = null;
let cachedAt = 0;

function text(value, fallback = "") {
  const out = typeof value === "string" ? value.trim() : "";
  return out || fallback;
}
function limited(value, fallback = "") {
  return text(value, fallback).slice(0, 160);
}
function optionalHttpsUrl(value) {
  const out = text(value);
  if (!out) return "";
  try {
    const url = new URL(out);
    return url.protocol === "https:" ? url.toString().slice(0, 2048) : "";
  } catch {
    return "";
  }
}
function hasGujarati(value) {
  return /[\u0A80-\u0AFF]/.test(String(value || ""));
}
function parseRawSettings(value, env) {
  let raw = {};
  try { raw = typeof value === "string" ? JSON.parse(value || "{}") : (value || {}); } catch {}
  const canonicalShop = limited(raw.shopName, env.APP_NAME || "ઝગમગ ડ્રેસીસ");
  const canonicalTitle = limited(raw.websiteTitle, canonicalShop);
  const shopNameGu = limited(raw.shopNameGu, hasGujarati(canonicalShop) ? canonicalShop : "ઝગમગ ડ્રેસીસ");
  const shopNameEn = limited(raw.shopNameEn, !hasGujarati(canonicalShop) ? canonicalShop : "Zhagmag Dresses");
  const websiteTitleGu = limited(raw.websiteTitleGu, hasGujarati(canonicalTitle) ? canonicalTitle : shopNameGu);
  const websiteTitleEn = limited(raw.websiteTitleEn, !hasGujarati(canonicalTitle) ? canonicalTitle : shopNameEn);
  const logoUrl = optionalHttpsUrl(raw.logoUrl || raw.shopLogoUrl);
  return {
    ...raw,
    shopName: canonicalShop,
    websiteTitle: canonicalTitle,
    shopNameGu,
    shopNameEn,
    websiteTitleGu,
    websiteTitleEn,
    logoUrl,
    defaultLanguage: raw.defaultLanguage === "EN" ? "EN" : "GU",
    dateFormat: raw.dateFormat === "YYYY-MM-DD" ? "YYYY-MM-DD" : "DD-MM-YYYY"
  };
}
function brandingFromRaw(raw, env) {
  const value = parseRawSettings(raw, env);
  return {
    shopNameGu: value.shopNameGu,
    shopNameEn: value.shopNameEn,
    websiteTitleGu: value.websiteTitleGu,
    websiteTitleEn: value.websiteTitleEn,
    logoUrl: value.logoUrl,
    fallbackShopName: value.shopName,
    fallbackWebsiteTitle: value.websiteTitle
  };
}
function displayPreferencesFromRaw(raw, env) {
  const value = parseRawSettings(raw, env);
  return { defaultLanguage: value.defaultLanguage, dateFormat: value.dateFormat, timeZone: "Asia/Kolkata" };
}
function remember(raw, env) {
  cachedSettings = parseRawSettings(raw, env);
  cachedAt = Date.now();
  return cachedSettings;
}
function settingsValueFromRow(row) {
  return row && typeof row.value_json === "string" ? row.value_json : null;
}
function captureResult(sql, result, state, env) {
  if (!SETTINGS_SELECT.test(String(sql || ""))) return;
  const row = Array.isArray(result?.results) ? result.results[0] : result;
  const value = settingsValueFromRow(row);
  if (value) {
    state.settings = remember(value, env);
  }
}
function patchSettingsJson(value, patch, env) {
  let raw = {};
  try { raw = JSON.parse(String(value || "{}")); } catch {}
  if (patch) {
    raw.shopNameGu = limited(patch.shopNameGu, raw.shopNameGu || raw.shopName || "ઝગમગ ડ્રેસીસ");
    raw.shopNameEn = limited(patch.shopNameEn, raw.shopNameEn || "Zhagmag Dresses");
    raw.websiteTitleGu = limited(patch.websiteTitleGu, raw.websiteTitleGu || raw.websiteTitle || raw.shopNameGu);
    raw.websiteTitleEn = limited(patch.websiteTitleEn, raw.websiteTitleEn || raw.shopNameEn);
    if (Object.prototype.hasOwnProperty.call(patch, "logoUrl")) raw.logoUrl = optionalHttpsUrl(patch.logoUrl);
  }
  return parseRawSettings(raw, env);
}
function wrapStatement(native, sql, state, env, bound = []) {
  return {
    __phase14ar: { native, sql: String(sql || ""), bound },
    bind(...values) {
      let next = values;
      if (SETTINGS_WRITE.test(String(sql || "")) && state.brandingPatch && typeof values[0] === "string") {
        const merged = patchSettingsJson(values[0], state.brandingPatch, env);
        next = [JSON.stringify(merged), ...values.slice(1)];
        state.settings = merged;
      }
      return wrapStatement(native.bind(...next), sql, state, env, next);
    },
    async first(...args) {
      const result = await native.first(...args);
      captureResult(sql, result, state, env);
      return result;
    },
    async all(...args) {
      const result = await native.all(...args);
      captureResult(sql, result, state, env);
      return result;
    },
    run(...args) { return native.run(...args); },
    raw(...args) { return native.raw(...args); }
  };
}
function proxyDb(realDb, state, env) {
  return new Proxy(realDb, {
    get(target, property, receiver) {
      if (property === "prepare") return sql => wrapStatement(target.prepare(sql), sql, state, env);
      if (property === "batch") return async statements => {
        const entries = statements.map(statement => statement?.__phase14ar || { native: statement, sql: "", bound: [] });
        const results = await target.batch(entries.map(entry => entry.native));
        entries.forEach((entry, index) => captureResult(entry.sql, results[index], state, env));
        return results;
      };
      const value = Reflect.get(target, property, receiver);
      return typeof value === "function" ? value.bind(target) : value;
    }
  });
}
function proxyEnv(env, state) {
  const db = proxyDb(env.DB, state, env);
  return new Proxy(env, {
    get(target, property, receiver) {
      if (property === "DB") return db;
      return Reflect.get(target, property, receiver);
    }
  });
}
function requestBranding(body) {
  if (!body || typeof body !== "object") return null;
  const hasLogoUrl = Object.prototype.hasOwnProperty.call(body, "logoUrl");
  const values = {
    shopNameGu: limited(body.shopNameGu),
    shopNameEn: limited(body.shopNameEn),
    websiteTitleGu: limited(body.websiteTitleGu),
    websiteTitleEn: limited(body.websiteTitleEn),
    ...(hasLogoUrl ? { logoUrl: optionalHttpsUrl(body.logoUrl) } : {})
  };
  return Object.values(values).some(Boolean) || hasLogoUrl ? values : null;
}
async function cachedOrReadSettings(env, state) {
  if (state.settings) return state.settings;
  if (cachedSettings && Date.now() - cachedAt < CACHE_TTL_MS) return cachedSettings;
  const row = await env.DB.prepare(`SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1`).first();
  return remember(row?.value_json || "{}", env);
}
function needsBranding(pathname) {
  return pathname === "/api/public/catalog" || pathname === "/api/public/bootstrap" || pathname === "/api/admin/settings/bootstrap" || pathname === "/api/admin/settings" || pathname.startsWith("/api/auth/");
}
async function rewriteResponse(request, response, env, state) {
  const url = new URL(request.url);
  if (!needsBranding(url.pathname) || !(response.headers.get("content-type") || "").includes("application/json")) return response;
  // Auth failures/validation responses do not need branding. Returning them immediately
  // avoids a settings D1 read on the unauthenticated Admin bootstrap path.
  if (url.pathname.startsWith("/api/auth/") && !response.ok) return response;
  let data;
  try { data = await response.clone().json(); } catch { return response; }
  const settings = await cachedOrReadSettings(env, state);
  if (response.ok && url.pathname === "/api/admin/settings" && state.settings) { cachedSettings = state.settings; cachedAt = Date.now(); }
  const branding = brandingFromRaw(settings, env);
  const displayPreferences = displayPreferencesFromRaw(settings, env);
  data.branding = branding;
  data.displayPreferences = { ...(data.displayPreferences || {}), ...displayPreferences };
  if (data.settings && typeof data.settings === "object") Object.assign(data.settings, branding);
  if (data.shop && typeof data.shop === "object") {
    Object.assign(data.shop, {
      nameGu: branding.shopNameGu,
      nameEn: branding.shopNameEn,
      websiteTitleGu: branding.websiteTitleGu,
      websiteTitleEn: branding.websiteTitleEn,
      logoUrl: branding.logoUrl,
      defaultLanguage: displayPreferences.defaultLanguage,
      dateFormat: displayPreferences.dateFormat
    });
  }
  if (url.pathname === "/api/public/bootstrap") {
    data.shopNameGu = branding.shopNameGu;
    data.shopNameEn = branding.shopNameEn;
    data.websiteTitleGu = branding.websiteTitleGu;
    data.websiteTitleEn = branding.websiteTitleEn;
    data.logoUrl = branding.logoUrl;
    data.defaultLanguage = displayPreferences.defaultLanguage;
  }
  const headers = new Headers(response.headers);
  headers.delete("content-length");
  if (url.pathname.startsWith("/api/auth/") || url.pathname.startsWith("/api/admin/settings")) headers.set("cache-control", "no-store");
  return new Response(JSON.stringify(data), { status: response.status, statusText: response.statusText, headers });
}

async function serveAdminShell(request, env) {
  if (!env.ASSETS || typeof env.ASSETS.fetch !== "function") return null;
  const sourceUrl = new URL(request.url);
  const assetUrl = new URL("/admin/index.html", sourceUrl.origin);
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
  headers.set("x-zhagmag-admin-shell", ADMIN_SHELL_MARKER);
  headers.delete("etag");
  return new Response(request.method === "HEAD" ? null : assetResponse.body, {
    status: assetResponse.status,
    statusText: assetResponse.statusText,
    headers
  });
}

// Chain marker for cumulative hardening: return core.fetch(request, env, ctx);
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if ((request.method === "GET" || request.method === "HEAD") && (url.pathname === "/admin" || url.pathname === "/admin/" || url.pathname === "/admin/index.html")) {
      const shell = await serveAdminShell(request, env);
      if (shell) return shell;
    }
    const state = { settings: null, brandingPatch: null };
    if (request.method === "PUT" && url.pathname === "/api/admin/settings") {
      try { state.brandingPatch = requestBranding(await request.clone().json()); } catch {}
    }
    const response = await core.fetch(request, proxyEnv(env, state), ctx);
    return rewriteResponse(request, response, env, state);
  }
};
