#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    admin_entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    root_js = (ROOT / "apps/admin-web/src/phase14u.js").read_text(encoding="utf-8")
    view_js = (ROOT / "apps/admin-web/src/phase14u-view.js").read_text(encoding="utf-8")
    stale_js = (ROOT / "apps/admin-web/src/phase14u-stale.js").read_text(encoding="utf-8")
    public_main = (ROOT / "apps/public-web/src/main.tsx").read_text(encoding="utf-8")
    public_bridge = (ROOT / "apps/public-web/src/phase14t-public.js").read_text(encoding="utf-8")
    public_css = (ROOT / "apps/public-web/src/phase14u-public.css").read_text(encoding="utf-8")
    smoke = (ROOT / "scripts/smoke-staging.mjs").read_text(encoding="utf-8")
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))

    assert manifest["version"] == "0.14.5" and manifest["phase"] in {"14U", "14V", "14W", "14X"}
    required = [
        "nativeContinuityCleanup",
        "adminPreviousViewMemory",
        "adminStaleWhileRefresh",
        "publicNativeBrowseState",
        "publicNativeCarouselMotion",
        "publicStaleWhileRefresh",
        "zeroDirectPhase14uApiCalls",
    ]
    assert all(manifest["scope"].get(flag) is True for flag in required)

    assert 'import "./phase14u.js";' in admin_entry
    for marker in ["phase14u.css", "phase14u-view.js", "phase14u-stale.js"]:
        assert marker in root_js
    for marker in ["sessionStorage", "uCapture", "uStore", "uLaterStore"]:
        assert marker in view_js
    assert 'document.querySelector(".phase14u-restore-view")?.remove()' in view_js
    assert 'querySelectorAll(".phase14t-restore-query").forEach(node=>node.remove())' in view_js
    assert 'document.querySelector(".page-head")?.appendChild' not in view_js
    assert "Restore previous view" not in view_js
    assert "fetch(" not in view_js and "fetch(" not in stale_js

    # Search/Apply refresh must never inject a second row/header or cloned previous-results list.
    for forbidden in [
        "cloneNode(",
        'document.createElement("div")',
        'insertAdjacentElement("afterend"',
        "Refreshing… showing previous results",
        "Previous results",
    ]:
        assert forbidden not in stale_js
    for marker in ["usClear", "phase14u-stale-copy", "phase14u-refresh-badge", 'setAttribute("role","status")']:
        assert marker in stale_js

    for marker in [
        "PUBLIC_BROWSE_KEY",
        "readBrowseState",
        "initialBrowse",
        'matchMedia("(prefers-reduced-motion: reduce)")',
        "document.hidden",
        "pauseUntil.current",
        "phase14u-refreshing",
        "phase14u-stale-grid",
        "Refreshing collection…",
    ]:
        assert marker in public_main or marker in public_css
    assert 'import "./phase14u-public.css";' in public_bridge
    assert "window.setInterval =" not in public_bridge
    assert "setAvailability({})" in public_main
    assert 'min={nextDate(pickupDate)||today()}' in public_main

    # Staging smoke must remain cache-fresh and browser execution must be strictly bounded.
    for marker in [
        "deploy/staging-assets/index.html",
        "expectedAssetPaths.every",
        "Public shell + exact deployed assets",
        "BROWSER_ATTEMPTS = 3",
        "BROWSER_TIMEOUT_MS = 15000",
        "timeout: BROWSER_TIMEOUT_MS",
        'killSignal: "SIGKILL"',
        "RETRY Public browser render",
        "asset_smoke=",
    ]:
        assert marker in smoke
    assert "BROWSER_ATTEMPTS = 5" not in smoke
    assert 'cache-control\": \"no-cache, no-store, max-age=0\"' in smoke

    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    assert "tests/phase14u_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14U native refresh + no duplicate list/header + bounded smoke regression: PASS")


if __name__ == "__main__":
    main()
