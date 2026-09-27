#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14am-search-flow.css").read_text(encoding="utf-8")
    phase14p = (ROOT / "apps/admin-web/src/phase14p.css").read_text(encoding="utf-8")
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))

    marker = 'import "./phase14am-search-flow.css";'
    assert marker in entry
    assert entry.index('import "./phase14ak-partial-return.js";') < entry.index(marker)

    assert '@media(min-width:761px)' in css
    for selector in [
        '.item-filters',
        '.customer-filters',
        '.booking-filters',
        '.pickup-toolbar',
        '.report-filters',
        '.users-toolbar',
        '.audit-filters',
        '.phase14p-column-head',
    ]:
        assert selector in css

    for rule in [
        'position:relative!important',
        'top:auto!important',
        'bottom:auto!important',
        'z-index:auto!important',
        'box-shadow:none!important',
    ]:
        assert rule in css

    # Final layer must not create another sticky/fixed toolbar, runtime, or data call.
    for forbidden in ['position:sticky', 'position:fixed', 'fetch(', '/api/', 'MutationObserver']:
        assert forbidden not in css

    # Existing mobile filter drawer remains owned by Phase 14P and is not overridden
    # because this final layer only applies from 761px upward.
    assert '@media(max-width:760px)' in phase14p
    assert 'position:fixed!important' in phase14p
    assert 'phase14p-filters-open' in phase14p

    assert 'tests/phase14am_search_flow_regression.py' in package['scripts']['test:hardening']
    print('Phase 14AM Fix2 search/list header non-overlap regression: PASS')


if __name__ == '__main__':
    main()
