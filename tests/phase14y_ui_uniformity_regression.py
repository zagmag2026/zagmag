#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14y-uniform.css").read_text(encoding="utf-8")

    assert 'import "./phase14y-uniform.css";' in entry
    assert entry.index('import "./phase14x-ui3.js";') < entry.index('import "./phase14y-uniform.css";')

    # This pass must remain visual-only: no runtime observer/fetch layer.
    assert "MutationObserver" not in css
    assert "fetch(" not in css
    assert "/api/" not in css

    # Global page rhythm and shell consistency.
    for token in [
        ".page-head",
        ".list-card,.panel,.report-shell,.users-shell,.settings-shell,.form-card,.info-card,.placeholder",
        ".list-head,.panel-head,.settings-title,.form-title,.fields-head,.subhead",
        ".stats .stat,.mini-stats>article",
        ".empty",
        ".pager",
    ]:
        assert token in css

    # Toolbar/control family across all primary Admin modules.
    for token in [
        ".item-filters,.customer-filters,.booking-filters,.pickup-toolbar,.report-filters,.users-toolbar,.audit-filters,.report-tabs,.settings-tabs",
        ".primary,.ghost,.danger,.danger-link,.icon-btn,.wa-button,.call-button,.profile",
        ".pickup-tabs button,.report-tabs button,.settings-tabs button",
    ]:
        assert token in css

    # Entity cards across every page must share the same border/radius language.
    for token in [
        ".category-row,.item-card,.customer-card,.booking-card,.pickup-card,.report-row,.user-card,.template-card,.audit-card,.field-row,.detail-items article",
        ".row-actions,.field-actions,.quick-actions,.dashboard-actions,.report-actions,.user-actions,.pickup-actions,.report-filter-actions",
        ".chip,.role-chip,.count,.pill,.corrected-label",
    ]:
        assert token in css

    # Mobile: no page-wide horizontal layout and consistent two-column summary rhythm.
    assert "@media(max-width:700px)" in css
    assert "overflow-x:clip!important" in css
    assert "grid-template-columns:repeat(2,minmax(0,1fr))!important" in css

    assert "tests/phase14y_ui_uniformity_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14Y unified Admin ERP visual system regression: PASS")


if __name__ == "__main__":
    main()
