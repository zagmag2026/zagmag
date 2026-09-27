export interface AuthEnv {
  DB: D1Database;
  APP_ENV: string;
  SESSION_TTL_HOURS?: string;
  COOKIE_SECURE?: string;
}

export type UserRole = "OWNER" | "STAFF";

export const STAFF_PERMISSION_KEYS = [
  "DASHBOARD",
  "ITEMS",
  "CUSTOMERS",
  "BOOKINGS",
  "PICKUPS",
  "RETURNS",
  "REPORTS"
] as const;

export type StaffPermission = typeof STAFF_PERMISSION_KEYS[number];

export interface SessionUser {
  id: string;
  name: string;
  email: string | null;
  mobile: string | null;
  role: UserRole;
  staffPermissions: StaffPermission[];
}

export function parseStaffPermissions(value: unknown): StaffPermission[] {
  let raw: unknown = value;
  if (typeof raw === "string") {
    try { raw = JSON.parse(raw); } catch { raw = []; }
  }
  if (!Array.isArray(raw)) return [];
  const allowed = new Set<string>(STAFF_PERMISSION_KEYS);
  return [...new Set(raw.map(item => String(item).trim().toUpperCase()).filter(item => allowed.has(item)))] as StaffPermission[];
}

export function hasStaffPermission(user: SessionUser, permission: StaffPermission): boolean {
  return user.role === "OWNER" || user.staffPermissions.includes(permission);
}

const encoder = new TextEncoder();
const PASSWORD_ITERATIONS = 210_000;
const MIN_SUPPORTED_PBKDF2_ITERATIONS = 100_000;
const MAX_SUPPORTED_PBKDF2_ITERATIONS = 600_000;

function bytesToBase64(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}

function base64ToBytes(value: string): Uint8Array {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

function base64Url(bytes: Uint8Array): string {
  return bytesToBase64(bytes).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function constantTimeEqual(a: Uint8Array, b: Uint8Array): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i += 1) diff |= a[i] ^ b[i];
  return diff === 0;
}


export function secureTextEqual(a: string, b: string): boolean {
  return constantTimeEqual(encoder.encode(a), encoder.encode(b));
}

export async function hashPassword(password: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    encoder.encode(password),
    "PBKDF2",
    false,
    ["deriveBits"]
  );
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations: PASSWORD_ITERATIONS, hash: "SHA-256" },
    keyMaterial,
    256
  );
  return `pbkdf2$${PASSWORD_ITERATIONS}$${bytesToBase64(salt)}$${bytesToBase64(new Uint8Array(bits))}`;
}

export async function verifyPassword(password: string, stored: string | null): Promise<boolean> {
  if (!stored) return false;
  const [scheme, iterationsText, saltText, expectedText] = stored.split("$");
  if (scheme !== "pbkdf2" || !iterationsText || !saltText || !expectedText) return false;
  const iterations = Number(iterationsText);
  if (
    !Number.isInteger(iterations) ||
    iterations < MIN_SUPPORTED_PBKDF2_ITERATIONS ||
    iterations > MAX_SUPPORTED_PBKDF2_ITERATIONS
  ) return false;

  const salt = base64ToBytes(saltText);
  const expected = base64ToBytes(expectedText);
  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    encoder.encode(password),
    "PBKDF2",
    false,
    ["deriveBits"]
  );
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations, hash: "SHA-256" },
    keyMaterial,
    expected.length * 8
  );
  return constantTimeEqual(new Uint8Array(bits), expected);
}

export function passwordNeedsRehash(stored: string | null): boolean {
  if (!stored) return true;
  const [scheme, iterationsText] = stored.split("$");
  const iterations = Number(iterationsText);
  return scheme !== "pbkdf2" || !Number.isInteger(iterations) || iterations < PASSWORD_ITERATIONS;
}

export async function sha256(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(value));
  return base64Url(new Uint8Array(digest));
}

export function validatePassword(password: string): string | null {
  if (password.length < 8) return "Password must contain at least 8 characters.";
  if (password.length > 128) return "Password is too long.";
  return null;
}

function sessionTtlSeconds(env: AuthEnv): number {
  const hours = Number(env.SESSION_TTL_HOURS || "12");
  const safeHours = Number.isFinite(hours) && hours >= 1 && hours <= 168 ? hours : 12;
  return Math.floor(safeHours * 3600);
}

