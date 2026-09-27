import {
  cleanupExpiredSessions,
  clearSessionCookie,
  createSession,
  deleteSessionFromRequest,
  getSessionUser,
  hashPassword,
  makeSessionCookie,
  parseStaffPermissions,
  secureTextEqual,
  sha256,
  STAFF_PERMISSION_KEYS,
  validatePassword,
  verifyPassword,
  type SessionUser,
  type StaffPermission,
  type UserRole
} from "./auth";
import {
  WHATSAPP_GLOBAL_LANGUAGE_MODES,
  WHATSAPP_LINKED_ACTIONS,
  WHATSAPP_PLACEHOLDERS,
  WHATSAPP_PLACEHOLDERS_BY_ACTION,
  WHATSAPP_PLACEHOLDER_SOURCES,
  normalizeWhatsAppGlobalLanguage,
  normalizeWhatsAppLinkedAction,
  validateTemplatePlaceholders,
  type WhatsAppGlobalLanguageMode,
  type WhatsAppLinkedAction
} from "./whatsapp-template-registry";

export interface Env {
  DB: D1Database;
  APP_NAME: string;
  APP_ENV: string;
  ALLOWED_ORIGINS?: string;
  SESSION_TTL_HOURS?: string;
  COOKIE_SECURE?: string;
  INITIAL_OWNER_SETUP_TOKEN?: string;
  PUBLIC_WHATSAPP_NUMBER?: string;
  PUBLIC_CALL_NUMBER?: string;
}

type JsonBody = Record<string, unknown>;
type FieldType = "TEXT" | "NUMBER" | "DROPDOWN" | "MULTI_SELECT" | "YES_NO" | "DATE";

const FIELD_TYPES = new Set<FieldType>(["TEXT", "NUMBER", "DROPDOWN", "MULTI_SELECT", "YES_NO", "DATE"]);
const ADMIN_ROLES: readonly UserRole[] = ["OWNER"];
const DEFAULT_STAFF_PERMISSIONS: StaffPermission[] = [...STAFF_PERMISSION_KEYS];

function allowedOrigin(request: Request, env: Env): string | null {
  const origin = request.headers.get("origin");
  if (!origin) return null;
  const requestOrigin = new URL(request.url).origin;
  if (origin === requestOrigin) return origin;
  const allowed = (env.ALLOWED_ORIGINS || "").split(",").map(v => v.trim()).filter(Boolean);
  return allowed.includes(origin) ? origin : null;
}

function responseHeaders(request: Request, env: Env): HeadersInit {
  const origin = allowedOrigin(request, env);
  return {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff",
    "referrer-policy": "no-referrer",
    "permissions-policy": "camera=(), microphone=(), geolocation=()",
    ...(origin ? {
      "access-control-allow-origin": origin,
      "access-control-allow-credentials": "true",
      vary: "Origin"
    } : {})
  };
}

function json(request: Request, env: Env, data: unknown, init: ResponseInit = {}): Response {
  const headers = new Headers(responseHeaders(request, env));
  if (init.headers) new Headers(init.headers).forEach((value, key) => headers.set(key, value));
  return new Response(JSON.stringify(data), { ...init, headers });
}

const MAX_JSON_BYTES = 256 * 1024;
const LOGIN_WINDOW_SECONDS = 15 * 60;
const LOGIN_MAX_FAILURES = 8;

function isMutation(method: string): boolean {
  return method === "POST" || method === "PUT" || method === "PATCH" || method === "DELETE";
}

function mutationRequestGuard(request: Request, env: Env): Response | null {
  if (!isMutation(request.method)) return null;
  const url = new URL(request.url);
  if (!url.pathname.startsWith("/api/")) return null;

  const origin = request.headers.get("origin");
  const isPublicReadPost = url.pathname === "/api/public/availability";
  if (!isPublicReadPost && origin && !allowedOrigin(request, env)) {
    return json(request, env, { ok: false, error: "ORIGIN_NOT_ALLOWED", message: "Request origin is not allowed." }, { status: 403 });
  }

  if (request.method !== "DELETE") {
    const contentType = (request.headers.get("content-type") || "").toLowerCase();
    if (!contentType.includes("application/json")) {
      return json(request, env, { ok: false, error: "UNSUPPORTED_MEDIA_TYPE", message: "application/json is required." }, { status: 415 });
    }
    const length = Number(request.headers.get("content-length") || "0");
    if (Number.isFinite(length) && length > MAX_JSON_BYTES) {
      return json(request, env, { ok: false, error: "REQUEST_TOO_LARGE", message: "Request body is too large." }, { status: 413 });
    }
  }
  return null;
}

const LOGIN_ACCOUNT_MAX_FAILURES = 16;

async function loginThrottleKeys(request: Request, identifier: string): Promise<Array<{key:string;limit:number}>> {
  const normalized = identifier.trim().toLowerCase();
  const ip = request.headers.get("cf-connecting-ip") || request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "unknown";
  return [
    { key: await sha256(`pair|${normalized}|${ip}`), limit: LOGIN_MAX_FAILURES },
    { key: await sha256(`account|${normalized}`), limit: LOGIN_ACCOUNT_MAX_FAILURES }
  ];
}

async function loginThrottleCheck(request: Request, env: Env, identifier: string): Promise<Response | null> {
  const keys = await loginThrottleKeys(request, identifier);
  const now = Math.floor(Date.now() / 1000);
  for (const entry of keys) {
    const row = await env.DB.prepare(`SELECT blocked_until_epoch FROM auth_login_limits WHERE throttle_key=?`).bind(entry.key).first<any>();
    if (row && Number(row.blocked_until_epoch || 0) > now) {
      const retryAfter = Math.max(1, Number(row.blocked_until_epoch) - now);
      return json(request, env, { ok: false, error: "TOO_MANY_ATTEMPTS", message: "Too many sign-in attempts. Try again later." }, { status: 429, headers: { "retry-after": String(retryAfter) } });
    }
  }
  return null;
}

async function recordLoginFailure(request: Request, env: Env, identifier: string): Promise<void> {
  const now = Math.floor(Date.now() / 1000);
  for (const entry of await loginThrottleKeys(request, identifier)) {
    const row = await env.DB.prepare(`SELECT failures,window_started_epoch FROM auth_login_limits WHERE throttle_key=?`).bind(entry.key).first<any>();
    const insideWindow = row && now - Number(row.window_started_epoch || 0) < LOGIN_WINDOW_SECONDS;
    const failures = insideWindow ? Number(row.failures || 0) + 1 : 1;
    const windowStart = insideWindow ? Number(row.window_started_epoch) : now;
    const blockedUntil = failures >= entry.limit ? now + LOGIN_WINDOW_SECONDS : 0;
    await env.DB.prepare(`INSERT INTO auth_login_limits (throttle_key,failures,window_started_epoch,blocked_until_epoch,updated_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP)
      ON CONFLICT(throttle_key) DO UPDATE SET failures=excluded.failures,window_started_epoch=excluded.window_started_epoch,blocked_until_epoch=excluded.blocked_until_epoch,updated_at=CURRENT_TIMESTAMP`)
      .bind(entry.key, failures, windowStart, blockedUntil).run();
  }
}

async function clearLoginFailures(request: Request, env: Env, identifier: string): Promise<void> {
  for (const entry of await loginThrottleKeys(request, identifier)) {
    await env.DB.prepare(`DELETE FROM auth_login_limits WHERE throttle_key=?`).bind(entry.key).run();
  }
}

async function readJson(request: Request): Promise<JsonBody | null> {
  try {
    const raw = await request.text();
    if (new TextEncoder().encode(raw).byteLength > MAX_JSON_BYTES) return null;
    const body = JSON.parse(raw);
    return body && typeof body === "object" && !Array.isArray(body) ? body as JsonBody : null;
  } catch { return null; }
}

function text(body: JsonBody, key: string): string {
  return typeof body[key] === "string" ? String(body[key]).trim() : "";
}

function bool(body: JsonBody, key: string, fallback: boolean): boolean {
  return typeof body[key] === "boolean" ? body[key] as boolean : fallback;
}

function int(body: JsonBody, key: string, fallback = 0): number {
  const raw = body[key];
  const value = typeof raw === "number" ? raw : Number(raw);
  return Number.isInteger(value) ? value : fallback;
}

function dbErrorHas(error: unknown, token: string): boolean {
  const message = error instanceof Error ? error.message : String(error ?? "");
  return message.includes(token);
}

function publicUser(user: SessionUser) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    mobile: user.mobile,
    role: user.role,
    staffPermissions: user.role === "OWNER" ? [] : user.staffPermissions
  };
}

function isRole(user: SessionUser, roles: readonly UserRole[]): boolean { return roles.includes(user.role); }

async function requireUser(request: Request, env: Env): Promise<SessionUser | Response> {
  const user = await getSessionUser(env, request);
  return user || json(request, env, { ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, { status: 401 });
}

async function requireRole(request: Request, env: Env, roles: readonly UserRole[]): Promise<SessionUser | Response> {
  const userOrResponse = await requireUser(request, env);
  if (userOrResponse instanceof Response) return userOrResponse;
  return isRole(userOrResponse, roles)
    ? userOrResponse
    : json(request, env, { ok: false, error: "FORBIDDEN", message: "You do not have permission for this action." }, { status: 403 });
}

async function audit(env: Env, userId: string | null, action: string, module: string, recordId: string | null, oldValue?: unknown, newValue?: unknown) {
  await env.DB.prepare(
    `INSERT INTO audit_logs (id, user_id, action, module, record_id, old_value_json, new_value_json)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    crypto.randomUUID(), userId, action, module, recordId,
    oldValue === undefined ? null : JSON.stringify(oldValue),
    newValue === undefined ? null : JSON.stringify(newValue)
  ).run();
}

async function bootstrapOwner(request: Request, env: Env): Promise<Response> {
  if (!env.INITIAL_OWNER_SETUP_TOKEN) return json(request, env, { ok: false, error: "SETUP_DISABLED", message: "Initial owner setup token is not configured." }, { status: 503 });
  const existing = await env.DB.prepare("SELECT COUNT(*) AS count FROM users").first<{ count: number }>();
  if ((existing?.count || 0) > 0) return json(request, env, { ok: false, error: "ALREADY_INITIALIZED", message: "Initial owner has already been created." }, { status: 409 });

  const body = await readJson(request);
  if (!body) return json(request, env, { ok: false, error: "INVALID_JSON" }, { status: 400 });
  if (!secureTextEqual(text(body, "setupToken"), env.INITIAL_OWNER_SETUP_TOKEN)) return json(request, env, { ok: false, error: "INVALID_SETUP_TOKEN", message: "Invalid setup token." }, { status: 403 });

  const name = text(body, "name");
  const email = normalizeUserEmail(text(body, "email"));
  const mobile = normalizeUserMobile(text(body, "mobile"));
  const password = typeof body.password === "string" ? body.password : "";
  const contactError = userContactError(email, mobile);
  if (!name || name.length > 120 || (!email && !mobile) || contactError) return json(request, env, { ok: false, error: "VALIDATION", message: contactError || "Name and email or mobile are required." }, { status: 400 });
  const passwordError = validatePassword(password);
  if (passwordError) return json(request, env, { ok: false, error: "VALIDATION", message: passwordError }, { status: 400 });

  const userId = crypto.randomUUID();
  const passwordHash = await hashPassword(password);
  await env.DB.prepare(`INSERT INTO users (id, name, email, mobile, password_hash, role, is_active) VALUES (?, ?, ?, ?, ?, 'OWNER', 1)`)
    .bind(userId, name, email || null, mobile || null, passwordHash).run();
  await audit(env, userId, "CREATE_INITIAL_OWNER", "AUTH", userId, undefined, { name, email: email || null, mobile: mobile || null, role: "OWNER" });
  return json(request, env, { ok: true, message: "Owner account created. You can now sign in." }, { status: 201 });
}

async function login(request: Request, env: Env): Promise<Response> {
  const body = await readJson(request);
  if (!body) return json(request, env, { ok: false, error: "INVALID_JSON" }, { status: 400 });
  const identifier = text(body, "identifier");
  const password = typeof body.password === "string" ? body.password : "";
  if (!identifier || !password || identifier.length > 254 || password.length > 128) return json(request, env, { ok: false, error: "VALIDATION", message: "Email/mobile and a valid password are required." }, { status: 400 });
  const throttled = await loginThrottleCheck(request, env, identifier);
  if (throttled) return throttled;

  const row = await env.DB.prepare(
    `SELECT id, name, email, mobile, password_hash, role, is_active, archived_at, staff_permissions_json FROM users
     WHERE (LOWER(email) = LOWER(?) OR mobile = ?) LIMIT 1`
  ).bind(identifier, identifier).first<any>();

  if (!row || Number(row.is_active) !== 1 || row.archived_at || !(await verifyPassword(password, row.password_hash))) {
    await recordLoginFailure(request, env, identifier);
    return json(request, env, { ok: false, error: "INVALID_CREDENTIALS", message: "Invalid email/mobile or password." }, { status: 401 });
  }

  const role: UserRole = String(row.role).toUpperCase() === "OWNER" ? "OWNER" : "STAFF";
  const user: SessionUser = {
    id: String(row.id),
    name: String(row.name),
    email: row.email ? String(row.email) : null,
    mobile: row.mobile ? String(row.mobile) : null,
    role,
    staffPermissions: role === "OWNER" ? [] : parseStaffPermissions(row.staff_permissions_json)
  };

  await clearLoginFailures(request, env, identifier);
  await cleanupExpiredSessions(env);
  const token = await createSession(env, user.id, request.headers.get("user-agent"));
  await env.DB.prepare("UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?").bind(user.id).run();
  await audit(env, user.id, "LOGIN", "AUTH", user.id);
  return json(request, env, { ok: true, user: publicUser(user) }, { headers: { "set-cookie": makeSessionCookie(env, token) } });
}

async function logout(request: Request, env: Env): Promise<Response> {
  const user = await getSessionUser(env, request);
  await deleteSessionFromRequest(env, request);
  if (user) await audit(env, user.id, "LOGOUT", "AUTH", user.id);
  return json(request, env, { ok: true }, { headers: { "set-cookie": clearSessionCookie(env) } });
}

function normalizePrefix(value: string): string { return value.toUpperCase().replace(/[^A-Z0-9]/g, ""); }

function fieldOptions(body: JsonBody, fieldType: FieldType): string[] | null {
  if (fieldType !== "DROPDOWN" && fieldType !== "MULTI_SELECT") return null;
  const raw = body.options;
  const values = Array.isArray(raw) ? raw.map(v => String(v).trim()).filter(Boolean) : [];
  return [...new Set(values)];
}

async function listCategories(request: Request, env: Env): Promise<Response> {
  const rows = await env.DB.prepare(
    `SELECT c.id, c.name, c.code_prefix, c.is_active, c.public_visible, c.display_order,
            c.created_at, c.updated_at,
            (SELECT COUNT(*) FROM category_fields f WHERE f.category_id = c.id) AS field_count,
            (SELECT COUNT(*) FROM items i WHERE i.category_id = c.id AND i.archived_at IS NULL) AS item_count
       FROM categories c
      ORDER BY c.display_order ASC, c.name COLLATE NOCASE ASC`
  ).all<Record<string, unknown>>();

  const fields = await env.DB.prepare(
    `SELECT id, category_id, field_name, field_type, is_required, default_value, options_json,
            public_visible, is_active, display_order, created_at, updated_at
       FROM category_fields
      ORDER BY category_id, display_order ASC, field_name COLLATE NOCASE ASC`
  ).all<Record<string, unknown>>();

  const grouped = new Map<string, unknown[]>();
  for (const row of fields.results || []) {
    const categoryId = String(row.category_id);
    const list = grouped.get(categoryId) || [];
    list.push({
      ...row,
      is_required: Number(row.is_required) === 1,
      public_visible: Number(row.public_visible) === 1,
      is_active: Number(row.is_active) === 1,
      options: row.options_json ? JSON.parse(String(row.options_json)) : []
    });
    grouped.set(categoryId, list);
  }

  return json(request, env, {
    ok: true,
    categories: (rows.results || []).map(row => ({
      ...row,
      is_active: Number(row.is_active) === 1,
      public_visible: Number(row.public_visible) === 1,
      fields: grouped.get(String(row.id)) || []
    }))
  });
}

async function createCategory(request: Request, env: Env, user: SessionUser): Promise<Response> {
  const body = await readJson(request);
  if (!body) return json(request, env, { ok: false, error: "INVALID_JSON" }, { status: 400 });
  const name = text(body, "name");
  const codePrefix = normalizePrefix(text(body, "codePrefix"));
  if (!name || codePrefix.length < 1 || codePrefix.length > 8) return json(request, env, { ok: false, error: "VALIDATION", message: "Category name and 1-8 character code prefix are required." }, { status: 400 });

  const existingName = await env.DB.prepare(`SELECT id FROM categories WHERE LOWER(name) = LOWER(?) LIMIT 1`).bind(name).first();
  if (existingName) return json(request, env, { ok: false, error: "DUPLICATE", message: "Category name already exists." }, { status: 409 });

  const id = crypto.randomUUID();
  const record = {
    id, name, code_prefix: codePrefix,
    is_active: bool(body, "isActive", true),
    public_visible: bool(body, "publicVisible", true),
    display_order: Math.max(0, int(body, "displayOrder", 0))
  };
  try {
    await env.DB.batch([
      env.DB.prepare(
        `INSERT INTO categories (id, name, code_prefix, is_active, public_visible, display_order)
         VALUES (?, ?, ?, ?, ?, ?)`
      ).bind(id, name, codePrefix, record.is_active ? 1 : 0, record.public_visible ? 1 : 0, record.display_order),
      env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,new_value_json) VALUES(?,?,?,?,?,?)`)
        .bind(crypto.randomUUID(),user.id,"CREATE","CATEGORY",id,JSON.stringify(record))
    ]);
  } catch (error) {
    if (dbErrorHas(error, "UNIQUE")) {
      return json(request, env, { ok: false, error: "DUPLICATE", message: "Category name or code prefix already exists." }, { status: 409 });
    }
    throw error;
  }
  return json(request, env, { ok: true, category: record }, { status: 201 });
}

async function updateCategory(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old = await env.DB.prepare(`SELECT id, name, code_prefix, is_active, public_visible, display_order FROM categories WHERE id = ?`).bind(id).first<Record<string, unknown>>();
  if (!old) return json(request, env, { ok: false, error: "NOT_FOUND", message: "Category not found." }, { status: 404 });
  const body = await readJson(request);
  if (!body) return json(request, env, { ok: false, error: "INVALID_JSON" }, { status: 400 });
  const name = text(body, "name") || String(old.name);
  const codePrefix = normalizePrefix(text(body, "codePrefix") || String(old.code_prefix));
  if (!name || codePrefix.length < 1 || codePrefix.length > 8) return json(request, env, { ok: false, error: "VALIDATION", message: "Category name and valid code prefix are required." }, { status: 400 });
  const existingName = await env.DB.prepare(`SELECT id FROM categories WHERE LOWER(name) = LOWER(?) AND id <> ? LIMIT 1`).bind(name, id).first();
  if (existingName) return json(request, env, { ok: false, error: "DUPLICATE", message: "Category name already exists." }, { status: 409 });
  const next = {
    id, name, code_prefix: codePrefix,
    is_active: bool(body, "isActive", Number(old.is_active) === 1),
    public_visible: bool(body, "publicVisible", Number(old.public_visible) === 1),
    display_order: Math.max(0, int(body, "displayOrder", Number(old.display_order) || 0))
  };
  try {
    await env.DB.batch([
      env.DB.prepare(
        `UPDATE categories SET name = ?, code_prefix = ?, is_active = ?, public_visible = ?, display_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?`
      ).bind(name, codePrefix, next.is_active ? 1 : 0, next.public_visible ? 1 : 0, next.display_order, id),
      env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
        .bind(crypto.randomUUID(),user.id,"UPDATE","CATEGORY",id,JSON.stringify(old),JSON.stringify(next))
    ]);
  } catch (error) {
    if (dbErrorHas(error, "UNIQUE")) {
      return json(request, env, { ok: false, error: "DUPLICATE", message: "Category name or code prefix already exists." }, { status: 409 });
    }
    throw error;
  }
  return json(request, env, { ok: true, category: next });
}

async function deleteCategory(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old = await env.DB.prepare(`SELECT id, name, code_prefix, is_active, public_visible, display_order FROM categories WHERE id = ?`).bind(id).first<Record<string, unknown>>();
  if (!old) return json(request, env, { ok: false, error: "NOT_FOUND", message: "Category not found." }, { status: 404 });
  const items = await env.DB.prepare(`SELECT COUNT(*) AS count FROM items WHERE category_id = ?`).bind(id).first<{count:number}>();
  if ((items?.count || 0) > 0) return json(request, env, { ok: false, error: "CATEGORY_IN_USE", message: "This category has linked Items. Delete those Items first, then delete the category." }, { status: 409 });
  await env.DB.batch([
    env.DB.prepare(`DELETE FROM category_fields WHERE category_id = ?`).bind(id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json) VALUES(?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),user.id,"DELETE","CATEGORY",id,JSON.stringify(old)),
    env.DB.prepare(`DELETE FROM categories WHERE id = ?`).bind(id)
  ]);
  return json(request, env, { ok: true });
}

async function createField(request: Request, env: Env, user: SessionUser, categoryId: string): Promise<Response> {
  const category = await env.DB.prepare(`SELECT id FROM categories WHERE id = ?`).bind(categoryId).first();
  if (!category) return json(request, env, { ok: false, error: "NOT_FOUND", message: "Category not found." }, { status: 404 });
  const body = await readJson(request);
  if (!body) return json(request, env, { ok: false, error: "INVALID_JSON" }, { status: 400 });
  const fieldName = text(body, "fieldName");
  const fieldType = text(body, "fieldType").toUpperCase() as FieldType;
  if (!fieldName || !FIELD_TYPES.has(fieldType)) return json(request, env, { ok: false, error: "VALIDATION", message: "Field name and valid field type are required." }, { status: 400 });
  const options = fieldOptions(body, fieldType);
  if ((fieldType === "DROPDOWN" || fieldType === "MULTI_SELECT") && (!options || options.length === 0)) return json(request, env, { ok: false, error: "VALIDATION", message: "Dropdown/Multi-select requires at least one option." }, { status: 400 });
  const duplicate = await env.DB.prepare(`SELECT id FROM category_fields WHERE category_id = ? AND LOWER(field_name) = LOWER(?) LIMIT 1`).bind(categoryId, fieldName).first();
  if (duplicate) return json(request, env, { ok: false, error: "DUPLICATE", message: "A field with this name already exists in the category." }, { status: 409 });

  const id = crypto.randomUUID();
  const record = {
    id, category_id: categoryId, field_name: fieldName, field_type: fieldType,
    is_required: bool(body, "isRequired", false), default_value: text(body, "defaultValue") || null,
    options: options || [], public_visible: bool(body, "publicVisible", true),
    is_active: bool(body, "isActive", true), display_order: Math.max(0, int(body, "displayOrder", 0))
  };
  await env.DB.batch([
    env.DB.prepare(
      `INSERT INTO category_fields
        (id, category_id, field_name, field_type, is_required, default_value, options_json, public_visible, is_active, display_order)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(id, categoryId, fieldName, fieldType, record.is_required ? 1 : 0, record.default_value,
      options ? JSON.stringify(options) : null, record.public_visible ? 1 : 0, record.is_active ? 1 : 0, record.display_order),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,new_value_json) VALUES(?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),user.id,"CREATE","CATEGORY_FIELD",id,JSON.stringify(record))
  ]);
  return json(request, env, { ok: true, field: record }, { status: 201 });
}

async function updateField(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old = await env.DB.prepare(`SELECT * FROM category_fields WHERE id = ?`).bind(id).first<Record<string, unknown>>();
  if (!old) return json(request, env, { ok: false, error: "NOT_FOUND", message: "Custom field not found." }, { status: 404 });
  const body = await readJson(request);
  if (!body) return json(request, env, { ok: false, error: "INVALID_JSON" }, { status: 400 });
  const fieldName = text(body, "fieldName") || String(old.field_name);
  const fieldType = (text(body, "fieldType") || String(old.field_type)).toUpperCase() as FieldType;
  if (!FIELD_TYPES.has(fieldType)) return json(request, env, { ok: false, error: "VALIDATION", message: "Invalid field type." }, { status: 400 });
  const options = fieldOptions(body, fieldType) ?? (old.options_json ? JSON.parse(String(old.options_json)) : null);
  if ((fieldType === "DROPDOWN" || fieldType === "MULTI_SELECT") && (!options || options.length === 0)) return json(request, env, { ok: false, error: "VALIDATION", message: "Dropdown/Multi-select requires at least one option." }, { status: 400 });
  const duplicate = await env.DB.prepare(`SELECT id FROM category_fields WHERE category_id = ? AND LOWER(field_name) = LOWER(?) AND id <> ? LIMIT 1`).bind(String(old.category_id), fieldName, id).first();
  if (duplicate) return json(request, env, { ok: false, error: "DUPLICATE", message: "A field with this name already exists in the category." }, { status: 409 });

  const next = {
    id, category_id: String(old.category_id), field_name: fieldName, field_type: fieldType,
    is_required: bool(body, "isRequired", Number(old.is_required) === 1),
    default_value: Object.prototype.hasOwnProperty.call(body, "defaultValue") ? (text(body, "defaultValue") || null) : (old.default_value ?? null),
    options: options || [], public_visible: bool(body, "publicVisible", Number(old.public_visible) === 1),
    is_active: bool(body, "isActive", Number(old.is_active) === 1),
    display_order: Math.max(0, int(body, "displayOrder", Number(old.display_order) || 0))
  };
  await env.DB.batch([
    env.DB.prepare(
      `UPDATE category_fields SET field_name = ?, field_type = ?, is_required = ?, default_value = ?, options_json = ?,
       public_visible = ?, is_active = ?, display_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?`
    ).bind(fieldName, fieldType, next.is_required ? 1 : 0, next.default_value,
      (fieldType === "DROPDOWN" || fieldType === "MULTI_SELECT") ? JSON.stringify(next.options) : null,
      next.public_visible ? 1 : 0, next.is_active ? 1 : 0, next.display_order, id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),user.id,"UPDATE","CATEGORY_FIELD",id,JSON.stringify(old),JSON.stringify(next))
  ]);
  return json(request, env, { ok: true, field: next });
}

async function deleteField(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old = await env.DB.prepare(`SELECT * FROM category_fields WHERE id = ?`).bind(id).first<Record<string, unknown>>();
  if (!old) return json(request, env, { ok: false, error: "NOT_FOUND", message: "Custom field not found." }, { status: 404 });
  const values = await env.DB.prepare(`SELECT COUNT(*) AS count FROM item_field_values WHERE category_field_id = ?`).bind(id).first<{count:number}>();
  if ((values?.count || 0) > 0) return json(request, env, { ok: false, error: "FIELD_IN_USE", message: "This field already has item data. Deactivate it instead of deleting." }, { status: 409 });
  await env.DB.batch([
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json) VALUES(?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),user.id,"DELETE","CATEGORY_FIELD",id,JSON.stringify(old)),
    env.DB.prepare(`DELETE FROM category_fields WHERE id = ?`).bind(id)
  ]);
  return json(request, env, { ok: true });
}

async function fieldDeleteAction(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old = await env.DB.prepare(`SELECT * FROM category_fields WHERE id = ?`).bind(id).first<Record<string, unknown>>();
  if (!old) return json(request, env, { ok: false, error: "NOT_FOUND", message: "Custom field not found." }, { status: 404 });

  const body = await readJson(request);
  if (!body) return json(request, env, { ok: false, error: "INVALID_JSON", message: "Invalid request body." }, { status: 400 });
  const action = text(body, "action").toUpperCase();
  const usage = await env.DB.prepare(
    `SELECT COUNT(DISTINCT item_id) AS count FROM item_field_values WHERE category_field_id = ?`
  ).bind(id).first<{count:number}>();
  const affectedItems = Number(usage?.count || 0);

  if (action === "DEACTIVATE") {
    if (Number(old.is_active) === 0) {
      return json(request, env, { ok: true, unchanged: true, affectedItems, message: "Custom field is already inactive." });
    }
    const next={ ...old, is_active: 0, affectedItems };
    await env.DB.batch([
      env.DB.prepare(`UPDATE category_fields SET is_active=0,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(id),
      env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
        .bind(crypto.randomUUID(),user.id,"DEACTIVATE","CATEGORY_FIELD",id,JSON.stringify(old),JSON.stringify(next))
    ]);
    return json(request, env, { ok: true, affectedItems, message: "Custom field deactivated. Existing Item data was preserved." });
  }

  if (action === "REMOVE_DATA_AND_DELETE") {
    if (text(body, "confirmation") !== "DELETE FIELD") {
      return json(request, env, {
        ok: false,
        error: "CONFIRMATION_REQUIRED",
        message: 'Type "DELETE FIELD" to remove field data and delete this custom field.'
      }, { status: 400 });
    }
    await env.DB.batch([
      env.DB.prepare(`DELETE FROM item_field_values WHERE category_field_id=?`).bind(id),
      env.DB.prepare(`DELETE FROM category_fields WHERE id=?`).bind(id),
      env.DB.prepare(
        `INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
         VALUES(?,?,?,?,?,?,?)`
      ).bind(
        crypto.randomUUID(),
        user.id,
        "REMOVE_DATA_AND_DELETE",
        "CATEGORY_FIELD",
        id,
        JSON.stringify(old),
        JSON.stringify({ deleted: true, affectedItems })
      )
    ]);
    return json(request, env, { ok: true, affectedItems, message: "Custom field data removed and field deleted." });
  }

  return json(request, env, { ok: false, error: "VALIDATION", message: "Choose Deactivate or Remove Data & Delete Field." }, { status: 400 });
}

