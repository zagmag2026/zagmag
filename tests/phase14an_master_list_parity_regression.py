#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14an-master-list-parity.css").read_text(encoding="utf-8")
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))

    marker = 'import "./phase14an-master-list-parity.css";'
    assert marker in entry
    assert entry.index('import "./phase14am-search-flow.css";') < entry.index(marker)

    # Protect the already-approved Item Master view: this final layer must target
    # only Customer/Booking list surfaces and must never restyle Item selectors.
    assert ".item-" not in css
    for selector in [
        'section.list-card:has(> .customer-filters)',
        'section.list-card:has(> .booking-filters)',
        '.phase14x-ui3-customer-list',
        '.customer-grid',
        '.booking-grid',
        '.customer-card',
        '.booking-card',
        '.phase14p-column-head',
    ]:
        assert selector in css

    # Approved master-list rhythm: shell header, toolbar controls, desktop ERP rows,
    # tablet spacing, mobile list cadence and pager spacing must stay aligned.
    for token in [
        'padding:12px 15px 10px!important',
        'padding:10px 15px!important',
        'min-height:40px!important',
        'padding:6px 10px 4px!important',
        'gap:5px!important',
        'min-height:76px!important',
        'border-radius:8px!important',
        'padding:10px 15px 14px!important',
        'padding:10px 12px 8px!important',
        'padding:9px 10px 2px!important',
        'padding:10px 12px 12px!important',
    ]:
        assert token in css

    # Customer special pill/inset treatment is explicitly neutralized.
    for token in [
        '.phase14x-ui3-customer-list{padding:0!important}',
        '.phase14x-ui3-customer-head{margin:0!important}',
        'border-radius:0!important',
        'background:transparent!important',
    ]:
        assert token in css

    # UI-only parity layer: no runtime/data behavior.
    for forbidden in ['fetch(', '/api/', 'MutationObserver', 'position:sticky', 'position:fixed']:
        assert forbidden not in css

    assert 'tests/phase14an_master_list_parity_regression.py' in package['scripts']['test:hardening']
    print('Phase 14AN Customer/Booking master-list parity regression: PASS')


if __name__ == '__main__':
    main()
