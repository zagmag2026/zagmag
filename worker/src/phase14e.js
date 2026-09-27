import core from "./phase14d.js";
import { getSessionUser } from "./auth.ts";

const CLOUDINARY_ITEM_FOLDER = "zhagmag-dresses/items";
const CLOUDINARY_BRANDING_FOLDER = "zhagmag-dresses/branding";
const MAX_IMAGE_BYTES = 8 * 1024 * 1024;
const ALLOWED_IMAGE_FORMATS = new Set(["jpg", "jpeg", "png", "webp"]);

function allowedOrigin(request, env) {
  const origin = request.headers.get("origin");
  if (!origin) return true;
  const self = new URL(request.url).origin;
  if (origin === self) return true;
  return String(env.ALLOWED_ORIGINS || "").split(",").map((v) => v.trim()).filter(Boolean).includes(origin);
}

function secureJson(request, env, data, init = {}) {
  const headers = new Headers({
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "referrer-policy": "no-referrer"
  });
  const origin = request.headers.get("origin");
  if (origin && allowedOrigin(request, env)) {
    headers.set("access-control-allow-origin", origin);
    headers.set("access-control-allow-credentials", "true");
    headers.set("vary", "Origin");
  }
  return new Response(JSON.stringify(data), { ...init, headers });
}

async function requireAdmin(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return { response: secureJson(request, env, { ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, { status: 401 }) };
  if (user.role !== "OWNER") return { response: secureJson(request, env, { ok: false, error: "FORBIDDEN", message: "Owner access required." }, { status: 403 }) };
  return { user };
}

function cloudinaryReady(env) {
  return Boolean(env.CLOUDINARY_CLOUD_NAME && env.CLOUDINARY_API_KEY && env.CLOUDINARY_API_SECRET);
}

async function sha1Hex(value) {
  const digest = await crypto.subtle.digest("SHA-1", new TextEncoder().encode(value));
  return [...new Uint8Array(digest)].map((v) => v.toString(16).padStart(2, "0")).join("");
}

async function signCloudinary(params, secret) {
  const base = Object.entries(params)
    .filter(([, value]) => value !== "" && value !== undefined && value !== null)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([key, value]) => `${key}=${value}`)
    .join("&");
  return sha1Hex(`${base}${secret}`);
}

async function uploadSignature(request, env) {
  if (!allowedOrigin(request, env)) return secureJson(request, env, { ok: false, error: "ORIGIN_NOT_ALLOWED", message: "Request origin is not allowed." }, { status: 403 });
  const auth = await requireAdmin(request, env); if (auth.response) return auth.response;
  if (!cloudinaryReady(env)) return secureJson(request, env, { ok: false, error: "CLOUDINARY_NOT_CONFIGURED", message: "Cloudinary is not configured for this environment." }, { status: 503 });
  await drainCleanupQueue(env, 5);
  const purpose = new URL(request.url).searchParams.get("purpose") === "logo" ? "logo" : "item";
  const timestamp = Math.floor(Date.now() / 1000);
  const folder = purpose === "logo" ? CLOUDINARY_BRANDING_FOLDER : CLOUDINARY_ITEM_FOLDER;
  const allowedFormats = "jpg,jpeg,png,webp";
  const signature = await signCloudinary({ allowed_formats: allowedFormats, folder, timestamp }, env.CLOUDINARY_API_SECRET);
  return secureJson(request, env, {
    ok: true,
    purpose,
    cloudName: env.CLOUDINARY_CLOUD_NAME,
    apiKey: env.CLOUDINARY_API_KEY,
    timestamp,
    folder,
    allowedFormats,
    signature,
    maxFiles: purpose === "logo" ? 1 : 8,
    maxBytes: MAX_IMAGE_BYTES
  });
}

function safePublicId(value) {
  const text = String(value || "").trim();
  return text && text.length <= 255 && !text.includes("..") && !/[\u0000-\u001f\u007f]/.test(text) ? text : "";
}

function normalizeAssets(raw) {
  if (!Array.isArray(raw)) return [];
  const seen = new Set();
  const out = [];
  for (const row of raw) {
    const url = String(row?.url || "").trim();
    const publicId = safePublicId(row?.publicId);
    if (!url || !publicId || seen.has(url)) continue;
    try { const parsed = new URL(url); if (parsed.protocol !== "https:") continue; } catch { continue; }
    seen.add(url); out.push({ url, publicId });
  }
  return out.slice(0, 8);
}

