import { existsSync, readFileSync } from "node:fs";

const mustExist = [
  "worker/wrangler.staging.toml.template",
  "worker/wrangler.production.toml.template",
  "worker/wrangler.toml",
  "worker/src/phase14o.js",
  "scripts/build-staging.mjs",
  "scripts/render-staging-config.mjs",
  "scripts/staging-preflight.mjs",
  "scripts/smoke-staging.mjs",
  "scripts/discover-staging-d1.mjs",
  "scripts/activate-staging.mjs",
  "docs/STAGING_DEPLOYMENT.md",
  "docs/STAGING_SMOKE_TEST.md",
  "database/migrations/0008_production_hardening.sql"
];

const missing = mustExist.filter((p) => !existsSync(p));
if (missing.length) throw new Error(`Release is missing: ${missing.join(", ")}`);

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

const gitignore = readFileSync(".gitignore", "utf8");
for (const secretish of [
  "worker/wrangler.staging.generated.toml",
  "deploy/staging-assets/",
  ".env"
]) {
  if (!gitignore.includes(secretish)) {
    throw new Error(`.gitignore is missing protection for ${secretish}`);
  }
}

const workflow = readFileSync(".github/workflows/deploy-staging.yml", "utf8");
if (!workflow.includes("workflow_dispatch:")) throw new Error("Staging workflow must preserve manual dispatch.");
if (!workflow.includes("push:") || !workflow.includes('branches:\n      - main') || !workflow.includes('paths:\n      - ".github/staging-run-trigger"')) {
  throw new Error("Staging push trigger must stay restricted to the dedicated trigger file on main.");
}
if (!workflow.includes("github.event_name == 'push' || inputs.run_smoke_test")) {
  throw new Error("Dedicated staging trigger must always run smoke verification.");
}
if (!workflow.includes("github.event_name == 'workflow_dispatch' && inputs.configure_owner_setup_secret")) {
  throw new Error("Push-triggered staging runs must never rotate the owner setup secret.");
}

const local = readFileSync("worker/wrangler.toml", "utf8");
const stageTemplate = readFileSync("worker/wrangler.staging.toml.template", "utf8");
const productionTemplate = readFileSync("worker/wrangler.production.toml.template", "utf8");
const configuredMain = workerMain(local);
if (workerMain(stageTemplate) !== configuredMain || workerMain(productionTemplate) !== configuredMain) {
  throw new Error("Local, staging and production Wrangler configs must use the same Worker entry.");
}
if (!existsSync(`worker/${configuredMain}`)) {
  throw new Error(`Configured Worker entry is missing: worker/${configuredMain}`);
}
if (!workerChainContains(configuredMain, "src/phase14o.js")) {
  throw new Error("Active Worker chain must preserve the Phase 14O hardened core.");
}
if (!stageTemplate.includes(REQUIRED_WORKER_FIRST) || !productionTemplate.includes(REQUIRED_WORKER_FIRST)) {
  throw new Error("Staging and production must route API, public shell and Admin shell through the Worker first.");
}
if (!stageTemplate.includes('COOKIE_SECURE = "true"')) {
  throw new Error("Staging cookies must remain secure.");
}

console.log("Staging release verification PASS");