async function reorderFields(request: Request, env: Env, user: SessionUser, categoryId: string): Promise<Response> {
  const body = await readJson(request);
  if (!body || !Array.isArray(body.fieldIds)) return json(request, env, { ok: false, error: "VALIDATION", message: "fieldIds array is required." }, { status: 400 });
  const ids = [...new Set(body.fieldIds.map(v => String(v)))];
  const existing = await env.DB.prepare(`SELECT id FROM category_fields WHERE category_id = ? ORDER BY display_order, field_name`).bind(categoryId).all<{id:string}>();
  const existingIds = (existing.results || []).map(r => r.id);
  if (ids.length !== existingIds.length || ids.some(id => !existingIds.includes(id))) return json(request, env, { ok: false, error: "VALIDATION", message: "Reorder list must contain every field in this category exactly once." }, { status: 400 });
  await env.DB.batch(ids.map((id, index) => env.DB.prepare(`UPDATE category_fields SET display_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND category_id = ?`).bind(index + 1, id, categoryId)));
  await audit(env, user.id, "REORDER", "CATEGORY_FIELD", categoryId, existingIds, ids);
  return json(request, env, { ok: true });
}


type ItemFieldRow = {
  id: string; category_id: string; field_name: string; field_type: FieldType; is_required: number;
  default_value: string | null; options_json: string | null; public_visible: number; is_active: number; display_order: number;
};

function itemCode(value: string): string { return value.toUpperCase().replace(/\s+/g, "-").replace(/[^A-Z0-9_-]/g, ""); }
function validImageUrl(value: string): boolean {
  try { const u = new URL(value); return u.protocol === "https:"; } catch { return false; }
}
function parseOptions(raw: string | null): string[] { try { const v = raw ? JSON.parse(raw) : []; return Array.isArray(v) ? v.map(String) : []; } catch { return []; } }

async function categoryFields(env: Env, categoryId: string): Promise<ItemFieldRow[]> {
  const result = await env.DB.prepare(
    `SELECT id, category_id, field_name, field_type, is_required, default_value, options_json, public_visible, is_active, display_order
       FROM category_fields WHERE category_id = ? AND is_active = 1 ORDER BY display_order, field_name COLLATE NOCASE`
  ).bind(categoryId).all<ItemFieldRow>();
  return result.results || [];
}

function normalizedFieldValue(field: ItemFieldRow, raw: unknown): { value: string | null; error?: string } {
  const required = field.is_required === 1;
  if (raw === undefined || raw === null || raw === "" || (Array.isArray(raw) && raw.length === 0)) {
    return required ? { value: null, error: `${field.field_name} is required.` } : { value: null };
  }
  if (field.field_type === "TEXT") return { value: String(raw).trim() };
  if (field.field_type === "NUMBER") {
    const n = Number(raw); return Number.isFinite(n) ? { value: String(n) } : { value: null, error: `${field.field_name} must be a number.` };
  }
  if (field.field_type === "YES_NO") {
    if (raw === true || raw === "1" || raw === 1 || String(raw).toLowerCase() === "yes") return { value: "1" };
    if (raw === false || raw === "0" || raw === 0 || String(raw).toLowerCase() === "no") return { value: "0" };
    return { value: null, error: `${field.field_name} must be Yes or No.` };
  }
  if (field.field_type === "DATE") {
    const v = String(raw).trim(); return /^\d{4}-\d{2}-\d{2}$/.test(v) ? { value: v } : { value: null, error: `${field.field_name} must be a valid date.` };
  }
  const options = parseOptions(field.options_json);
  if (field.field_type === "DROPDOWN") {
    const v = String(raw).trim(); return options.includes(v) ? { value: v } : { value: null, error: `${field.field_name} has an invalid option.` };
  }
  if (field.field_type === "MULTI_SELECT") {
    const values = Array.isArray(raw) ? raw.map(v => String(v).trim()).filter(Boolean) : String(raw).split(",").map(v => v.trim()).filter(Boolean);
    if (values.some(v => !options.includes(v))) return { value: null, error: `${field.field_name} contains an invalid option.` };
    return { value: JSON.stringify([...new Set(values)]) };
  }
  return { value: String(raw).trim() };
}

async function validateItemPayload(request: Request, env: Env, oldCategoryId?: string, oldItemId?: string) {
  const body = await readJson(request);
  if (!body) return { response: json(request, env, { ok:false, error:"INVALID_JSON", message:"Invalid request body." }, { status:400 }) };
  const code = itemCode(text(body, "itemCode"));
  const itemName = text(body, "itemName");
  const categoryId = text(body, "categoryId");
  const totalQuantity = int(body, "totalQuantity", -1);
  const rentAmount = int(body, "rentAmount", 0);
  if (!code || code.length > 40 || !itemName || itemName.length > 120 || !categoryId || totalQuantity < 0 || rentAmount < 0 || rentAmount > 100000000) {
    return { response: json(request, env, { ok:false, error:"VALIDATION", message:"Item Code, Item Name, Category, non-negative Total Quantity and Rent are required." }, { status:400 }) };
  }
  const category = await env.DB.prepare(`SELECT id, name, is_active FROM categories WHERE id = ?`).bind(categoryId).first<{id:string;name:string;is_active:number}>();
  if (!category) return { response: json(request, env, { ok:false, error:"VALIDATION", message:"Category not found." }, { status:400 }) };
  if (category.is_active !== 1 && categoryId !== oldCategoryId) return { response: json(request, env, { ok:false, error:"VALIDATION", message:"Inactive category cannot be selected for a new item." }, { status:400 }) };

  const fields = await categoryFields(env, categoryId);
  const rawFieldValues = body.fieldValues && typeof body.fieldValues === "object" && !Array.isArray(body.fieldValues) ? body.fieldValues as Record<string,unknown> : {};
  const controlledClearAllowed = Boolean(oldCategoryId && categoryId === oldCategoryId);
  const requestedClearIds = controlledClearAllowed && Array.isArray(body.clearFieldIds)
    ? [...new Set(body.clearFieldIds.map(v => String(v).trim()).filter(Boolean))]
    : [];
  const fieldIds = new Set(fields.map(field => field.id));
  if (requestedClearIds.some(id => !fieldIds.has(id))) {
    return { response: json(request, env, { ok:false, error:"VALIDATION", message:"One cleared custom field is not valid for this Item category." }, { status:400 }) };
  }
  const clearFieldIds = new Set(requestedClearIds);
  const fieldValues: Array<{fieldId:string; value:string}> = [];
  for (const field of fields) {
    if (clearFieldIds.has(field.id)) continue;
    const raw = rawFieldValues[field.id] ?? (field.default_value ?? undefined);
    const parsed = normalizedFieldValue(field, raw);
    if (parsed.error) return { response: json(request, env, { ok:false, error:"VALIDATION", message:parsed.error }, { status:400 }) };
    if (parsed.value !== null) fieldValues.push({ fieldId:field.id, value:parsed.value });
  }

  const rawImages = Array.isArray(body.imageUrls) ? body.imageUrls : [];
  const images = [...new Set(rawImages.map(v => String(v).trim()).filter(Boolean))].slice(0, 8);
  const legacyImageUrls = new Set<string>();
  if (oldItemId) {
    const existingImages = await env.DB.prepare("SELECT image_url FROM item_images WHERE item_id=?").bind(oldItemId).all<any>();
    for (const row of existingImages.results || []) legacyImageUrls.add(String(row.image_url || ""));
  }
  if (images.some(v => !validImageUrl(v) && !legacyImageUrls.has(v))) return { response: json(request, env, { ok:false, error:"VALIDATION", message:"Every new photo must use a valid HTTPS URL." }, { status:400 }) };
  const cloudinaryAssets:Array<{url:string;publicId:string}> = [];
  const seenAssetUrls = new Set<string>();
  if (Array.isArray(body.cloudinaryAssets)) {
    for (const raw of body.cloudinaryAssets.slice(0, 8)) {
      if (!raw || typeof raw !== "object" || Array.isArray(raw)) continue;
      const url = typeof (raw as any).url === "string" ? String((raw as any).url).trim() : "";
      const publicId = typeof (raw as any).publicId === "string" ? String((raw as any).publicId).trim() : "";
      if (!url || !publicId || publicId.length > 255 || !images.includes(url) || seenAssetUrls.has(url)) continue;
      seenAssetUrls.add(url);
      cloudinaryAssets.push({ url, publicId });
    }
  }
  return { body, code, itemName, categoryId, totalQuantity, rentAmount, category, fieldValues, clearFieldIds: [...clearFieldIds], images, cloudinaryAssets,
    isActive: bool(body, "isActive", true), publicVisible: bool(body, "publicVisible", true) };
}

async function minimumRequiredQuantity(env: Env, itemId: string): Promise<number> {
  const rows = await env.DB.prepare(
    `SELECT b.pickup_date, b.return_date, bi.booked_qty, bi.closed_qty, bi.given_qty, bi.returned_qty
       FROM booking_items bi JOIN bookings b ON b.id = bi.booking_id
      WHERE bi.item_id = ? AND b.status NOT IN ('CANCELLED','RETURNED')`
  ).bind(itemId).all<{pickup_date:string;return_date:string;booked_qty:number;closed_qty:number;given_qty:number;returned_qty:number}>();
  const events = new Map<string, number>();
  let outstandingGiven = 0;
  for (const row of rows.results || []) {
    outstandingGiven += Math.max(0, Number(row.given_qty) - Number(row.returned_qty));
    const start = row.pickup_date, end = row.return_date;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(start) || !/^\d{4}-\d{2}-\d{2}$/.test(end)) continue;
    const activeBooked = Math.max(0, Number(row.booked_qty) - Number(row.closed_qty || 0));
    events.set(start, (events.get(start) || 0) + activeBooked);
    const d = new Date(`${end}T00:00:00Z`); d.setUTCDate(d.getUTCDate() + 1); const after = d.toISOString().slice(0,10);
    events.set(after, (events.get(after) || 0) - activeBooked);
  }
  let running = 0, peak = 0;
  for (const day of [...events.keys()].sort()) { running += events.get(day) || 0; peak = Math.max(peak, running); }
  return Math.max(peak, outstandingGiven);
}

async function itemListData(env: Env, url: URL) {
  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(50, Math.max(5, Number(url.searchParams.get("pageSize") || "20") || 20));
  const search = (url.searchParams.get("search") || "").trim();
  const categoryId = (url.searchParams.get("categoryId") || "").trim();
  const status = (url.searchParams.get("status") || "").trim();
  const publicFilter = (url.searchParams.get("public") || "").trim();
  const showArchived = url.searchParams.get("archived") === "1";
  const where: string[] = [showArchived ? "1=1" : "i.archived_at IS NULL"];
  const binds: unknown[] = [];
  if (search) { where.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))"); binds.push(`%${search}%`,`%${search}%`); }
  if (categoryId) { where.push("i.category_id = ?"); binds.push(categoryId); }
  if (status === "active") where.push("i.is_active = 1"); else if (status === "inactive") where.push("i.is_active = 0");
  if (publicFilter === "yes") where.push("i.public_visible = 1"); else if (publicFilter === "no") where.push("i.public_visible = 0");
  const clause = where.join(" AND ");
  const today = new Date().toISOString().slice(0,10);
  const countStmt = env.DB.prepare(`SELECT COUNT(*) AS count FROM items i WHERE ${clause}`).bind(...binds);
  const rowsStmt = env.DB.prepare(
    `SELECT i.id, i.item_code, i.item_name, i.category_id, c.name AS category_name, i.total_quantity,
            COALESCE(i.rent_amount,0) AS rent_amount,
            i.is_active, i.public_visible, i.archived_at, i.created_at, i.updated_at,
            COALESCE((SELECT SUM(MAX(0, bi.booked_qty - bi.closed_qty - bi.given_qty)) FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id
                      WHERE bi.item_id=i.id AND b.status NOT IN ('CANCELLED','RETURNED') AND b.pickup_date <= ? AND b.return_date >= ?),0) AS booked_qty,
            COALESCE((SELECT SUM(MAX(0, bi.given_qty - bi.returned_qty)) FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id
                      WHERE bi.item_id=i.id AND b.status NOT IN ('CANCELLED','RETURNED')),0) AS given_qty
       FROM items i JOIN categories c ON c.id=i.category_id WHERE ${clause}
      ORDER BY i.updated_at DESC, i.item_name COLLATE NOCASE ASC LIMIT ? OFFSET ?`
  ).bind(today, today, ...binds, pageSize, (page-1)*pageSize);
  const [countRes, rowsRes] = await env.DB.batch([countStmt, rowsStmt]);
  const total = Number((countRes.results?.[0] as any)?.count || 0);
  const items = (rowsRes.results || []) as Array<Record<string,unknown>>;
  if (items.length) {
    const ids = items.map(r=>String(r.id)); const ph=ids.map(()=>"?").join(",");
    const [vals, imgs] = await env.DB.batch([
      env.DB.prepare(`SELECT v.item_id, v.category_field_id, v.value_text, f.field_name, f.field_type FROM item_field_values v JOIN category_fields f ON f.id=v.category_field_id WHERE v.item_id IN (${ph}) ORDER BY f.display_order`).bind(...ids),
      env.DB.prepare(`SELECT id, item_id, image_url, is_primary, display_order FROM item_images WHERE item_id IN (${ph}) ORDER BY item_id, is_primary DESC, display_order`).bind(...ids)
    ]);
    const byItemVals = new Map<string, any[]>(), byItemImgs = new Map<string, any[]>();
    for (const v of vals.results || []) { const id=String((v as any).item_id); const a=byItemVals.get(id)||[]; a.push(v); byItemVals.set(id,a); }
    for (const v of imgs.results || []) { const id=String((v as any).item_id); const a=byItemImgs.get(id)||[]; a.push(v); byItemImgs.set(id,a); }
    for (const row of items) {
      const booked=Number(row.booked_qty||0), given=Number(row.given_qty||0), totalQty=Number(row.total_quantity||0);
      row.available_qty=Math.max(0,totalQty-booked-given); row.field_values=byItemVals.get(String(row.id))||[]; row.images=byItemImgs.get(String(row.id))||[];
    }
  }
  return { items, pagination:{page,pageSize,total,totalPages:Math.max(1,Math.ceil(total/pageSize))} };
}

async function adminItemBootstrap(request: Request, env: Env): Promise<Response> {
  const url = new URL(request.url);
  const [cats, fields, summary] = await env.DB.batch([
    env.DB.prepare(`SELECT id,name,code_prefix,is_active,public_visible,display_order FROM categories ORDER BY display_order,name COLLATE NOCASE`),
    env.DB.prepare(`SELECT id,category_id,field_name,field_type,is_required,default_value,options_json,public_visible,is_active,display_order FROM category_fields ORDER BY category_id,display_order,field_name COLLATE NOCASE`),
    env.DB.prepare(`SELECT COUNT(*) AS item_count, COALESCE(SUM(total_quantity),0) AS total_quantity FROM items WHERE archived_at IS NULL`)
  ]);
  const categoryRows=(cats.results||[]) as any[], fieldRows=(fields.results||[]) as any[];
  const grouped=new Map<string,any[]>(); for(const f of fieldRows){const a=grouped.get(f.category_id)||[]; a.push({...f,is_required:Number(f.is_required)===1,public_visible:Number(f.public_visible)===1,is_active:Number(f.is_active)===1,options:parseOptions(f.options_json)}); grouped.set(f.category_id,a);}
  const categories=categoryRows.map(c=>({...c,is_active:Number(c.is_active)===1,public_visible:Number(c.public_visible)===1,fields:grouped.get(c.id)||[]}));
  const list=await itemListData(env,url); const sum=(summary.results?.[0]||{}) as any;
  return json(request,env,{ok:true,categories,...list,summary:{itemCount:Number(sum.item_count||0),totalQuantity:Number(sum.total_quantity||0)}});
}

async function listItems(request: Request, env: Env): Promise<Response> { return json(request,env,{ok:true,...await itemListData(env,new URL(request.url))}); }

async function createItem(request: Request, env: Env, user: SessionUser): Promise<Response> {
  const parsed = await validateItemPayload(request,env); if ("response" in parsed && parsed.response) return parsed.response;
  const duplicate=await env.DB.prepare(`SELECT id FROM items WHERE item_code=? LIMIT 1`).bind(parsed.code).first();
  if(duplicate) return json(request,env,{ok:false,error:"DUPLICATE_ITEM_CODE",message:"Item Code already exists."},{status:409});
  const id=crypto.randomUUID(); const statements:any[]=[env.DB.prepare(`INSERT INTO items (id,item_code,item_name,category_id,total_quantity,rent_amount,is_active,public_visible) VALUES (?,?,?,?,?,?,?,?)`).bind(id,parsed.code,parsed.itemName,parsed.categoryId,parsed.totalQuantity,parsed.rentAmount,parsed.isActive?1:0,parsed.publicVisible?1:0)];
  for(const v of parsed.fieldValues) statements.push(env.DB.prepare(`INSERT INTO item_field_values (id,item_id,category_field_id,value_text) VALUES (?,?,?,?)`).bind(crypto.randomUUID(),id,v.fieldId,v.value));
  parsed.images.forEach((url,index)=>statements.push(env.DB.prepare(`INSERT INTO item_images (id,item_id,image_url,is_primary,display_order) VALUES (?,?,?,?,?)`).bind(crypto.randomUUID(),id,url,index===0?1:0,index+1)));
  parsed.cloudinaryAssets.forEach(asset=>statements.push(
    env.DB.prepare(`INSERT INTO cloudinary_item_assets(image_url,public_id,item_id,updated_at)
      VALUES (?,?,?,CURRENT_TIMESTAMP)
      ON CONFLICT(image_url) DO UPDATE SET public_id=excluded.public_id,item_id=excluded.item_id,updated_at=CURRENT_TIMESTAMP`)
      .bind(asset.url,asset.publicId,id)
  ));
  statements.push(env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,new_value_json) VALUES(?,?,?,?,?,?)`)
    .bind(crypto.randomUUID(),user.id,"CREATE","ITEM",id,JSON.stringify({itemCode:parsed.code,itemName:parsed.itemName,categoryId:parsed.categoryId,totalQuantity:parsed.totalQuantity,rentAmount:parsed.rentAmount})));
  await env.DB.batch(statements);
  return json(request,env,{ok:true,id,message:"Item added."},{status:201});
}

async function updateItem(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old=await env.DB.prepare(`SELECT * FROM items WHERE id=?`).bind(id).first<any>(); if(!old) return json(request,env,{ok:false,error:"NOT_FOUND",message:"Item not found."},{status:404});
  const parsed=await validateItemPayload(request,env,String(old.category_id)); if("response" in parsed && parsed.response) return parsed.response;
  const duplicate=await env.DB.prepare(`SELECT id FROM items WHERE item_code=? AND id<>? LIMIT 1`).bind(parsed.code,id).first(); if(duplicate) return json(request,env,{ok:false,error:"DUPLICATE_ITEM_CODE",message:"Item Code already exists."},{status:409});
  const used=await env.DB.prepare(`SELECT COUNT(*) AS count FROM booking_items WHERE item_id=?`).bind(id).first<{count:number}>();
  if(String(old.category_id)!==parsed.categoryId && Number(used?.count||0)>0) return json(request,env,{ok:false,error:"ITEM_IN_USE",message:"Category cannot be changed after an item has booking history."},{status:409});
  const minQty=await minimumRequiredQuantity(env,id); if(parsed.totalQuantity<minQty) return json(request,env,{ok:false,error:"QUANTITY_IN_USE",message:`Total Quantity cannot be below ${minQty} because existing bookings/given items require it.`},{status:409});
  const expectedUpdatedAt=text(parsed.body,"expectedUpdatedAt");
  const nextUpdatedAt=new Date().toISOString();
  const guarded=Boolean(expectedUpdatedAt);
  const statements:any[]=[
    guarded
      ? env.DB.prepare(`UPDATE items SET item_code=?,item_name=?,category_id=?,total_quantity=?,rent_amount=?,is_active=?,public_visible=?,updated_at=? WHERE id=? AND updated_at=?`).bind(parsed.code,parsed.itemName,parsed.categoryId,parsed.totalQuantity,parsed.rentAmount,parsed.isActive?1:0,parsed.publicVisible?1:0,nextUpdatedAt,id,expectedUpdatedAt)
      : env.DB.prepare(`UPDATE items SET item_code=?,item_name=?,category_id=?,total_quantity=?,rent_amount=?,is_active=?,public_visible=?,updated_at=? WHERE id=?`).bind(parsed.code,parsed.itemName,parsed.categoryId,parsed.totalQuantity,parsed.rentAmount,parsed.isActive?1:0,parsed.publicVisible?1:0,nextUpdatedAt,id),
    env.DB.prepare(`DELETE FROM item_field_values WHERE item_id=? AND EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`).bind(id,id,nextUpdatedAt),
    env.DB.prepare(`DELETE FROM item_images WHERE item_id=? AND EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`).bind(id,id,nextUpdatedAt)
  ];
  for(const v of parsed.fieldValues) statements.push(env.DB.prepare(`INSERT INTO item_field_values (id,item_id,category_field_id,value_text)
    SELECT ?,?,?,? WHERE EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`).bind(crypto.randomUUID(),id,v.fieldId,v.value,id,nextUpdatedAt));
  parsed.images.forEach((url,index)=>statements.push(env.DB.prepare(`INSERT INTO item_images (id,item_id,image_url,is_primary,display_order)
    SELECT ?,?,?,?,? WHERE EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`).bind(crypto.randomUUID(),id,url,index===0?1:0,index+1,id,nextUpdatedAt)));
  if (parsed.images.length) {
    const placeholders=parsed.images.map(()=>"?").join(",");
    statements.push(env.DB.prepare(`DELETE FROM cloudinary_item_assets
      WHERE item_id=? AND image_url NOT IN (${placeholders})
        AND EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`)
      .bind(id,...parsed.images,id,nextUpdatedAt));
  } else {
    statements.push(env.DB.prepare(`DELETE FROM cloudinary_item_assets
      WHERE item_id=? AND EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`)
      .bind(id,id,nextUpdatedAt));
  }
  parsed.cloudinaryAssets.forEach(asset=>statements.push(
    env.DB.prepare(`INSERT INTO cloudinary_item_assets(image_url,public_id,item_id,updated_at)
      SELECT ?,?,?,CURRENT_TIMESTAMP WHERE EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)
      ON CONFLICT(image_url) DO UPDATE SET public_id=excluded.public_id,item_id=excluded.item_id,updated_at=CURRENT_TIMESTAMP`)
      .bind(asset.url,asset.publicId,id,id,nextUpdatedAt)
  ));
  statements.push(env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
    SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`).bind(
      crypto.randomUUID(),user.id,"UPDATE","ITEM",id,JSON.stringify(old),
      JSON.stringify({itemCode:parsed.code,itemName:parsed.itemName,categoryId:parsed.categoryId,totalQuantity:parsed.totalQuantity,rentAmount:parsed.rentAmount,isActive:parsed.isActive,publicVisible:parsed.publicVisible,clearFieldIds:parsed.clearFieldIds}),
      id,nextUpdatedAt
    ));
  try{
    const result=await env.DB.batch(statements);
    if(guarded && Number((result[0] as any)?.meta?.changes||0)===0){
      return json(request,env,{ok:false,error:"STALE_WRITE",message:"This item changed on another device. Refresh before saving again."},{status:409});
    }
  }catch(error){
    if(dbErrorHas(error,"ITEM_QUANTITY_IN_USE"))return json(request,env,{ok:false,error:"QUANTITY_IN_USE",message:"Total Quantity is now required by another active booking. Refresh and try again."},{status:409});
    throw error;
  }
  return json(request,env,{ok:true,updatedAt:nextUpdatedAt,message:"Item updated."});
}

