#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    js = (ROOT / "apps/admin-web/src/phase14p.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14p.css").read_text(encoding="utf-8")
    main_tsx = (ROOT / "apps/admin-web/src/main.tsx").read_text(encoding="utf-8")
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))

    assert manifest["version"] == "0.14.5"
    assert manifest["phase"].startswith("14")
    for flag in [
        "structuralErpAdminUi",
        "itemDrawerWorkflow",
        "desktopOperationalColumnHeaders",
        "mobileFilterSheet",
        "actionOverflowMenus",
        "dashboardOperationalKpiPriority",
    ]:
        assert manifest["scope"].get(flag) is True

    assert 'import "./phase14p.js";' in entry
    assert 'import "./phase14p.css";' in js

    for marker in ["item-editor", "item-filters", "items-grid", "customer-grid", "booking-grid", 'className="stats"']:
        assert marker in main_tsx

    for marker in [
        "phase14p-item-drawer-open",
        "phase14p-drawer-backdrop",
        "phase14p-drawer-close",
        "phase14p-add-item",
        "setItemDrawer",
    ]:
        assert marker in js or marker in css
    assert "position:fixed!important" in css
    assert "transform:translateX(102%)" in css

    for marker in [
        "phase14p-items-columns",
        "phase14p-customers-columns",
        "phase14p-bookings-columns",
        "phase14p-column-head",
    ]:
        assert marker in js and marker in css
    assert "display:contents!important" in css
    assert "@media(min-width:981px)" in css

    assert "phase14p-filter-toggle" in js and "phase14p-mobile-filter-open" in js
    assert ".phase14p-filter-backdrop{display:none}" in css
    assert "phase14p-filters-open" in css
    assert "@media(max-width:760px)" in css

    assert "phase14p-overflow" in js and "phase14p-more-toggle" in js
    assert 'aria-haspopup' in js and 'aria-expanded' in js
    assert "source.click()" in js
    assert "delete|cancel" in js

    assert ".stats>.stat:nth-child(4)" in css
    assert ".stats>.stat:nth-child(7)" in css
    assert "dashboardOperationalKpiPriority" in json.dumps(manifest)

    assert "new MutationObserver(scheduleScan)" in js
    assert "{ childList: true, subtree: true }" in js
    assert 'event.key !== "Escape"' in js

    print("Phase 14P structural ERP admin UI regression: PASS")


if __name__ == "__main__":
    main()
