import { execFileSync } from "node:child_process";
import { cpSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { resolve } from "node:path";

const cwd = process.cwd();
const runNpm = (args) => execFileSync(process.platform === "win32" ? "npm.cmd" : "npm", args, { cwd, stdio: "inherit" });

runNpm(["run", "build:shared"]);
runNpm(["run", "build:public"]);
runNpm(["run", "build:admin"]);
runNpm(["exec", "--", "tsc", "-p", "worker/tsconfig.json", "--noEmit"]);

const out = resolve(cwd, "deploy/staging-assets");
rmSync(out, { recursive: true, force: true });
mkdirSync(out, { recursive: true });

const publicDist = resolve(cwd, "apps/public-web/dist");
const adminDist = resolve(cwd, "apps/admin-web/dist");
if (!existsSync(publicDist) || !existsSync(adminDist)) throw new Error("Frontend build output is missing.");

cpSync(publicDist, out, { recursive: true });
mkdirSync(resolve(out, "admin"), { recursive: true });
cpSync(adminDist, resolve(out, "admin"), { recursive: true });

console.log("Staging assets prepared:");
console.log("  Public: /");
console.log("  Admin : /admin/");
console.log("  API   : /api/*");
