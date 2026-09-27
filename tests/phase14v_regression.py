#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    public_bridge = (ROOT / "apps/public-web/src/phase14t-public.js").read_text(encoding="utf-8")
    public_guard = (ROOT / "apps/public-web/src/phase14v-public.js").read_text(encoding="utf-8")
    public_css = (ROOT / "apps/public-web/src/phase14v-public.css").read_text(encoding="utf-8")
    admin_stale = (ROOT / "apps/admin-web/src/phase14u-stale.js").read_text(encoding="utf-8")

    assert manifest["version"] == "0.14.5" and manifest["phase"] in {"14V", "14W", "14X"}
    assert manifest["scope"]["pricing"] is False
    assert manifest["scope"]["publicNoPricing"] is True
    assert manifest["scope"]["operationalCardEveryItemVisible"] is True
    for flag in [
        "requestIntegrityHardening",
        "publicDataRequestSerialization",
        "publicRequestTimeoutRecovery",
        "publicCrossRequestInteractionGuard",
        "adminStaleSnapshotIsolation",
        "zeroExtraPhase14vApiCalls",
    ]:
        assert manifest["scope"].get(flag) is True

    assert 'import "./phase14v-public.js";' in public_bridge
    for marker in [
        'import "./phase14v-public.css";',
        "PV_TIMEOUT_MS = 25000",
        "pvNativeFetch = window.fetch.bind(window)",
        'path === "/api/public/catalog"',
        'path === "/api/public/availability"',
        "new AbortController()",
        "controller.signal",
        "phase14v-public-data-busy",
        "Request timed out. Please try again.",
        'event.target.matches(".search-form")',
    ]:
        assert marker in public_guard
    assert public_guard.count("pvNativeFetch(input") == 2
    assert "fetch(`${" not in public_guard
    assert "phase14v-public-data-busy" in public_css

    # Phase 14AM intentionally removed stale-list cloning from Admin Search/Apply refresh.
    # Phase 14V still guards zero extra requests and safe refresh presentation, but it must
    # no longer require the retired snapshot implementation.
    for marker in [
        "function usClear()",
        "usClear();",
        'loading.setAttribute("role","status")',
        'loading.setAttribute("aria-live","polite")',
        "MutationObserver(usSchedule)",
    ]:
        assert marker in admin_stale
    for forbidden in [
        "cloneNode(",
        "insertAdjacentElement(",
        "usSnapshot",
        "usRemember",
        "usShow",
        'badge.textContent="Refreshing… showing previous results"',
    ]:
        assert forbidden not in admin_stale
    assert "fetch(" not in admin_stale

    assert "tests/phase14v_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14V request integrity + native Admin refresh regression: PASS")


if __name__ == "__main__":
    main()
