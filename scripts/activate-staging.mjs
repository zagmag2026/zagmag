import { execFileSync, spawnSync } from "node:child_process";

const npm = process.platform === "win32" ? "npm.cmd" : "npm";
const npx = process.platform === "win32" ? "npx.cmd" : "npx";
const cwd = process.cwd();

function run(file, args, opts = {}) {
  execFileSync(file, args, { cwd, stdio: "inherit", ...opts });
}

function capture(file, args) {
  return execFileSync(file, args, { cwd, encoding: "utf8", stdio: ["ignore","pipe","inherit"] }).trim();
}

const dryRun = process.argv.includes("--dry-run");
const skipSecret = process.argv.includes("--skip-owner-secret");

let d1Id = (process.env.STAGING_D1_DATABASE_ID || "").trim();
if (!d1Id) {
  d1Id = capture(npm, ["run", "--silent", "staging:discover"]);
}
if (!/^[0-9a-fA-F-]{20,}$/.test(d1Id)) {
  throw new Error("Resolved staging D1 database UUID is invalid.");
}

console.log(`Using staging D1 UUID: ${d1Id}`);

run(npm, ["install", "--no-audit", "--no-fund"]);
run(npm, ["run", "test:hardening"]);
run(npm, ["run", "build:staging"]);

run(npm, ["run", "render:staging-config"], {
  env: { ...process.env, STAGING_D1_DATABASE_ID: d1Id }
});
run(npm, ["run", "preflight:staging"]);
run(npm, ["run", "staging:verify"]);

if (dryRun) {
  console.log("Dry-run complete. No remote migration/deploy was executed.");
  process.exit(0);
}

if (!process.env.CLOUDFLARE_API_TOKEN) {
  console.log("CLOUDFLARE_API_TOKEN is not set; Wrangler may use an existing local login.");
}

run(npx, [
  "wrangler","d1","migrations","apply","DB","--remote",
  "--config","worker/wrangler.staging.generated.toml"
]);

if (!skipSecret) {
  const token = (process.env.INITIAL_OWNER_SETUP_TOKEN || "").trim();
  if (token.length < 24) {
    throw new Error("INITIAL_OWNER_SETUP_TOKEN must be at least 24 characters, or pass --skip-owner-secret.");
  }
  const result = spawnSync(npx, [
    "wrangler","secret","put","INITIAL_OWNER_SETUP_TOKEN",
    "--config","worker/wrangler.staging.generated.toml"
  ], {
    cwd,
    input: token,
    encoding: "utf8",
    stdio: ["pipe","inherit","inherit"],
    env: process.env
  });
  if (result.status !== 0) throw new Error("Failed to set INITIAL_OWNER_SETUP_TOKEN.");
}

run(npx, [
  "wrangler","deploy",
  "--config","worker/wrangler.staging.generated.toml"
]);

console.log("Staging deployment command completed.");
console.log("Set STAGING_BASE_URL to the deployed https:// URL, then run: npm run smoke:staging");
