#!/usr/bin/env python3
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]

def main():
    entry=(ROOT/'apps/admin-web/src/phase14k.js').read_text(encoding='utf-8')
    root_js=(ROOT/'apps/admin-web/src/phase14s.js').read_text(encoding='utf-8')
    nav=(ROOT/'apps/admin-web/src/phase14s-nav.js').read_text(encoding='utf-8')
    ops=(ROOT/'apps/admin-web/src/phase14s-ops.js').read_text(encoding='utf-8')
    polish=(ROOT/'apps/admin-web/src/phase14s-polish.js').read_text(encoding='utf-8')
    css=(ROOT/'apps/admin-web/src/phase14s.css').read_text(encoding='utf-8')
    manifest=json.loads((ROOT/'project-manifest.json').read_text(encoding='utf-8'))
    assert manifest['version']=='0.14.5' and manifest['phase'].startswith('14')
    for flag in ['finalUxReliability','navigationUnsavedGuard','accountModalUnsavedGuard','canonicalBookingDateAdapter','runtimePhaseLabelCleanup','operationalBookingDeepLink','availabilityFreshnessUi','modalFocusContainment','loadingSkeletonPolish','noDirectUxApiCalls']:
        assert manifest['scope'].get(flag) is True
    assert 'import "./phase14s.js";' in entry
    for marker in ['phase14s.css','phase14s-nav.js','phase14s-ops.js','phase14s-polish.js']: assert marker in root_js
    for marker in ['phase14s-nav-backdrop','Leave without saving?','phase14r-unsaved','.tabs button','.topbar .profile','.user-modal','.password-modal','beforeunload','trapTab']: assert marker in nav or marker in css
    for marker in ['today','addDay','dates','r.min=min','setInput','.pickup-card:not(.return-card)','.return-card','Open Pickup','Open Return','stage','Select to check exact availability','phase14s-checking']: assert marker in ops or marker in css
    assert 'const prior=window.fetch.bind(window)' in ops and 'return await prior(...args)' in ops
    assert r'\/api\/admin\/bookings\/availability' in ops
    assert 'sub.textContent="Admin Panel"' in polish and 'phase14s-loading' in polish and 'phase14s-loading' in css
    assert 'prefers-reduced-motion' in css
    assert 'new MutationObserver(schedule)' in ops and '{childList:true,subtree:true}' in ops
    print('Phase 14S final UX reliability regression: PASS')

if __name__=='__main__': main()
