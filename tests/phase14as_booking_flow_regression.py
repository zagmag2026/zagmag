#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
runtime = (ROOT / "apps/admin-web/src/phase14as-booking-flow.js").read_text(encoding="utf-8")
fix1 = (ROOT / "apps/admin-web/src/phase14as-fix1.js").read_text(encoding="utf-8")
fix2 = (ROOT / "apps/admin-web/src/phase14as-fix2.js").read_text(encoding="utf-8")
styles = (ROOT / "apps/admin-web/src/phase14as-fix1.css").read_text(encoding="utf-8")
loader = (ROOT / "apps/admin-web/src/phase14h.js").read_text(encoding="utf-8")
main = (ROOT / "apps/admin-web/src/main.tsx").read_text(encoding="utf-8")
manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))

assert manifest["version"] == "0.14.5"
assert 'import "./phase14as-booking-flow.js";' in loader
assert 'import "./phase14as-fix1.js";' in loader
assert 'import "./phase14as-fix2.js";' in loader
assert 'import "./phase14as-fix1.css";' in fix1

# Customer List -> Create Booking stays visible after async Customer renders.
assert 'phase14as-create-booking' in fix1
assert 'customerRootObserver.observe(root,{childList:true,subtree:true})' in fix1
assert '.phase14as-create-booking' in styles

# Fix2 owns the click before the older DOM handlers, stores the selected customer,
# then opens the actual Bookings tab on the next animation frame.
assert 'document.addEventListener("click"' in fix2
assert 'event.stopImmediatePropagation()' in fix2 or 'e.stopImmediatePropagation()' in fix2
assert 'sessionStorage.setItem(KEY' in fix2
assert 'dataset.phase14arSource==="Bookings"' in fix2
assert 'requestAnimationFrame(()=>{const tab=bookingTab()' in fix2
assert 'tab.click()' in fix2
assert 'document.querySelector(".booking-form")' in fix2
assert '760' in fix2

# Live customer search continues to use the existing bounded search pipeline, while
# Fix2 re-resolves the current React customer select and paints a visible result list.
assert 'Search customer / mobile' in main
assert '/api/admin/customers?' in main
assert 'phase14as-customer-results' in fix2
assert 'phase14as-customer-result' in fix2
# Keep these semantic checks independent of compact declaration formatting.
assert 'f.querySelectorAll("select")' in fix2
assert '!s.closest(".booking-lines")' in fix2
assert 'selectObserver=new MutationObserver(schedule)' in fix2
assert 'formObserver.observe(f,{childList:true,subtree:true})' in fix2
assert 'nativeSelect(sel,o.value)' in fix2
assert 'nativeInput(input,"")' in fix2
assert 'fetch(' not in fix2
assert '.phase14as-customer-results' in styles

# Customer prefill no longer depends on picker.nextElementSibling after Fix1 inserted
# its result panel/hint between the picker and the React Customer label.
assert 'function applyPending(input,sel)' in fix2
assert 'digits(v.textContent).includes(x.mobile)' in fix2
assert 'nativeInput(input,x.mobile)' in fix2

# Category-first item selection stays hydrated after asynchronous React option updates.
assert 'phase14as-category-field' in fix1
assert 'itemObservers' in fix1
assert 'o.observe(select,{childList:true})' in fix1
assert 'categories.length?"Select category":"Loading categories…"' in fix1
assert 'select.disabled=!category' in fix1
assert 'option.hidden=!match' in fix1
assert 'option.disabled=!match' in fix1

# No periodic polling or new API/D1 path was added by this UI repair.
assert 'setInterval(' not in runtime
assert 'setInterval(' not in fix1
assert 'setInterval(' not in fix2
assert 'bookingLinesObserver.observe(lines,{childList:true})' in fix1

# Existing booking availability/save pipeline remains authoritative.
assert '/api/admin/bookings/availability' in main
assert 'itemIds:selectedIds.split("|")' in main
assert 'items:form.items' in main

print("Phase 14AS Fix2 booking navigation + visible customer autocomplete regression: PASS")