async function archiveItem(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old=await env.DB.prepare(`SELECT * FROM items WHERE id=?`).bind(id).first<any>(); if(!old) return json(request,env,{ok:false,error:"NOT_FOUND",message:"Item not found."},{status:404});
  const active=await env.DB.prepare(`SELECT COUNT(*) AS count FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id WHERE bi.item_id=? AND b.status NOT IN ('CANCELLED','RETURNED')`).bind(id).first<{count:number}>();
  if(Number(active?.count||0)>0) return json(request,env,{ok:false,error:"ACTIVE_BOOKINGS",message:"Item has active/pending bookings and cannot be archived yet."},{status:409});
  await env.DB.batch([
    env.DB.prepare(`UPDATE items SET archived_at=CURRENT_TIMESTAMP,is_active=0,public_visible=0,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),user.id,"ARCHIVE","ITEM",id,JSON.stringify(old),JSON.stringify({archived:true}))
  ]);
  return json(request,env,{ok:true,message:"Item archived."});
}
async function restoreItem(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old=await env.DB.prepare(`SELECT * FROM items WHERE id=?`).bind(id).first<any>(); if(!old) return json(request,env,{ok:false,error:"NOT_FOUND",message:"Item not found."},{status:404});
  await env.DB.batch([
    env.DB.prepare(`UPDATE items SET archived_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),user.id,"RESTORE","ITEM",id,JSON.stringify(old),JSON.stringify({archived:false}))
  ]);
  return json(request,env,{ok:true,message:"Item restored."});
}
async function deleteItem(request: Request, env: Env, user: SessionUser, id: string): Promise<Response> {
  const old=await env.DB.prepare(`SELECT * FROM items WHERE id=?`).bind(id).first<any>(); if(!old) return json(request,env,{ok:false,error:"NOT_FOUND",message:"Item not found."},{status:404});
  const used=await env.DB.prepare(`SELECT COUNT(*) AS count FROM booking_items WHERE item_id=?`).bind(id).first<{count:number}>(); if(Number(used?.count||0)>0) return json(request,env,{ok:false,error:"ITEM_IN_USE",message:"Item has booking history. Archive it instead of deleting."},{status:409});
  const billUsage=await env.DB.prepare(`SELECT COUNT(*) AS count FROM bill_items WHERE item_id=?`).bind(id).first<{count:number}>();
  await env.DB.batch([
    // Historical Bill rows keep immutable item snapshots. Detach the live master FK.
    env.DB.prepare(`UPDATE bill_items SET item_id=NULL WHERE item_id=?`).bind(id),
    env.DB.prepare(`UPDATE whatsapp_activity_logs
      SET item_name_snapshot=COALESCE(item_name_snapshot,?),item_id=NULL
      WHERE item_id=?`).bind(String(old.item_name||"Deleted Item"),id),
    env.DB.prepare(`DELETE FROM cloudinary_item_assets WHERE item_id=?`).bind(id),
    env.DB.prepare(`DELETE FROM item_field_values WHERE item_id=?`).bind(id),
    env.DB.prepare(`DELETE FROM item_images WHERE item_id=?`).bind(id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),user.id,"DELETE","ITEM",id,JSON.stringify(old),JSON.stringify({deleted:true,billRowsPreserved:Number(billUsage?.count||0),financialHistoryPreserved:true})),
    env.DB.prepare(`DELETE FROM items WHERE id=?`).bind(id)
  ]);
  return json(request,env,{ok:true,message:"Item deleted. Billing history was preserved."});
}


type CustomerRow = {
  id:string; name:string; mobile:string; alternate_mobile:string|null; address:string|null; notes:string|null;
  is_active:number; archived_at:string|null; created_at:string; updated_at:string;
};

function normalizeCustomerMobile(value:string): string { const digits=value.replace(/[^0-9]/g, ""); return digits.length===12 && digits.startsWith("91") ? digits.slice(2) : digits; }
function customerPhoneValid(value:string): boolean { return /^\d{10,15}$/.test(value); }

async function customerListData(env:Env,url:URL){
  const page=Math.max(1,Number(url.searchParams.get("page")||"1")||1);
  const pageSize=Math.min(50,Math.max(5,Number(url.searchParams.get("pageSize")||"20")||20));
  const search=(url.searchParams.get("search")||"").trim();
  const status=(url.searchParams.get("status")||"").trim();
  const showArchived=url.searchParams.get("archived")==="1";
  const where:string[]=[showArchived?"1=1":"c.archived_at IS NULL"]; const binds:unknown[]=[];
  if(search){ const mobileSearch=normalizeCustomerMobile(search); if(mobileSearch){where.push("(LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR COALESCE(c.alternate_mobile,'') LIKE ?)");binds.push(`%${search}%`,`%${mobileSearch}%`,`%${mobileSearch}%`);} else {where.push("LOWER(c.name) LIKE LOWER(?)");binds.push(`%${search}%`);} }
  if(status==="active")where.push("c.is_active=1"); else if(status==="inactive")where.push("c.is_active=0");
  const whereSql=where.join(" AND ");
  const count=await env.DB.prepare(`SELECT COUNT(*) AS count FROM customers c WHERE ${whereSql}`).bind(...binds).first<{count:number}>();
  const total=Number(count?.count||0), offset=(page-1)*pageSize;
  const result=await env.DB.prepare(`
    SELECT c.id,c.name,c.mobile,c.alternate_mobile,c.address,c.notes,c.is_active,c.archived_at,c.created_at,c.updated_at,
           (SELECT COUNT(*) FROM bookings b WHERE b.customer_id=c.id) AS total_bookings,
           (SELECT COUNT(*) FROM bookings b WHERE b.customer_id=c.id AND b.status NOT IN ('CANCELLED','RETURNED')) AS active_bookings,
           (SELECT MAX(b.booking_date) FROM bookings b WHERE b.customer_id=c.id) AS last_booking_date
      FROM customers c WHERE ${whereSql}
     ORDER BY c.name COLLATE NOCASE ASC LIMIT ? OFFSET ?`).bind(...binds,pageSize,offset).all<any>();
  return {customers:(result.results||[]).map(r=>({...r,is_active:Number(r.is_active)===1,total_bookings:Number(r.total_bookings||0),active_bookings:Number(r.active_bookings||0)})),pagination:{page,pageSize,total,totalPages:Math.max(1,Math.ceil(total/pageSize))}};
}

async function listCustomers(request:Request,env:Env):Promise<Response>{return json(request,env,{ok:true,...await customerListData(env,new URL(request.url))});}

async function createCustomer(request:Request,env:Env,user:SessionUser):Promise<Response>{
  const body=await readJson(request);if(!body)return json(request,env,{ok:false,error:"INVALID_JSON"},{status:400});
  const name=text(body,"name");const mobile=normalizeCustomerMobile(text(body,"mobile"));const alternate=normalizeCustomerMobile(text(body,"alternateMobile"));
  const address=text(body,"address");const notes=text(body,"notes");
  if(!name||name.length>120||!customerPhoneValid(mobile))return json(request,env,{ok:false,error:"VALIDATION",message:"Customer Name and a valid 10-15 digit Mobile Number are required."},{status:400});
  if(alternate&&!customerPhoneValid(alternate))return json(request,env,{ok:false,error:"VALIDATION",message:"Alternate Mobile must contain 10-15 digits."},{status:400});
  const dup=await env.DB.prepare(`SELECT id,name,mobile FROM customers WHERE mobile=? LIMIT 1`).bind(mobile).first<any>();
  if(dup)return json(request,env,{ok:false,error:"DUPLICATE_MOBILE",message:`Mobile Number already exists for ${dup.name}.`,existingCustomer:dup},{status:409});
  const id=crypto.randomUUID();
  await env.DB.prepare(`INSERT INTO customers (id,name,mobile,alternate_mobile,address,notes,is_active) VALUES (?,?,?,?,?,?,1)`).bind(id,name,mobile,alternate||null,address||null,notes||null).run();
  await audit(env,user.id,"CREATE","CUSTOMER",id,undefined,{name,mobile,alternateMobile:alternate||null,address:address||null,notes:notes||null});
  return json(request,env,{ok:true,id,message:"Customer added."},{status:201});
}

async function updateCustomer(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT * FROM customers WHERE id=?`).bind(id).first<CustomerRow>();if(!old)return json(request,env,{ok:false,error:"NOT_FOUND",message:"Customer not found."},{status:404});
  const body=await readJson(request);if(!body)return json(request,env,{ok:false,error:"INVALID_JSON"},{status:400});
  const name=text(body,"name")||old.name;const mobile=normalizeCustomerMobile(text(body,"mobile")||old.mobile);const alternate=normalizeCustomerMobile(text(body,"alternateMobile"));
  const address=typeof body.address==="string"?text(body,"address"):(old.address||"");const notes=typeof body.notes==="string"?text(body,"notes"):(old.notes||"");
  if(!name||name.length>120||!customerPhoneValid(mobile))return json(request,env,{ok:false,error:"VALIDATION",message:"Customer Name and a valid 10-15 digit Mobile Number are required."},{status:400});
  if(alternate&&!customerPhoneValid(alternate))return json(request,env,{ok:false,error:"VALIDATION",message:"Alternate Mobile must contain 10-15 digits."},{status:400});
  const dup=await env.DB.prepare(`SELECT id,name FROM customers WHERE mobile=? AND id<>? LIMIT 1`).bind(mobile,id).first<any>();if(dup)return json(request,env,{ok:false,error:"DUPLICATE_MOBILE",message:`Mobile Number already exists for ${dup.name}.`},{status:409});
  const isActive=bool(body,"isActive",old.is_active===1);
  await env.DB.prepare(`UPDATE customers SET name=?,mobile=?,alternate_mobile=?,address=?,notes=?,is_active=?,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(name,mobile,alternate||null,address||null,notes||null,isActive?1:0,id).run();
  await audit(env,user.id,"UPDATE","CUSTOMER",id,old,{name,mobile,alternateMobile:alternate||null,address:address||null,notes:notes||null,isActive});
  return json(request,env,{ok:true,message:"Customer updated."});
}

async function archiveCustomer(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT * FROM customers WHERE id=?`).bind(id).first<CustomerRow>();if(!old)return json(request,env,{ok:false,error:"NOT_FOUND",message:"Customer not found."},{status:404});
  const active=await env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings WHERE customer_id=? AND status NOT IN ('CANCELLED','RETURNED')`).bind(id).first<{count:number}>();
  if(Number(active?.count||0)>0)return json(request,env,{ok:false,error:"ACTIVE_BOOKINGS",message:"Customer has active/pending bookings and cannot be archived yet."},{status:409});
  await env.DB.prepare(`UPDATE customers SET archived_at=CURRENT_TIMESTAMP,is_active=0,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(id).run();await audit(env,user.id,"ARCHIVE","CUSTOMER",id,old,{archived:true});return json(request,env,{ok:true,message:"Customer archived."});
}
async function restoreCustomer(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT * FROM customers WHERE id=?`).bind(id).first<CustomerRow>();if(!old)return json(request,env,{ok:false,error:"NOT_FOUND",message:"Customer not found."},{status:404});
  await env.DB.prepare(`UPDATE customers SET archived_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(id).run();await audit(env,user.id,"RESTORE","CUSTOMER",id,old,{archived:false});return json(request,env,{ok:true,message:"Customer restored."});
}
async function deleteCustomer(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT * FROM customers WHERE id=?`).bind(id).first<CustomerRow>();if(!old)return json(request,env,{ok:false,error:"NOT_FOUND",message:"Customer not found."},{status:404});
  const used=await env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings WHERE customer_id=?`).bind(id).first<{count:number}>();if(Number(used?.count||0)>0)return json(request,env,{ok:false,error:"CUSTOMER_IN_USE",message:"Customer has booking history. Archive it instead of deleting."},{status:409});
  await env.DB.prepare(`DELETE FROM customers WHERE id=?`).bind(id).run();await audit(env,user.id,"DELETE","CUSTOMER",id,old,undefined);return json(request,env,{ok:true,message:"Customer deleted."});
}

async function customerHistory(request:Request,env:Env,id:string):Promise<Response>{
  const customer=await env.DB.prepare(`SELECT id,name,mobile,alternate_mobile,address,notes,is_active,archived_at,created_at,updated_at FROM customers WHERE id=?`).bind(id).first<any>();
  if(!customer)return json(request,env,{ok:false,error:"NOT_FOUND",message:"Customer not found."},{status:404});
  const bookings=await env.DB.prepare(`
    SELECT b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,b.notes,b.created_at,
           u.name AS booked_by,
           COALESCE(GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', '),'') AS items_summary,
           COALESCE(SUM(bi.booked_qty),0) AS booked_qty,
           COALESCE(SUM(bi.given_qty),0) AS given_qty,
           COALESCE(SUM(bi.returned_qty),0) AS returned_qty
      FROM bookings b LEFT JOIN users u ON u.id=b.created_by_user_id
      LEFT JOIN booking_items bi ON bi.booking_id=b.id LEFT JOIN items i ON i.id=bi.item_id
     WHERE b.customer_id=? GROUP BY b.id ORDER BY b.booking_date DESC,b.created_at DESC LIMIT 100`).bind(id).all<any>();
  return json(request,env,{ok:true,customer:{...customer,is_active:Number(customer.is_active)===1},bookings:bookings.results||[]});
}


type BookingLineInput = { itemId: string; quantity: number };
type AvailabilityRow = { id:string; item_code:string; item_name:string; total_quantity:number; is_active:number; archived_at:string|null; reserved_qty:number };

function validDateOnly(value:string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date=new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0,10)===value;
}
function addIsoDays(value:string,days:number): string {
  const date=new Date(`${value}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate()+days);
  return date.toISOString().slice(0,10);
}
function istDayStartUtc(value:string): string {
  return new Date(`${value}T00:00:00+05:30`).toISOString().slice(0,19).replace('T',' ');
}
function businessToday(): string {
  try { return new Intl.DateTimeFormat('en-CA',{ timeZone:'Asia/Kolkata', year:'numeric', month:'2-digit', day:'2-digit' }).format(new Date()); }
  catch { return new Date().toISOString().slice(0,10); }
}
function bookingNumber(): string { return `BK-${businessToday().replace(/-/g,'')}-${crypto.randomUUID().slice(0,6).toUpperCase()}`; }

function parseBookingLines(body: JsonBody): BookingLineInput[] | null {
  if (!Array.isArray(body.items) || body.items.length < 1 || body.items.length > 30) return null;
  const result: BookingLineInput[] = []; const seen = new Set<string>();
  for (const raw of body.items) {
    if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return null;
    const itemId = typeof (raw as any).itemId === 'string' ? String((raw as any).itemId).trim() : '';
    const quantity = Number((raw as any).quantity);
    if (!itemId || !Number.isInteger(quantity) || quantity <= 0 || quantity > 100000 || seen.has(itemId)) return null;
    seen.add(itemId); result.push({itemId,quantity});
  }
  return result;
}

async function availabilityRows(env:Env,pickupDate:string,returnDate:string,itemIds:string[],excludeBookingId:string|null=null):Promise<AvailabilityRow[]> {
  if (!itemIds.length) return [];
  const placeholders=itemIds.map(()=>'?').join(',');
  const result=await env.DB.prepare(`
    SELECT i.id,i.item_code,i.item_name,i.total_quantity,i.is_active,i.archived_at,
           COALESCE(SUM(CASE
             WHEN b.id IS NULL THEN 0
             WHEN b.pickup_date <= ? AND b.return_date >= ? THEN bi.booked_qty-bi.closed_qty
             WHEN b.return_date < ? AND bi.given_qty > bi.returned_qty THEN bi.given_qty-bi.returned_qty
             ELSE 0 END),0) AS reserved_qty
      FROM items i
      LEFT JOIN booking_items bi ON bi.item_id=i.id
      LEFT JOIN bookings b ON b.id=bi.booking_id
       AND b.status NOT IN ('CANCELLED','RETURNED')
       AND (? IS NULL OR b.id <> ?)
     WHERE i.id IN (${placeholders})
     GROUP BY i.id
  `).bind(returnDate,pickupDate,pickupDate,excludeBookingId,excludeBookingId,...itemIds).all<any>();
  return (result.results||[]).map(r=>({...r,total_quantity:Number(r.total_quantity||0),is_active:Number(r.is_active||0),reserved_qty:Number(r.reserved_qty||0)}));
}

async function validateBookingPayload(request:Request,env:Env,oldBookingId:string|null=null,oldCustomerId:string|null=null,existingItemIds:Set<string>=new Set()) {
  const body=await readJson(request); if(!body) return {response:json(request,env,{ok:false,error:'INVALID_JSON',message:'Invalid request body.'},{status:400})};
  const customerId=text(body,'customerId'), pickupDate=text(body,'pickupDate'), returnDate=text(body,'returnDate'), notes=text(body,'notes');
  const items=parseBookingLines(body);
  if(!customerId || !validDateOnly(pickupDate) || !validDateOnly(returnDate) || !items){
    return {response:json(request,env,{ok:false,error:'VALIDATION',message:'Customer, valid Pickup/Return dates and at least one item with positive quantity are required.'},{status:400})};
  }
  if(pickupDate<businessToday()) {
    return {response:json(request,env,{ok:false,error:'PAST_PICKUP_DATE',message:'Past pickup dates are not allowed.'},{status:400})};
  }
  if(returnDate<=pickupDate) {
    return {response:json(request,env,{ok:false,error:'INVALID_RENTAL_DATE_RANGE',message:'Return Date must be at least one day after Pickup Date.'},{status:400})};
  }
  const customer=await env.DB.prepare(`SELECT id,name,mobile,is_active,archived_at FROM customers WHERE id=?`).bind(customerId).first<any>();
  if(!customer) return {response:json(request,env,{ok:false,error:'CUSTOMER_NOT_FOUND',message:'Customer not found.'},{status:400})};
  if((Number(customer.is_active)!==1 || customer.archived_at) && customerId!==oldCustomerId) return {response:json(request,env,{ok:false,error:'CUSTOMER_INACTIVE',message:'Inactive/archived customer cannot be selected for a new booking.'},{status:409})};
  const rows=await availabilityRows(env,pickupDate,returnDate,items.map(v=>v.itemId),oldBookingId);
  const map=new Map(rows.map(r=>[r.id,r]));
  for(const line of items){
    const row=map.get(line.itemId); if(!row) return {response:json(request,env,{ok:false,error:'ITEM_NOT_FOUND',message:'One selected item no longer exists.'},{status:400})};
    if((row.is_active!==1 || row.archived_at) && !existingItemIds.has(line.itemId)) return {response:json(request,env,{ok:false,error:'ITEM_INACTIVE',message:`${row.item_name} is inactive or archived.`},{status:409})};
    const available=Math.max(0,row.total_quantity-row.reserved_qty);
    if(line.quantity>available) return {response:json(request,env,{ok:false,error:'INSUFFICIENT_AVAILABILITY',message:`${row.item_name}: only ${available} quantity is available for the selected dates.`,itemId:line.itemId,availableQuantity:available},{status:409})};
  }
  return {body,customerId,pickupDate,returnDate,notes,items,customer,availability:rows};
}

