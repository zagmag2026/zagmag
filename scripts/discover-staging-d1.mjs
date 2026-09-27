\
import { execFileSync } from "node:child_process";

const name = process.env.STAGING_D1_NAME || "zhagmag-dresses-staging";
const npm = process.platform === "win32" ? "npx.cmd" : "npx";

function runWrangler(args) {
  return execFileSync(npm, ["wrangler", ...args], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "inherit"]
  });
}

const output = runWrangler(["d1", "list", "--json"]);
const list = JSON.parse(output);
const matches = list.filter((db) => db.name === name);

if (matches.length === 0) {
  console.error(`No D1 database named "${name}" was found.`);
  console.error(`Create it first: npx wrangler d1 create ${name} --location apac`);
  process.exit(2);
}
if (matches.length > 1) {
  console.error(`More than one D1 database named "${name}" was returned; refusing to guess.`);
  process.exit(3);
}

const db = matches[0];
const id = db.uuid || db.id;
if (!id) throw new Error("D1 database result did not contain a UUID.");

console.log(id);
