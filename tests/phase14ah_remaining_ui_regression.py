from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADMIN_CSS = (ROOT / "apps/admin-web/src/phase14ah-operational-settings.css").read_text()
ADMIN_ENTRY = (ROOT / "apps/admin-web/src/phase14k.js").read_text()
PUBLIC_JS = (ROOT / "apps/public-web/src/phase14ah-public-contact.js").read_text()
PUBLIC_CSS = (ROOT / "apps/public-web/src/phase14ah-public-contact.css").read_text()
PUBLIC_ENTRY = (ROOT / "apps/public-web/src/phase14c.js").read_text()
PUBLIC_MAIN = (ROOT / "apps/public-web/src/main.tsx").read_text()

assert 'import "./phase14ah-operational-settings.css";' in ADMIN_ENTRY
assert 'import("./phase14ah-public-contact.js")' in PUBLIC_ENTRY
assert 'import "./phase14ah-public-contact.css";' in PUBLIC_JS

# Settings Basic/Public controls share canonical field/toggle/save geometry.
for marker in [
    '.settings-section .settings-form-grid input',
    '.settings-section .toggle-row',
    '.settings-section .settings-save>.primary',
    'min-height:42px!important',
]:
    assert marker in ADMIN_CSS

# Pickup, Return and Correction operational editors share the same compact system.
for marker in [
    '.pickup-panel .pickup-lines',
    '.pickup-panel .return-lines',
    '.pickup-panel input:not([type="checkbox"])',
    '.pickup-panel>.primary.full',
    '.correction-card>.primary.full',
    '.correction-card>.form-title>button::before',
]:
    assert marker in ADMIN_CSS

# Public footer icons use real inline SVGs, not visible ligature words such as chat/call.
assert 'document.createElementNS(SVG_NS, "svg")' in PUBLIC_JS
assert 'phase14ah-footer-icon' in PUBLIC_JS
assert 'ICON_PATHS' in PUBLIC_JS
assert '.phase14ah-footer-contact::before' in PUBLIC_CSS
assert 'content:none!important' in PUBLIC_CSS
# React source owns the footer markup. WhatsApp now uses the central General Inquiry flow
# while Call remains a direct tel: action; the visual decorator supports both anchors and buttons.
assert 'className="footer-actions"' in PUBLIC_MAIN
assert 'data-contact-kind="whatsapp"' in PUBLIC_MAIN
assert 'prepareWhatsAppInquiry()' in PUBLIC_MAIN
assert '/api/public/whatsapp-inquiry' in PUBLIC_MAIN
assert 'tel:+${callDigits}' in PUBLIC_MAIN
assert '.footer-actions a.btn,.footer-actions button.btn' in PUBLIC_JS

# The footer decorator must not add data calls or polling.
assert 'fetch(' not in PUBLIC_JS
assert 'setInterval(' not in PUBLIC_JS
assert 'MutationObserver' in PUBLIC_JS

print("Phase 14AH remaining UI regression: PASS")
