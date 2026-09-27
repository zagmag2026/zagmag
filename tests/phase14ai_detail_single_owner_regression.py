from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
ENTRY = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
CSS = (ROOT / "apps/admin-web/src/phase14ai-detail-single-owner.css").read_text(encoding="utf-8")
OPS = (ROOT / "apps/admin-web/src/phase14t-ops.js").read_text(encoding="utf-8")
PACKAGE = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))


def require(condition, message):
    if not condition:
        raise AssertionError(message)


require('import "./phase14ai-detail-single-owner.css";' in ENTRY,
        "Phase14AI single-owner layer must load from the Admin entry")

# Phase14Q's generated detail action strip may still exist for legacy runtime
# compatibility, but it must never be a second visible action owner.
for marker in [
    '.phase14q-detail-bar',
    'display:none!important',
    'pointer-events:none!important',
]:
    require(marker in CSS, f"Missing duplicate detail-bar suppression: {marker}")

# Pickup/Return keep one sticky Save owner even when React replaces the source
# button during a render.
for marker in [
    '.pickup-panel.phase14t-op-panel > .primary.full:not(.phase14t-op-save)',
    '.pickup-panel > .phase14t-source-save',
    'button.hidden = true',
    'button.setAttribute("aria-hidden", "true")',
    'button.tabIndex = -1',
]:
    require(marker in CSS or marker in OPS, f"Missing single Save-owner guard: {marker}")

# Correction mode must not expose the parent mutation action at the same time.
for marker in [
    '.pickup-panel:has(.correction-card) > .primary.full',
    '.pickup-panel:has(.correction-card) > .pickup-actions',
    '.pickup-panel:has(.correction-card) > label',
]:
    require(marker in CSS, f"Missing correction single-action guard: {marker}")

# Mobile Pickup/Return rows become readable cards instead of squeezed tables.
for marker in [
    '.pickup-panel .pickup-line-head',
    '.pickup-panel .return-line-head',
    'content:"Give Now"',
    'content:"Return Now"',
    'content:"Condition Note"',
    'content:"Already Given"',
    '@media(max-width:760px)',
]:
    require(marker in CSS, f"Missing mobile operational-row treatment: {marker}")

# This release is presentation/runtime ownership only; no data calls were added.
require('fetch(' not in OPS and '/api/' not in OPS,
        "Operational Save owner must not add API calls")
require('url(' not in CSS.lower() and '/api/' not in CSS.lower(),
        "Phase14AI CSS must stay data-call free")

hardening = PACKAGE.get("scripts", {}).get("test:hardening", "")
require("phase14ai_detail_single_owner_regression.py" in hardening,
        "Phase14AI regression must run in test:hardening")

print("Phase 14AI detail/history single-owner + mobile operational rows regression: PASS")
