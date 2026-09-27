import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const id = (process.env.STAGING_D1_DATABASE_ID || "").trim();
if (!/^[0-9a-fA-F-]{20,}$/.test(id)) {
  throw new Error("STAGING_D1_DATABASE_ID is missing or invalid.");
}
const templatePath = resolve("worker/wrangler.staging.toml.template");
const outputPath = resolve("worker/wrangler.staging.generated.toml");
const template = readFileSync(templatePath, "utf8");
const rendered = template.replaceAll("__STAGING_D1_DATABASE_ID__", id);
if (rendered.includes("__STAGING_D1_DATABASE_ID__")) throw new Error("D1 placeholder was not fully rendered.");
writeFileSync(outputPath, rendered, "utf8");
console.log("Generated worker/wrangler.staging.generated.toml");
