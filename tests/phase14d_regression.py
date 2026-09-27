#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def worker_main(config: str) -> str:
    for raw in config.splitlines():
        line = raw.strip()
        if line.startswith("main = "):
            value = line.split("=", 1)[1].strip()
            assert len(value) >= 2 and value[0] == '"' and value[-1] == '"'
            return value[1:-1]
    raise AssertionError("Wrangler main entry is missing")


def main():
    admin = (ROOT / "apps/admin-web/src/phase14d.js").read_text(encoding="utf-8")
    admin_dates = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    public = (ROOT / "apps/public-web/src/phase14c.js").read_text(encoding="utf-8")
    worker = (ROOT / "worker/src/phase14d.js").read_text(encoding="utf-8")
    strict_worker = (ROOT / "worker/src/phase14o.js").read_text(encoding="utf-8")
    admin_html = (ROOT / "apps/admin-web/index.html").read_text(encoding="utf-8")
    public_html = (ROOT / "apps/public-web/index.html").read_text(encoding="utf-8")
    local = (ROOT / "worker/wrangler.toml").read_text(encoding="utf-8")
    staging = (ROOT / "worker/wrangler.staging.toml.template").read_text(encoding="utf-8")
    production = (ROOT / "worker/wrangler.production.toml.template").read_text(encoding="utf-8")
    assert "phase14e.js" in admin_html and '/src/phase14c.js' in public_html
    assert '/src/phase14h.js' not in public_html
    assert 'rel="modulepreload"' not in public_html
    assert 'void import("./main.tsx")' in admin
    assert 'void import("./main.tsx")' in public
    assert "0.14.5-public-boot-r4" in public_html

    compact_admin_dates = admin_dates.replace(" ", "")
    compact_public = public.replace(" ", "")
    assert "nextDateValue(pickup.value)" in compact_admin_dates
    assert "returnDate.min=minimum" in compact_admin_dates
    assert 'document.addEventListener("submit"' in admin_dates
    assert "event.stopImmediatePropagation()" in admin_dates
    assert "nextDateValue(pickup.value)" in compact_public
    assert "returnDate.min=minimum" in compact_public
    assert 'document.addEventListener("click"' in public
    assert "event.stopImmediatePropagation()" in public

    assert "loadVisualItems" in worker and "item_lines" in worker
    assert "INVALID_RENTAL_DATE_RANGE" in strict_worker
    assert "returnDate <= pickupDate" in strict_worker
    assert "await response.clone().json()" in admin
    assert "fallbackLines" in admin and "No image" in admin
    assert "bookingsByNo" in admin and "itemsByName" in admin
    assert "setInterval(scheduleScan,1200)" in admin

    configured_main = worker_main(local)
    staging_main = worker_main(staging)
    production_main = worker_main(production)
    assert configured_main == staging_main
    assert production_main == "src/phase14at-booking-lifecycle.js"
    assert configured_main.startswith("src/") and configured_main.endswith(".js")
    assert (ROOT / "worker" / configured_main).is_file()
    assert (ROOT / "worker" / production_main).is_file()

    configured_worker = (ROOT / "worker" / configured_main).read_text(encoding="utf-8")
    screen4_worker = (ROOT / "worker/src/phase14aw-screen4-bookings.js").read_text(encoding="utf-8")
    assert 'import core from "' in configured_worker
    assert "core.fetch(request,env,ctx)" in configured_worker.replace(" ", ""), "Configured Worker must continue delegating unmatched routes to the prior hardened core."
    assert 'import core from "./phase14av-customers-screen3.js";' in screen4_worker

    worker_first = 'run_worker_first = ["/api/*", "/", "/index.html", "/admin", "/admin/", "/admin/index.html"]'
    assert worker_first in staging
    assert worker_first in production
    assert "[assets]" in staging and "[assets]" in production
    print("Cumulative production hardening regression: PASS")


if __name__ == "__main__":
    main()