async function bookingListData(env:Env,url:URL){
  const page=Math.max(1,Number(url.searchParams.get('page')||'1')||1); const pageSize=Math.min(50,Math.max(5,Number(url.searchParams.get('pageSize')||'20')||20));
  const search=(url.searchParams.get('search')||'').trim(), status=(url.searchParams.get('status')||'').trim(), from=(url.searchParams.get('from')||'').trim(), to=(url.searchParams.get('to')||'').trim();
  const where:string[]=['1=1']; const binds:unknown[]=[];
  if(search){where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))))`); const q=`%${search}%`;binds.push(q,q,q,q,q);}
  if(status){where.push('b.status=?');binds.push(status)}
  if(validDateOnly(from)){where.push('b.pickup_date>=?');binds.push(from)}
  if(validDateOnly(to)){where.push('b.return_date<=?');binds.push(to)}
  const clause=where.join(' AND ');
  const [countRes,rowsRes]=await env.DB.batch([
    env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`SELECT b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,b.notes,b.created_at,b.updated_at,
      c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,u.name AS booked_by,
      COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items_summary,
      COALESCE((SELECT SUM(bi.booked_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS booked_qty,
      COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS given_qty,
      COALESCE((SELECT SUM(bi.returned_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS returned_qty
      FROM bookings b JOIN customers c ON c.id=b.customer_id LEFT JOIN users u ON u.id=b.created_by_user_id
      WHERE ${clause} ORDER BY b.created_at DESC LIMIT ? OFFSET ?`).bind(...binds,pageSize,(page-1)*pageSize)
  ]);
  const total=Number((countRes.results?.[0] as any)?.count||0);
  return {bookings:rowsRes.results||[],pagination:{page,pageSize,total,totalPages:Math.max(1,Math.ceil(total/pageSize))}};
}

async function listBookings(request:Request,env:Env):Promise<Response>{return json(request,env,{ok:true,...await bookingListData(env,new URL(request.url))});}

async function bookingBootstrap(request:Request,env:Env):Promise<Response>{
  const [customers,items,relatedItems]=await env.DB.batch([
    env.DB.prepare(`SELECT id,name,mobile FROM customers WHERE is_active=1 AND archived_at IS NULL ORDER BY updated_at DESC,name COLLATE NOCASE LIMIT 50`),
    env.DB.prepare(`SELECT i.id,i.item_code,i.item_name,i.category_id,c.name AS category_name,i.total_quantity,
      COALESCE(i.rent_amount,0) AS rent_amount,
      (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order LIMIT 1) AS image_url,
      COALESCE((SELECT json_group_array(image_url) FROM (
        SELECT image_url FROM item_images im2 WHERE im2.item_id=i.id
        ORDER BY im2.is_primary DESC,im2.display_order ASC LIMIT 8
      )),'[]') AS images_json
      FROM items i JOIN categories c ON c.id=i.category_id WHERE i.is_active=1 AND i.archived_at IS NULL ORDER BY c.display_order,c.name,i.item_name LIMIT 300`),
    env.DB.prepare(`
      SELECT r.source_item_id,r.related_item_id,r.display_order
      FROM item_related_items r
      JOIN items source ON source.id=r.source_item_id
      JOIN items related ON related.id=r.related_item_id
      WHERE source.is_active=1 AND source.archived_at IS NULL
        AND related.is_active=1 AND related.archived_at IS NULL
      ORDER BY r.source_item_id,r.display_order,r.related_item_id
      LIMIT 5000
    `)
  ]);
  return json(request,env,{
    ok:true,
    customers:customers.results||[],
    items:items.results||[],
    relatedItems:relatedItems.results||[]
  });
}

async function bookingAvailability(request:Request,env:Env):Promise<Response>{
  const body=await readJson(request); if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const pickupDate=text(body,'pickupDate'), returnDate=text(body,'returnDate'), excludeBookingId=text(body,'excludeBookingId')||null;
  const ids=Array.isArray(body.itemIds)?[...new Set(body.itemIds.map(v=>String(v).trim()).filter(Boolean))].slice(0,30):[];
  if(!validDateOnly(pickupDate)||!validDateOnly(returnDate)||!ids.length)return json(request,env,{ok:false,error:'VALIDATION',message:'Valid Pickup Date, Return Date and selected items are required.'},{status:400});
  if(pickupDate<businessToday()) return json(request,env,{ok:false,error:'PAST_PICKUP_DATE',message:'Past pickup dates are not allowed.'},{status:400});
  if(returnDate<=pickupDate) return json(request,env,{ok:false,error:'INVALID_RENTAL_DATE_RANGE',message:'Return Date must be at least one day after Pickup Date.'},{status:400});
  const rows=await availabilityRows(env,pickupDate,returnDate,ids,excludeBookingId);
  return json(request,env,{ok:true,availability:rows.map(r=>({itemId:r.id,itemCode:r.item_code,itemName:r.item_name,totalQuantity:r.total_quantity,reservedQuantity:r.reserved_qty,availableQuantity:Math.max(0,r.total_quantity-r.reserved_qty)}))});
}

async function createBooking(request:Request,env:Env,user:SessionUser):Promise<Response>{
  const parsed=await validateBookingPayload(request,env); if('response' in parsed && parsed.response)return parsed.response;
  const requestKey=text(parsed.body,'requestKey');
  const advanceAmount=int(parsed.body,'advanceAmount',0);
  const confirmationState=text(parsed.body,'confirmationState').toUpperCase() || 'BOOKED';
  if(!['BOOKED','RESERVED'].includes(confirmationState))return json(request,env,{ok:false,error:'VALIDATION',message:'Invalid booking confirmation state.'},{status:400});
  if(advanceAmount<0 || advanceAmount>100000000)return json(request,env,{ok:false,error:'VALIDATION',message:'Advance Amount must be zero or a positive amount.'},{status:400});
  if(requestKey.length<8 || requestKey.length>120)return json(request,env,{ok:false,error:'VALIDATION',message:'A valid booking request key is required.'},{status:400});
  const existing=await env.DB.prepare(`SELECT id,booking_no,confirmation_state FROM bookings WHERE request_key=? LIMIT 1`).bind(requestKey).first<any>();
  if(existing)return json(request,env,{ok:true,duplicate:true,id:existing.id,bookingNo:existing.booking_no,confirmationState:String(existing.confirmation_state||'BOOKED'),message:'Booking was already saved.'});
  const id=crypto.randomUUID(), no=bookingNumber(), bookingDate=businessToday();
  const statements:any[]=[env.DB.prepare(`INSERT INTO bookings (id,booking_no,customer_id,booking_date,pickup_date,return_date,status,confirmation_state,advance_amount,notes,created_by_user_id,request_key) VALUES (?,?,?,?,?,?,'BOOKED',?,?,?,?,?)`).bind(id,no,parsed.customerId,bookingDate,parsed.pickupDate,parsed.returnDate,confirmationState,advanceAmount,parsed.notes||null,user.id,requestKey)];
  for(const line of parsed.items)statements.push(env.DB.prepare(`INSERT INTO booking_items (id,booking_id,item_id,booked_qty,given_qty,returned_qty) VALUES (?,?,?,?,0,0)`).bind(crypto.randomUUID(),id,line.itemId,line.quantity));
  statements.push(env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json) VALUES (?,?,?,?,?,?)`).bind(crypto.randomUUID(),user.id,'CREATE','BOOKING',id,JSON.stringify({bookingNo:no,customerId:parsed.customerId,pickupDate:parsed.pickupDate,returnDate:parsed.returnDate,confirmationState,advanceAmount,items:parsed.items})));
  try{await env.DB.batch(statements);}catch(error){
    const duplicate=await env.DB.prepare(`SELECT id,booking_no,confirmation_state FROM bookings WHERE request_key=? LIMIT 1`).bind(requestKey).first<any>();
    if(duplicate)return json(request,env,{ok:true,duplicate:true,id:duplicate.id,bookingNo:duplicate.booking_no,confirmationState:String(duplicate.confirmation_state||'BOOKED'),message:'Booking was already saved.'});
    if(dbErrorHas(error,'INSUFFICIENT_AVAILABILITY'))return json(request,env,{ok:false,error:'INSUFFICIENT_AVAILABILITY',message:'Availability changed while saving. Please review quantities and try again.'},{status:409});
    throw error;
  }
  return json(request,env,{ok:true,id,bookingNo:no,confirmationState,message:confirmationState==='RESERVED'?'Booking reserved.':'Booking created.'},{status:201});
}

async function updateBooking(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT * FROM bookings WHERE id=?`).bind(id).first<any>(); if(!old)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  const oldItems=(await env.DB.prepare(`SELECT item_id,booked_qty,given_qty,returned_qty FROM booking_items WHERE booking_id=?`).bind(id).all<any>()).results||[];
  if(old.status!=='BOOKED' || String(old.confirmation_state||'BOOKED')!=='RESERVED' || oldItems.some((v:any)=>Number(v.given_qty)>0))return json(request,env,{ok:false,error:'BOOKING_LOCKED',message:'Only a Reserved Booking can be edited before pickup starts.'},{status:409});
  const parsed=await validateBookingPayload(request,env,id,old.customer_id,new Set(oldItems.map((v:any)=>String(v.item_id)))); if('response' in parsed && parsed.response)return parsed.response;
  const advanceAmount=int(parsed.body,'advanceAmount',Number(old.advance_amount||0));
  if(advanceAmount<0 || advanceAmount>100000000)return json(request,env,{ok:false,error:'VALIDATION',message:'Advance Amount must be zero or a positive amount.'},{status:400});
  const expectedUpdatedAt=text(parsed.body,'expectedUpdatedAt');
  const nextUpdatedAt=new Date().toISOString();
  const guarded=Boolean(expectedUpdatedAt);
  const statements:any[]=[
    guarded
      ? env.DB.prepare(`UPDATE bookings SET customer_id=?,pickup_date=?,return_date=?,advance_amount=?,notes=?,updated_at=? WHERE id=? AND updated_at=? AND status='BOOKED' AND confirmation_state='RESERVED'`).bind(parsed.customerId,parsed.pickupDate,parsed.returnDate,advanceAmount,parsed.notes||null,nextUpdatedAt,id,expectedUpdatedAt)
      : env.DB.prepare(`UPDATE bookings SET customer_id=?,pickup_date=?,return_date=?,advance_amount=?,notes=?,updated_at=? WHERE id=? AND status='BOOKED' AND confirmation_state='RESERVED'`).bind(parsed.customerId,parsed.pickupDate,parsed.returnDate,advanceAmount,parsed.notes||null,nextUpdatedAt,id),
    env.DB.prepare(`DELETE FROM booking_items WHERE booking_id=? AND EXISTS (SELECT 1 FROM bookings WHERE id=? AND updated_at=?)`).bind(id,id,nextUpdatedAt)
  ];
  for(const line of parsed.items)statements.push(env.DB.prepare(`INSERT INTO booking_items (id,booking_id,item_id,booked_qty,given_qty,returned_qty)
    SELECT ?,?,?,?,0,0 WHERE EXISTS (SELECT 1 FROM bookings WHERE id=? AND updated_at=?)`).bind(crypto.randomUUID(),id,line.itemId,line.quantity,id,nextUpdatedAt));
  statements.push(env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json)
    SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM bookings WHERE id=? AND updated_at=?)`).bind(
      crypto.randomUUID(),user.id,'UPDATE','BOOKING',id,
      JSON.stringify({booking:old,items:oldItems}),
      JSON.stringify({customerId:parsed.customerId,pickupDate:parsed.pickupDate,returnDate:parsed.returnDate,advanceAmount,notes:parsed.notes,items:parsed.items}),
      id,nextUpdatedAt
    ));
  try{
    const result=await env.DB.batch(statements);
    if(guarded && Number((result[0] as any)?.meta?.changes||0)===0){
      return json(request,env,{ok:false,error:'STALE_WRITE',message:'This booking changed on another device. Refresh and review before saving again.'},{status:409});
    }
  }catch(error){
    if(dbErrorHas(error,'INSUFFICIENT_AVAILABILITY'))return json(request,env,{ok:false,error:'INSUFFICIENT_AVAILABILITY',message:'Availability changed while updating. The original booking was kept unchanged; review quantities and try again.'},{status:409});
    throw error;
  }
  return json(request,env,{ok:true,updatedAt:nextUpdatedAt,message:'Booking updated.'});
}

async function cancelBooking(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT b.*,COALESCE(SUM(bi.given_qty),0) AS given_qty FROM bookings b LEFT JOIN booking_items bi ON bi.booking_id=b.id WHERE b.id=? GROUP BY b.id`).bind(id).first<any>();
  if(!old)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  if(old.status==='CANCELLED')return json(request,env,{ok:true,message:'Booking is already cancelled.'});
  if(old.status!=='BOOKED'||Number(old.given_qty||0)>0)return json(request,env,{ok:false,error:'BOOKING_LOCKED',message:'Only a booking with no pickup can be cancelled.'},{status:409});

  const body=(await readJson(request))||{};
  const advanceAmount=Math.max(0,Number(old.advance_amount||0));
  let settlementStatus=text(body,'advanceSettlementStatus').toUpperCase();
  let refundAmount=int(body,'advanceRefundAmount',0);
  if(advanceAmount>0){
    if(!['FULL_REFUND','PARTIAL_REFUND','NO_REFUND'].includes(settlementStatus)){
      return json(request,env,{ok:false,error:'ADVANCE_SETTLEMENT_REQUIRED',message:'Select Full Refund, Partial Refund or No Refund for the Advance Amount.'},{status:400});
    }
    if(settlementStatus==='FULL_REFUND') refundAmount=advanceAmount;
    else if(settlementStatus==='NO_REFUND') refundAmount=0;
    else if(refundAmount<=0 || refundAmount>=advanceAmount){
      return json(request,env,{ok:false,error:'INVALID_REFUND_AMOUNT',message:'Partial Refund must be more than ₹0 and less than the Advance Amount.'},{status:400});
    }
  }else{
    settlementStatus='';
    refundAmount=0;
  }

  const linkedBill=await env.DB.prepare(`SELECT * FROM bills WHERE booking_id=? LIMIT 1`).bind(id).first<any>();
  const cancelledAt=new Date().toISOString();
  const settlementAt=advanceAmount>0?cancelledAt:null;
  const settlementUser=advanceAmount>0?user.id:null;
  const nextAudit={
    status:'CANCELLED',
    advanceAmount,
    advanceSettlementStatus:settlementStatus||null,
    advanceRefundAmount:refundAmount,
    advanceRetainedAmount:Math.max(0,advanceAmount-refundAmount)
  };
  const bookingOpenSql=`EXISTS (
    SELECT 1 FROM bookings bx
    WHERE bx.id=? AND bx.status='BOOKED'
      AND NOT EXISTS (SELECT 1 FROM booking_items bix WHERE bix.booking_id=bx.id AND bix.given_qty>0)
  )`;
  const statements:any[]=[
    env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json)
      SELECT ?,?,?,?,?,?,? WHERE ${bookingOpenSql}`).bind(
        crypto.randomUUID(),user.id,'CANCEL','BOOKING',id,JSON.stringify(old),JSON.stringify(nextAudit),id
      )
  ];

  if(linkedBill && ['DRAFT','FINAL'].includes(String(linkedBill.status))){
    statements.push(
      env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json)
        SELECT ?,?,?,?,?,?,? WHERE EXISTS (
          SELECT 1 FROM bills bl WHERE bl.id=? AND bl.status IN ('DRAFT','FINAL')
        ) AND ${bookingOpenSql}`).bind(
          crypto.randomUUID(),user.id,'CANCEL','BILL',String(linkedBill.id),JSON.stringify(linkedBill),
          JSON.stringify({status:'CANCELLED',reason:'Order cancelled'}),String(linkedBill.id),id
        )
    );
    statements.push(
      env.DB.prepare(`UPDATE bills
        SET status='CANCELLED',cancelled_by_user_id=?,cancelled_at=?,cancellation_reason='Order cancelled',updated_at=?
        WHERE booking_id=? AND status IN ('DRAFT','FINAL') AND ${bookingOpenSql}`)
        .bind(user.id,cancelledAt,cancelledAt,id,id)
    );
  }

  statements.push(
    env.DB.prepare(`UPDATE bookings
      SET status='CANCELLED',cancelled_at=?,advance_refund_amount=?,advance_settlement_status=?,
          advance_settled_at=?,advance_settled_by_user_id=?,updated_at=?
      WHERE id=? AND status='BOOKED'
        AND NOT EXISTS (SELECT 1 FROM booking_items bi WHERE bi.booking_id=? AND bi.given_qty>0)`)
      .bind(cancelledAt,refundAmount,settlementStatus||null,settlementAt,settlementUser,cancelledAt,id,id)
  );

  const result=await env.DB.batch(statements);
  const bookingResult=result[result.length-1] as any;
  if(Number(bookingResult?.meta?.changes||0)===0){
    const current=await env.DB.prepare(`SELECT status FROM bookings WHERE id=?`).bind(id).first<any>();
    if(String(current?.status||'')==='CANCELLED')return json(request,env,{ok:true,duplicate:true,message:'Booking is already cancelled.'});
    return json(request,env,{ok:false,error:'BOOKING_LOCKED',message:'Only a booking with no pickup can be cancelled.'},{status:409});
  }
  const message=advanceAmount>0
    ? `Booking cancelled. Advance settlement saved: refund ₹${refundAmount}, retained ₹${Math.max(0,advanceAmount-refundAmount)}.`
    : 'Booking cancelled. Reserved quantity is released.';
  return json(request,env,{ok:true,message,advanceSettlementStatus:settlementStatus||null,advanceRefundAmount:refundAmount});
}

async function bookingDetail(request:Request,env:Env,id:string):Promise<Response>{
  const booking=await env.DB.prepare(`SELECT b.*,c.name AS customer_name,c.mobile AS customer_mobile,c.alternate_mobile AS customer_alternate_mobile,u.name AS booked_by
    FROM bookings b JOIN customers c ON c.id=b.customer_id LEFT JOIN users u ON u.id=b.created_by_user_id WHERE b.id=?`).bind(id).first<any>();
  if(!booking)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  const [items,timeline]=await env.DB.batch([
    env.DB.prepare(`SELECT bi.id AS booking_item_id,bi.item_id,bi.booked_qty,bi.given_qty,bi.returned_qty,i.item_code,i.item_name,c.name AS category_name,
      (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order LIMIT 1) AS image_url,
      COALESCE((SELECT json_group_array(image_url) FROM (
        SELECT image_url FROM item_images im2 WHERE im2.item_id=i.id
        ORDER BY im2.is_primary DESC,im2.display_order ASC LIMIT 8
      )),'[]') AS images_json
      FROM booking_items bi JOIN items i ON i.id=bi.item_id JOIN categories c ON c.id=i.category_id WHERE bi.booking_id=? ORDER BY i.item_name`).bind(id),
    env.DB.prepare(`SELECT a.id,a.action,a.old_value_json,a.new_value_json,a.created_at,u.name AS user_name FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id WHERE a.module='BOOKING' AND a.record_id=? ORDER BY a.created_at DESC LIMIT 100`).bind(id)
  ]);
  return json(request,env,{ok:true,booking,items:items.results||[],timeline:timeline.results||[]});
}


type PickupLineInput = { bookingItemId:string; quantity:number };

function parsePickupLines(body: JsonBody, allowZero=false): PickupLineInput[] | null {
  if (!Array.isArray(body.items) || body.items.length < 1 || body.items.length > 30) return null;
  const result:PickupLineInput[]=[]; const seen=new Set<string>();
  for(const raw of body.items){
    if(!raw || typeof raw!=='object' || Array.isArray(raw)) return null;
    const bookingItemId=typeof (raw as any).bookingItemId==='string'?String((raw as any).bookingItemId).trim():'';
    const quantity=Number((raw as any).quantity);
    if(!bookingItemId || !Number.isInteger(quantity) || quantity < (allowZero?0:1) || quantity>100000 || seen.has(bookingItemId)) return null;
    seen.add(bookingItemId); result.push({bookingItemId,quantity});
  }
  return result;
}

async function pickupListData(env:Env,url:URL){
  const view=(url.searchParams.get('view')||'today').trim();
  const search=(url.searchParams.get('search')||'').trim();
  const page=Math.max(1,Number(url.searchParams.get('page')||'1')||1);
  const pageSize=Math.min(50,Math.max(5,Number(url.searchParams.get('pageSize')||'20')||20));
  const today=businessToday(); const where:string[]=[`b.status NOT IN ('CANCELLED','RETURNED')`]; const binds:unknown[]=[];
  if(view==='today'){where.push('b.pickup_date=?');binds.push(today)}
  else if(view==='upcoming'){where.push(`b.pickup_date>? AND b.status IN ('BOOKED','PARTIALLY_GIVEN')`);binds.push(today)}
  else if(view==='partial'){where.push(`b.status='PARTIALLY_GIVEN'`)}
  else if(view==='completed'){where.push(`b.status='GIVEN'`)}
  else { where.push(`b.status IN ('BOOKED','PARTIALLY_GIVEN','GIVEN')`); }
  if(search){const q=`%${search}%`;where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))))`);binds.push(q,q,q,q,q)}
  const clause=where.join(' AND ');
  const [countRes,rowsRes]=await env.DB.batch([
    env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.created_at,c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,
      COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items_summary,
      COALESCE((SELECT SUM(bi.booked_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS booked_qty,
      COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS given_qty,
      COALESCE((SELECT SUM(MAX(0,bi.booked_qty-bi.closed_qty-bi.given_qty)) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS remaining_qty,
      COALESCE((SELECT MAX(pe.pickup_at) FROM pickup_events pe WHERE pe.booking_id=b.id),NULL) AS last_pickup_at
      FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE ${clause}
      ORDER BY CASE WHEN b.pickup_date=? THEN 0 WHEN b.pickup_date>? THEN 1 ELSE 2 END,b.pickup_date ASC,b.created_at DESC LIMIT ? OFFSET ?`).bind(...binds,today,today,pageSize,(page-1)*pageSize)
  ]);
  const total=Number((countRes.results?.[0] as any)?.count||0);
  return {pickups:rowsRes.results||[],pagination:{page,pageSize,total,totalPages:Math.max(1,Math.ceil(total/pageSize))},today};
}

async function listPickups(request:Request,env:Env):Promise<Response>{return json(request,env,{ok:true,...await pickupListData(env,new URL(request.url))});}

async function pickupBootstrap(request:Request,env:Env):Promise<Response>{
  return json(request,env,{ok:true,...await pickupListData(env,new URL(request.url))});
}

async function pickupDetail(request:Request,env:Env,bookingId:string):Promise<Response>{
  const booking=await env.DB.prepare(`SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.notes,c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,c.alternate_mobile AS customer_alternate_mobile,u.name AS booked_by
    FROM bookings b JOIN customers c ON c.id=b.customer_id LEFT JOIN users u ON u.id=b.created_by_user_id WHERE b.id=?`).bind(bookingId).first<any>();
  if(!booking)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  const [items,events,eventItems]=await env.DB.batch([
    env.DB.prepare(`SELECT bi.id AS booking_item_id,bi.item_id,bi.booked_qty,bi.given_qty,bi.returned_qty,(bi.booked_qty-bi.closed_qty-bi.given_qty) AS remaining_to_give,i.item_code,i.item_name,c.name AS category_name,
      (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order LIMIT 1) AS image_url,
      COALESCE((SELECT json_group_array(image_url) FROM (
        SELECT image_url FROM item_images im2 WHERE im2.item_id=i.id
        ORDER BY im2.is_primary DESC,im2.display_order ASC LIMIT 8
      )),'[]') AS images_json
      FROM booking_items bi JOIN items i ON i.id=bi.item_id JOIN categories c ON c.id=i.category_id WHERE bi.booking_id=? ORDER BY i.item_name`).bind(bookingId),
    env.DB.prepare(`SELECT pe.id,pe.pickup_at,pe.notes,pe.correction_of_event_id,pe.created_at,u.name AS given_by,c.name AS given_to,
      COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || pei.qty_given, ', ') FROM pickup_event_items pei JOIN booking_items bi ON bi.id=pei.booking_item_id JOIN items i ON i.id=bi.item_id WHERE pei.pickup_event_id=pe.id),'') AS items_summary
      FROM pickup_events pe JOIN users u ON u.id=pe.handled_by_user_id JOIN customers c ON c.id=pe.given_to_customer_id
      WHERE pe.booking_id=? ORDER BY pe.created_at DESC`).bind(bookingId),
    env.DB.prepare(`SELECT pei.pickup_event_id,pei.booking_item_id,pei.qty_given,i.item_code,i.item_name
      FROM pickup_event_items pei JOIN booking_items bi ON bi.id=pei.booking_item_id JOIN items i ON i.id=bi.item_id
      JOIN pickup_events pe ON pe.id=pei.pickup_event_id WHERE pe.booking_id=? ORDER BY pei.pickup_event_id,i.item_name`).bind(bookingId)
  ]);
  return json(request,env,{ok:true,booking,items:items.results||[],events:events.results||[],eventItems:eventItems.results||[]});
}

async function createPickup(request:Request,env:Env,user:SessionUser,bookingId:string):Promise<Response>{
  const body=await readJson(request); if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const requestKey=text(body,'requestKey'), notes=text(body,'notes'); const lines=parsePickupLines(body,false);
  if(requestKey.length<8 || requestKey.length>120 || !lines)return json(request,env,{ok:false,error:'VALIDATION',message:'A valid request key and at least one positive Give Now quantity are required.'},{status:400});
  const existing=await env.DB.prepare(`SELECT id,booking_id FROM pickup_events WHERE request_key=?`).bind(requestKey).first<any>();
  if(existing)return json(request,env,{ok:true,duplicate:true,eventId:existing.id,message:'Pickup was already saved.'});
  const booking=await env.DB.prepare(`SELECT b.*,c.name AS customer_name,c.mobile AS customer_mobile FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE b.id=?`).bind(bookingId).first<any>();
  if(!booking)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  if(!['BOOKED','PARTIALLY_GIVEN'].includes(String(booking.status)))return json(request,env,{ok:false,error:'PICKUP_LOCKED',message:'This booking is not open for pickup.'},{status:409});
  const itemRows=(await env.DB.prepare(`SELECT id,item_id,booked_qty,closed_qty,given_qty,returned_qty FROM booking_items WHERE booking_id=?`).bind(bookingId).all<any>()).results||[];
  const returnStarted=await env.DB.prepare(`SELECT id FROM return_events WHERE booking_id=? LIMIT 1`).bind(bookingId).first<any>();
  if(returnStarted || itemRows.some((v:any)=>Number(v.returned_qty)>0))return json(request,env,{ok:false,error:'PICKUP_LOCKED',message:'Pickup cannot be changed after return has started.'},{status:409});
  const map=new Map(itemRows.map((v:any)=>[String(v.id),v]));
  for(const line of lines){const row=map.get(line.bookingItemId);if(!row)return json(request,env,{ok:false,error:'INVALID_ITEM',message:'One pickup item does not belong to this booking.'},{status:400});const remaining=Number(row.booked_qty)-Number(row.closed_qty||0)-Number(row.given_qty);if(line.quantity>remaining)return json(request,env,{ok:false,error:'QTY_EXCEEDS_REMAINING',message:`Give quantity exceeds the remaining booked quantity (${remaining}).`},{status:409});}
  const projected=new Map(itemRows.map((v:any)=>[String(v.id),Number(v.given_qty)])); for(const line of lines)projected.set(line.bookingItemId,(projected.get(line.bookingItemId)||0)+line.quantity);
  const newStatus=itemRows.every((v:any)=>Number(projected.get(String(v.id))||0)>=Number(v.booked_qty)-Number(v.closed_qty||0))?'GIVEN':'PARTIALLY_GIVEN';
  const eventId=crypto.randomUUID(), pickupAt=new Date().toISOString(); const statements:any[]=[
    env.DB.prepare(`INSERT INTO pickup_events (id,booking_id,given_to_customer_id,handled_by_user_id,pickup_at,notes,request_key) VALUES (?,?,?,?,?,?,?)`).bind(eventId,bookingId,booking.customer_id,user.id,pickupAt,notes||null,requestKey)
  ];
  for(const line of lines){statements.push(env.DB.prepare(`UPDATE booking_items SET given_qty=given_qty+?,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(line.quantity,line.bookingItemId));statements.push(env.DB.prepare(`INSERT INTO pickup_event_items (id,pickup_event_id,booking_item_id,qty_given) VALUES (?,?,?,?)`).bind(crypto.randomUUID(),eventId,line.bookingItemId,line.quantity));}
  statements.push(env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json) VALUES (?,?,?,?,?,?)`).bind(crypto.randomUUID(),user.id,'PICKUP','BOOKING',bookingId,JSON.stringify({eventId,givenTo:booking.customer_id,givenBy:user.id,pickupAt,items:lines,status:newStatus,notes:notes||null})));
  try{await env.DB.batch(statements);}catch(error){const dup=await env.DB.prepare(`SELECT id FROM pickup_events WHERE request_key=?`).bind(requestKey).first<any>();if(dup)return json(request,env,{ok:true,duplicate:true,eventId:dup.id,message:'Pickup was already saved.'});if(dbErrorHas(error,'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE'))return json(request,env,{ok:false,error:'QTY_CHANGED',message:'Pickup quantity changed while saving. Refresh the booking and try again.'},{status:409});throw error;}
  const saved=await env.DB.prepare(`SELECT status FROM bookings WHERE id=?`).bind(bookingId).first<any>();
  const savedStatus=String(saved?.status||newStatus);
  return json(request,env,{ok:true,eventId,status:savedStatus,message:savedStatus==='GIVEN'?'Pickup completed.':'Partial pickup saved.'},{status:201});
}

async function correctPickup(request:Request,env:Env,user:SessionUser,eventId:string):Promise<Response>{
  const body=await readJson(request); if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const requestKey=text(body,'requestKey'), notes=text(body,'notes'); const lines=parsePickupLines(body,true);
  if(requestKey.length<8 || requestKey.length>120 || !lines)return json(request,env,{ok:false,error:'VALIDATION',message:'Valid correction values are required.'},{status:400});
  const dup=await env.DB.prepare(`SELECT id FROM pickup_events WHERE request_key=?`).bind(requestKey).first<any>();if(dup)return json(request,env,{ok:true,duplicate:true,eventId:dup.id,message:'Correction was already saved.'});
  const original=await env.DB.prepare(`SELECT * FROM pickup_events WHERE id=?`).bind(eventId).first<any>();if(!original)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Pickup event not found.'},{status:404});
  if(original.correction_of_event_id)return json(request,env,{ok:false,error:'INVALID_CORRECTION',message:'Correct the original pickup event, not a correction record.'},{status:409});
  const already=await env.DB.prepare(`SELECT id FROM pickup_events WHERE correction_of_event_id=? LIMIT 1`).bind(eventId).first<any>();if(already)return json(request,env,{ok:false,error:'ALREADY_CORRECTED',message:'This pickup event already has a correction.'},{status:409});
  const booking=await env.DB.prepare(`SELECT id,status FROM bookings WHERE id=?`).bind(original.booking_id).first<any>();
  if(!booking)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  const bookingItems=(await env.DB.prepare(`SELECT id,booked_qty,closed_qty,given_qty,returned_qty FROM booking_items WHERE booking_id=?`).bind(original.booking_id).all<any>()).results||[];
  const returnStarted=await env.DB.prepare(`SELECT id FROM return_events WHERE booking_id=? LIMIT 1`).bind(original.booking_id).first<any>();
  if(returnStarted || bookingItems.some((v:any)=>Number(v.returned_qty)>0))return json(request,env,{ok:false,error:'CORRECTION_LOCKED',message:'Pickup correction is locked after any return has started.'},{status:409});
  const oldLines=(await env.DB.prepare(`SELECT booking_item_id,qty_given FROM pickup_event_items WHERE pickup_event_id=?`).bind(eventId).all<any>()).results||[];
  const originalIds=new Set(oldLines.map((v:any)=>String(v.booking_item_id))); if(!lines.every(v=>originalIds.has(v.bookingItemId)))return json(request,env,{ok:false,error:'INVALID_ITEM',message:'Correction can only change items from the original pickup event.'},{status:400});
  const oldMap=new Map(oldLines.map((v:any)=>[String(v.booking_item_id),Number(v.qty_given)])); const newMap=new Map<string,number>(); for(const id of originalIds)newMap.set(id,0); for(const line of lines)newMap.set(line.bookingItemId,line.quantity);
  const itemMap=new Map(bookingItems.map((v:any)=>[String(v.id),v])); const projectedGiven=new Map(bookingItems.map((v:any)=>[String(v.id),Number(v.given_qty)]));
  for(const id of originalIds){const row=itemMap.get(id);if(!row)continue;const delta=(newMap.get(id)||0)-(oldMap.get(id)||0);const next=Number(row.given_qty)+delta;if(next<Number(row.returned_qty)||next>Number(row.booked_qty)-Number(row.closed_qty||0))return json(request,env,{ok:false,error:'QTY_OUT_OF_RANGE',message:'Corrected pickup quantity would make booking quantities invalid.'},{status:409});projectedGiven.set(id,next)}
  const newStatus=bookingItems.every((v:any)=>Number(projectedGiven.get(String(v.id))||0)>=Number(v.booked_qty)-Number(v.closed_qty||0))?'GIVEN':(bookingItems.some((v:any)=>Number(projectedGiven.get(String(v.id))||0)>0)?'PARTIALLY_GIVEN':'BOOKED');
  const correctionId=crypto.randomUUID(), pickupAt=new Date().toISOString(); const statements:any[]=[env.DB.prepare(`INSERT INTO pickup_events (id,booking_id,given_to_customer_id,handled_by_user_id,pickup_at,notes,correction_of_event_id,request_key) VALUES (?,?,?,?,?,?,?,?)`).bind(correctionId,original.booking_id,original.given_to_customer_id,user.id,pickupAt,notes||null,eventId,requestKey)];
  for(const id of originalIds){const delta=(newMap.get(id)||0)-(oldMap.get(id)||0);if(delta!==0)statements.push(env.DB.prepare(`UPDATE booking_items SET given_qty=given_qty+?,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(delta,id));const qty=newMap.get(id)||0;if(qty>0)statements.push(env.DB.prepare(`INSERT INTO pickup_event_items (id,pickup_event_id,booking_item_id,qty_given) VALUES (?,?,?,?)`).bind(crypto.randomUUID(),correctionId,id,qty));}
  statements.push(env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES (?,?,?,?,?,?,?)`).bind(crypto.randomUUID(),user.id,'CORRECT_PICKUP','BOOKING',original.booking_id,JSON.stringify({eventId,items:Object.fromEntries(oldMap)}),JSON.stringify({correctionId,items:Object.fromEntries(newMap),status:newStatus,notes:notes||null})));
  try{await env.DB.batch(statements);}catch(error){const exists=await env.DB.prepare(`SELECT id FROM pickup_events WHERE request_key=?`).bind(requestKey).first<any>();if(exists)return json(request,env,{ok:true,duplicate:true,eventId:exists.id,message:'Correction was already saved.'});if(dbErrorHas(error,'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE'))return json(request,env,{ok:false,error:'QTY_CHANGED',message:'Pickup quantities changed while correcting. Refresh and try again.'},{status:409});throw error;}
  const saved=await env.DB.prepare(`SELECT status FROM bookings WHERE id=?`).bind(original.booking_id).first<any>();
  return json(request,env,{ok:true,eventId:correctionId,status:String(saved?.status||newStatus),message:'Pickup correction saved with audit history.'});
}


type ReturnLineInput = { bookingItemId:string; quantity:number; conditionNote:string };

function parseReturnLines(body: JsonBody, allowZero=false): ReturnLineInput[] | null {
  if (!Array.isArray(body.items) || body.items.length < 1 || body.items.length > 30) return null;
  const result:ReturnLineInput[]=[]; const seen=new Set<string>();
  for(const raw of body.items){
    if(!raw || typeof raw!=='object' || Array.isArray(raw)) return null;
    const bookingItemId=typeof (raw as any).bookingItemId==='string'?String((raw as any).bookingItemId).trim():'';
    const quantity=Number((raw as any).quantity);
    const conditionNote=typeof (raw as any).conditionNote==='string'?String((raw as any).conditionNote).trim().slice(0,500):'';
    if(!bookingItemId || !Number.isInteger(quantity) || quantity < (allowZero?0:1) || quantity>100000 || seen.has(bookingItemId)) return null;
    seen.add(bookingItemId); result.push({bookingItemId,quantity,conditionNote});
  }
  return result;
}

function statusFromReturnProjection(rows:any[], returned:Map<string,number>):string {
  const totalGiven=rows.reduce((sum:any,v:any)=>sum+Number(v.given_qty||0),0);
  const totalReturned=rows.reduce((sum:any,v:any)=>sum+Number(returned.get(String(v.id))??v.returned_qty??0),0);
  const returnPending=rows.some((v:any)=>Number(v.given_qty||0)>Number(returned.get(String(v.id))??v.returned_qty??0));
  const pickupPending=rows.some((v:any)=>Number(v.given_qty||0)<Number(v.booked_qty||0)-Number(v.closed_qty||0));
  if(totalGiven>0 && !returnPending && !pickupPending) return 'RETURNED';
  if(pickupPending && !returnPending) return 'PARTIALLY_GIVEN';
  if(totalReturned>0) return 'PARTIALLY_RETURNED';
  if(totalGiven>0) return pickupPending?'PARTIALLY_GIVEN':'GIVEN';
  return 'BOOKED';
}

async function returnListData(env:Env,url:URL){
  const view=(url.searchParams.get('view')||'today').trim();
  const search=(url.searchParams.get('search')||'').trim();
  const page=Math.max(1,Number(url.searchParams.get('page')||'1')||1);
  const pageSize=Math.min(50,Math.max(5,Number(url.searchParams.get('pageSize')||'20')||20));
  const today=businessToday(); const where:string[]=[`b.status <> 'CANCELLED'`]; const binds:unknown[]=[];
  const pendingExpr=`COALESCE((SELECT SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=b.id),0)`;
  const givenExpr=`COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0)`;
  if(view==='today'){where.push('b.return_date=?');where.push(`${pendingExpr}>0`);binds.push(today)}
  else if(view==='pending'){where.push(`${pendingExpr}>0`);where.push(`${givenExpr}>0`)}
  else if(view==='overdue'){where.push('b.return_date<?');where.push(`${pendingExpr}>0`);binds.push(today)}
  else if(view==='returned'){where.push(`b.status='RETURNED'`)}
  else { where.push(`${givenExpr}>0`); }
  if(search){const q=`%${search}%`;where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ? OR EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))))`);binds.push(q,q,q,q,q)}
  const clause=where.join(' AND ');
  const baseSelect=`SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.created_at,c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,
      COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.given_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id AND bi.given_qty>0),'') AS items_summary,
      ${givenExpr} AS given_qty,
      COALESCE((SELECT SUM(bi.returned_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0) AS returned_qty,
      ${pendingExpr} AS pending_qty,
      (SELECT MAX(re.return_at) FROM return_events re WHERE re.booking_id=b.id) AS last_return_at,
      CASE WHEN b.return_date < ? AND ${pendingExpr}>0 THEN CAST(julianday(?) - julianday(b.return_date) AS INTEGER) ELSE 0 END AS overdue_days
      FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE ${clause}`;
  const count=await env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE ${clause}`).bind(...binds).first<any>();
  const rows=await env.DB.prepare(`${baseSelect} ORDER BY CASE WHEN b.return_date<? AND ${pendingExpr}>0 THEN 0 ELSE 1 END,b.return_date ASC,b.created_at DESC LIMIT ? OFFSET ?`)
    .bind(today,today,...binds,today,pageSize,(page-1)*pageSize).all<any>();
  const total=Number(count?.count||0);
  return {returns:rows.results||[],pagination:{page,pageSize,total,pages:Math.max(1,Math.ceil(total/pageSize))}};
}

async function returnBootstrap(request:Request,env:Env):Promise<Response>{
  return json(request,env,{ok:true,...await returnListData(env,new URL(request.url))});
}

async function listReturns(request:Request,env:Env):Promise<Response>{
  return json(request,env,{ok:true,...await returnListData(env,new URL(request.url))});
}

async function returnDetail(request:Request,env:Env,bookingId:string):Promise<Response>{
  const booking=await env.DB.prepare(`SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.notes,c.id AS customer_id,c.name AS customer_name,c.mobile AS customer_mobile,c.alternate_mobile AS customer_alternate_mobile,u.name AS booked_by
    FROM bookings b JOIN customers c ON c.id=b.customer_id LEFT JOIN users u ON u.id=b.created_by_user_id WHERE b.id=?`).bind(bookingId).first<any>();
  if(!booking)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  const [items,events,eventItems]=await env.DB.batch([
    env.DB.prepare(`SELECT bi.id AS booking_item_id,bi.item_id,bi.booked_qty,bi.given_qty,bi.returned_qty,(bi.given_qty-bi.returned_qty) AS pending_to_return,i.item_code,i.item_name,c.name AS category_name,
      (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order LIMIT 1) AS image_url,
      COALESCE((SELECT json_group_array(image_url) FROM (
        SELECT image_url FROM item_images im2 WHERE im2.item_id=i.id
        ORDER BY im2.is_primary DESC,im2.display_order ASC LIMIT 8
      )),'[]') AS images_json
      FROM booking_items bi JOIN items i ON i.id=bi.item_id JOIN categories c ON c.id=i.category_id WHERE bi.booking_id=? ORDER BY i.item_name`).bind(bookingId),
    env.DB.prepare(`SELECT re.id,re.return_at,re.notes,re.correction_of_event_id,re.created_at,u.name AS received_by,
      COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || rei.qty_returned, ', ') FROM return_event_items rei JOIN booking_items bi ON bi.id=rei.booking_item_id JOIN items i ON i.id=bi.item_id WHERE rei.return_event_id=re.id),'') AS items_summary
      FROM return_events re JOIN users u ON u.id=re.received_by_user_id WHERE re.booking_id=? ORDER BY re.created_at DESC`).bind(bookingId),
    env.DB.prepare(`SELECT rei.return_event_id,rei.booking_item_id,rei.qty_returned,rei.condition_note,i.item_code,i.item_name
      FROM return_event_items rei JOIN booking_items bi ON bi.id=rei.booking_item_id JOIN items i ON i.id=bi.item_id
      JOIN return_events re ON re.id=rei.return_event_id WHERE re.booking_id=? ORDER BY rei.return_event_id,i.item_name`).bind(bookingId)
  ]);
  return json(request,env,{ok:true,booking,items:items.results||[],events:events.results||[],eventItems:eventItems.results||[]});
}

async function createReturn(request:Request,env:Env,user:SessionUser,bookingId:string):Promise<Response>{
  const body=await readJson(request); if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const requestKey=text(body,'requestKey'), notes=text(body,'notes'); const lines=parseReturnLines(body,false);
  if(requestKey.length<8 || requestKey.length>120 || !lines)return json(request,env,{ok:false,error:'VALIDATION',message:'A valid request key and at least one positive Return Now quantity are required.'},{status:400});
  const existing=await env.DB.prepare(`SELECT id,booking_id FROM return_events WHERE request_key=?`).bind(requestKey).first<any>();
  if(existing)return json(request,env,{ok:true,duplicate:true,eventId:existing.id,message:'Return was already saved.'});
  const booking=await env.DB.prepare(`SELECT b.*,c.name AS customer_name,c.mobile AS customer_mobile FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE b.id=?`).bind(bookingId).first<any>();
  if(!booking)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Booking not found.'},{status:404});
  if(booking.status==='CANCELLED')return json(request,env,{ok:false,error:'RETURN_LOCKED',message:'Cancelled booking cannot be returned.'},{status:409});
  const itemRows=(await env.DB.prepare(`SELECT id,item_id,booked_qty,given_qty,returned_qty FROM booking_items WHERE booking_id=?`).bind(bookingId).all<any>()).results||[];
  if(!itemRows.some((v:any)=>Number(v.given_qty)>Number(v.returned_qty)))return json(request,env,{ok:false,error:'NOTHING_TO_RETURN',message:'There is no pending given quantity to return.'},{status:409});
  const map=new Map(itemRows.map((v:any)=>[String(v.id),v]));
  for(const line of lines){const row=map.get(line.bookingItemId);if(!row)return json(request,env,{ok:false,error:'INVALID_ITEM',message:'One return item does not belong to this booking.'},{status:400});const pending=Number(row.given_qty)-Number(row.returned_qty);if(line.quantity>pending)return json(request,env,{ok:false,error:'QTY_EXCEEDS_PENDING',message:`Return quantity exceeds pending quantity (${pending}).`},{status:409});}
  const projected=new Map(itemRows.map((v:any)=>[String(v.id),Number(v.returned_qty)])); for(const line of lines)projected.set(line.bookingItemId,(projected.get(line.bookingItemId)||0)+line.quantity);
  const newStatus=statusFromReturnProjection(itemRows,projected);
  const eventId=crypto.randomUUID(), returnAt=new Date().toISOString(); const statements:any[]=[
    env.DB.prepare(`INSERT INTO return_events (id,booking_id,received_by_user_id,return_at,notes,request_key) VALUES (?,?,?,?,?,?)`).bind(eventId,bookingId,user.id,returnAt,notes||null,requestKey)
  ];
  for(const line of lines){statements.push(env.DB.prepare(`UPDATE booking_items SET returned_qty=returned_qty+?,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(line.quantity,line.bookingItemId));statements.push(env.DB.prepare(`INSERT INTO return_event_items (id,return_event_id,booking_item_id,qty_returned,condition_note) VALUES (?,?,?,?,?)`).bind(crypto.randomUUID(),eventId,line.bookingItemId,line.quantity,line.conditionNote||null));}
  statements.push(env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json) VALUES (?,?,?,?,?,?)`).bind(crypto.randomUUID(),user.id,'RETURN','BOOKING',bookingId,JSON.stringify({eventId,receivedBy:user.id,returnAt,items:lines,status:newStatus,notes:notes||null})));
  try{await env.DB.batch(statements);}catch(error){const dup=await env.DB.prepare(`SELECT id FROM return_events WHERE request_key=?`).bind(requestKey).first<any>();if(dup)return json(request,env,{ok:true,duplicate:true,eventId:dup.id,message:'Return was already saved.'});if(dbErrorHas(error,'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE'))return json(request,env,{ok:false,error:'QTY_CHANGED',message:'Return quantity changed while saving. Refresh the booking and try again.'},{status:409});throw error;}
  const saved=await env.DB.prepare(`SELECT status FROM bookings WHERE id=?`).bind(bookingId).first<any>();
  const savedStatus=String(saved?.status||newStatus);
  return json(request,env,{ok:true,eventId,status:savedStatus,message:savedStatus==='RETURNED'?'Return completed.':'Partial return saved.'},{status:201});
}

async function correctReturn(request:Request,env:Env,user:SessionUser,eventId:string):Promise<Response>{
  const body=await readJson(request); if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const requestKey=text(body,'requestKey'), notes=text(body,'notes'); const lines=parseReturnLines(body,true);
  if(requestKey.length<8 || requestKey.length>120 || !lines)return json(request,env,{ok:false,error:'VALIDATION',message:'Valid correction values are required.'},{status:400});
  const dup=await env.DB.prepare(`SELECT id FROM return_events WHERE request_key=?`).bind(requestKey).first<any>();if(dup)return json(request,env,{ok:true,duplicate:true,eventId:dup.id,message:'Correction was already saved.'});
  const original=await env.DB.prepare(`SELECT * FROM return_events WHERE id=?`).bind(eventId).first<any>();if(!original)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Return event not found.'},{status:404});
  if(original.correction_of_event_id)return json(request,env,{ok:false,error:'INVALID_CORRECTION',message:'Correct the original return event, not a correction record.'},{status:409});
  const already=await env.DB.prepare(`SELECT id FROM return_events WHERE correction_of_event_id=? LIMIT 1`).bind(eventId).first<any>();if(already)return json(request,env,{ok:false,error:'ALREADY_CORRECTED',message:'This return event already has a correction.'},{status:409});
  const bookingItems=(await env.DB.prepare(`SELECT id,booked_qty,closed_qty,given_qty,returned_qty FROM booking_items WHERE booking_id=?`).bind(original.booking_id).all<any>()).results||[];
  const oldLines=(await env.DB.prepare(`SELECT booking_item_id,qty_returned,condition_note FROM return_event_items WHERE return_event_id=?`).bind(eventId).all<any>()).results||[];
  const originalIds=new Set(oldLines.map((v:any)=>String(v.booking_item_id))); if(!lines.every(v=>originalIds.has(v.bookingItemId)))return json(request,env,{ok:false,error:'INVALID_ITEM',message:'Correction can only change items from the original return event.'},{status:400});
  const oldMap=new Map(oldLines.map((v:any)=>[String(v.booking_item_id),Number(v.qty_returned)])); const newMap=new Map<string,number>(); for(const id of originalIds)newMap.set(id,0); for(const line of lines)newMap.set(line.bookingItemId,line.quantity);
  const itemMap=new Map(bookingItems.map((v:any)=>[String(v.id),v])); const projectedReturned=new Map(bookingItems.map((v:any)=>[String(v.id),Number(v.returned_qty)]));
  for(const id of originalIds){const row=itemMap.get(id);if(!row)continue;const delta=(newMap.get(id)||0)-(oldMap.get(id)||0);const next=Number(row.returned_qty)+delta;if(next<0||next>Number(row.given_qty))return json(request,env,{ok:false,error:'QTY_OUT_OF_RANGE',message:'Corrected return quantity would make booking quantities invalid.'},{status:409});projectedReturned.set(id,next)}
  const newStatus=statusFromReturnProjection(bookingItems,projectedReturned);
  const correctionId=crypto.randomUUID(), returnAt=new Date().toISOString(); const statements:any[]=[env.DB.prepare(`INSERT INTO return_events (id,booking_id,received_by_user_id,return_at,notes,correction_of_event_id,request_key) VALUES (?,?,?,?,?,?,?)`).bind(correctionId,original.booking_id,user.id,returnAt,notes||null,eventId,requestKey)];
  const inputMap=new Map(lines.map(v=>[v.bookingItemId,v]));
  for(const id of originalIds){const delta=(newMap.get(id)||0)-(oldMap.get(id)||0);if(delta!==0)statements.push(env.DB.prepare(`UPDATE booking_items SET returned_qty=returned_qty+?,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(delta,id));const qty=newMap.get(id)||0;const input=inputMap.get(id);if(qty>0)statements.push(env.DB.prepare(`INSERT INTO return_event_items (id,return_event_id,booking_item_id,qty_returned,condition_note) VALUES (?,?,?,?,?)`).bind(crypto.randomUUID(),correctionId,id,qty,input?.conditionNote||null));}
  statements.push(env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES (?,?,?,?,?,?,?)`).bind(crypto.randomUUID(),user.id,'CORRECT_RETURN','BOOKING',original.booking_id,JSON.stringify({eventId,items:Object.fromEntries(oldMap)}),JSON.stringify({correctionId,items:Object.fromEntries(newMap),status:newStatus,notes:notes||null})));
  try{await env.DB.batch(statements);}catch(error){const exists=await env.DB.prepare(`SELECT id FROM return_events WHERE request_key=?`).bind(requestKey).first<any>();if(exists)return json(request,env,{ok:true,duplicate:true,eventId:exists.id,message:'Correction was already saved.'});if(dbErrorHas(error,'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE'))return json(request,env,{ok:false,error:'QTY_CHANGED',message:'Return quantities changed while correcting. Refresh and try again.'},{status:409});throw error;}
  const saved=await env.DB.prepare(`SELECT status FROM bookings WHERE id=?`).bind(original.booking_id).first<any>();
  return json(request,env,{ok:true,eventId:correctionId,status:String(saved?.status||newStatus),message:'Return correction saved with audit history.'});
}


type DashboardListRow = {
  id:string; booking_no:string; booking_date?:string; pickup_date:string; return_date:string; status:string;
  customer_name:string; customer_mobile:string; booked_by?:string|null; items_summary:string;
  booked_qty:number; given_qty:number; returned_qty:number; pending_qty:number; remaining_to_give:number; overdue_days:number;
};

function dashboardJsonList(value: unknown): DashboardListRow[] {
  if (typeof value !== "string" || !value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

async function dashboardOverview(request:Request,env:Env):Promise<Response>{
  const today=businessToday();
  const row=await env.DB.prepare(`
    WITH booking_rollup AS (
      SELECT
        b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,b.created_at,
        c.name AS customer_name,c.mobile AS customer_mobile,
        u.name AS booked_by,
        COALESCE(GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', '),'') AS items_summary,
        COALESCE(SUM(bi.booked_qty),0) AS booked_qty,
        COALESCE(SUM(bi.given_qty),0) AS given_qty,
        COALESCE(SUM(bi.returned_qty),0) AS returned_qty,
        COALESCE(SUM(CASE WHEN bi.given_qty > bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS pending_qty,
        COALESCE(SUM(CASE WHEN bi.booked_qty-bi.closed_qty > bi.given_qty THEN bi.booked_qty-bi.closed_qty-bi.given_qty ELSE 0 END),0) AS remaining_to_give
      FROM bookings b
      JOIN customers c ON c.id=b.customer_id
      LEFT JOIN users u ON u.id=b.created_by_user_id
      LEFT JOIN booking_items bi ON bi.booking_id=b.id
      LEFT JOIN items i ON i.id=bi.item_id
      GROUP BY b.id
    ),
    inventory AS (
      SELECT
        COUNT(*) AS total_items,
        COALESCE(SUM(total_quantity),0) AS total_quantity
      FROM items
      WHERE archived_at IS NULL AND is_active=1
    )
    SELECT
      (SELECT COUNT(*) FROM categories WHERE is_active=1) AS total_categories,
      (SELECT total_items FROM inventory) AS total_items,
      (SELECT total_quantity FROM inventory) AS total_quantity,
      MAX(0,
        (SELECT total_quantity FROM inventory) -
        COALESCE((SELECT SUM(pending_qty) FROM booking_rollup WHERE status <> 'CANCELLED'),0)
      ) AS available_quantity,
      COALESCE((SELECT SUM(remaining_to_give) FROM booking_rollup WHERE status NOT IN ('CANCELLED','RETURNED')),0) AS booked_quantity,
      COALESCE((SELECT SUM(pending_qty) FROM booking_rollup WHERE status <> 'CANCELLED'),0) AS given_quantity,
      COALESCE((SELECT COUNT(*) FROM booking_rollup WHERE return_date < ? AND pending_qty > 0 AND status <> 'CANCELLED'),0) AS overdue_returns,

      COALESCE((SELECT json_group_array(json_object(
        'id',id,'booking_no',booking_no,'booking_date',booking_date,'pickup_date',pickup_date,'return_date',return_date,
        'status',status,'customer_name',customer_name,'customer_mobile',customer_mobile,'booked_by',booked_by,
        'items_summary',items_summary,'booked_qty',booked_qty,'given_qty',given_qty,'returned_qty',returned_qty,
        'pending_qty',pending_qty,'remaining_to_give',remaining_to_give,'overdue_days',0
      )) FROM (
        SELECT * FROM booking_rollup
        WHERE booking_date=? AND status <> 'CANCELLED'
        ORDER BY created_at DESC LIMIT 8
      )),'[]') AS today_bookings_json,

      COALESCE((SELECT json_group_array(json_object(
        'id',id,'booking_no',booking_no,'booking_date',booking_date,'pickup_date',pickup_date,'return_date',return_date,
        'status',status,'customer_name',customer_name,'customer_mobile',customer_mobile,'booked_by',booked_by,
        'items_summary',items_summary,'booked_qty',booked_qty,'given_qty',given_qty,'returned_qty',returned_qty,
        'pending_qty',pending_qty,'remaining_to_give',remaining_to_give,'overdue_days',0
      )) FROM (
        SELECT * FROM booking_rollup
        WHERE pickup_date=? AND status <> 'CANCELLED'
        ORDER BY CASE WHEN remaining_to_give>0 THEN 0 ELSE 1 END, created_at DESC LIMIT 8
      )),'[]') AS today_pickups_json,

      COALESCE((SELECT json_group_array(json_object(
        'id',id,'booking_no',booking_no,'booking_date',booking_date,'pickup_date',pickup_date,'return_date',return_date,
        'status',status,'customer_name',customer_name,'customer_mobile',customer_mobile,'booked_by',booked_by,
        'items_summary',items_summary,'booked_qty',booked_qty,'given_qty',given_qty,'returned_qty',returned_qty,
        'pending_qty',pending_qty,'remaining_to_give',remaining_to_give,'overdue_days',0
      )) FROM (
        SELECT * FROM booking_rollup
        WHERE return_date=? AND given_qty>0 AND status <> 'CANCELLED'
        ORDER BY CASE WHEN pending_qty>0 THEN 0 ELSE 1 END, created_at DESC LIMIT 8
      )),'[]') AS today_returns_json,

      COALESCE((SELECT json_group_array(json_object(
        'id',id,'booking_no',booking_no,'booking_date',booking_date,'pickup_date',pickup_date,'return_date',return_date,
        'status',status,'customer_name',customer_name,'customer_mobile',customer_mobile,'booked_by',booked_by,
        'items_summary',items_summary,'booked_qty',booked_qty,'given_qty',given_qty,'returned_qty',returned_qty,
        'pending_qty',pending_qty,'remaining_to_give',remaining_to_give,
        'overdue_days',CAST(julianday(?) - julianday(return_date) AS INTEGER)
      )) FROM (
        SELECT * FROM booking_rollup
        WHERE return_date < ? AND pending_qty>0 AND status <> 'CANCELLED'
        ORDER BY return_date ASC, created_at ASC LIMIT 8
      )),'[]') AS overdue_returns_json
  `).bind(today,today,today,today,today,today).first<any>();

  if(!row)return json(request,env,{ok:false,error:'DASHBOARD_UNAVAILABLE',message:'Unable to load dashboard.'},{status:500});
  return json(request,env,{
    ok:true,
    today,
    summary:{
      totalCategories:Number(row.total_categories||0),
      totalItems:Number(row.total_items||0),
      totalQuantity:Number(row.total_quantity||0),
      availableQuantity:Number(row.available_quantity||0),
      bookedQuantity:Number(row.booked_quantity||0),
      givenQuantity:Number(row.given_quantity||0),
      overdueReturns:Number(row.overdue_returns||0)
    },
    todayBookings:dashboardJsonList(row.today_bookings_json),
    todayPickups:dashboardJsonList(row.today_pickups_json),
    todayReturns:dashboardJsonList(row.today_returns_json),
    overdueReturns:dashboardJsonList(row.overdue_returns_json)
  });
}



type ReportType = "item-history" | "customer-history" | "current-given" | "overdue" | "date-bookings" | "category-stock";
type ReportFilters = {
  page:number; pageSize:number; fromDate:string; toDate:string; categoryId:string; itemSearch:string; customerSearch:string; status:string; search:string;
};

function reportFilters(request:Request):ReportFilters{
  const url=new URL(request.url);
  const page=Math.max(1,Number(url.searchParams.get('page')||'1')||1);
  const pageSize=Math.min(50,Math.max(10,Number(url.searchParams.get('pageSize')||'20')||20));
  const fromDate=(url.searchParams.get('fromDate')||'').trim();
  const toDate=(url.searchParams.get('toDate')||'').trim();
  return {
    page,pageSize,
    fromDate:validDateOnly(fromDate)?fromDate:'',
    toDate:validDateOnly(toDate)?toDate:'',
    categoryId:(url.searchParams.get('categoryId')||'').trim(),
    itemSearch:(url.searchParams.get('itemSearch')||'').trim(),
    customerSearch:(url.searchParams.get('customerSearch')||'').trim(),
    status:(url.searchParams.get('status')||'').trim().toUpperCase(),
    search:(url.searchParams.get('search')||'').trim()
  };
}

function reportPaging(filters:ReportFilters,total:number){
  return {page:filters.page,pageSize:filters.pageSize,total,pages:Math.max(1,Math.ceil(total/filters.pageSize))};
}

function like(value:string){return `%${value}%`;}

async function reportsBootstrap(request:Request,env:Env):Promise<Response>{
  const [cats]=await env.DB.batch([
    env.DB.prepare(`SELECT id,name FROM categories WHERE is_active=1 ORDER BY display_order,name COLLATE NOCASE`)
  ]);
  return json(request,env,{ok:true,categories:cats.results||[],statuses:['BOOKED','PARTIALLY_GIVEN','GIVEN','PARTIALLY_RETURNED','RETURNED','CANCELLED']});
}

async function reportItemHistory(request:Request,env:Env,filters:ReportFilters):Promise<Response>{
  const outer:string[]=["i.archived_at IS NULL"]; const outerBinds:unknown[]=[];
  if(filters.categoryId){outer.push('i.category_id=?');outerBinds.push(filters.categoryId);}
  if(filters.itemSearch){outer.push('(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))');outerBinds.push(like(filters.itemSearch),like(filters.itemSearch));}
  if(filters.search){outer.push('(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?))');outerBinds.push(like(filters.search),like(filters.search),like(filters.search));}
  const inner:string[]=['1=1']; const innerBinds:unknown[]=[];
  if(filters.fromDate){inner.push('b.booking_date>=?');innerBinds.push(filters.fromDate);}
  if(filters.toDate){inner.push('b.booking_date<=?');innerBinds.push(filters.toDate);}
  if(filters.customerSearch){inner.push('(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)');innerBinds.push(like(filters.customerSearch),like(filters.customerSearch));}
  if(filters.status){inner.push('b.status=?');innerBinds.push(filters.status);}
  else inner.push("b.status<>'CANCELLED'");
  const whereOuter=outer.join(' AND '), whereInner=inner.join(' AND ');
  const requireMatch=Boolean(filters.fromDate||filters.toDate||filters.customerSearch||filters.status);
  const count= requireMatch
    ? await env.DB.prepare(`WITH matched AS (SELECT DISTINCT bi.item_id FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN customers cu ON cu.id=b.customer_id WHERE ${whereInner}) SELECT COUNT(*) AS count FROM items i JOIN categories c ON c.id=i.category_id JOIN matched m ON m.item_id=i.id WHERE ${whereOuter}`).bind(...innerBinds,...outerBinds).first<any>()
    : await env.DB.prepare(`SELECT COUNT(*) AS count FROM items i JOIN categories c ON c.id=i.category_id WHERE ${whereOuter}`).bind(...outerBinds).first<any>();
  const rows=await env.DB.prepare(`WITH agg AS (
    SELECT bi.item_id,COUNT(DISTINCT b.id) AS total_bookings,
      COALESCE(SUM(bi.booked_qty),0) AS booked_qty,COALESCE(SUM(bi.given_qty),0) AS given_qty,
      COALESCE(SUM(bi.returned_qty),0) AS returned_qty,
      COALESCE(SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS pending_qty,
      MAX(b.booking_date) AS last_booking_date
    FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN customers cu ON cu.id=b.customer_id
    WHERE ${whereInner} GROUP BY bi.item_id
  )
  SELECT i.id,i.item_code,i.item_name,c.name AS category_name,i.total_quantity,
    COALESCE(a.total_bookings,0) AS total_bookings,COALESCE(a.booked_qty,0) AS booked_qty,
    COALESCE(a.given_qty,0) AS given_qty,COALESCE(a.returned_qty,0) AS returned_qty,
    COALESCE(a.pending_qty,0) AS pending_qty,a.last_booking_date
  FROM items i JOIN categories c ON c.id=i.category_id LEFT JOIN agg a ON a.item_id=i.id
  WHERE ${whereOuter}${requireMatch?' AND a.item_id IS NOT NULL':''}
  ORDER BY COALESCE(a.total_bookings,0) DESC,i.item_name COLLATE NOCASE
  LIMIT ? OFFSET ?`).bind(...innerBinds,...outerBinds,filters.pageSize,(filters.page-1)*filters.pageSize).all<any>();
  return json(request,env,{ok:true,type:'item-history',rows:rows.results||[],pagination:reportPaging(filters,Number(count?.count||0))});
}

async function reportCustomerHistory(request:Request,env:Env,filters:ReportFilters):Promise<Response>{
  const outer:string[]=["cu.archived_at IS NULL"]; const outerBinds:unknown[]=[];
  if(filters.customerSearch){outer.push('(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)');outerBinds.push(like(filters.customerSearch),like(filters.customerSearch));}
  if(filters.search){outer.push('(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)');outerBinds.push(like(filters.search),like(filters.search));}
  const inner:string[]=['1=1']; const innerBinds:unknown[]=[];
  if(filters.fromDate){inner.push('b.booking_date>=?');innerBinds.push(filters.fromDate);}
  if(filters.toDate){inner.push('b.booking_date<=?');innerBinds.push(filters.toDate);}
  if(filters.status){inner.push('b.status=?');innerBinds.push(filters.status);}
  if(filters.categoryId){inner.push('EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND ix.category_id=?)');innerBinds.push(filters.categoryId);}
  if(filters.itemSearch){inner.push('EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?)))');innerBinds.push(like(filters.itemSearch),like(filters.itemSearch));}
  const whereOuter=outer.join(' AND '), whereInner=inner.join(' AND ');
  const requireMatch=Boolean(filters.fromDate||filters.toDate||filters.status||filters.categoryId||filters.itemSearch);
  const count= requireMatch
    ? await env.DB.prepare(`WITH matched AS (SELECT DISTINCT b.customer_id FROM bookings b WHERE ${whereInner}) SELECT COUNT(*) AS count FROM customers cu JOIN matched m ON m.customer_id=cu.id WHERE ${whereOuter}`).bind(...innerBinds,...outerBinds).first<any>()
    : await env.DB.prepare(`SELECT COUNT(*) AS count FROM customers cu WHERE ${whereOuter}`).bind(...outerBinds).first<any>();
  const rows=await env.DB.prepare(`WITH booking_agg AS (
    SELECT b.customer_id,COUNT(DISTINCT b.id) AS total_bookings,
      COUNT(DISTINCT CASE WHEN b.status='CANCELLED' THEN b.id END) AS cancelled_bookings,
      COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' THEN bi.booked_qty ELSE 0 END),0) AS booked_qty,
      COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' THEN bi.given_qty ELSE 0 END),0) AS given_qty,
      COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' THEN bi.returned_qty ELSE 0 END),0) AS returned_qty,
      COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS pending_qty,
      MAX(b.booking_date) AS last_booking_date
    FROM bookings b LEFT JOIN booking_items bi ON bi.booking_id=b.id WHERE ${whereInner}
    GROUP BY b.customer_id
  )
  SELECT cu.id,cu.name,cu.mobile,cu.alternate_mobile,cu.is_active,
    COALESCE(a.total_bookings,0) AS total_bookings,COALESCE(a.cancelled_bookings,0) AS cancelled_bookings,
    COALESCE(a.booked_qty,0) AS booked_qty,COALESCE(a.given_qty,0) AS given_qty,
    COALESCE(a.returned_qty,0) AS returned_qty,COALESCE(a.pending_qty,0) AS pending_qty,a.last_booking_date
  FROM customers cu LEFT JOIN booking_agg a ON a.customer_id=cu.id
  WHERE ${whereOuter}${requireMatch?' AND a.customer_id IS NOT NULL':''}
  ORDER BY COALESCE(a.last_booking_date,'') DESC,cu.name COLLATE NOCASE
  LIMIT ? OFFSET ?`).bind(...innerBinds,...outerBinds,filters.pageSize,(filters.page-1)*filters.pageSize).all<any>();
  return json(request,env,{ok:true,type:'customer-history',rows:rows.results||[],pagination:reportPaging(filters,Number(count?.count||0))});
}

function reportLineFilters(filters:ReportFilters,overdue:boolean){
  const where:string[]=["b.status<>'CANCELLED'","bi.given_qty>bi.returned_qty"]; const binds:unknown[]=[];
  if(overdue){where.push('b.return_date<?');binds.push(businessToday());}
  if(filters.fromDate){where.push('b.return_date>=?');binds.push(filters.fromDate);}
  if(filters.toDate){where.push('b.return_date<=?');binds.push(filters.toDate);}
  if(filters.categoryId){where.push('i.category_id=?');binds.push(filters.categoryId);}
  if(filters.itemSearch){where.push('(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))');binds.push(like(filters.itemSearch),like(filters.itemSearch));}
  if(filters.customerSearch){where.push('(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)');binds.push(like(filters.customerSearch),like(filters.customerSearch));}
  if(filters.status){where.push('b.status=?');binds.push(filters.status);}
  if(filters.search){where.push('(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ? OR LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))');const q=like(filters.search);binds.push(q,q,q,q,q);}
  return {where:where.join(' AND '),binds};
}

async function reportGivenLines(request:Request,env:Env,filters:ReportFilters,overdue:boolean):Promise<Response>{
  const f=reportLineFilters(filters,overdue);
  const count=await env.DB.prepare(`SELECT COUNT(*) AS count FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN customers cu ON cu.id=b.customer_id JOIN items i ON i.id=bi.item_id WHERE ${f.where}`).bind(...f.binds).first<any>();
  const rows=await env.DB.prepare(`SELECT b.id AS booking_id,b.booking_no,b.pickup_date,b.return_date,b.status,
    cu.id AS customer_id,cu.name AS customer_name,cu.mobile AS customer_mobile,
    i.id AS item_id,i.item_code,i.item_name,c.name AS category_name,
    bi.booked_qty,bi.given_qty,bi.returned_qty,(bi.given_qty-bi.returned_qty) AS pending_qty,
    CASE WHEN b.return_date<? THEN CAST(julianday(?) - julianday(b.return_date) AS INTEGER) ELSE 0 END AS overdue_days
    FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN customers cu ON cu.id=b.customer_id
    JOIN items i ON i.id=bi.item_id JOIN categories c ON c.id=i.category_id
    WHERE ${f.where}
    ORDER BY ${overdue?'b.return_date ASC':'b.return_date ASC'},cu.name COLLATE NOCASE,i.item_name COLLATE NOCASE
    LIMIT ? OFFSET ?`).bind(businessToday(),businessToday(),...f.binds,filters.pageSize,(filters.page-1)*filters.pageSize).all<any>();
  return json(request,env,{ok:true,type:overdue?'overdue':'current-given',rows:rows.results||[],pagination:reportPaging(filters,Number(count?.count||0))});
}

async function reportDateBookings(request:Request,env:Env,filters:ReportFilters):Promise<Response>{
  const where:string[]=['1=1']; const binds:unknown[]=[];
  if(filters.fromDate){where.push('b.booking_date>=?');binds.push(filters.fromDate);}
  if(filters.toDate){where.push('b.booking_date<=?');binds.push(filters.toDate);}
  if(filters.status){where.push('b.status=?');binds.push(filters.status);}
  if(filters.customerSearch){where.push('(LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ?)');binds.push(like(filters.customerSearch),like(filters.customerSearch));}
  if(filters.categoryId){where.push('EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND ix.category_id=?)');binds.push(filters.categoryId);}
  if(filters.itemSearch){where.push('EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?)))');binds.push(like(filters.itemSearch),like(filters.itemSearch));}
  if(filters.search){const q=like(filters.search);where.push(`(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(cu.name) LIKE LOWER(?) OR cu.mobile LIKE ? OR EXISTS (SELECT 1 FROM booking_items bx JOIN items ix ON ix.id=bx.item_id WHERE bx.booking_id=b.id AND (LOWER(ix.item_code) LIKE LOWER(?) OR LOWER(ix.item_name) LIKE LOWER(?))))`);binds.push(q,q,q,q,q);}
  const clause=where.join(' AND ');
  const count=await env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings b JOIN customers cu ON cu.id=b.customer_id WHERE ${clause}`).bind(...binds).first<any>();
  const rows=await env.DB.prepare(`SELECT b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,cu.name AS customer_name,cu.mobile AS customer_mobile,u.name AS booked_by,
    COALESCE((SELECT GROUP_CONCAT(i.item_name || ' × ' || bi.booked_qty, ', ') FROM booking_items bi JOIN items i ON i.id=bi.item_id WHERE bi.booking_id=b.id),'') AS items_summary,
    COALESCE((SELECT SUM(booked_qty) FROM booking_items WHERE booking_id=b.id),0) AS booked_qty,
    COALESCE((SELECT SUM(given_qty) FROM booking_items WHERE booking_id=b.id),0) AS given_qty,
    COALESCE((SELECT SUM(returned_qty) FROM booking_items WHERE booking_id=b.id),0) AS returned_qty
    FROM bookings b JOIN customers cu ON cu.id=b.customer_id LEFT JOIN users u ON u.id=b.created_by_user_id
    WHERE ${clause} ORDER BY b.booking_date DESC,b.created_at DESC LIMIT ? OFFSET ?`).bind(...binds,filters.pageSize,(filters.page-1)*filters.pageSize).all<any>();
  return json(request,env,{ok:true,type:'date-bookings',rows:rows.results||[],pagination:reportPaging(filters,Number(count?.count||0))});
}

async function reportCategoryStock(request:Request,env:Env,filters:ReportFilters):Promise<Response>{
  const where:string[]=['c.is_active=1']; const binds:unknown[]=[];
  if(filters.categoryId){where.push('c.id=?');binds.push(filters.categoryId);}
  if(filters.search){where.push('LOWER(c.name) LIKE LOWER(?)');binds.push(like(filters.search));}
  const clause=where.join(' AND ');
  const count=await env.DB.prepare(`SELECT COUNT(*) AS count FROM categories c WHERE ${clause}`).bind(...binds).first<any>();
  const rows=await env.DB.prepare(`WITH item_rollup AS (
    SELECT i.category_id,COUNT(*) AS total_items,COALESCE(SUM(i.total_quantity),0) AS total_quantity
    FROM items i WHERE i.archived_at IS NULL AND i.is_active=1 GROUP BY i.category_id
  ), flow AS (
    SELECT i.category_id,
      COALESCE(SUM(CASE WHEN b.status NOT IN ('CANCELLED','RETURNED') AND bi.booked_qty-bi.closed_qty>bi.given_qty THEN bi.booked_qty-bi.closed_qty-bi.given_qty ELSE 0 END),0) AS booked_qty,
      COALESCE(SUM(CASE WHEN b.status<>'CANCELLED' AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS given_qty
    FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id JOIN items i ON i.id=bi.item_id GROUP BY i.category_id
  )
  SELECT c.id,c.name,c.code_prefix,COALESCE(ir.total_items,0) AS total_items,COALESCE(ir.total_quantity,0) AS total_quantity,
    COALESCE(f.booked_qty,0) AS booked_qty,COALESCE(f.given_qty,0) AS given_qty,
    MAX(0,COALESCE(ir.total_quantity,0)-COALESCE(f.given_qty,0)) AS available_qty
  FROM categories c LEFT JOIN item_rollup ir ON ir.category_id=c.id LEFT JOIN flow f ON f.category_id=c.id
  WHERE ${clause} ORDER BY c.display_order,c.name COLLATE NOCASE LIMIT ? OFFSET ?`).bind(...binds,filters.pageSize,(filters.page-1)*filters.pageSize).all<any>();
  return json(request,env,{ok:true,type:'category-stock',rows:rows.results||[],pagination:reportPaging(filters,Number(count?.count||0))});
}

async function adminReport(request:Request,env:Env):Promise<Response>{
  const filters=reportFilters(request); const type=(new URL(request.url).searchParams.get('type')||'date-bookings') as ReportType;
  if(type==='item-history')return reportItemHistory(request,env,filters);
  if(type==='customer-history')return reportCustomerHistory(request,env,filters);
  if(type==='current-given')return reportGivenLines(request,env,filters,false);
  if(type==='overdue')return reportGivenLines(request,env,filters,true);
  if(type==='category-stock')return reportCategoryStock(request,env,filters);
  return reportDateBookings(request,env,filters);
}



const WORKING_DAY_KEYS = ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"] as const;
type WorkingDayKey = typeof WORKING_DAY_KEYS[number];
type WorkingHoursDay = { isOpen:boolean; openTime:string; closeTime:string };
type WorkingHours = Record<WorkingDayKey,WorkingHoursDay>;

type SiteSettings = {
  shopName: string;
  websiteTitle: string;
  websiteUrl: string;
  logoUrl: string;
  logoPublicId: string;
  contactNumber: string;
  whatsappNumber: string;
  address: string;
  defaultLanguage: "GU" | "EN";
  dateFormat: "DD-MM-YYYY" | "YYYY-MM-DD";
  workingHours: WorkingHours;
  publicCatalogEnabled: boolean;
  showWhatsApp: boolean;
  showCall: boolean;
  availabilityMode: "STATUS_ONLY" | "EXACT";
  fewLeftThreshold: number;
  whatsappTemplateLanguage: WhatsAppGlobalLanguageMode;
};

type WhatsAppTemplateRow = {
  id:string;
  template_key:string;
  template_name:string;
  message_text:string;
  message_gu:string|null;
  message_en:string|null;
  language_mode:string;
  linked_action:string|null;
  is_active:number;
  created_at:string;
  updated_at:string;
};

function whatsappTemplateMessages(body:JsonBody) {
  const legacy=text(body,"messageText");
  const messageGu=text(body,"messageGu") || legacy;
  const messageEn=text(body,"messageEn");
  if(!messageGu || !messageEn) {
    return {error:"Gujarati Message and English Message are both required."};
  }
  return {messageGu,messageEn,messageText:messageGu};
}

function defaultWorkingHours():WorkingHours {
  return Object.fromEntries(
    WORKING_DAY_KEYS.map(day=>[day,{isOpen:false,openTime:"09:00",closeTime:"20:00"}])
  ) as WorkingHours;
}

function validWorkingTime(value:unknown):string|null {
  if(typeof value!=='string'||!/^\d{2}:\d{2}$/.test(value))return null;
  const [hour,minute]=value.split(':').map(Number);
  if(!Number.isInteger(hour)||!Number.isInteger(minute)||hour<0||hour>23||minute<0||minute>59)return null;
  return value;
}

function workingTimeMinutes(value:string):number {
  const [hour,minute]=value.split(':').map(Number);
  return hour*60+minute;
}

function storedWorkingHours(value:unknown,fallback:WorkingHours):WorkingHours {
  const raw=value&&typeof value==='object'&&!Array.isArray(value)?value as Record<string,unknown>:{};
  return Object.fromEntries(WORKING_DAY_KEYS.map(day=>{
    const fallbackDay=fallback[day];
    const hasCandidate=Boolean(raw[day]&&typeof raw[day]==='object'&&!Array.isArray(raw[day]));
    const candidate=hasCandidate?raw[day] as Record<string,unknown>:{};
    const candidateOpenTime=validWorkingTime(candidate.openTime);
    const candidateCloseTime=validWorkingTime(candidate.closeTime);
    const openTime=candidateOpenTime||fallbackDay.openTime;
    const closeTime=candidateCloseTime||fallbackDay.closeTime;
    const requestedOpen=typeof candidate.isOpen==='boolean'?candidate.isOpen:fallbackDay.isOpen;
    const validCandidateTimes=!hasCandidate||(candidateOpenTime!==null&&candidateCloseTime!==null);
    const isOpen=requestedOpen&&validCandidateTimes&&workingTimeMinutes(openTime)<workingTimeMinutes(closeTime);
    return [day,{isOpen,openTime,closeTime}];
  })) as WorkingHours;
}

function parseWorkingHoursBody(value:unknown,fallback:WorkingHours):WorkingHours|string {
  if(value===undefined||value===null)return fallback;
  if(typeof value!=='object'||Array.isArray(value))return 'Working Hours must contain day-wise settings.';
  const raw=value as Record<string,unknown>;
  const result={...fallback} as WorkingHours;
  for(const day of WORKING_DAY_KEYS){
    if(raw[day]===undefined)continue;
    if(!raw[day]||typeof raw[day]!=='object'||Array.isArray(raw[day]))return `${day}: invalid working-hours row.`;
    const row=raw[day] as Record<string,unknown>;
    if(typeof row.isOpen!=='boolean')return `${day}: Open/Closed value is required.`;
    const openTime=validWorkingTime(row.openTime);
    const closeTime=validWorkingTime(row.closeTime);
    if(!openTime||!closeTime)return `${day}: opening and closing time must use HH:mm.`;
    if(row.isOpen&&workingTimeMinutes(openTime)>=workingTimeMinutes(closeTime)){
      return `${day}: closing time must be later than opening time.`;
    }
    result[day]={isOpen:row.isOpen,openTime,closeTime};
  }
  return result;
}

function defaultSiteSettings(env:Env):SiteSettings {
  return {
    shopName: env.APP_NAME || "ઝગમગ ડ્રેસીસ",
    websiteTitle: env.APP_NAME || "ઝગમગ ડ્રેસીસ",
    websiteUrl: "",
    logoUrl: "",
    logoPublicId: "",
    contactNumber: env.PUBLIC_CALL_NUMBER || env.PUBLIC_WHATSAPP_NUMBER || "",
    whatsappNumber: env.PUBLIC_WHATSAPP_NUMBER || "",
    address: "",
    defaultLanguage: "GU",
    dateFormat: "DD-MM-YYYY",
    workingHours: defaultWorkingHours(),
    publicCatalogEnabled: true,
    showWhatsApp: true,
    showCall: true,
    availabilityMode: "EXACT",
    fewLeftThreshold: 2,
    whatsappTemplateLanguage: "BOTH"
  };
}

function parseSiteSettings(value:unknown,env:Env):SiteSettings {
  const defaults=defaultSiteSettings(env);
  let raw:Record<string,unknown>={};
  try { if(typeof value==='string'&&value) raw=JSON.parse(value) as Record<string,unknown>; } catch {}
  const language=raw.defaultLanguage==='EN'?'EN':'GU';
  const dateFormat=raw.dateFormat==='YYYY-MM-DD'?'YYYY-MM-DD':'DD-MM-YYYY';
  const availabilityMode=raw.availabilityMode==='STATUS_ONLY'?'STATUS_ONLY':'EXACT';
  const threshold=Math.min(20,Math.max(1,Number(raw.fewLeftThreshold)||2));
  const whatsappTemplateLanguage=normalizeWhatsAppGlobalLanguage(raw.whatsappTemplateLanguage);
  const str=(key:string,fallback:string)=>typeof raw[key]==='string'?String(raw[key]).trim():fallback;
  const flag=(key:string,fallback:boolean)=>typeof raw[key]==='boolean'?Boolean(raw[key]):fallback;
  return {
    shopName:str('shopName',defaults.shopName)||defaults.shopName,
    websiteTitle:str('websiteTitle',defaults.websiteTitle)||defaults.websiteTitle,
    websiteUrl:str('websiteUrl',defaults.websiteUrl),
    logoUrl:str('logoUrl',defaults.logoUrl),
    logoPublicId:str('logoPublicId',defaults.logoPublicId),
    contactNumber:str('contactNumber',defaults.contactNumber),
    whatsappNumber:str('whatsappNumber',defaults.whatsappNumber),
    address:str('address',defaults.address),
    defaultLanguage:language,
    dateFormat,
    workingHours:storedWorkingHours(raw.workingHours,defaults.workingHours),
    publicCatalogEnabled:flag('publicCatalogEnabled',defaults.publicCatalogEnabled),
    showWhatsApp:flag('showWhatsApp',defaults.showWhatsApp),
    showCall:flag('showCall',defaults.showCall),
    availabilityMode,
    fewLeftThreshold:threshold,
    whatsappTemplateLanguage
  };
}

async function getSiteSettings(env:Env):Promise<SiteSettings>{
  const row=await env.DB.prepare(`SELECT value_json FROM settings WHERE key='site_settings'`).first<{value_json:string}>();
  return parseSiteSettings(row?.value_json,env);
}

function parseSettingsBody(body:JsonBody,env:Env,current?:SiteSettings):SiteSettings | string {
  const base=current||defaultSiteSettings(env);
  const shopName=text(body,'shopName');
  const websiteTitle=text(body,'websiteTitle');
  if(!shopName||!websiteTitle)return 'Shop Name and Website Title are required.';
  const websiteUrl=text(body,'websiteUrl');
  if(websiteUrl){
    try{
      const parsedWebsite=new URL(websiteUrl);
      if(!['http:','https:'].includes(parsedWebsite.protocol)||!parsedWebsite.hostname){
        return 'Website must use a valid http:// or https:// URL.';
      }
    }catch{
      return 'Website must use a valid http:// or https:// URL.';
    }
  }
  const logoUrl=text(body,'logoUrl');
  if(logoUrl && !/^https:\/\//i.test(logoUrl))return 'Logo URL must use https://.';
  const requestedLogoPublicId=Object.prototype.hasOwnProperty.call(body,'logoPublicId') ? text(body,'logoPublicId') : base.logoPublicId;
  const logoPublicId=logoUrl ? requestedLogoPublicId : "";
  const defaultLanguage=text(body,'defaultLanguage')==='EN'?'EN':'GU';
  const dateFormat=text(body,'dateFormat')==='YYYY-MM-DD'?'YYYY-MM-DD':'DD-MM-YYYY';
  const availabilityMode=text(body,'availabilityMode')==='STATUS_ONLY'?'STATUS_ONLY':'EXACT';
  const threshold=Math.min(20,Math.max(1,int(body,'fewLeftThreshold',base.fewLeftThreshold)));
  const whatsappTemplateLanguage=normalizeWhatsAppGlobalLanguage(body.whatsappTemplateLanguage ?? base.whatsappTemplateLanguage);
  const workingHours=parseWorkingHoursBody(body.workingHours,base.workingHours);
  if(typeof workingHours==='string')return workingHours;
  return {
    shopName, websiteTitle, websiteUrl, logoUrl, logoPublicId,
    contactNumber:text(body,'contactNumber'),
    whatsappNumber:text(body,'whatsappNumber'),
    address:text(body,'address'),
    defaultLanguage, dateFormat, workingHours,
    publicCatalogEnabled:bool(body,'publicCatalogEnabled',base.publicCatalogEnabled),
    showWhatsApp:bool(body,'showWhatsApp',base.showWhatsApp),
    showCall:bool(body,'showCall',base.showCall),
    availabilityMode, fewLeftThreshold:threshold, whatsappTemplateLanguage
  };
}

async function adminSettingsBootstrap(request:Request,env:Env):Promise<Response>{
  const [settingsRes,templatesRes,usersRes,modulesRes,actionsRes]=await env.DB.batch([
    env.DB.prepare(`SELECT value_json,updated_at FROM settings WHERE key='site_settings' LIMIT 1`),
    env.DB.prepare(`SELECT id,template_key,template_name,message_text,message_gu,message_en,language_mode,linked_action,is_active,created_at,updated_at FROM whatsapp_templates ORDER BY template_name COLLATE NOCASE`),
    env.DB.prepare(`SELECT id,name,role FROM users ORDER BY name COLLATE NOCASE`),
    env.DB.prepare(`SELECT DISTINCT module FROM audit_logs WHERE module<>'' ORDER BY module COLLATE NOCASE LIMIT 100`),
    env.DB.prepare(`SELECT DISTINCT action FROM audit_logs WHERE action<>'' ORDER BY action COLLATE NOCASE LIMIT 100`)
  ]);
  const settingsRow=(settingsRes.results?.[0] as any)||null;
  return json(request,env,{
    ok:true,
    settings:parseSiteSettings(settingsRow?.value_json,env),
    updatedAt:settingsRow?.updated_at||null,
    templates:(templatesRes.results||[]).map((r:any)=>({...r,is_active:Number(r.is_active)===1})),
    users:usersRes.results||[],
    auditModules:(modulesRes.results||[]).map((r:any)=>String(r.module)),
    auditActions:(actionsRes.results||[]).map((r:any)=>String(r.action)),
    templatePlaceholders:WHATSAPP_PLACEHOLDERS.map(key=>`{${key}}`),
    templatePlaceholderRegistry:Object.fromEntries(
      Object.entries(WHATSAPP_PLACEHOLDERS_BY_ACTION).map(([action,keys])=>[
        action,
        keys.map(key=>`{${key}}`)
      ])
    ),
    templatePlaceholderSources:WHATSAPP_PLACEHOLDER_SOURCES,
    templateLanguageModes:WHATSAPP_GLOBAL_LANGUAGE_MODES,
    templateLinkedActions:WHATSAPP_LINKED_ACTIONS
  });
}

async function updateSiteSettings(request:Request,env:Env,user:SessionUser):Promise<Response>{
  const body=await readJson(request); if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const row=await env.DB.prepare(`SELECT value_json,updated_at FROM settings WHERE key='site_settings' LIMIT 1`).first<any>();
  const old=parseSiteSettings(row?.value_json,env); const parsed=parseSettingsBody(body,env,old);
  if(typeof parsed==='string')return json(request,env,{ok:false,error:'VALIDATION',message:parsed},{status:400});
  const expectedUpdatedAt=text(body,'expectedUpdatedAt');
  const nextUpdatedAt=new Date().toISOString();
  if(!row){
    await env.DB.batch([
      env.DB.prepare(`INSERT INTO settings(key,value_json,updated_by_user_id,updated_at) VALUES('site_settings',?,?,?)`).bind(JSON.stringify(parsed),user.id,nextUpdatedAt),
      env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
        .bind(crypto.randomUUID(),user.id,'UPDATE','SETTINGS','site_settings',JSON.stringify(old),JSON.stringify(parsed))
    ]);
    return json(request,env,{ok:true,settings:parsed,updatedAt:nextUpdatedAt,message:'Settings updated.'});
  }
  const result=await env.DB.batch([
    expectedUpdatedAt
      ? env.DB.prepare(`UPDATE settings SET value_json=?,updated_by_user_id=?,updated_at=? WHERE key='site_settings' AND updated_at=?`).bind(JSON.stringify(parsed),user.id,nextUpdatedAt,expectedUpdatedAt)
      : env.DB.prepare(`UPDATE settings SET value_json=?,updated_by_user_id=?,updated_at=? WHERE key='site_settings'`).bind(JSON.stringify(parsed),user.id,nextUpdatedAt),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
      SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM settings WHERE key='site_settings' AND updated_at=?)`).bind(
        crypto.randomUUID(),user.id,'UPDATE','SETTINGS','site_settings',JSON.stringify(old),JSON.stringify(parsed),nextUpdatedAt
      )
  ]);
  if(expectedUpdatedAt && Number((result[0] as any)?.meta?.changes||0)===0){
    return json(request,env,{ok:false,error:'STALE_WRITE',message:'Settings changed on another device. Refresh and review before saving again.'},{status:409});
  }
  return json(request,env,{ok:true,settings:parsed,updatedAt:nextUpdatedAt,message:'Settings updated.'});
}

function normalizedTemplateKey(value:string):string{return value.toLowerCase().replace(/[^a-z0-9]+/g,'_').replace(/^_+|_+$/g,'').slice(0,80);}

async function createWhatsAppTemplate(request:Request,env:Env,user:SessionUser):Promise<Response>{
  const body=await readJson(request);if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const name=text(body,'templateName'), key=normalizedTemplateKey(text(body,'templateKey')||name);
  const linkedAction=normalizeWhatsAppLinkedAction(body.linkedAction);
  const messages=whatsappTemplateMessages(body);
  if(!key||!name)return json(request,env,{ok:false,error:'VALIDATION',message:'Template Key and Name are required.'},{status:400});
  if(!linkedAction)return json(request,env,{ok:false,error:'VALIDATION',message:'Linked To is required.'},{status:400});
  if('error' in messages)return json(request,env,{ok:false,error:'VALIDATION',message:messages.error},{status:400});
  const placeholderError=validateTemplatePlaceholders(linkedAction,[messages.messageGu,messages.messageEn]);
  if(placeholderError)return json(request,env,{ok:false,error:'INVALID_PLACEHOLDER',message:placeholderError},{status:400});
  const id=crypto.randomUUID();
  const active=bool(body,'isActive',true);
  try{
    await env.DB.batch([
      env.DB.prepare(`INSERT INTO whatsapp_templates
        (id,template_key,template_name,message_text,message_gu,message_en,language_mode,linked_action,is_active)
        VALUES (?,?,?,?,?,?,?,?,?)`).bind(
          id,key,name,messages.messageText,messages.messageGu||null,messages.messageEn||null,
          "ALL",linkedAction,active?1:0
        ),
      env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,new_value_json) VALUES (?,?,?,?,?,?)`).bind(
        crypto.randomUUID(),user.id,'CREATE','WHATSAPP_TEMPLATE',id,
        JSON.stringify({templateKey:key,templateName:name,languageMode:"GLOBAL",linkedAction,isActive:active})
      )
    ]);
  }catch(e:any){
    const raw=String(e?.message||e).toLowerCase();
    if(raw.includes('ux_whatsapp_templates_active_link'))return json(request,env,{ok:false,error:'DUPLICATE_LINK',message:'Another active template is already linked to this action.'},{status:409});
    if(raw.includes('unique'))return json(request,env,{ok:false,error:'DUPLICATE',message:'Template key already exists or this linked action already has an active template.'},{status:409});
    throw e;
  }
  return json(request,env,{ok:true,id,message:'WhatsApp template added.'},{status:201});
}

