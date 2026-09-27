#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    ui = (ROOT / "apps/admin-web/src/phase14x-ui3.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14x-ui3.css").read_text(encoding="utf-8")

    assert 'import "./phase14x-ui3.js";' in entry
    assert 'import "./phase14x-ui3.css";' in ui
    assert "fetch(" not in ui and "api(" not in ui and "setInterval" not in ui

    # Queue-tab overflow must stay inside the tab strip and active tabs auto-center.
    for token in [
        "phase14x-ui3-queue-toolbar",
        "phase14x-ui3-scroll-tabs",
        "centerActiveTab",
        "tabs.scrollTo",
        "overflow-x:auto",
        "scrollbar-width:none",
    ]:
        assert token in ui or token in css

    # Missed Pickup dashboard card must use the same customer-first structure as normal cards.
    assert "matchMissedDashboardCard" in ui
    assert "dashboard-row-head" in ui
    assert "phase14x-ui3-booking-metric" in ui
    assert 'card.dataset.phase14xUi3MissedMatched = "1"' in ui
    assert ".phase14x-ui3-missed-card" in css

    # Fix2: repeated scans must keep exactly one Booking metric and repair already-duplicated cards.
    assert 'querySelectorAll(":scope > .phase14x-ui3-booking-metric")' in ui
    assert "const candidates = [...generated, ...legacy];" in ui
    assert "candidates.slice(1).forEach(node => node.remove());" in ui
    assert 'addClasses(metric, "phase14x-ui3-booking-metric")' in ui
    assert "metric.replaceChildren(label, value);" in ui
    assert 'setTextIfChanged(label, "Booking")' in ui
    assert "setTextIfChanged(value, bookingNo)" in ui

    # Customer list is compact but retains all existing actions.
    for selector in [
        ".phase14x-ui3-customer-card",
        ".phase14x-ui3-customer-stats",
        ".phase14x-ui3-customer-quick",
        ".phase14x-ui3-customer-more",
    ]:
        assert selector in css
    assert "decorateCustomerPage" in ui

    # Category prefix is no longer used as the visual icon; a Material category icon is separate.
    assert "decorateCategoryCard" in ui
    assert 'return "checkroom"' in ui
    assert "phase14x-ui3-category-card-icon" in ui and ".phase14x-ui3-category-card-icon" in css
    assert "phase14x-ui3-category-prefix" in ui and ".phase14x-ui3-category-prefix.prefix" in css
    assert "Material Symbols Rounded" in css
    assert "phase14x-ui3-category-stats" in css

    # UI3 performance hardening: DOM decoration must settle instead of self-triggering forever.
    assert "scheduledFrame = requestAnimationFrame(scan)" in ui
    assert "queueMicrotask(scan)" not in ui
    observer = ui.split("new MutationObserver(schedule).observe", 1)[1]
    assert "childList: true" in observer and "subtree: true" in observer
    assert "attributes: true" not in observer and "attributeFilter" not in observer
    assert "if (icon.textContent !== glyph) icon.textContent = glyph;" in ui
    assert "setTextIfChanged(mobile, parsed.mobile)" in ui
    assert 'card.dataset.phase14xUi3Customer === "1"' in ui
    assert 'title === "Customers"' in ui and 'title === "Categories & Custom Fields"' in ui

    assert "tests/phase14x_ui3_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14X Fix10 UI3 compact queue/customer/category + performance Fix2 regression: PASS")


if __name__ == "__main__":
    main()
