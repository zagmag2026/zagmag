from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JS = (ROOT / "apps/admin-web/src/phase14ag-secondary-form-canonical.js").read_text()
CSS = (ROOT / "apps/admin-web/src/phase14ag-secondary-form-canonical.css").read_text()
ENTRY = (ROOT / "apps/admin-web/src/phase14k.js").read_text()
MAIN = (ROOT / "apps/admin-web/src/main.tsx").read_text()

assert 'import "./phase14ag-secondary-form-canonical.js";' in ENTRY
assert 'import "./phase14ag-secondary-form-canonical.css";' in JS

# All remaining Create/Edit variants are owned by the canonical secondary layer.
for marker in ['kind: "password"', 'kind: "template"', 'kind: "field"']:
    assert marker in JS
for marker in ['phase14ag-template-open', 'phase14ag-field-open', 'phase14ag-password-open']:
    assert marker in JS and marker in CSS

# Category field browsing must not automatically open the editor.
assert 'fieldRequestedOpen = false' in JS
assert 'button.closest(".fields-head")' in JS
assert 'button.closest(".field-row")' in JS
assert '.field-form.phase14ag-secondary-idle{display:none!important}' in CSS

# Template and field use one portal close; password uses the same Material close grammar.
assert 'phase14ag-close' in JS and 'phase14ag-close::before' in CSS
assert 'data-phase14ag-kind="password"' in CSS
assert 'content:"close"' in CSS

# Common dirty close / Escape / focus-trap guards.
assert 'Discard unsaved ${state.label} changes?' in JS
assert 'event.key === "Escape"' in JS
assert 'event.key !== "Tab"' in JS
assert 'window.confirm' in JS

# No network/polling side effects in the UI canonicalizer.
assert 'fetch(' not in JS
assert 'setInterval(' not in JS

# Same sticky primary footer geometry, including Settings template save wrapper.
assert '.phase14ag-secondary-form>.primary.full' in CSS
assert '.phase14ag-secondary-form>.settings-save' in CSS
assert 'position:sticky!important' in CSS

# Source forms still exist; this phase is visual/interaction-only.
assert 'className="template-editor"' in MAIN
assert 'className="field-form"' in MAIN
assert 'className="password-modal"' in MAIN

print("Phase 14AG secondary canonical forms regression: PASS")
