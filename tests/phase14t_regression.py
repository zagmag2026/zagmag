#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    root_js = (ROOT / "apps/admin-web/src/phase14t.js").read_text(encoding="utf-8")
    core = (ROOT / "apps/admin-web/src/phase14t-core.js").read_text(encoding="utf-8")
    ops = (ROOT / "apps/admin-web/src/phase14t-ops.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14t.css").read_text(encoding="utf-8")
    public_entry = (ROOT / "apps/public-web/src/phase14c.js").read_text(encoding="utf-8")
    public_js = (ROOT / "apps/public-web/src/phase14t-public.js").read_text(encoding="utf-8")
    public_css = (ROOT / "apps/public-web/src/phase14t-public.css").read_text(encoding="utf-8")
    public_main = (ROOT / "apps/public-web/src/main.tsx").read_text(encoding="utf-8")
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))

    assert manifest["version"] == "0.14.5" and manifest["phase"].startswith("14")
    required = ["taskContinuityPublicMobileUx","extendedOperationalUnsavedGuard","moduleScrollMemory","savedSearchRestore","stickyPickupReturnSaveBar","publicPortraitMobileCards","publicCompactMobileBrowse","publicStickyCategoryRail","publicReducedMotionCarouselGuard","publicDetailModalFocusContainment","zeroAutomaticContinuityApiCalls"]
    assert all(manifest["scope"].get(flag) is True for flag in required)

    assert 'import "./phase14t.js";' in entry
    for marker in ["phase14t.css","phase14t-core.js","phase14t-ops.js"]:
        assert marker in root_js
    for marker in ["phase14t-guard-backdrop","sessionStorage","beforeunload","cRemember"]:
        assert marker in core or marker in css
    assert 'querySelectorAll(".phase14t-restore-query").forEach(node=>node.remove())' in core
    assert 'insertAdjacentElement("afterend"' not in core
    assert 'Restore “' not in core
    for marker in ["phase14t-op-footer","phase14t-source-save"]:
        assert marker in ops or marker in css
    # Phase 14AI renamed the summary variables while preserving the same sticky-save behavior.
    assert "Selected ${selected} · Pending ${pending}" in ops

    assert 'import "./phase14t-public.js";' in public_entry
    assert 'import "./phase14t-public.css";' in public_js
    assert "phase14t-public-modal-open" in public_js
    assert "ptVisibleModal" in public_js and "ptFocusable" in public_js
    # From Phase 14U onward carousel motion is React-owned instead of a global setInterval monkey patch.
    assert "window.setInterval =" not in public_js
    assert 'matchMedia("(prefers-reduced-motion: reduce)")' in public_main
    assert "document.hidden" in public_main and "pauseUntil.current" in public_main
    assert ".image-button,.image-button.public-card-carousel{aspect-ratio:4/5!important" in public_css
    assert "#categories.compact-section{position:sticky" in public_css
    assert ".item-head{position:sticky" in public_css

    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    assert "tests/phase14t_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14T task continuity + silent search memory regression: PASS")


if __name__ == "__main__":
    main()
