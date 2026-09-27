from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def require(condition, message):
    if not condition:
        raise AssertionError(message)

def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    admin = (ROOT / "apps/admin-web/src/phase14ak-partial-return.js").read_text(encoding="utf-8")
    worker = (ROOT / "worker/src/phase14ak-partial-return.js").read_text(encoding="utf-8")
    phase14al = (ROOT / "worker/src/phase14al-sequential-weekday.js").read_text(encoding="utf-8")
    outer_worker = (ROOT / "worker/src/phase14ar-bilingual-branding.js").read_text(encoding="utf-8")
    lifecycle_worker = (ROOT / "worker/src/phase14at-booking-lifecycle.js").read_text(encoding="utf-8")
    dashboard_worker = (ROOT / "worker/src/phase14au-dashboard-screen2.js").read_text(encoding="utf-8")
    customers_worker = (ROOT / "worker/src/phase14av-customers-screen3.js").read_text(encoding="utf-8")
    screen4_worker = (ROOT / "worker/src/phase14aw-screen4-bookings.js").read_text(encoding="utf-8")
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    staging = (ROOT / "worker/wrangler.staging.toml.template").read_text(encoding="utf-8")
    production = (ROOT / "worker/wrangler.production.toml.template").read_text(encoding="utf-8")
    local = (ROOT / "worker/wrangler.toml").read_text(encoding="utf-8")

    require('import "./phase14ak-partial-return.js";' in entry, "Admin Phase14AK runtime must load from the Admin entry")
    require('Partially Returned' in admin, "Return queue must expose a Partially Returned tab")
    require('url.pathname === "/api/admin/returns"' in admin, "Admin runtime must only rewrite Return list requests")
    require('url.searchParams.get("view") === "pending"' in admin, "Admin partial tab must reuse the existing Pending Return load action")
    require('url.searchParams.set("view", "partial")' in admin, "Admin partial tab must request the dedicated partial view")
    require('return priorFetch(url.toString(), init)' in admin, "Partial tab must reuse the existing fetch rather than add another API call")

    require('import core from "./phase14o.js";' in worker, "Worker Phase14AK must preserve the hardened Phase14O chain")
    require('url.pathname === "/api/admin/returns"' in worker and 'url.searchParams.get("view") === "partial"' in worker, "Worker must intercept only Return partial-view GETs")
    require("b.status='PARTIALLY_RETURNED'" in worker, "Partial Return view must be status-exact")
    require('COUNT(*) OVER() AS partial_total' in worker, "Partial Return pagination must use one D1 query")
    require('pendingExpr' in worker and 'givenExpr' in worker, "Partial Return view must retain pending/given quantity guards")
    require('env.DB.batch' not in worker, "Phase14AK partial Return view must not add a second D1 statement")

    require('import core from "./phase14ak-partial-return.js";' in phase14al, "Phase14AL must preserve Phase14AK partial-return behavior")
    require('return core.fetch(request, env, ctx);' in phase14al, "Phase14AL must delegate unrelated requests through Phase14AK")
    require('import core from "./phase14al-sequential-weekday.js";' in outer_worker, "Phase14AR must preserve Phase14AL")
    require('import core from "./phase14ar-bilingual-branding.js";' in lifecycle_worker, "Phase14AT must preserve Phase14AR and the partial-return chain")
    require('import core from "./phase14at-booking-lifecycle.js";' in dashboard_worker, "Phase14AU must preserve Phase14AT lifecycle behavior")
    require('import core from "./phase14au-dashboard-screen2.js";' in customers_worker, "Phase14AV must preserve Phase14AU and the lifecycle chain")
    require('import core from "./phase14av-customers-screen3.js";' in screen4_worker, "Phase14AW must preserve Phase14AV and the full staging chain")
    expected_staging = 'main = "src/phase14aw-screen4-bookings.js"'
    expected_production = 'main = "src/phase14at-booking-lifecycle.js"'
    require(expected_staging in staging and expected_staging in local, "Local/staging Worker configs must point at the cumulative Screen 4 wrapper")
    require(expected_production in production, "Production Worker must remain pinned at the Phase14AT lifecycle wrapper")
    require(package.get("version") == "0.14.5", "Version must remain 0.14.5")
    require("tests/phase14ak_partial_return_regression.py" in package["scripts"]["test:hardening"], "Phase14AK regression must run in test:hardening")

    print("Phase 14AK Partially Returned preserved through Screen 4 staging chain and Phase14AT production: PASS")

if __name__ == "__main__":
    main()