async function updateWhatsAppTemplate(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT id,template_key,template_name,message_text,message_gu,message_en,language_mode,linked_action,is_active,updated_at FROM whatsapp_templates WHERE id=?`).bind(id).first<any>();
  if(!old)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Template not found.'},{status:404});
  const body=await readJson(request);if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const name=text(body,'templateName'), key=normalizedTemplateKey(text(body,'templateKey')||old.template_key);
  const linkedAction=Object.prototype.hasOwnProperty.call(body,'linkedAction')
    ? normalizeWhatsAppLinkedAction(body.linkedAction)
    : normalizeWhatsAppLinkedAction(old.linked_action);
  const legacyMessage=text(body,'messageText');
  const explicitGu=text(body,'messageGu');
  const explicitEn=text(body,'messageEn');
  const mergedBody:JsonBody={
    ...body,
    messageGu:explicitGu || (legacyMessage && !explicitEn ? legacyMessage : String(old.message_gu||'')),
    messageEn:explicitEn || (legacyMessage && !explicitGu ? legacyMessage : String(old.message_en||'')),
    messageText:legacyMessage || String(old.message_text||'')
  };
  const messages=whatsappTemplateMessages(mergedBody);
  if(!key||!name)return json(request,env,{ok:false,error:'VALIDATION',message:'Template Key and Name are required.'},{status:400});
  if(!linkedAction)return json(request,env,{ok:false,error:'VALIDATION',message:'Linked To is required.'},{status:400});
  if('error' in messages)return json(request,env,{ok:false,error:'VALIDATION',message:messages.error},{status:400});
  const placeholderError=validateTemplatePlaceholders(linkedAction,[messages.messageGu,messages.messageEn]);
  if(placeholderError)return json(request,env,{ok:false,error:'INVALID_PLACEHOLDER',message:placeholderError},{status:400});
  const active=bool(body,'isActive',Number(old.is_active)===1);
  const expectedUpdatedAt=text(body,'expectedUpdatedAt');
  const nextUpdatedAt=new Date().toISOString();
  const next={
    templateKey:key,templateName:name,messageText:messages.messageText,
    messageGu:messages.messageGu||null,messageEn:messages.messageEn||null,
    languageMode:"GLOBAL",linkedAction,isActive:active,updatedAt:nextUpdatedAt
  };
  try{
    const result=await env.DB.batch([
      expectedUpdatedAt
        ? env.DB.prepare(`UPDATE whatsapp_templates
            SET template_key=?,template_name=?,message_text=?,message_gu=?,message_en=?,language_mode=?,linked_action=?,is_active=?,updated_at=?
            WHERE id=? AND updated_at=?`).bind(
              key,name,messages.messageText,messages.messageGu||null,messages.messageEn||null,
              "ALL",linkedAction,active?1:0,nextUpdatedAt,id,expectedUpdatedAt
            )
        : env.DB.prepare(`UPDATE whatsapp_templates
            SET template_key=?,template_name=?,message_text=?,message_gu=?,message_en=?,language_mode=?,linked_action=?,is_active=?,updated_at=?
            WHERE id=?`).bind(
              key,name,messages.messageText,messages.messageGu||null,messages.messageEn||null,
              "ALL",linkedAction,active?1:0,nextUpdatedAt,id
            ),
      env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json,new_value_json)
        SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM whatsapp_templates WHERE id=? AND updated_at=?)`).bind(
        crypto.randomUUID(),user.id,'UPDATE','WHATSAPP_TEMPLATE',id,JSON.stringify(old),JSON.stringify(next),id,nextUpdatedAt
      )
    ]);
    if(expectedUpdatedAt && Number((result[0] as any)?.meta?.changes||0)===0){
      return json(request,env,{ok:false,error:'STALE_WRITE',message:'This WhatsApp template changed on another device. Refresh before saving again.'},{status:409});
    }
  }catch(e:any){
    const raw=String(e?.message||e).toLowerCase();
    if(raw.includes('ux_whatsapp_templates_active_link'))return json(request,env,{ok:false,error:'DUPLICATE_LINK',message:'Another active template is already linked to this action.'},{status:409});
    if(raw.includes('unique'))return json(request,env,{ok:false,error:'DUPLICATE',message:'Template key already exists or this linked action already has an active template.'},{status:409});
    throw e;
  }
  return json(request,env,{ok:true,updatedAt:nextUpdatedAt,message:'WhatsApp template updated.'});
}