function publicIdFromCloudinaryUrl(env, rawUrl) {
  try {
    const url = new URL(String(rawUrl || ""));
    if (url.protocol !== "https:" || url.hostname !== "res.cloudinary.com") return "";
    const parts = url.pathname.split("/").filter(Boolean).map(decodeURIComponent);
    if (parts[0] !== String(env.CLOUDINARY_CLOUD_NAME || "") || parts[1] !== "image" || parts[2] !== "upload") return "";
    let tail = parts.slice(3);
    if (tail[0] && /^v\d+$/.test(tail[0])) tail = tail.slice(1);
    if (!tail.length) return "";
    const last = tail[tail.length - 1];
    tail[tail.length - 1] = last.replace(/\.[A-Za-z0-9]+$/, "");
    return tail.join("/");
  } catch {
    return "";
  }
}

async function cloudinaryResource(env, publicId) {
  if (!cloudinaryReady(env)) return null;
  const encoded = String(publicId).split("/").map(encodeURIComponent).join("/");
  const auth = btoa(`${env.CLOUDINARY_API_KEY}:${env.CLOUDINARY_API_SECRET}`);
  const response = await fetch(
    `https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/resources/image/upload/${encoded}`,
    { headers: { Authorization: `Basic ${auth}` } }
  );
  if (!response.ok) return null;
  try { return await response.json(); } catch { return null; }
}

async function validateUploadedAssets(request, env, targetItemId, rawAssets) {
  const assets = normalizeAssets(rawAssets);
  for (const asset of assets) {
    if (!asset.publicId.startsWith(CLOUDINARY_ITEM_FOLDER + "/") || publicIdFromCloudinaryUrl(env, asset.url) !== asset.publicId) {
      return { response: secureJson(request, env, { ok: false, error: "INVALID_CLOUDINARY_ASSET", message: "One uploaded image is not owned by this Item upload flow." }, { status: 400 }) };
    }
    const mapped = await env.DB.prepare(
      `SELECT item_id FROM cloudinary_item_assets WHERE image_url=? OR public_id=? LIMIT 1`
    ).bind(asset.url, asset.publicId).first();
    if (mapped && String(mapped.item_id || "") !== String(targetItemId || "")) {
      return { response: secureJson(request, env, { ok: false, error: "CLOUDINARY_ASSET_IN_USE", message: "One uploaded image is already attached to another Item." }, { status: 409 }) };
    }
    const resource = await cloudinaryResource(env, asset.publicId);
    const format = String(resource?.format || "").toLowerCase();
    const bytes = Number(resource?.bytes || 0);
    if (!resource || String(resource.public_id || "") !== asset.publicId || String(resource.secure_url || "") !== asset.url ||
        !ALLOWED_IMAGE_FORMATS.has(format) || !Number.isFinite(bytes) || bytes <= 0 || bytes > MAX_IMAGE_BYTES) {
      return { response: secureJson(request, env, { ok: false, error: "INVALID_CLOUDINARY_ASSET", message: "Uploaded images must be JPG, PNG or WebP and no larger than 8 MB." }, { status: 400 }) };
    }
  }
  return { assets };
}

async function queueCleanup(env, publicId, errorMessage) {
  try {
    await env.DB.prepare(`
      INSERT INTO cloudinary_cleanup_queue(public_id,attempts,last_error,updated_at)
      VALUES(?,1,?,CURRENT_TIMESTAMP)
      ON CONFLICT(public_id) DO UPDATE SET
        attempts=cloudinary_cleanup_queue.attempts+1,
        last_error=excluded.last_error,
        updated_at=CURRENT_TIMESTAMP
    `).bind(publicId, String(errorMessage || "cleanup failed").slice(0, 500)).run();
  } catch (error) {
    if (String(env.APP_ENV || "").toLowerCase() !== "production") {
      console.warn("Cloudinary cleanup queue unavailable", error instanceof Error ? error.message : "unknown");
    }
  }
}

async function destroyCloudinary(env, publicIds) {
  if (!cloudinaryReady(env)) return;
  for (const publicId of [...new Set(publicIds.map(safePublicId).filter(Boolean))]) {
    try {
      const timestamp = Math.floor(Date.now() / 1000);
      const signature = await signCloudinary({ public_id: publicId, timestamp }, env.CLOUDINARY_API_SECRET);
      const body = new FormData();
      body.set("public_id", publicId);
      body.set("timestamp", String(timestamp));
      body.set("api_key", env.CLOUDINARY_API_KEY);
      body.set("signature", signature);
      const response = await fetch(`https://api.cloudinary.com/v1_1/${encodeURIComponent(env.CLOUDINARY_CLOUD_NAME)}/image/destroy`, { method: "POST", body });
      let payload = {};
      try { payload = await response.json(); } catch {}
      const result = String(payload?.result || "").toLowerCase();
      if (!response.ok || !["ok","not found"].includes(result)) {
        throw new Error(String(payload?.error?.message || payload?.message || `Cloudinary destroy failed (${response.status})`));
      }
      try {
        await env.DB.prepare("DELETE FROM cloudinary_cleanup_queue WHERE public_id=?").bind(publicId).run();
      } catch {}
    } catch (error) {
      await queueCleanup(env, publicId, error instanceof Error ? error.message : "cleanup failed");
      if (String(env.APP_ENV || "").toLowerCase() !== "production") {
        console.warn("Cloudinary cleanup queued", error instanceof Error ? error.message : "unknown");
      }
    }
  }
}

