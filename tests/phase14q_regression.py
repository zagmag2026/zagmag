#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    js = (ROOT / "apps/admin-web/src/phase14q.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14q.css").read_text(encoding="utf-8")
    main_tsx = (ROOT / "apps/admin-web/src/main.tsx").read_text(encoding="utf-8")
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))

    assert manifest["version"] == "0.14.5"
    assert manifest["phase"].startswith("14")
    for flag in [
        "customerDrawerWorkflow",
        "bookingDrawerWorkflow",
        "categoryExpandedSplitView",
        "detailStickyContactBar",
        "formDrawerFocusTrap",
    ]:
        assert manifest["scope"].get(flag) is True

    assert 'import "./phase14q.js";' in entry
    assert 'import "./phase14q.css";' in js

    for marker in ["customer-layout", "booking-layout", "category-row", "fields-zone", "history-panel"]:
        assert marker in main_tsx

    for marker in [
        "phase14q-customer-drawer-open",
        "phase14q-booking-drawer-open",
        "phase14q-form-drawer-open",
        "phase14q-add-customer",
        "phase14q-add-booking",
        "qSetDrawer",
        "qCloseDrawers",
    ]:
        assert marker in js or marker in css

    assert "position:fixed!important" in css
    assert "transform:translateX(102%)" in css
    assert "width:min(580px,100vw)" in css
    assert "width:min(820px,100vw)" in css

    assert "phase14q-category-expanded" in js and "phase14q-category-expanded" in css
    assert "@media(min-width:1051px)" in css
    assert "grid-template-columns:minmax(250px,.42fr)" in css

    assert "phase14q-detail-bar" in js and "phase14q-detail-bar" in css
    assert "position:sticky;bottom:0" in css
    assert "https://wa.me/" in js and "tel:" in js

    assert 'event.key === "Escape"' in js
    assert 'event.key !== "Tab"' in js
    assert "event.shiftKey" in js
    assert "new MutationObserver(qScheduleScan)" in js
    assert "{ childList: true, subtree: true }" in js

    # Legacy page behavior is still present; Phase 14Q only changes presentation/interaction.
    assert 'Customer updated.' in main_tsx and 'Booking updated.' in main_tsx
    assert '/api/admin/customers' in main_tsx and '/api/admin/bookings' in main_tsx

    print("Phase 14Q admin form + detail structural UI regression: PASS")


if __name__ == "__main__":
    main()