async function deleteWhatsAppTemplate(request:Request,env:Env,user:SessionUser,id:string):Promise<Response>{
  const old=await env.DB.prepare(`SELECT id,template_key,template_name,message_text,message_gu,message_en,language_mode,linked_action,is_active FROM whatsapp_templates WHERE id=?`).bind(id).first<any>();
  if(!old)return json(request,env,{ok:false,error:'NOT_FOUND',message:'Template not found.'},{status:404});
  await env.DB.batch([
    env.DB.prepare(`DELETE FROM whatsapp_templates WHERE id=?`).bind(id),
    env.DB.prepare(`INSERT INTO audit_logs (id,user_id,action,module,record_id,old_value_json) VALUES (?,?,?,?,?,?)`).bind(crypto.randomUUID(),user.id,'DELETE','WHATSAPP_TEMPLATE',id,JSON.stringify(old))
  ]);
  return json(request,env,{ok:true,message:'WhatsApp template deleted.'});
}

async function listAuditLogs(request:Request,env:Env):Promise<Response>{
  const u=new URL(request.url); const page=Math.max(1,Number(u.searchParams.get('page')||'1')||1); const pageSize=Math.min(50,Math.max(10,Number(u.searchParams.get('pageSize')||'20')||20));
  const module=(u.searchParams.get('module')||'').trim(), action=(u.searchParams.get('action')||'').trim(), userId=(u.searchParams.get('userId')||'').trim(), search=(u.searchParams.get('search')||'').trim();
  const fromDate=(u.searchParams.get('fromDate')||'').trim(), toDate=(u.searchParams.get('toDate')||'').trim();
  const where:string[]=['1=1']; const binds:unknown[]=[];
  if(module){where.push('a.module=?');binds.push(module);} if(action){where.push('a.action=?');binds.push(action);} if(userId){where.push('a.user_id=?');binds.push(userId);}
  if(fromDate&&validDateOnly(fromDate)){where.push('a.created_at>=?');binds.push(istDayStartUtc(fromDate));}
  if(toDate&&validDateOnly(toDate)){where.push('a.created_at<?');binds.push(istDayStartUtc(addIsoDays(toDate,1)));}
  if(search){where.push(`(LOWER(COALESCE(u.name,'')) LIKE LOWER(?) OR LOWER(COALESCE(a.record_id,'')) LIKE LOWER(?) OR LOWER(COALESCE(a.old_value_json,'')) LIKE LOWER(?) OR LOWER(COALESCE(a.new_value_json,'')) LIKE LOWER(?))`);const q=`%${search}%`;binds.push(q,q,q,q);}
  const clause=where.join(' AND ');
  const [countRes,rowsRes]=await env.DB.batch([
    env.DB.prepare(`SELECT COUNT(*) AS count FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`SELECT a.id,a.user_id,a.action,a.module,a.record_id,a.old_value_json,a.new_value_json,a.created_at,u.name AS user_name,u.role AS user_role
      FROM audit_logs a LEFT JOIN users u ON u.id=a.user_id WHERE ${clause} ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?`).bind(...binds,pageSize,(page-1)*pageSize)
  ]);
  const total=Number((countRes.results?.[0] as any)?.count||0);
  return json(request,env,{ok:true,logs:rowsRes.results||[],pagination:{page,pageSize,total,pages:Math.max(1,Math.ceil(total/pageSize))}});
}

type PublicCatalogField = { fieldId:string; name:string; type:string; value:string };
type PublicCatalogItem = {
  id:string; itemCode:string; itemName:string; categoryId:string; categoryName:string;
  primaryImage:string|null; images:string[]; fields:PublicCatalogField[];
};

function parseJsonArray(value: unknown): any[] {
  if (Array.isArray(value)) return value;
  if (typeof value !== "string" || !value) return [];
  try { const parsed=JSON.parse(value); return Array.isArray(parsed)?parsed:[]; } catch { return []; }
}

function publicFieldDisplay(type:string,value:string): string {
  if(type==='YES_NO') return value==='1'?'Yes':'No';
  if(type==='MULTI_SELECT') {
    try { const parsed=JSON.parse(value); return Array.isArray(parsed)?parsed.map(v=>String(v)).join(', '):value; } catch { return value; }
  }
  return value;
}

async function publicCatalog(request:Request,env:Env):Promise<Response>{
  const url=new URL(request.url);
  const page=Math.max(1,Number(url.searchParams.get('page')||'1')||1);
  const pageSize=Math.min(24,Math.max(6,Number(url.searchParams.get('pageSize')||'12')||12));
  const search=(url.searchParams.get('search')||'').trim();
  const categoryId=(url.searchParams.get('categoryId')||'').trim();
  const where:string[]=["i.archived_at IS NULL","i.is_active=1","i.public_visible=1","c.is_active=1","c.public_visible=1"];
  const binds:unknown[]=[];
  if(search){where.push("(LOWER(i.item_code) LIKE LOWER(?) OR LOWER(i.item_name) LIKE LOWER(?))"); const q=`%${search}%`;binds.push(q,q);}
  if(categoryId){where.push("i.category_id=?");binds.push(categoryId);}
  const clause=where.join(' AND ');
  const [settingsRes,categoryRes,countRes,itemRes]=await env.DB.batch([
    env.DB.prepare(`SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1`),
    env.DB.prepare(`SELECT c.id,c.name,c.code_prefix,c.display_order,
      (SELECT COUNT(*) FROM items i WHERE i.category_id=c.id AND i.archived_at IS NULL AND i.is_active=1 AND i.public_visible=1) AS item_count
      FROM categories c WHERE c.is_active=1 AND c.public_visible=1
      ORDER BY c.display_order,c.name COLLATE NOCASE`),
    env.DB.prepare(`SELECT COUNT(*) AS count FROM items i JOIN categories c ON c.id=i.category_id WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`SELECT i.id,i.item_code,i.item_name,i.category_id,c.name AS category_name,
      (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order LIMIT 1) AS primary_image,
      COALESCE((SELECT json_group_array(image_url) FROM (SELECT image_url FROM item_images im2 WHERE im2.item_id=i.id ORDER BY im2.is_primary DESC,im2.display_order LIMIT 8)),'[]') AS images_json,
      COALESCE((SELECT json_group_array(json_object('fieldId',f.id,'name',f.field_name,'type',f.field_type,'value',v.value_text))
        FROM item_field_values v JOIN category_fields f ON f.id=v.category_field_id
        WHERE v.item_id=i.id AND f.is_active=1 AND f.public_visible=1),'[]') AS fields_json
      FROM items i JOIN categories c ON c.id=i.category_id
      WHERE ${clause}
      ORDER BY c.display_order,c.name COLLATE NOCASE,i.updated_at DESC,i.item_name COLLATE NOCASE
      LIMIT ? OFFSET ?`).bind(...binds,pageSize,(page-1)*pageSize)
  ]);
  const settings=parseSiteSettings((settingsRes.results?.[0] as any)?.value_json,env);
  const publicShop={
    name:settings.shopName, websiteTitle:settings.websiteTitle, logoUrl:settings.logoUrl||null, address:settings.address||null,
    whatsappNumber:settings.showWhatsApp&&settings.whatsappNumber?settings.whatsappNumber:null,
    callNumber:settings.showCall&&settings.contactNumber?settings.contactNumber:null
  };
  if(!settings.publicCatalogEnabled){
    return json(request,env,{ok:true,catalogEnabled:false,shop:publicShop,pricingEnabled:false,availabilityMode:settings.availabilityMode,categories:[],items:[],pagination:{page:1,pageSize,total:0,totalPages:1}},
      {headers:{"cache-control":"public, max-age=30, s-maxage=60"}});
  }
  const total=Number((countRes.results?.[0] as any)?.count||0);
  const items:PublicCatalogItem[]=((itemRes.results||[]) as any[]).map(row=>({
    id:String(row.id), itemCode:String(row.item_code), itemName:String(row.item_name),
    categoryId:String(row.category_id), categoryName:String(row.category_name),
    primaryImage:row.primary_image?String(row.primary_image):null,
    images:parseJsonArray(row.images_json).map(v=>String(v)),
    fields:parseJsonArray(row.fields_json).map((f:any)=>({fieldId:String(f.fieldId||''),name:String(f.name||''),type:String(f.type||''),value:publicFieldDisplay(String(f.type||''),String(f.value??''))}))
  }));
  return json(request,env,{
    ok:true,catalogEnabled:true,shop:publicShop,pricingEnabled:false,availabilityMode:settings.availabilityMode,
    categories:(categoryRes.results||[]).map((r:any)=>({id:String(r.id),name:String(r.name),codePrefix:String(r.code_prefix),itemCount:Number(r.item_count||0)})),
    items,
    pagination:{page,pageSize,total,totalPages:Math.max(1,Math.ceil(total/pageSize))}
  },{headers:{"cache-control":"public, max-age=60, s-maxage=120"}});
}