async function drainCleanupQueue(env, limit = 5) {
  let rows = [];
  try {
    const result = await env.DB.prepare(
      "SELECT public_id FROM cloudinary_cleanup_queue ORDER BY updated_at ASC LIMIT ?"
    ).bind(Math.max(1, Math.min(20, Number(limit) || 5))).all();
    rows = result.results || [];
  } catch {
    return;
  }
  if (rows.length) await destroyCloudinary(env, rows.map(row => String(row.public_id || "")));
}

async function destroyUnmappedAssets(env, rawAssets) {
  const assets = normalizeAssets(rawAssets);
  const removable = [];
  for (const asset of assets) {
    const mapped = await env.DB.prepare(
      `SELECT 1 AS found FROM cloudinary_item_assets WHERE image_url=? OR public_id=? LIMIT 1`
    ).bind(asset.url, asset.publicId).first();
    if (!mapped) removable.push(asset.publicId);
  }
  await destroyCloudinary(env, removable);
  return removable.length;
}

async function currentLogoPublicId(env) {
  try {
    const row = await env.DB.prepare("SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1").first();
    const raw = row?.value_json ? JSON.parse(String(row.value_json)) : {};
    return safePublicId(raw?.logoPublicId);
  } catch { return ""; }
}

async function validateManagedAsset(request, env, asset, allowedFolders) {
  const folderOk = allowedFolders.some(folder => asset.publicId.startsWith(folder + "/"));
  if (!folderOk || publicIdFromCloudinaryUrl(env, asset.url) !== asset.publicId) {
    return secureJson(request, env, { ok: false, error: "INVALID_CLOUDINARY_ASSET", message: "Uploaded image is not owned by this managed upload flow." }, { status: 400 });
  }
  const resource = await cloudinaryResource(env, asset.publicId);
  const format = String(resource?.format || "").toLowerCase();
  const bytes = Number(resource?.bytes || 0);
  if (!resource || String(resource.public_id || "") !== asset.publicId || String(resource.secure_url || "") !== asset.url ||
      !ALLOWED_IMAGE_FORMATS.has(format) || !Number.isFinite(bytes) || bytes <= 0 || bytes > MAX_IMAGE_BYTES) {
    return secureJson(request, env, { ok: false, error: "INVALID_CLOUDINARY_ASSET", message: "Uploaded images must be JPG, PNG or WebP and no larger than 8 MB." }, { status: 400 });
  }
  return null;
}

async function discardUploads(request, env) {
  if (!allowedOrigin(request, env)) return secureJson(request, env, { ok: false, error: "ORIGIN_NOT_ALLOWED", message: "Request origin is not allowed." }, { status: 403 });
  const auth = await requireAdmin(request, env); if (auth.response) return auth.response;
  await drainCleanupQueue(env, 5);
  let body = null;
  try { body = await request.json(); } catch { body = null; }
  const assets = normalizeAssets(body?.assets);
  if (!assets.length) return secureJson(request, env, { ok: true, discarded: 0 });

  const savedLogoId = await currentLogoPublicId(env);
  const removable = [];
  for (const asset of assets) {
    const invalid = await validateManagedAsset(request, env, asset, [CLOUDINARY_ITEM_FOLDER, CLOUDINARY_BRANDING_FOLDER]);
    if (invalid) return invalid;
    if (asset.publicId.startsWith(CLOUDINARY_BRANDING_FOLDER + "/")) {
      if (asset.publicId !== savedLogoId) removable.push(asset.publicId);
      continue;
    }
    const mapped = await env.DB.prepare(
      `SELECT 1 AS found FROM cloudinary_item_assets WHERE image_url=? OR public_id=? LIMIT 1`
    ).bind(asset.url, asset.publicId).first();
    if (!mapped) removable.push(asset.publicId);
  }

  await destroyCloudinary(env, removable);
  return secureJson(request, env, { ok: true, discarded: removable.length });
}

function removedAssetPublicIds(previousAssets, imageUrls) {
  const keep = new Set((imageUrls || []).map((v) => String(v).trim()).filter(Boolean));
  return (previousAssets || [])
    .filter((row) => !keep.has(String(row.image_url || "")))
    .map((row) => String(row.public_id || ""))
    .filter(Boolean);
}

