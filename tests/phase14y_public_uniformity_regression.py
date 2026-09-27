from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
BOOT = (ROOT / "apps/public-web/src/phase14c.js").read_text(encoding="utf-8")
CSS = (ROOT / "apps/public-web/src/phase14y-public-uniform.css").read_text(encoding="utf-8")
MAIN = (ROOT / "apps/public-web/src/main.tsx").read_text(encoding="utf-8")
I18N = (ROOT / "apps/public-web/src/phase14ar-public-i18n.js").read_text(encoding="utf-8")
ADMIN = (ROOT / "apps/admin-web/src/phase14y-uniform.css").read_text(encoding="utf-8")
PACKAGE = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))


def require(condition, message):
    if not condition:
        raise AssertionError(message)


# Final visual layer must load after the React/public CSS graph. Keep this check
# formatting-agnostic so a valid multiline promise chain does not fail CI.
main_pos = BOOT.find('import("./main.tsx")')
uniform_pos = BOOT.find('import("./phase14y-public-uniform.css")')
stability_pos = BOOT.find('import("./phase14aa-public-stability.css")')
contact_pos = BOOT.find('import("./phase14ah-public-contact.js")')
require(main_pos >= 0, "Public boot must import main.tsx")
require(uniform_pos > main_pos, "Public uniform CSS must load after main.tsx")
require(stability_pos > uniform_pos, "Public stability CSS must load after Phase 14Y uniform CSS")
if contact_pos >= 0:
    require(contact_pos > stability_pos, "Public contact hardening must load after public stability CSS")
require("MutationObserver" not in CSS and "window.fetch" not in CSS and "/api/" not in CSS,
        "Public uniform layer must stay visual-only")

# Shared product-family tokens.
for token in ["#7d1747", "#eadfe4", "--public-ui-shell-r:14px", "--public-ui-control-r:9px"]:
    require(token in CSS, f"Missing public uniform token: {token}")
require("--ui-shell-r:14px" in ADMIN and "--ui-control-r:9px" in ADMIN,
        "Admin Phase 14Y reference tokens missing")

# Public keeps its customer-facing layout but must use the same Material icon family.
require("Material+Symbols+Rounded" in CSS and 'font-family:"Material Symbols Rounded"' in CSS,
        "Material Symbols Rounded must be the public icon family")
for glyph in ["category", "checkroom", "event_available", "contact_phone", "search", "visibility", "chevron_left", "chevron_right"]:
    require(f'content:"{glyph}"' in CSS, f"Missing Material icon mapping: {glyph}")
for legacy in ['content:"▦"', 'content:"▣"', 'content:"◫"', 'content:"☎"', 'content:"⌕"', 'content:"ⓘ"']:
    require(legacy not in CSS, f"Legacy decorative icon returned in final public layer: {legacy}")

# Core surfaces all participate in the final matching layer.
for selector in [".topbar", ".hero-card", ".category-chip", ".availability-section", ".item-card",
                 ".notice,.empty-state", ".pager", ".detail-modal", ".availability-badge"]:
    require(selector in CSS, f"Missing public uniform surface: {selector}")

# Mobile item cards retain Call and all base React copy is canonical English so\n# the runtime i18n layer can render one complete language consistently.\nrequire(".card-actions .icon-action{display:inline-flex!important" in CSS,\n        "Public mobile item Call action must remain visible")\nrequire(".card-actions .btn,.card-actions .icon-action{flex:1 1 30%!important" in CSS,\n        "Narrow public cards must share action width including Call")\nfor mixed in ["ઝગમગ કલેક્શન", "Collection જુઓ", "Availability તપાસો", "આ પસંદગી માટે કોઈ item મળ્યું નથી.", "વધુ વિગતો ઉપલબ્ધ નથી."]:\n    require(mixed not in MAIN, f"Mixed base-language public copy returned: {mixed}")\nfor token in ['"Message Preview":"મેસેજ પૂર્વદર્શન"', '"Open WhatsApp":"વોટ્સએપ ખોલો"', '"Cancel":"રદ કરો"']:\n    require(token in I18N, f"Public bilingual coverage missing: {token}")\nrequire("whatsapp-preview-modal" in MAIN, "WhatsApp preview modal must opt into translated modal heading copy")\n\n# The regression is part of the hardening chain.
hardening = PACKAGE.get("scripts", {}).get("test:hardening", "")
require("phase14y_public_uniformity_regression.py" in hardening,
        "Public uniformity regression must run in test:hardening")

print("Phase 14Y public/Admin visual uniformity regression: PASS")
