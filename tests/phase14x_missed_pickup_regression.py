#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    main_ui = (ROOT / "apps/admin-web/src/main.tsx").read_text(encoding="utf-8")
    ui = (ROOT / "apps/admin-web/src/phase14x-missed-pickup.js").read_text(encoding="utf-8")
    items = (ROOT / "apps/admin-web/src/phase14x-missed-items.js").read_text(encoding="utf-8")
    polish = (ROOT / "apps/admin-web/src/phase14x-missed-pickup-polish.js").read_text(encoding="utf-8")
    polish_css = (ROOT / "apps/admin-web/src/phase14x-missed-pickup-polish.css").read_text(encoding="utf-8")
    status = (ROOT / "apps/admin-web/src/phase14x-operational-status.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14x-missed-pickup.css").read_text(encoding="utf-8")
    worker = (ROOT / "worker/src/phase14o.js").read_text(encoding="utf-8")
    core = (ROOT / "worker/src/index.ts").read_text(encoding="utf-8")

    assert 'import "./phase14x-missed-pickup.js";' in entry
    assert 'import "./phase14x-missed-items.js";' in entry
    assert 'import "./phase14x-missed-pickup-polish.js";' in entry

    # Dedicated missed view is derived from existing booking dates/status/quantities.
    assert 'url.searchParams.get("view") === "missed"' in worker
    assert "b.status IN ('BOOKED','PARTIALLY_GIVEN')" in worker
    assert 'b.pickup_date < ?' in worker
    assert 'WHERE remaining_qty>0' in worker
    assert 'COUNT(*) OVER() AS missed_total' in worker
    assert 'missed_days' in worker
    assert 'item_lines_json' in worker and 'image_url' in worker

    # Dashboard stays one HTTP request; missed data is appended to the existing bundled payload.
    assert 'url.pathname === "/api/admin/dashboard"' in worker
    assert 'data.summary = { ...(data.summary || {}), missedPickups: missed.total }' in worker
    assert 'data.missedPickups = missed.pickups' in worker

    # Phase 14AQ: Missed Pickups is a native React Dashboard group member, not a later DOM prepend.
    assert 'missedPickups:number' in main_ui
    assert 'missedPickups:DashboardEntry[]' in main_ui
    assert '["Missed Pickups",data.summary.missedPickups||0]' in main_ui
    assert 'kind:"missed"|"booking"|"pickup"|"return"|"overdue"' in main_ui
    assert 'list("Missed Pickups",data.missedPickups||[],"missed")' in main_ui
    assert 'phase14x-missed-panel' in main_ui
    assert 'phase14x-missed-kpi' in main_ui
    assert 'data-missed-action="pickup"' in main_ui
    assert 'data-missed-action="reschedule"' in main_ui
    assert 'data-missed-action="cancel"' in main_ui
    assert 'data-missed-action={missed?"open-missed":undefined}' in main_ui

    # Legacy Dashboard DOM injection must stay gone; runtime owns only the Pickup-tab queue/actions.
    assert 'Dashboard Missed Pickups is rendered natively by React' in ui
    assert 'syncDashboard' not in ui
    assert 'liveGrid.prepend' not in ui
    assert 'isDashboardRequest' not in ui
    assert '/api/admin/dashboard' not in ui
    assert 'setInterval' not in ui

    # Pickup UX: dedicated queue + existing operational actions, no background polling.
    for token in ['Missed Pickup', 'Give Now', 'Reschedule', 'Cancel', 'bridgePickup', 'bridgeBooking']:
        assert token in ui
    assert 'view: "missed"' in ui
    assert 'phase14x-missed-tab' in css
    assert 'phase14x-missed-panel' in css
    assert 'phase14x-missed-kpi' in css

    # Every missed booking item is preserved individually, including its primary image.
    assert 'rowsByBooking' in items
    assert 'phase14d-item-grid' in items
    assert 'phase14d-item-card' in items
    assert 'image_url' in items
    assert 'remember(data?.missedPickups)' in items
    assert 'remember(data?.pickups)' in items
    assert 'setInterval' not in items

    # UI2: identity first, items next, operational metrics next, actions last; KPI is visibly actionable.
    assert 'UI-only; observes existing DOM and never creates API/Worker calls' in polish
    assert 'fetch(' not in polish and 'api(' not in polish
    assert 'moveFirst(card, head)' in polish
    assert 'moveAfter(grid, head)' in polish
    assert 'head.classList.add("dashboard-row-head")' in polish
    assert 'removeDuplicateBookingMetric(card)' in polish
    assert 'phase14x-missed-kpi-chevron' in polish
    assert 'queueMicrotask' in polish and 'MutationObserver(schedule)' in polish
    assert '.dashboard-row>.phase14x-missed-identity' in polish_css
    assert '.dashboard-row>.phase14d-item-grid' in polish_css
    assert '.dashboard-row>.dashboard-meta' in polish_css
    assert '.phase14x-missed-kpi-chevron' in polish_css

    # A partially-given past pickup remains a missed/pending pickup until all booked qty is given.
    assert '(status === "BOOKED" || status === "PARTIALLY_GIVEN") && pickup && pickup < today' in status
    assert '(status === "BOOKED" || status === "PARTIALLY_GIVEN") && pickup' in status

    # No-pickup bookings must never become overdue returns: overdue requires given > returned.
    assert 'bi.given_qty>bi.returned_qty' in core
    assert "view==='overdue'" in core
    assert 'pendingExpr}>0' in core

    assert 'tests/phase14x_missed_pickup_regression.py' in package["scripts"]["test:hardening"]
    print("Phase 14X missed pickup + Phase 14AQ native Dashboard group regression: PASS")


if __name__ == "__main__":
    main()