async function deleteAssetMap(env, itemId) {
  const rows = await env.DB.prepare(`SELECT public_id FROM cloudinary_item_assets WHERE item_id=?`).bind(itemId).all();
  await env.DB.prepare(`DELETE FROM cloudinary_item_assets WHERE item_id=?`).bind(itemId).run();
  await destroyCloudinary(env, (rows.results || []).map((row) => String(row.public_id || "")));
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/api/admin/cloudinary/signature" && request.method === "POST") return uploadSignature(request, env);
    if (url.pathname === "/api/admin/cloudinary/discard" && request.method === "POST") return discardUploads(request, env);

    const isItemWrite = (url.pathname === "/api/admin/items" && request.method === "POST") || (/^\/api\/admin\/items\/[^/]+$/.test(url.pathname) && request.method === "PUT");
    const isItemDelete = /^\/api\/admin\/items\/[^/]+$/.test(url.pathname) && request.method === "DELETE";
    const isSettingsWrite = url.pathname === "/api/admin/settings" && request.method === "PUT";
    let body = null;
    if (isItemWrite || isSettingsWrite) {
      try { body = await request.clone().json(); } catch { body = null; }
    }
    if (isItemWrite || isItemDelete || isSettingsWrite) await drainCleanupQueue(env, 5);

    let previousLogo = { url:"", publicId:"" };
    let nextLogoAsset = null;
    if (isSettingsWrite && body) {
      try {
        const row = await env.DB.prepare("SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1").first();
        const raw = row?.value_json ? JSON.parse(String(row.value_json)) : {};
        previousLogo = { url:String(raw?.logoUrl || ""), publicId:safePublicId(raw?.logoPublicId) };
      } catch {}
      const nextUrl = String(body.logoUrl || "").trim();
      const nextPublicId = safePublicId(body.logoPublicId);
      const logoChanged = nextUrl !== previousLogo.url;
      if (logoChanged && nextUrl) {
        if (!nextPublicId) {
          return secureJson(request, env, { ok:false, error:"MANAGED_LOGO_REQUIRED", message:"Use the managed logo upload control for a new logo." }, { status:400 });
        }
        const candidate = { url:nextUrl, publicId:nextPublicId };
        const invalid = await validateManagedAsset(request, env, candidate, [CLOUDINARY_BRANDING_FOLDER]);
        if (invalid) return invalid;
        nextLogoAsset = candidate;
      }
      if (!logoChanged && previousLogo.publicId && !nextPublicId) {
        body.logoPublicId = previousLogo.publicId;
        const headers = new Headers(request.headers);
        headers.delete("content-length");
        request = new Request(request.url, {
          method: request.method,
          headers,
          body: JSON.stringify(body)
        });
      }
    }
    const deleteId = isItemDelete ? decodeURIComponent(url.pathname.split("/").pop() || "") : "";
    const targetItemId = isItemWrite && request.method === "PUT" ? decodeURIComponent(url.pathname.split("/").pop() || "") : "";
    let validatedAssets = [];
    if (isItemWrite && body) {
      const checked = await validateUploadedAssets(request, env, targetItemId, body.cloudinaryAssets);
      if (checked.response) return checked.response;
      validatedAssets = checked.assets || [];
    }
    const previousAssets = isItemWrite && targetItemId
      ? await env.DB.prepare(`SELECT image_url,public_id FROM cloudinary_item_assets WHERE item_id=?`).bind(targetItemId).all()
      : null;
    const deleteAssets = isItemDelete && deleteId
      ? await env.DB.prepare(`SELECT public_id FROM cloudinary_item_assets WHERE item_id=?`).bind(deleteId).all()
      : null;

    const response = await core.fetch(request, env);
    if (!response.ok) {
      if (isItemWrite && validatedAssets.length) {
        try { await destroyUnmappedAssets(env, validatedAssets); } catch {}
      }
      if (isSettingsWrite && nextLogoAsset && nextLogoAsset.publicId !== previousLogo.publicId) {
        await destroyCloudinary(env, [nextLogoAsset.publicId]);
      }
      return response;
    }

    try {
      if (isItemWrite && body) {
        const removed = removedAssetPublicIds(previousAssets?.results || [], Array.isArray(body.imageUrls) ? body.imageUrls : []);
        if (removed.length) await destroyCloudinary(env, removed);
      } else if (isItemDelete && deleteId) {
        await destroyCloudinary(env, (deleteAssets?.results || []).map((row) => String(row.public_id || "")));
      } else if (isSettingsWrite && previousLogo.publicId) {
        const nextPublicId = safePublicId(body?.logoPublicId);
        if (nextPublicId !== previousLogo.publicId) await destroyCloudinary(env, [previousLogo.publicId]);
      }
    } catch (error) {
      if (String(env.APP_ENV || "").toLowerCase() !== "production") {
        console.warn("Phase14E Cloudinary asset sync skipped", error instanceof Error ? error.message : "unknown");
      }
    }
    return response;
  }
};
