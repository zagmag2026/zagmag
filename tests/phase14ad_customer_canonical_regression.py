from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSS = (ROOT / "apps/admin-web/src/phase14ad-customer-canonical.css").read_text()
ENTRY = (ROOT / "apps/admin-web/src/phase14k.js").read_text()
MAIN = (ROOT / "apps/admin-web/src/main.tsx").read_text()


def require(value: bool, message: str) -> None:
    if not value:
        raise AssertionError(message)


# Final visual owner must load after all previous Customer UI layers.
require('import "./phase14ad-customer-canonical.css";' in ENTRY,
        "Phase 14AD canonical Customer CSS is not imported")
require(ENTRY.index('import "./phase14ad-customer-canonical.css";') >
        ENTRY.index('import "./phase14ac-visual-regression-fix.css";'),
        "Phase 14AD must load after Phase 14AC")

# React still owns real customer identity/data; this release must not replace it
# with generated or duplicated DOM data.
for token in [
    'className="customer-head"',
    'className="customer-avatar"',
    '<strong>{c.name}</strong>',
    '{c.mobile}</a>',
    'className="customer-stats"',
    'className="quick-actions"',
]:
    require(token in MAIN, f"Customer source identity token missing: {token}")

# Canonical one-row list and explicit identity/activity/actions geometry.
for token in [
    'grid-template-columns:minmax(0,1fr)!important;',
    'grid-template-areas:"identity activity actions"!important;',
    'grid-area:identity!important;',
    'grid-area:activity!important;',
    'grid-area:actions!important;',
    'content-visibility:visible!important;',
    'visibility:visible!important;',
    'opacity:1!important;',
    'min-height:40px!important;',
    'height:auto!important;',
    'max-height:none!important;',
]:
    require(token in CSS, f"Canonical Customer visibility/geometry rule missing: {token}")

# Fix1: Customer filters must remain in normal document flow on desktop/tablet.
# Phase 14P's global sticky toolbar used to cover the upper half of row 1 in
# Android Chrome desktop-site mode.
require('@media(min-width:761px)' in CSS,
        "Non-mobile Customer overlap guard missing")
for token in [
    '.phase14x-ui3-customer-list>.customer-filters',
    'position:relative!important;',
    'top:auto!important;',
    'box-shadow:none!important;',
    'padding-top:8px!important;',
    'min-height:74px!important;',
    'min-height:44px!important;',
    'white-space:normal!important;',
]:
    require(token in CSS, f"Customer first-row overlap protection missing: {token}")

# Fix2: On desktop/tablet More is the fourth peer action, not a second full-width
# row. Mobile intentionally keeps More on its own row for touch usability.
for token in [
    '.customer-card>.quick-actions>.phase14p-overflow{',
    'grid-column:auto!important;',
    'grid-row:auto!important;',
    'justify-self:stretch!important;',
    '.customer-card>.quick-actions>.phase14p-overflow>.phase14p-more-toggle{',
    'min-height:31px!important;',
]:
    require(token in CSS, f"Desktop Customer More alignment rule missing: {token}")
require('grid-column:1/-1!important;' in CSS,
        "Mobile Customer More row ownership must remain explicit")

# Android Chrome desktop-site commonly lands around a 980px layout viewport;
# keep that width in the canonical row instead of an old intermediate layout.
require('@media(min-width:761px) and (max-width:1100px)' in CSS,
        "Tablet/desktop-site Customer geometry guard missing")

# Mobile must preserve all information in the same order, not hide identity.
require('@media(max-width:760px)' in CSS, "Mobile Customer canonical rule missing")
require('grid-template-areas:"identity" "activity" "actions"!important;' in CSS,
        "Mobile Customer information order missing")
require('grid-template-columns:repeat(3,minmax(0,1fr))!important;' in CSS,
        "Compact 3-column Customer metrics/actions rule missing")

# Secondary management actions remain owned by More only.
require('.customer-card>.row-actions.phase14p-source-group' in CSS,
        "Customer source action row is not suppressed")
require('.customer-card .phase14p-overflow-source' in CSS,
        "Customer More source actions are not suppressed")

# This phase is deliberately CSS-only: no new observer/fetch/polling runtime.
require('fetch(' not in CSS and 'MutationObserver' not in CSS and 'setInterval' not in CSS,
        "Phase 14AD must remain visual-only")

print("Phase 14AD canonical Customer identity/layout + toolbar/action-row regression: PASS")