function secureCookies(env: AuthEnv): boolean {
  return env.COOKIE_SECURE !== "false";
}

export function sessionCookieName(env: AuthEnv): string {
  return secureCookies(env) ? "__Host-zhagmag_session" : "zhagmag_session";
}

function getCookie(request: Request, name: string): string | null {
  const header = request.headers.get("cookie") || "";
  for (const part of header.split(";")) {
    const [key, ...rest] = part.trim().split("=");
    if (key === name) return decodeURIComponent(rest.join("="));
  }
  return null;
}

export function makeSessionCookie(env: AuthEnv, token: string): string {
  const parts = [
    `${sessionCookieName(env)}=${encodeURIComponent(token)}`,
    "Path=/",
    "HttpOnly",
    "SameSite=Lax",
    `Max-Age=${sessionTtlSeconds(env)}`
  ];
  if (secureCookies(env)) parts.push("Secure");
  return parts.join("; ");
}

const sessionUserCache = new WeakMap<Request, Promise<SessionUser | null>>();

export function clearSessionCookie(env: AuthEnv): string {
  const parts = [
    `${sessionCookieName(env)}=`,
    "Path=/",
    "HttpOnly",
    "SameSite=Lax",
    "Max-Age=0"
  ];
  if (secureCookies(env)) parts.push("Secure");
  return parts.join("; ");
}

export async function createSession(
  env: AuthEnv,
  userId: string,
  userAgent: string | null
): Promise<string> {
  const token = base64Url(crypto.getRandomValues(new Uint8Array(32)));
  const tokenHash = await sha256(token);
  const nowEpoch = Math.floor(Date.now() / 1000);
  const expiresAt = nowEpoch + sessionTtlSeconds(env);

  const sessionId = crypto.randomUUID();
  await env.DB.batch([
    env.DB.prepare(
      `INSERT INTO auth_sessions
        (id, user_id, token_hash, expires_at_epoch, user_agent)
       VALUES (?, ?, ?, ?, ?)`
    ).bind(sessionId, userId, tokenHash, expiresAt, userAgent?.slice(0, 300) || null),
    env.DB.prepare(
      `DELETE FROM auth_sessions
        WHERE user_id = ? AND id NOT IN (
          SELECT id FROM auth_sessions WHERE user_id = ? ORDER BY expires_at_epoch DESC, created_at DESC LIMIT 10
        )`
    ).bind(userId, userId)
  ]);

  return token;
}

export async function deleteSessionFromRequest(env: AuthEnv, request: Request): Promise<void> {
  const token = getCookie(request, sessionCookieName(env));
  if (!token) return;
  const tokenHash = await sha256(token);
  await env.DB.prepare("DELETE FROM auth_sessions WHERE token_hash = ?").bind(tokenHash).run();
}

export async function getSessionUser(env: AuthEnv, request: Request): Promise<SessionUser | null> {
  const cached = sessionUserCache.get(request);
  if (cached) return cached;

  const pending = (async () => {
    const token = getCookie(request, sessionCookieName(env));
    if (!token) return null;

    const tokenHash = await sha256(token);
    const nowEpoch = Math.floor(Date.now() / 1000);
    const row = await env.DB.prepare(
      `SELECT u.id, u.name, u.email, u.mobile, u.role, u.staff_permissions_json
         FROM auth_sessions s
         JOIN users u ON u.id = s.user_id
        WHERE s.token_hash = ?
          AND s.expires_at_epoch > ?
          AND u.is_active = 1
          AND u.archived_at IS NULL
        LIMIT 1`
    )
      .bind(tokenHash, nowEpoch)
      .first<Record<string, unknown>>();

    if (!row) return null;

    const role: UserRole = String(row.role).toUpperCase() === "OWNER" ? "OWNER" : "STAFF";
    return {
      id: String(row.id),
      name: String(row.name),
      email: row.email ? String(row.email) : null,
      mobile: row.mobile ? String(row.mobile) : null,
      role,
      staffPermissions: role === "OWNER" ? [] : parseStaffPermissions(row.staff_permissions_json)
    };
  })();

  sessionUserCache.set(request, pending);
  return pending;
}

export async function cleanupExpiredSessions(env: AuthEnv): Promise<void> {
  const nowEpoch = Math.floor(Date.now() / 1000);
  await env.DB.prepare("DELETE FROM auth_sessions WHERE expires_at_epoch <= ?").bind(nowEpoch).run();
}
