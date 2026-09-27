#!/usr/bin/env python3
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]

def main():
    entry=(ROOT/'apps/admin-web/src/phase14k.js').read_text(encoding='utf-8')
    js=(ROOT/'apps/admin-web/src/phase14r.js').read_text(encoding='utf-8')
    css=(ROOT/'apps/admin-web/src/phase14r.css').read_text(encoding='utf-8')
    main_tsx=(ROOT/'apps/admin-web/src/main.tsx').read_text(encoding='utf-8')
    manifest=json.loads((ROOT/'project-manifest.json').read_text(encoding='utf-8'))
    assert manifest['version']=='0.14.5'
    assert manifest['phase'].startswith('14')
    for flag in ['userExperienceHardening','unsavedChangesGuard','mobileBookingStepFlow','visualBookingItemPicker','inlineFormValidation','mobileReportFilterSheet','mobileActionSheet','contextualDetailActions','zeroExtraUxApiCalls']:
        assert manifest['scope'].get(flag) is True
    assert 'import "./phase14r.js";' in entry
    assert 'import "./phase14r.css";' in js
    for marker in ['phase14r-discard-backdrop','rConfirmClose','beforeunload','phase14r-stepper','rValidateBookingStep','phase14r-booking-review','phase14r-item-picker','phase14r-selected-item','rMapServerError','phase14r-context-actions','Go to Pickup','Go to Returns']:
        assert marker in js or marker in css
    assert 'const rNativeFetch = window.fetch.bind(window)' in js
    assert 'response.clone().json()' in js
    assert r'\/api\/admin\/bookings\/bootstrap' in js
    assert r'\/api\/admin\/bookings\/availability' in js
    assert 'image_url:string|null' in main_tsx
    assert 'new MutationObserver(rScheduleScan)' in js
    print('Phase 14R user experience hardening regression: PASS')

if __name__=='__main__': main()
