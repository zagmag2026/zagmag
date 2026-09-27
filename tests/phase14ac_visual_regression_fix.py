#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14ac-visual-regression-fix.css").read_text(encoding="utf-8")
    report_runtime = (ROOT / "apps/admin-web/src/phase14d.js").read_text(encoding="utf-8")

    assert 'import "./phase14ac-visual-regression-fix.css";' in entry
    assert entry.index('import "./phase14ab-customer-actions.css";') < entry.index('import "./phase14ac-visual-regression-fix.css";')

    # Customer Master desktop must be a full-width row list before the card's
    # internal Customer | Activity | Contact/Manage columns are resolved.
    for token in [
        '@media(min-width:981px)',
        '.customer-grid,',
        '.phase14x-ui3-customer-grid',
        'grid-template-columns:minmax(0,1fr)!important',
        '.phase14x-ui3-customer-card.customer-card',
        'width:100%!important',
        'min-width:0!important',
    ]:
        assert token in css

    # Global portal controls are hidden by default and visible only while their
    # own drawer-open body state exists.
    assert '.phase14p-drawer-close,\n.phase14q-form-close{\n  display:none!important;' in css
    assert 'body.phase14p-item-drawer-open>.phase14p-drawer-close' in css
    assert 'body.phase14q-form-drawer-open>.phase14q-form-close' in css
    assert 'display:grid!important' in css

    for forbidden in ['MutationObserver', 'fetch(', '/api/']:
        assert forbidden not in css

    # Reports keep the Phase14AB single-owner safeguards intact.
    assert 'function uniqueLines(lines)' in report_runtime
    assert 'function existingVisualGrid(node)' in report_runtime
    assert 'if(node.classList?.contains("report-primary"))node.querySelectorAll(":scope > .phase14d-item-card").forEach(card=>card.remove());' in report_runtime
    assert 'phase14dSingle' not in report_runtime

    assert 'tests/phase14ac_visual_regression_fix.py' in package['scripts']['test:hardening']
    print('Phase 14AC customer desktop + portal close visual regression: PASS')


if __name__ == '__main__':
    main()
