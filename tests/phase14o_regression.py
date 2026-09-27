#!/usr/bin/env python3
from pathlib import Path
import json

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
    admin = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    public = (ROOT / "apps/public-web/src/phase14c.js").read_text(encoding="utf-8")
    worker = (ROOT / "worker/src/phase14o.js").read_text(encoding="utf-8")
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))

    assert manifest["version"] == "0.14.5"
    for flag in [
        "strictNextDayRentalDates",
        "adminStrictDateSubmitGate",
        "publicStrictDateCheckGate",
        "workerStrictDateBoundary",
        "finalProductionGate",
    ]:
        assert manifest["scope"].get(flag) is True

    # Admin UI: +1 day minimum plus capture-phase submit gate.
    assert "nextDateValue(pickup.value)" in admin
    assert "returnDate.min=minimum" in admin
    assert 'document.addEventListener("submit"' in admin
    assert "event.preventDefault()" in admin
    assert "event.stopImmediatePropagation()" in admin

    # Public UI: +1 day default/min plus capture-phase availability check gate.
    assert "nextDateValue(pickup.value)" in public
    assert "returnDate.min=minimum" in public
    assert 'document.addEventListener("click"' in public
    assert '.availability-box button' in public
    assert "event.stopImmediatePropagation()" in public

    # Worker/API boundary: Phase 14O remains the hardened core even when later wrappers are active.
    assert 'import core from "./phase14e.js"' in worker
    assert "returnDate <= pickupDate" in worker
    assert "INVALID_RENTAL_DATE_RANGE" in worker
    assert "/api/admin/bookings" in worker
    assert "/api/admin/bookings/availability" in worker
    assert "/api/public/availability" in worker
    assert 'request.method === "PUT"' in worker
    assert "env.ASSETS" in worker and "x-zhagmag-shell" in worker
    assert "no-store, no-cache, must-revalidate, max-age=0" in worker

    local = (ROOT / "worker" / "wrangler.toml").read_text(encoding="utf-8")
    staging = (ROOT / "worker" / "wrangler.staging.toml.template").read_text(encoding="utf-8")
    production = (ROOT / "worker" / "wrangler.production.toml.template").read_text(encoding="utf-8")
    configured_main = worker_main(local)
    staging_main = worker_main(staging)
    production_main = worker_main(production)
    assert configured_main == staging_main
    assert production_main == "src/phase14at-booking-lifecycle.js"
    assert configured_main.startswith("src/") and configured_main.endswith(".js")
    assert (ROOT / "worker" / configured_main).is_file()
    assert (ROOT / "worker" / production_main).is_file()
    configured_worker = (ROOT / "worker" / configured_main).read_text(encoding="utf-8")
    assert 'import core from "' in configured_worker
    assert "core.fetch(request,env,ctx)" in configured_worker.replace(" ", "")

    print("Phase 14O hardened-core + staging/production Worker split regression: PASS")


if __name__ == "__main__":
    main()
