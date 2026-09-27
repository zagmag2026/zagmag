#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    admin_entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    admin_css = (ROOT / "apps/admin-web/src/phase14aa-ui-stability.css").read_text(encoding="utf-8")
    action_css = (ROOT / "apps/admin-web/src/phase14aa-form-action-parity.css").read_text(encoding="utf-8")
    customer_css = (ROOT / "apps/admin-web/src/phase14ab-customer-actions.css").read_text(encoding="utf-8")
    phase14d = (ROOT / "apps/admin-web/src/phase14d.js").read_text(encoding="utf-8")
    booking_state = (ROOT / "apps/admin-web/src/phase14x-booking-item-state.js").read_text(encoding="utf-8")
    public_entry = (ROOT / "apps/public-web/src/phase14c.js").read_text(encoding="utf-8")
    public_css = (ROOT / "apps/public-web/src/phase14aa-public-stability.css").read_text(encoding="utf-8")

    # Final Admin layers must load after previous visual systems and remain CSS-only.
    assert 'import "./phase14aa-ui-stability.css";' in admin_entry
    assert 'import "./phase14aa-form-action-parity.css";' in admin_entry
    assert 'import "./phase14ab-customer-actions.css";' in admin_entry
    assert admin_entry.index('import "./phase14z-missed-parity.css";') < admin_entry.index('import "./phase14aa-ui-stability.css";')
    assert admin_entry.index('import "./phase14aa-ui-stability.css";') < admin_entry.index('import "./phase14aa-form-action-parity.css";')
    assert admin_entry.index('import "./phase14aa-form-action-parity.css";') < admin_entry.index('import "./phase14ab-customer-actions.css";')
    for css in [admin_css, action_css, customer_css]:
        for forbidden in ["MutationObserver", "fetch(", "/api/"]:
            assert forbidden not in css

    # Reports: Phase14D has exactly one item-grid owner. Legacy single-card injection is
    # removed at source, duplicate grids are collapsed, and identical item lines are deduped.
    assert 'function uniqueLines(lines)' in phase14d
    assert 'function existingVisualGrid(node)' in phase14d
    assert 'grids.slice(1).forEach(grid=>grid.remove());' in phase14d
    assert 'lines=uniqueLines(lines);' in phase14d
    assert 'phase14dSingle' not in phase14d
    assert 'primary.prepend(visualCard' not in phase14d
    assert 'if(row&&primary)renderBookingItems(primary,row' in phase14d
    assert '.report-primary:has(> .phase14d-item-grid) > .phase14d-item-card' in admin_css

    # Customers: desktop identity/activity stay visible; mobile has the intended
    # 3-column stats/contact rhythm; Edit/Archive/Delete sources never reappear beside More.
    for token in [
        '.customer-card>.row-actions.phase14p-source-group',
        '.customer-card .phase14p-overflow-source',
        '.customer-card>.customer-head',
        '.customer-card>.customer-stats',
        'grid-template-columns:repeat(3,minmax(0,1fr))!important',
        '.customer-card>.quick-actions>.phase14p-overflow',
        '.phase14p-customers-columns>span:nth-child(4)',
    ]:
        assert token in customer_css
    assert 'display:none!important' in customer_css

    # User/Category/Item/Customer editors must share one close/control/action grammar.
    for token in [
        '.phase14p-drawer-close',
        '.phase14q-form-close',
        '.phase14x-category-cancel',
        '.user-modal>.form-title>button',
        'content:"close"!important',
        '--form-control-h:42px',
    ]:
        assert token in admin_css
    for token in [
        '.user-modal>.primary.full',
        '.manage-grid>.form-card>.primary.full',
        '.item-editor>.form-card>.primary.full',
        '.customer-layout>.form-card>.primary.full',
        'position:static!important',
        'width:100%!important',
        'border-radius:9px!important',
    ]:
        assert token in action_css

    # Booking availability decorator must be idempotent and must not observe text-node
    # mutations that its own status copy can generate.
    assert 'function bSetText(' in booking_state
    assert 'function bSetClass(' in booking_state
    assert 'if (node.textContent !== next) node.textContent = next;' in booking_state
    assert 'characterData: true' not in booking_state
    assert 'new MutationObserver(bSchedule).observe(document.documentElement, { childList: true, subtree: true });' in booking_state

    # Public footer icons load after Phase14Y and use Material Symbols for both actions.
    assert 'phase14aa-public-stability.css' in public_entry
    assert public_entry.index('phase14y-public-uniform.css') < public_entry.index('phase14aa-public-stability.css')
    for token in [
        '.footer-actions .btn::before',
        'font-family:"Material Symbols Rounded"!important',
        'content:"chat"!important',
        'content:"call"!important',
    ]:
        assert token in public_css
    for forbidden in ["MutationObserver", "fetch(", "/api/"]:
        assert forbidden not in public_css

    assert "tests/phase14aa_ui_stability_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14AB reports/customer UI regression: PASS")


if __name__ == "__main__":
    main()
