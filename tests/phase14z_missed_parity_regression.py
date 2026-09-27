from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
css = (ROOT / "apps/admin-web/src/phase14z-missed-parity.css").read_text(encoding="utf-8")
missed = (ROOT / "apps/admin-web/src/phase14x-missed-pickup.js").read_text(encoding="utf-8")
polish = (ROOT / "apps/admin-web/src/phase14x-missed-pickup-polish.js").read_text(encoding="utf-8")
icons = (ROOT / "apps/admin-web/src/phase14x-icon-runtime.js").read_text(encoding="utf-8")
package = (ROOT / "package.json").read_text(encoding="utf-8")

assert 'import "./phase14z-missed-parity.css";' in entry, "Phase14Z parity CSS must be loaded"
assert entry.index('import "./phase14y-uniform.css";') < entry.index('import "./phase14z-missed-parity.css";'), "Parity CSS must be the final Admin visual layer"

# Visual-only: no runtime/network additions in the parity layer.
for forbidden in ("fetch(", "MutationObserver", "setInterval", "setTimeout", "addEventListener"):
    assert forbidden not in css, f"Visual parity CSS must not add runtime behavior: {forbidden}"

# The outer panel must use the same neutral Dashboard shell; semantic pink belongs inside the status row.
for token in (
    ".phase14x-missed-panel{",
    "background:#fff!important",
    "border-color:var(--ui-line,#eadfe4)!important",
    "border-radius:var(--ui-shell-r,14px)!important",
    ".phase14x-missed-panel>.panel-head{",
    "background:#fff!important",
):
    assert token in css, f"Missing neutral Dashboard shell parity token: {token}"

# Exact card anatomy: identity -> item -> three metrics -> actions.
for token in (
    ".phase14x-missed-panel .dashboard-row.phase14x-ui3-missed-card{",
    "grid-template-columns:minmax(0,1fr)!important",
    ".phase14x-missed-panel .dashboard-row>.phase14d-item-grid .phase14d-item-card{",
    "grid-template-columns:48px minmax(0,1fr)!important",
    ".phase14x-missed-panel .dashboard-row>.dashboard-meta{",
    "grid-template-columns:minmax(0,1.55fr) minmax(0,.72fr) minmax(0,1fr)!important",
    ".phase14x-missed-panel .dashboard-row>.dashboard-actions{",
    "grid-template-columns:repeat(5,minmax(0,1fr))!important",
):
    assert token in css, f"Missing exact Missed Pickup parity rule: {token}"

# Mobile must remain compact, full-width and keep every action visible.
assert "@media(max-width:700px)" in css
assert "grid-template-columns:repeat(3,minmax(0,1fr))!important" in css
assert "display:none" not in css, "Parity layer must not hide Missed Pickup information/actions"

# Preserve all workflow actions from the existing business/UI runtime.
for action in ('"WhatsApp"', '"Call"', '"Give Now"', '"Reschedule"', '"Cancel"'):
    assert action in missed, f"Existing Missed Pickup action unexpectedly missing: {action}"

# Missed Pickups must use the same global KPI icon/chevron owner as every other KPI.
assert '"Missed Pickups": "event_busy"' in icons, "Missed Pickups KPI must have a Material icon mapping"
assert 'kpi.classList.add("phase14x-kpi")' in polish, "Missed KPI must join the common KPI component"
assert 'kpi.classList.remove("phase14x-missed-kpi-actionable")' in polish, "Legacy custom KPI affordance must be removed"
assert 'querySelectorAll(":scope > .phase14x-missed-kpi-chevron").forEach(node => node.remove())' in polish, "Stale custom chevrons must be cleaned"
assert 'chevron.textContent = "›"' not in polish, "Custom chevron creation must not return"
assert ".phase14x-missed-kpi-chevron" not in css, "Phase14Z must not style a second KPI chevron"
assert 'content:"chevron_right"' not in css, "Missed KPI chevron must come only from the global KPI component"

assert "phase14z_missed_parity_regression.py" in package, "Phase14Z regression must run in test:hardening"

print("Phase 14Z missed-pickup exact visual parity + single KPI icon owner regression: PASS")
