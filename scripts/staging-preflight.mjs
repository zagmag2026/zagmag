import { existsSync, readFileSync } from "node:fs";

const required = [
  "deploy/staging-assets/index.html",
  "deploy/staging-assets/admin/index.html",
  "worker/wrangler.staging.generated.toml",
  "database/migrations/0008_production_hardening.sql",
  "worker/src/phase14o.js"
];
const missing = required.filter((p) => !existsSync(p));
if (missing.length) throw new Error(`Missing staging files: ${missing.join(", ")}`);

const REQUIRED_WORKER_FIRST = 'run_worker_first = ["/api/*", "/", "/index.html", "/admin", "/admin/", "/admin/index.html"]';

function workerMain(text) {
  const match = text.match(/^main\s*=\s*"([^"]+)"\s*$/m);
  if (!match) throw new Error("Wrangler main entry is missing.");
  return match[1];
}

function workerChainContains(entry, target) {
  let current = entry;
  const seen = new Set();
  while (!seen.has(current)) {
    seen.add(current);
    if (current === target) return true;
    const path = `worker/${current}`;
    if (!existsSync(path)) return false;
    const source = readFileSync(path, "utf8");
    const match = source.match(/import\s+core\s+from\s+"\.\/([^"]+)"/);
    if (!match) return false;
    current = `src/${match[1]}`;
  }
  return false;
}

const config = readFileSync("worker/wrangler.staging.generated.toml", "utf8");
const template = readFileSync("worker/wrangler.staging.toml.template", "utf8");
for (const token of ["__STAGING_D1_DATABASE_ID__", "REPLACE_WITH_D1_DATABASE_ID"]) {
  if (config.includes(token)) throw new Error(`Unresolved placeholder: ${token}`);
}
const configuredMain = workerMain(config);
if (configuredMain !== workerMain(template)) throw new Error("Generated staging Worker entry does not match its template.");
if (!existsSync(`worker/${configuredMain}`)) throw new Error(`Configured Worker entry is missing: worker/${configuredMain}`);
if (!workerChainContains(configuredMain, "src/phase14o.js")) throw new Error("Active Worker chain must preserve the Phase 14O hardened core.");
if (!template.includes(REQUIRED_WORKER_FIRST) || !config.includes(REQUIRED_WORKER_FIRST)) {
  throw new Error("Static asset/API/Admin-shell routing guard is missing.");
}
if (!config.includes('APP_ENV = "staging"')) throw new Error("APP_ENV must be staging.");
if (!config.includes('COOKIE_SECURE = "true"')) throw new Error("Secure cookies must remain enabled.");

console.log("Staging preflight PASS");