type PublicAvailabilityStatus = "AVAILABLE" | "LIMITED" | "FEW_LEFT" | "NOT_AVAILABLE";

function publicAvailabilityStatus(available:number):PublicAvailabilityStatus {
  if(available<=0)return "NOT_AVAILABLE";
  if(available<=2)return "FEW_LEFT";
  if(available<=5)return "LIMITED";
  return "AVAILABLE";
}

async function publicAvailability(request:Request,env:Env):Promise<Response>{
  const body=await readJson(request); if(!body)return json(request,env,{ok:false,error:'INVALID_JSON'},{status:400});
  const pickupDate=text(body,'pickupDate'), returnDate=text(body,'returnDate');
  const ids=Array.isArray(body.itemIds)?[...new Set(body.itemIds.map(v=>String(v).trim()).filter(Boolean))].slice(0,24):[];
  if(!validDateOnly(pickupDate)||!validDateOnly(returnDate)||!ids.length)
    return json(request,env,{ok:false,error:'VALIDATION',message:'Valid Pickup Date, Return Date and at least one item are required.'},{status:400});
  if(pickupDate<businessToday())
    return json(request,env,{ok:false,error:'PAST_PICKUP_DATE',message:'Past pickup dates are not allowed.'},{status:400});
  if(returnDate<=pickupDate)
    return json(request,env,{ok:false,error:'INVALID_RENTAL_DATE_RANGE',message:'Return Date must be at least one day after Pickup Date.'},{status:400});
  const settings=await getSiteSettings(env);
  if(!settings.publicCatalogEnabled)return json(request,env,{ok:false,error:'CATALOG_DISABLED',message:'Public catalog is currently unavailable.'},{status:403});
  const placeholders=ids.map(()=>'?').join(',');
  const rows=await env.DB.prepare(`SELECT i.id,i.total_quantity,
    COALESCE(SUM(CASE
      WHEN b.id IS NULL THEN 0
      WHEN b.pickup_date<=? AND b.return_date>=? THEN bi.booked_qty-bi.closed_qty
      WHEN b.return_date<? AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty
      ELSE 0 END),0) AS reserved_qty
    FROM items i JOIN categories c ON c.id=i.category_id
    LEFT JOIN booking_items bi ON bi.item_id=i.id
    LEFT JOIN bookings b ON b.id=bi.booking_id AND b.status NOT IN ('CANCELLED','RETURNED')
    WHERE i.id IN (${placeholders}) AND i.archived_at IS NULL AND i.is_active=1 AND i.public_visible=1 AND c.is_active=1 AND c.public_visible=1
    GROUP BY i.id`).bind(returnDate,pickupDate,pickupDate,...ids).all<any>();
  const availability=(rows.results||[]).map((r:any)=>{
    const available=Math.max(0,Number(r.total_quantity||0)-Number(r.reserved_qty||0));
    const status=publicAvailabilityStatus(available);
    return settings.availabilityMode==='EXACT'?{itemId:String(r.id),status,availableQuantity:available}:{itemId:String(r.id),status};
  });
  return json(request,env,{ok:true,pickupDate,returnDate,availabilityMode:settings.availabilityMode,availability});
}



function normalizeUserMobile(value: string): string {
  const digits = value.replace(/[^0-9]/g, "");
  return digits.length === 12 && digits.startsWith("91") ? digits.slice(2) : digits;
}

function normalizeUserEmail(value: string): string { return value.trim().toLowerCase(); }
function userContactError(email: string, mobile: string): string | null {
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return "Enter a valid email address.";
  if (mobile && !/^\d{10}$/.test(mobile)) return "Enter a valid 10-digit mobile number.";
  return null;
}

function normalizeManagedRole(value: unknown): UserRole {
  return String(value || "").trim().toUpperCase() === "OWNER" ? "OWNER" : "STAFF";
}

function staffPermissionsFromBody(body: JsonBody, role: UserRole, fallback: StaffPermission[] = DEFAULT_STAFF_PERMISSIONS): StaffPermission[] {
  if (role === "OWNER") return [];
  if (!Object.prototype.hasOwnProperty.call(body, "staffPermissions")) return fallback;
  return parseStaffPermissions(body.staffPermissions);
}

function safeUserSnapshot(row: any) {
  const role=normalizeManagedRole(row.role);
  return {
    id:String(row.id),
    name:String(row.name),
    mobile:row.mobile ? String(row.mobile) : null,
    role,
    isActive:Number(row.is_active)===1,
    archivedAt:row.archived_at ? String(row.archived_at) : null,
    lastLoginAt:row.last_login_at ? String(row.last_login_at) : null,
    staffPermissions:role==="OWNER" ? [] : parseStaffPermissions(row.staff_permissions_json)
  };
}

async function ensureOwnerContinuity(request: Request, env: Env, targetId: string, targetRole: UserRole, nextRole: UserRole, nextActive: boolean, archived: boolean): Promise<Response | null> {
  if (targetRole !== "OWNER") return null;
  const remainsOwner = nextRole === "OWNER" && nextActive && !archived;
  if (remainsOwner) return null;
  const count = await env.DB.prepare(
    `SELECT COUNT(*) AS count FROM users WHERE role='OWNER' AND is_active=1 AND archived_at IS NULL AND id<>?`
  ).bind(targetId).first<{count:number}>();
  return Number(count?.count || 0) > 0 ? null : json(request, env, { ok:false, error:"LAST_OWNER", message:"At least one active Owner account must remain." }, { status:409 });
}

