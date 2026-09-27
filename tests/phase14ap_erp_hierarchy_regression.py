#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14ap-erp-hierarchy.css").read_text(encoding="utf-8")
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))

    marker = 'import "./phase14ap-erp-hierarchy.css";'
    assert marker in entry
    assert entry.index('import "./phase14ao-erp-neutral.css";') < entry.index(marker)

    # Dashboard: the injected Missed Pickups KPI makes eight cards. Keep a balanced
    # 4x2 desktop layout and a 2-column mobile layout with explicit positions.
    for token in [
        'grid-template-columns:repeat(4,minmax(0,1fr))!important',
        '.stats>.phase14x-missed-kpi{grid-column:3!important;grid-row:1!important}',
        '.stats>.stat:nth-child(8){grid-column:4!important;grid-row:2!important}',
        'grid-auto-rows:max-content!important',
        '.live-grid>.dashboard-panel{',
        'min-height:0!important',
        'grid-template-columns:repeat(2,minmax(0,1fr))!important',
        '.stats>.phase14x-missed-kpi{grid-column:1!important;grid-row:2!important}',
    ]:
        assert token in css

    # All audited Admin areas receive hierarchy/separation coverage.
    for selector in [
        '.category-row', '.item-card', '.customer-card', '.booking-card', '.pickup-card',
        '.return-card.is-overdue', '.report-row', '.user-card', '.settings-shell', '.audit-card',
        '.history-panel>.history-title', '.pickup-line-head', '.return-line-head', '.fields-head',
    ]:
        assert selector in css

    # Operational state remains semantic through accent lines, never full-card fills.
    assert '[data-operational-tone]' in css
    assert 'border-left:3px solid var(--op-accent)!important' in css
    for token in ['.status-booked', '.status-partially_given', '.status-given', '.status-partially_returned', '.status-returned', '.status-cancelled']:
        assert token in css

    # Users master visually follows Header -> Search/Filters -> Rows -> Pager.
    for token in [
        '.users-shell>.list-head{order:1!important}',
        '.users-shell>.users-toolbar{order:2!important}',
        '.users-shell>.users-grid,.users-shell>.empty{order:3!important}',
        '.users-shell>.pager{order:4!important}',
    ]:
        assert token in css

    # Neutral ERP grouping is permitted; pastel/full-card semantic backgrounds are not.
    assert '--erp-head-bg:#f1f3f5' in css
    for forbidden in ['#eef7ff', '#fff1f3', '#fff8e8', '#eef9f2', '#f6f2ff', 'fetch(', '/api/', 'MutationObserver']:
        assert forbidden not in css

    # Final layer is CSS-only and must stay after the no-pastel standard.
    assert 'tests/phase14ap_erp_hierarchy_regression.py' in package['scripts']['test:hardening']
    assert package['version'] == '0.14.5'
    print('Phase 14AP all-page ERP hierarchy regression: PASS')


if __name__ == '__main__':
    main()
