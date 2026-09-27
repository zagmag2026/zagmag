import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";

const base = (process.env.STAGING_BASE_URL || "").replace(/\/$/, "");
if (!base.startsWith("https://")) throw new Error("STAGING_BASE_URL must be an https:// URL.");

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
const localPublicShell = readFileSync(new URL("../deploy/staging-assets/index.html", import.meta.url), "utf8");
const expectedAssetPaths = [...localPublicShell.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map(match => match[1]);
if (!expectedAssetPaths.length) throw new Error("Local staging public shell has no built assets.");

async function get(path) {
  const response = await fetch(`${base}${path}`, {
    redirect: "follow",
    cache: "no-store",
    headers: { "cache-control": "no-cache, no-store, max-age=0", pragma: "no-cache" }
  });
  const body = await response.text();
  return { response, body };
}

async function postJson(path, payload) {
  const response = await fetch(`${base}${path}`, {
    method: "POST",
    redirect: "follow",
    cache: "no-store",
    headers: {
      "content-type": "application/json",
      "cache-control": "no-cache, no-store, max-age=0",
      pragma: "no-cache"
    },
    body: JSON.stringify(payload)
  });
  const body = await response.text();
  return { response, body };
}

async function check(path, test, label) {
  const { response, body } = await get(path);
  if (!response.ok || !test(response, body)) {
    throw new Error(`${label} failed: HTTP ${response.status} ${body.slice(0, 250)}`);
  }
  console.log(`PASS ${label}`);
  return { response, body };
}

async function checkJsonPost(path, payload, test, label) {
  const { response, body } = await postJson(path, payload);
  if (!test(response, body)) {
    throw new Error(`${label} failed: HTTP ${response.status} ${body.slice(0, 250)}`);
  }
  console.log(`PASS ${label}`);
  return { response, body };
}

async function checkWithRetry(pathFactory, test, label, attempts = 10, delayMs = 2000) {
  let last = null;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    const path = typeof pathFactory === "function" ? pathFactory(attempt) : pathFactory;
    const current = await get(path);
    last = current;
    if (current.response.ok && test(current.response, current.body)) {
      console.log(`PASS ${label}${attempt > 1 ? ` after ${attempt} attempts` : ""}`);
      return current;
    }
    if (attempt < attempts) {
      console.log(`RETRY ${label} (${attempt}/${attempts})`);
      await sleep(delayMs);
    }
  }
  throw new Error(`${label} failed after ${attempts} attempts: HTTP ${last?.response?.status ?? "?"} ${(last?.body || "").slice(0, 250)}`);
}

function resolveChrome() {
  const candidates = [process.env.CHROME_BIN, "google-chrome", "google-chrome-stable", "chromium", "chromium-browser"].filter(Boolean);
  for (const candidate of candidates) {
    const probe = spawnSync(candidate, ["--version"], { encoding: "utf8" });
    if (probe.status === 0) return candidate;
  }
  throw new Error("Headless Chrome/Chromium is required for browser smoke test.");
}

function expectedMime(path) {
  if (path.endsWith(".js")) return "javascript";
  if (path.endsWith(".css")) return "text/css";
  return "";
}

async function verifyAssetTree(seedPaths) {
  const queue = [...new Set(seedPaths)];
  const seen = new Set();
  while (queue.length) {
    const assetPath = queue.shift();
    if (!assetPath || seen.has(assetPath)) continue;
    seen.add(assetPath);
    const asset = await checkWithRetry(
      attempt => `${assetPath}${assetPath.includes("?") ? "&" : "?"}asset_smoke=${Date.now()}_${attempt}`,
      (response, body) => {
        const type = response.headers.get("content-type") || "";
        const expected = expectedMime(assetPath.split("?")[0]);
        return body.length > 0 && (!expected || type.includes(expected));
      },
      `Public asset ${assetPath}`,
      6,
      1500
    );
    if (!assetPath.split("?")[0].endsWith(".js")) continue;
    for (const match of asset.body.matchAll(/["']((?:\.{1,2}\/|\/assets\/)[^"'\\]+?\.(?:js|css))["']/g)) {
      const resolved = new URL(match[1], `${base}${assetPath.split("?")[0]}`).pathname;
      if (resolved.startsWith("/assets/") && !seen.has(resolved)) queue.push(resolved);
    }
  }
}

const publicShell = await checkWithRetry(
  attempt => `/?shell_smoke=${Date.now()}_${attempt}`,
  (response, body) => {
    const cacheControl = response.headers.get("cache-control") || "";
    return response.headers.get("x-zhagmag-shell") === "worker-r1" &&
      cacheControl.includes("no-store") &&
      body.includes('id="root"') &&
      body.includes('type="module"') &&
      body.includes('0.14.5-public-boot-r4') &&
      expectedAssetPaths.every(path => body.includes(path));
  },
  "Public shell + exact deployed assets",
  12,
  2000
);