async function listManagedUsers(request: Request, env: Env, actor: SessionUser): Promise<Response> {
  const url = new URL(request.url);
  const search = (url.searchParams.get("search") || "").trim();
  const role = (url.searchParams.get("role") || "").trim().toUpperCase();
  const status = (url.searchParams.get("status") || "active").trim();
  const page = Math.max(1, Number(url.searchParams.get("page") || "1") || 1);
  const pageSize = Math.min(10, Math.max(1, Number(url.searchParams.get("pageSize") || "10") || 10));
  const where:string[] = ["1=1"]; const args:any[] = [];
  if (search) {
    where.push("(u.name LIKE ? OR COALESCE(u.mobile,'') LIKE ?)");
    const q=`%${search}%`;
    args.push(q,q);
  }
  if (["OWNER","STAFF"].includes(role)) { where.push("u.role=?"); args.push(role); }
  if (status === "active") where.push("u.is_active=1 AND u.archived_at IS NULL");
  else if (status === "inactive") where.push("u.is_active=0 AND u.archived_at IS NULL");
  else if (status === "archived") where.push("u.archived_at IS NOT NULL");
  else if (status === "all") { /* no status filter */ }
  else where.push("u.archived_at IS NULL");

  const whereSql=where.join(" AND ");
  const rowsResult=await env.DB.prepare(`
    WITH page_users AS (
      SELECT u.id,u.name,u.mobile,u.role,u.is_active,u.last_login_at,u.archived_at,u.created_at,u.updated_at,u.staff_permissions_json,
             COUNT(*) OVER() AS __total
      FROM users u
      WHERE ${whereSql}
      ORDER BY CASE u.role WHEN 'OWNER' THEN 0 ELSE 1 END, u.name COLLATE NOCASE
      LIMIT ? OFFSET ?
    ),
    booking_counts AS (
      SELECT b.created_by_user_id AS user_id,COUNT(*) AS bookings_created
      FROM bookings b JOIN page_users pu ON pu.id=b.created_by_user_id
      GROUP BY b.created_by_user_id
    ),
    pickup_counts AS (
      SELECT p.handled_by_user_id AS user_id,COUNT(*) AS pickups_handled
      FROM pickup_events p JOIN page_users pu ON pu.id=p.handled_by_user_id
      GROUP BY p.handled_by_user_id
    ),
    return_counts AS (
      SELECT r.received_by_user_id AS user_id,COUNT(*) AS returns_handled
      FROM return_events r JOIN page_users pu ON pu.id=r.received_by_user_id
      GROUP BY r.received_by_user_id
    )
    SELECT pu.*,COALESCE(bc.bookings_created,0) AS bookings_created,
           COALESCE(pc.pickups_handled,0) AS pickups_handled,
           COALESCE(rc.returns_handled,0) AS returns_handled
    FROM page_users pu
    LEFT JOIN booking_counts bc ON bc.user_id=pu.id
    LEFT JOIN pickup_counts pc ON pc.user_id=pu.id
    LEFT JOIN return_counts rc ON rc.user_id=pu.id
    ORDER BY CASE pu.role WHEN 'OWNER' THEN 0 ELSE 1 END, pu.name COLLATE NOCASE
  `).bind(...args,pageSize,(page-1)*pageSize).all();
  const rows=(rowsResult?.results||[]) as any[];
  const total=rows.length ? Number(rows[0].__total||0) : 0;
  const pages=Math.max(1,Math.ceil(total/pageSize));
  const safePage=Math.min(page,pages);
  return json(request,env,{
    ok:true,
    currentUser:publicUser(actor),
    permissionOptions:STAFF_PERMISSION_KEYS,
    users:rows.map((r:any)=>{
      const role=normalizeManagedRole(r.role);
      return {
        id:String(r.id),
        name:String(r.name),
        mobile:r.mobile?String(r.mobile):null,
        role,
        isActive:Number(r.is_active)===1,
        archivedAt:r.archived_at?String(r.archived_at):null,
        lastLoginAt:r.last_login_at?String(r.last_login_at):null,
        createdAt:String(r.created_at),
        updatedAt:String(r.updated_at),
        staffPermissions:role==="OWNER"?[]:parseStaffPermissions(r.staff_permissions_json),
        bookingsCreated:Number(r.bookings_created||0),
        pickupsHandled:Number(r.pickups_handled||0),
        returnsHandled:Number(r.returns_handled||0)
      };
    }),
    pagination:{page:safePage,pageSize,total,pages}
  });
}

async function createManagedUser(request: Request, env: Env, actor: SessionUser): Promise<Response> {
  const body=await readJson(request);
  if(!body) return json(request,env,{ok:false,error:"INVALID_JSON"},{status:400});
  const name=text(body,"name");
  const mobile=normalizeUserMobile(text(body,"mobile"));
  const role=normalizeManagedRole(body.role);
  const password=typeof body.password==="string"?body.password:"";
  const contactError=userContactError("",mobile);
  if(!name || !mobile) return json(request,env,{ok:false,error:"VALIDATION",message:"Name and mobile number are required."},{status:400});
  if(contactError) return json(request,env,{ok:false,error:"VALIDATION",message:contactError},{status:400});
  const passwordError=validatePassword(password);
  if(passwordError) return json(request,env,{ok:false,error:"VALIDATION",message:passwordError},{status:400});
  const duplicate=await env.DB.prepare(`SELECT id FROM users WHERE mobile=? LIMIT 1`).bind(mobile).first<any>();
  if(duplicate) return json(request,env,{ok:false,error:"DUPLICATE",message:"Mobile number is already used by another user."},{status:409});
  const permissions=staffPermissionsFromBody(body,role);
  const id=crypto.randomUUID();
  const passwordHash=await hashPassword(password);
  const next={id,name,mobile,role,isActive:true,archivedAt:null,staffPermissions:permissions};
  await env.DB.batch([
    env.DB.prepare(`
      INSERT INTO users(id,name,email,mobile,password_hash,role,is_active,archived_at,staff_permissions_json)
      VALUES(?,?,NULL,?,?,?,?,NULL,?)
    `).bind(id,name,mobile,passwordHash,role,1,JSON.stringify(permissions)),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,new_value_json) VALUES(?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),actor.id,"CREATE_USER","USERS",id,JSON.stringify(next))
  ]);
  return json(request,env,{ok:true,message:"User created.",user:next},{status:201});
}

async function updateManagedUser(request: Request, env: Env, actor: SessionUser, id: string): Promise<Response> {
  const current=await env.DB.prepare(`
    SELECT id,name,email,mobile,role,is_active,archived_at,last_login_at,staff_permissions_json
    FROM users WHERE id=?
  `).bind(id).first<any>();
  if(!current) return json(request,env,{ok:false,error:"NOT_FOUND",message:"User not found."},{status:404});
  const body=await readJson(request);
  if(!body) return json(request,env,{ok:false,error:"INVALID_JSON"},{status:400});
  const name=text(body,"name");
  const mobile=normalizeUserMobile(text(body,"mobile"));
  const currentRole=normalizeManagedRole(current.role);
  const nextRole=normalizeManagedRole(body.role || currentRole);
  const nextActive=bool(body,"isActive",Number(current.is_active)===1);
  if(!name || !mobile) return json(request,env,{ok:false,error:"VALIDATION",message:"Name and mobile number are required."},{status:400});
  const contactError=userContactError("",mobile);
  if(contactError) return json(request,env,{ok:false,error:"VALIDATION",message:contactError},{status:400});
  if(id===actor.id && (!nextActive || nextRole!==actor.role)) return json(request,env,{ok:false,error:"SELF_PROTECTION",message:"You cannot deactivate or change your own role."},{status:409});
  if(current.archived_at) return json(request,env,{ok:false,error:"ARCHIVED",message:"Restore this user before editing."},{status:409});
  const ownerBlock=await ensureOwnerContinuity(request,env,id,currentRole,nextRole,nextActive,false);
  if(ownerBlock) return ownerBlock;
  const duplicate=await env.DB.prepare(`SELECT id FROM users WHERE id<>? AND mobile=? LIMIT 1`).bind(id,mobile).first<any>();
  if(duplicate) return json(request,env,{ok:false,error:"DUPLICATE",message:"Mobile number is already used by another user."},{status:409});
  const fallback=parseStaffPermissions(current.staff_permissions_json);
  const permissions=staffPermissionsFromBody(body,nextRole,fallback.length?fallback:DEFAULT_STAFF_PERMISSIONS);
  const currentPermissions=currentRole==="OWNER"?[]:fallback;
  const permissionsChanged=
    JSON.stringify([...currentPermissions].sort())!==JSON.stringify([...permissions].sort());
  const old=safeUserSnapshot(current);
  const expectedUpdatedAt=text(body,"expectedUpdatedAt");
  const nextUpdatedAt=new Date().toISOString();
  const next={id,name,mobile,role:nextRole,isActive:nextActive,archivedAt:null,lastLoginAt:current.last_login_at||null,staffPermissions:permissions,updatedAt:nextUpdatedAt};
  const statements:any[]=[
    expectedUpdatedAt
      ? env.DB.prepare(`UPDATE users SET name=?,mobile=?,role=?,is_active=?,staff_permissions_json=?,updated_at=? WHERE id=? AND updated_at=?`).bind(name,mobile,nextRole,nextActive?1:0,JSON.stringify(permissions),nextUpdatedAt,id,expectedUpdatedAt)
      : env.DB.prepare(`UPDATE users SET name=?,mobile=?,role=?,is_active=?,staff_permissions_json=?,updated_at=? WHERE id=?`).bind(name,mobile,nextRole,nextActive?1:0,JSON.stringify(permissions),nextUpdatedAt,id)
  ];
  if(!nextActive || nextRole!==currentRole || permissionsChanged) {
    statements.push(env.DB.prepare(`DELETE FROM auth_sessions WHERE user_id=? AND EXISTS (SELECT 1 FROM users WHERE id=? AND updated_at=?)`).bind(id,id,nextUpdatedAt));
  }
  statements.push(env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
    SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM users WHERE id=? AND updated_at=?)`).bind(
      crypto.randomUUID(),actor.id,"UPDATE_USER","USERS",id,JSON.stringify(old),JSON.stringify(next),id,nextUpdatedAt
    ));
  try{
    const result=await env.DB.batch(statements);
    if(expectedUpdatedAt && Number((result[0] as any)?.meta?.changes||0)===0){
      return json(request,env,{ok:false,error:"STALE_WRITE",message:"This user changed on another device. Refresh before saving again."},{status:409});
    }
  }catch(error){
    if(dbErrorHas(error,"LAST_OWNER"))return json(request,env,{ok:false,error:"LAST_OWNER",message:"At least one active Owner account must remain."},{status:409});
    throw error;
  }
  return json(request,env,{ok:true,message:"User updated.",user:next});
}

async function resetManagedUserPassword(request: Request, env: Env, actor: SessionUser, id: string): Promise<Response> {
  const target=await env.DB.prepare(`SELECT id,name,role,is_active,archived_at FROM users WHERE id=?`).bind(id).first<any>();
  if(!target) return json(request,env,{ok:false,error:"NOT_FOUND",message:"User not found."},{status:404});
  if(target.archived_at) return json(request,env,{ok:false,error:"ARCHIVED",message:"Restore this user before resetting the password."},{status:409});
  const body=await readJson(request);
  if(!body) return json(request,env,{ok:false,error:"INVALID_JSON"},{status:400});
  const password=typeof body.password==="string"?body.password:"";
  const passwordError=validatePassword(password);
  if(passwordError) return json(request,env,{ok:false,error:"VALIDATION",message:passwordError},{status:400});
  const hash=await hashPassword(password);
  await env.DB.batch([
    env.DB.prepare(`UPDATE users SET password_hash=?,updated_at=CURRENT_TIMESTAMP WHERE id=?`).bind(hash,id),
    env.DB.prepare(`DELETE FROM auth_sessions WHERE user_id=?`).bind(id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,new_value_json) VALUES(?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),actor.id,"RESET_USER_PASSWORD","USERS",id,JSON.stringify({userId:id,userName:String(target.name)}))
  ]);
  return json(request,env,{ok:true,message:id===actor.id?"Password changed. Please sign in again.":"Password reset. Existing sessions were signed out."});
}

async function archiveManagedUser(request: Request, env: Env, actor: SessionUser, id: string): Promise<Response> {
  const current=await env.DB.prepare(`
    SELECT id,name,mobile,role,is_active,archived_at,last_login_at,staff_permissions_json
    FROM users WHERE id=?
  `).bind(id).first<any>();
  if(!current) return json(request,env,{ok:false,error:"NOT_FOUND",message:"User not found."},{status:404});
  if(id===actor.id) return json(request,env,{ok:false,error:"SELF_PROTECTION",message:"You cannot archive your own account."},{status:409});
  if(current.archived_at) return json(request,env,{ok:true,message:"User is already archived."});
  const currentRole=normalizeManagedRole(current.role);
  const ownerBlock=await ensureOwnerContinuity(request,env,id,currentRole,currentRole,false,true);
  if(ownerBlock) return ownerBlock;
  const old=safeUserSnapshot(current);
  const archivedAt=new Date().toISOString();
  const next={...old,isActive:false,archivedAt};
  try{
    await env.DB.batch([
      env.DB.prepare(`UPDATE users SET is_active=0,archived_at=?,updated_at=? WHERE id=?`).bind(archivedAt,archivedAt,id),
      env.DB.prepare(`DELETE FROM auth_sessions WHERE user_id=?`).bind(id),
      env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
        .bind(crypto.randomUUID(),actor.id,"ARCHIVE_USER","USERS",id,JSON.stringify(old),JSON.stringify(next))
    ]);
  }catch(error){
    if(dbErrorHas(error,"LAST_OWNER"))return json(request,env,{ok:false,error:"LAST_OWNER",message:"At least one active Owner account must remain."},{status:409});
    throw error;
  }
  return json(request,env,{ok:true,message:"User archived. History is preserved."});
}

async function restoreManagedUser(request: Request, env: Env, actor: SessionUser, id: string): Promise<Response> {
  const current=await env.DB.prepare(`
    SELECT id,name,mobile,role,is_active,archived_at,last_login_at,staff_permissions_json
    FROM users WHERE id=?
  `).bind(id).first<any>();
  if(!current) return json(request,env,{ok:false,error:"NOT_FOUND",message:"User not found."},{status:404});
  if(!current.archived_at) return json(request,env,{ok:true,message:"User is not archived."});
  const old=safeUserSnapshot(current);
  const restoredAt=new Date().toISOString();
  const next={...old,isActive:true,archivedAt:null};
  await env.DB.batch([
    env.DB.prepare(`UPDATE users SET is_active=1,archived_at=NULL,updated_at=? WHERE id=?`).bind(restoredAt,id),
    env.DB.prepare(`INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json) VALUES(?,?,?,?,?,?,?)`)
      .bind(crypto.randomUUID(),actor.id,"RESTORE_USER","USERS",id,JSON.stringify(old),JSON.stringify(next))
  ]);
  return json(request,env,{ok:true,message:"User restored and activated."});
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      const origin = allowedOrigin(request, env);
      if (!origin) return new Response(null, { status: 403, headers: { "cache-control": "no-store" } });
      return new Response(null, { status: 204, headers: {
        "access-control-allow-origin": origin,
        "access-control-allow-credentials": "true",
        "access-control-allow-methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "access-control-allow-headers": "content-type",
        "access-control-max-age": "86400",
        vary: "Origin"
      }});
    }

    const requestGuard = mutationRequestGuard(request, env);
    if (requestGuard) return requestGuard;

    try {
    if (url.pathname === "/api/health" && request.method === "GET") {
      const db = await env.DB.prepare("SELECT 1 AS ok").first<{ ok: number }>();
      return json(request, env, { ok: db?.ok === 1, app: env.APP_NAME, environment: env.APP_ENV, phase: "13" });
    }

    if (url.pathname === "/api/public/bootstrap" && request.method === "GET") {
      const settings=await getSiteSettings(env);
      return json(request, env, {
        shopName: settings.shopName, websiteTitle: settings.websiteTitle, publicCatalog: settings.publicCatalogEnabled, pricingEnabled: false,
        maintenanceModuleEnabled: false, currentCategory: "Choli", availabilityMode:settings.availabilityMode,
        whatsappNumber: settings.showWhatsApp&&settings.whatsappNumber?settings.whatsappNumber:null,
        callNumber: settings.showCall&&settings.contactNumber?settings.contactNumber:null
      }, { headers: { "cache-control": "public, max-age=60, s-maxage=120" } });
    }
    if (url.pathname === "/api/public/catalog" && request.method === "GET") return publicCatalog(request, env);
    if (url.pathname === "/api/public/availability" && request.method === "POST") return publicAvailability(request, env);

    if (url.pathname === "/api/auth/bootstrap-owner" && request.method === "POST") return bootstrapOwner(request, env);
    if (url.pathname === "/api/auth/login" && request.method === "POST") return login(request, env);
    if (url.pathname === "/api/auth/logout" && request.method === "POST") return logout(request, env);
    if (url.pathname === "/api/auth/me" && request.method === "GET") {
      const userOrResponse = await requireUser(request, env);
      return userOrResponse instanceof Response ? userOrResponse : json(request, env, { ok: true, user: publicUser(userOrResponse) });
    }


    if (url.pathname === "/api/admin/users" && request.method === "GET") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return listManagedUsers(request, env, user);
    }
    if (url.pathname === "/api/admin/users" && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return createManagedUser(request, env, user);
    }
    const userPasswordMatch=url.pathname.match(/^\/api\/admin\/users\/([^/]+)\/password$/);
    if (userPasswordMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return resetManagedUserPassword(request, env, user, decodeURIComponent(userPasswordMatch[1]));
    }
    const userArchiveMatch=url.pathname.match(/^\/api\/admin\/users\/([^/]+)\/(archive|restore)$/);
    if (userArchiveMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return userArchiveMatch[2] === "archive"
        ? archiveManagedUser(request, env, user, decodeURIComponent(userArchiveMatch[1]))
        : restoreManagedUser(request, env, user, decodeURIComponent(userArchiveMatch[1]));
    }
    const managedUserMatch=url.pathname.match(/^\/api\/admin\/users\/([^/]+)$/);
    if (managedUserMatch && request.method === "PUT") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return updateManagedUser(request, env, user, decodeURIComponent(managedUserMatch[1]));
    }

    if (url.pathname === "/api/admin/dashboard" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return dashboardOverview(request, env);
    }

    if (url.pathname === "/api/admin/settings/bootstrap" && request.method === "GET") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return adminSettingsBootstrap(request, env);
    }
    if (url.pathname === "/api/admin/settings" && request.method === "PUT") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return updateSiteSettings(request, env, user);
    }
    if (url.pathname === "/api/admin/whatsapp-templates" && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return createWhatsAppTemplate(request, env, user);
    }
    const whatsappTemplateMatch=url.pathname.match(/^\/api\/admin\/whatsapp-templates\/([^/]+)$/);
    if (whatsappTemplateMatch && request.method === "PUT") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return updateWhatsAppTemplate(request, env, user, decodeURIComponent(whatsappTemplateMatch[1]));
    }
    if (whatsappTemplateMatch && request.method === "DELETE") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return deleteWhatsAppTemplate(request, env, user, decodeURIComponent(whatsappTemplateMatch[1]));
    }
    if (url.pathname === "/api/admin/audit-logs" && request.method === "GET") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return listAuditLogs(request, env);
    }

    if (url.pathname === "/api/admin/reports/bootstrap" && request.method === "GET") {
      const userOrResponse = await requireUser(request, env); if (userOrResponse instanceof Response) return userOrResponse;
      return reportsBootstrap(request, env);
    }
    if (url.pathname === "/api/admin/reports" && request.method === "GET") {
      const userOrResponse = await requireUser(request, env); if (userOrResponse instanceof Response) return userOrResponse;
      return adminReport(request, env);
    }

    if (url.pathname === "/api/admin/categories" && request.method === "GET") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return listCategories(request, env);
    }
    if (url.pathname === "/api/admin/categories" && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return createCategory(request, env, user);
    }

    const categoryMatch = url.pathname.match(/^\/api\/admin\/categories\/([^/]+)$/);
    if (categoryMatch && request.method === "PUT") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return updateCategory(request, env, user, decodeURIComponent(categoryMatch[1]));
    }
    if (categoryMatch && request.method === "DELETE") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return deleteCategory(request, env, user, decodeURIComponent(categoryMatch[1]));
    }

    const createFieldMatch = url.pathname.match(/^\/api\/admin\/categories\/([^/]+)\/fields$/);
    if (createFieldMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return createField(request, env, user, decodeURIComponent(createFieldMatch[1]));
    }
    const reorderMatch = url.pathname.match(/^\/api\/admin\/categories\/([^/]+)\/fields\/reorder$/);
    if (reorderMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return reorderFields(request, env, user, decodeURIComponent(reorderMatch[1]));
    }
    const fieldDeleteActionMatch = url.pathname.match(/^\/api\/admin\/category-fields\/([^/]+)\/delete-action$/);
    if (fieldDeleteActionMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return fieldDeleteAction(request, env, user, decodeURIComponent(fieldDeleteActionMatch[1]));
    }

    const fieldMatch = url.pathname.match(/^\/api\/admin\/category-fields\/([^/]+)$/);
    if (fieldMatch && request.method === "PUT") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return updateField(request, env, user, decodeURIComponent(fieldMatch[1]));
    }
    if (fieldMatch && request.method === "DELETE") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return deleteField(request, env, user, decodeURIComponent(fieldMatch[1]));
    }



    if (url.pathname === "/api/admin/customers" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return listCustomers(request, env);
    }
    if (url.pathname === "/api/admin/customers" && request.method === "POST") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return createCustomer(request, env, user);
    }
    const customerHistoryMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/history$/);
    if (customerHistoryMatch && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return customerHistory(request, env, decodeURIComponent(customerHistoryMatch[1]));
    }
    const customerMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)$/);
    if (customerMatch && request.method === "PUT") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return updateCustomer(request, env, user, decodeURIComponent(customerMatch[1]));
    }
    if (customerMatch && request.method === "DELETE") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return deleteCustomer(request, env, user, decodeURIComponent(customerMatch[1]));
    }
    const customerArchiveMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/(archive|restore)$/);
    if (customerArchiveMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return customerArchiveMatch[2] === "archive"
        ? archiveCustomer(request, env, user, decodeURIComponent(customerArchiveMatch[1]))
        : restoreCustomer(request, env, user, decodeURIComponent(customerArchiveMatch[1]));
    }

    if (url.pathname === "/api/admin/items/bootstrap" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return adminItemBootstrap(request, env);
    }
    if (url.pathname === "/api/admin/items" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return listItems(request, env);
    }
    if (url.pathname === "/api/admin/items" && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return createItem(request, env, user);
    }
    const itemMatch = url.pathname.match(/^\/api\/admin\/items\/([^/]+)$/);
    if (itemMatch && request.method === "PUT") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return updateItem(request, env, user, decodeURIComponent(itemMatch[1]));
    }
    if (itemMatch && request.method === "DELETE") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return deleteItem(request, env, user, decodeURIComponent(itemMatch[1]));
    }
    const itemArchiveMatch = url.pathname.match(/^\/api\/admin\/items\/([^/]+)\/(archive|restore)$/);
    if (itemArchiveMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return itemArchiveMatch[2] === "archive"
        ? archiveItem(request, env, user, decodeURIComponent(itemArchiveMatch[1]))
        : restoreItem(request, env, user, decodeURIComponent(itemArchiveMatch[1]));
    }



    if (url.pathname === "/api/admin/returns/bootstrap" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return returnBootstrap(request, env);
    }
    if (url.pathname === "/api/admin/returns" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return listReturns(request, env);
    }
    const returnCorrectionMatch = url.pathname.match(/^\/api\/admin\/returns\/events\/([^/]+)\/correct$/);
    if (returnCorrectionMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return correctReturn(request, env, user, decodeURIComponent(returnCorrectionMatch[1]));
    }
    const returnMatch = url.pathname.match(/^\/api\/admin\/returns\/([^/]+)$/);
    if (returnMatch && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return returnDetail(request, env, decodeURIComponent(returnMatch[1]));
    }
    if (returnMatch && request.method === "POST") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return createReturn(request, env, user, decodeURIComponent(returnMatch[1]));
    }

    if (url.pathname === "/api/admin/pickups/bootstrap" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return pickupBootstrap(request, env);
    }
    if (url.pathname === "/api/admin/pickups" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return listPickups(request, env);
    }
    const pickupCorrectionMatch = url.pathname.match(/^\/api\/admin\/pickups\/events\/([^/]+)\/correct$/);
    if (pickupCorrectionMatch && request.method === "POST") {
      const user = await requireRole(request, env, ADMIN_ROLES); if (user instanceof Response) return user;
      return correctPickup(request, env, user, decodeURIComponent(pickupCorrectionMatch[1]));
    }
    const pickupMatch = url.pathname.match(/^\/api\/admin\/pickups\/([^/]+)$/);
    if (pickupMatch && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return pickupDetail(request, env, decodeURIComponent(pickupMatch[1]));
    }
    if (pickupMatch && request.method === "POST") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return createPickup(request, env, user, decodeURIComponent(pickupMatch[1]));
    }

    if (url.pathname === "/api/admin/bookings/bootstrap" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return bookingBootstrap(request, env);
    }
    if (url.pathname === "/api/admin/bookings/availability" && request.method === "POST") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return bookingAvailability(request, env);
    }
    if (url.pathname === "/api/admin/bookings" && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return listBookings(request, env);
    }
    if (url.pathname === "/api/admin/bookings" && request.method === "POST") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return createBooking(request, env, user);
    }
    const bookingCancelMatch = url.pathname.match(/^\/api\/admin\/bookings\/([^/]+)\/cancel$/);
    if (bookingCancelMatch && request.method === "POST") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return cancelBooking(request, env, user, decodeURIComponent(bookingCancelMatch[1]));
    }
    const bookingMatch = url.pathname.match(/^\/api\/admin\/bookings\/([^/]+)$/);
    if (bookingMatch && request.method === "GET") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return bookingDetail(request, env, decodeURIComponent(bookingMatch[1]));
    }
    if (bookingMatch && request.method === "PUT") {
      const user = await requireUser(request, env); if (user instanceof Response) return user;
      return updateBooking(request, env, user, decodeURIComponent(bookingMatch[1]));
    }

    return json(request, env, { ok: false, error: "NOT_FOUND", message: "API route not found." }, { status: 404 });
    } catch (error) {
      if (String(env.APP_ENV || "").toLowerCase() !== "production") {
        console.error("Unhandled API error", error instanceof Error ? error.message : "unknown");
      }
      return json(request, env, { ok: false, error: "INTERNAL_ERROR", message: "Unexpected server error. Please try again." }, { status: 500 });
    }
  }
};
