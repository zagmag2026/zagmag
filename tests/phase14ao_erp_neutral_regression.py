from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
css = (ROOT / "apps/admin-web/src/phase14ao-erp-neutral.css").read_text(encoding="utf-8")
entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")

required = [
    "--erp-canvas:#f5f6f8",
    ":where(.booking-card,.pickup-card,.dashboard-row,.history-list article,.report-row)[data-operational-tone]",
    "background:var(--erp-surface)!important",
    ".wa-button{",
    ".call-button{",
    ".danger,.danger-link,.phase14p-more-menu button.danger",
    ".phase14r-discard-confirm",
    ':where(button,a)[data-ui-icon="1"]::before',
    ".tabs button.active,.report-tabs button.active,.settings-tabs button.active",
    "min-height:42px!important",
]
for marker in required:
    assert marker in css, f"missing Phase 14AO ERP-neutral marker: {marker}"

# Final layer must load after all earlier visual parity layers.
assert 'import "./phase14ao-erp-neutral.css";' in entry
assert entry.index('import "./phase14ao-erp-neutral.css";') > entry.index('import "./phase14an-master-list-parity.css";')

# Pastel semantic fills are intentionally converted to white/neutral surfaces in this final layer.
for selector in [
    ".stats .stat,.mini-stats>article",
    ".customer-stats span,.booking-dates span,.pickup-qty span,.dashboard-meta span,.report-metrics span",
    ".item-filters,.customer-filters,.booking-filters,.pickup-toolbar,.report-filters,.users-toolbar,.audit-filters",
    ".chip,.chip.good,.chip.public,.chip.required,.active-chip,.inactive-chip",
]:
    assert selector in css, f"missing neutralized surface family: {selector}"

# UI-only layer: never add runtime/network behavior here.
assert "fetch(" not in css
assert "XMLHttpRequest" not in css
assert "WebSocket" not in css

print("Phase 14AO ERP no-pastel surfaces + button standard regression: PASS")