const assetPaths = [...publicShell.body.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map(match => match[1]);
if (!assetPaths.length) throw new Error("Public shell asset discovery failed: no built assets found.");
await verifyAssetTree(assetPaths);

await checkWithRetry(
  "/admin/",
  (response, body) => {
    const cacheControl = response.headers.get("cache-control") || "";
    return response.headers.get("x-zhagmag-admin-shell") === "worker-ar-admin-r1" &&
      cacheControl.includes("no-store") &&
      body.includes('id="root"') &&
      body.includes('type="module"');
  },
  "Admin plain shell + no-store",
  6,
  1500
);
await check("/api/health", (_, body) => {
  try { return JSON.parse(body).ok === true; } catch { return false; }
}, "API health");
await checkJsonPost("/api/auth/login", {}, (response, body) => {
  if (response.status !== 400 || !response.headers.get("content-type")?.includes("application/json")) return false;
  try {
    const data = JSON.parse(body);
    return data.ok === false && data.error === "VALIDATION" && typeof data.message === "string";
  } catch { return false; }
}, "Admin auth route validation");
await check("/api/public/catalog", (response, body) => {
  if (!response.headers.get("content-type")?.includes("application/json")) return false;
  try {
    const data = JSON.parse(body);
    return data.ok === true && data.shop && typeof data.shop.name === "string" && Array.isArray(data.items) && Array.isArray(data.categories) && data.pagination && Number.isFinite(Number(data.pagination.totalPages));
  } catch { return false; }
}, "Public catalog payload");

const chrome = resolveChrome();
const BROWSER_ATTEMPTS = 3;
const BROWSER_TIMEOUT_MS = 15000;
let browserLast = null;
let browserRendered = "";
let browserPassed = false;
for (let attempt = 1; attempt <= BROWSER_ATTEMPTS; attempt += 1) {
  const browser = spawnSync(chrome, [
    "--headless=new",
    "--no-sandbox",
    "--disable-gpu",
    "--disable-dev-shm-usage",
    "--hide-scrollbars",
    "--virtual-time-budget=8000",
    "--dump-dom",
    `${base}/?browser_smoke=${Date.now()}_${attempt}`
  ], { encoding: "utf8", maxBuffer: 20 * 1024 * 1024, timeout: BROWSER_TIMEOUT_MS, killSignal: "SIGKILL" });
  browserLast = browser;
  browserRendered = browser.stdout || "";
  if (!browser.error && browser.status === 0 && browserRendered.includes('class="app"')) {
    console.log(`PASS Public browser render${attempt > 1 ? ` after ${attempt} attempts` : ""}`);
    browserPassed = true;
    break;
  }
  if (attempt < BROWSER_ATTEMPTS) {
    console.log(`RETRY Public browser render (${attempt}/${BROWSER_ATTEMPTS})`);
    await sleep(2000);
  }
}
if (!browserPassed) {
  if (browserLast?.error || browserLast?.status !== 0) {
    throw new Error(`Public browser launch failed after ${BROWSER_ATTEMPTS} attempts: ${browserLast?.error?.message || browserLast?.stderr?.slice(-1200) || `exit ${browserLast?.status}`}`);
  }
  const diagnostic = browserRendered.match(/<pre id="boot-diagnostic"[^>]*>([\s\S]*?)<\/pre>/i)?.[1] || "NO_DIAGNOSTIC_NODE";
  throw new Error(`Public browser render failed after ${BROWSER_ATTEMPTS} attempts. Diagnostic: ${diagnostic.slice(0, 1600)}\nDOM: ${browserRendered.slice(0, 1800)}\n${(browserLast?.stderr || "").slice(-1200)}`);
}

let adminBrowserLast = null;
let adminBrowserRendered = "";
let adminBrowserPassed = false;
const ADMIN_MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36";
for (let attempt = 1; attempt <= BROWSER_ATTEMPTS; attempt += 1) {
  const browser = spawnSync(chrome, [
    "--headless=new",
    "--no-sandbox",
    "--disable-gpu",
    "--disable-dev-shm-usage",
    "--hide-scrollbars",
    "--window-size=390,844",
    `--user-agent=${ADMIN_MOBILE_USER_AGENT}`,
    "--virtual-time-budget=10000",
    "--dump-dom",
    `${base}/admin/`
  ], { encoding: "utf8", maxBuffer: 20 * 1024 * 1024, timeout: BROWSER_TIMEOUT_MS, killSignal: "SIGKILL" });
  adminBrowserLast = browser;
  adminBrowserRendered = browser.stdout || "";
  const guAuth = adminBrowserRendered.includes('data-phase14ar-auth-ready="GU"') && adminBrowserRendered.includes("લૉગ ઇન");
  const enAuth = adminBrowserRendered.includes('data-phase14ar-auth-ready="EN"') && adminBrowserRendered.includes("Sign in");
  const hasAuthForm = adminBrowserRendered.includes('class="auth-page"') &&
    adminBrowserRendered.includes('class="auth-card"') &&
    adminBrowserRendered.includes('type="password"') &&
    adminBrowserRendered.includes('class="primary full"') &&
    adminBrowserRendered.includes('phase14ar-language-button') &&
    !adminBrowserRendered.includes('class="splash"') &&
    (guAuth || enAuth);
  if (!browser.error && browser.status === 0 && hasAuthForm) {
    console.log(`PASS Admin mobile plain-path browser render + bilingual auth${attempt > 1 ? ` after ${attempt} attempts` : ""}`);
    adminBrowserPassed = true;
    break;
  }
  if (attempt < BROWSER_ATTEMPTS) {
    console.log(`RETRY Admin mobile plain-path browser render + bilingual auth (${attempt}/${BROWSER_ATTEMPTS})`);
    await sleep(2000);
  }
}
if (!adminBrowserPassed) {
  if (adminBrowserLast?.error || adminBrowserLast?.status !== 0) {
    throw new Error(`Admin mobile browser launch failed after ${BROWSER_ATTEMPTS} attempts: ${adminBrowserLast?.error?.message || adminBrowserLast?.stderr?.slice(-1200) || `exit ${adminBrowserLast?.status}`}`);
  }
  throw new Error(`Admin mobile plain-path browser render + bilingual auth failed after ${BROWSER_ATTEMPTS} attempts. DOM: ${adminBrowserRendered.slice(0, 2200)}\n${(adminBrowserLast?.stderr || "").slice(-1200)}`);
}

console.log("Staging smoke test PASS");
