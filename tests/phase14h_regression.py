#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]


def worker_main(config: str) -> str:
    for raw in config.splitlines():
        line=raw.strip()
        if line.startswith("main = "):
            value=line.split("=",1)[1].strip()
            assert len(value)>=2 and value[0]=='"' and value[-1]=='"'
            return value[1:-1]
    raise AssertionError("Wrangler main entry is missing")


public_html=(ROOT/'apps/public-web/index.html').read_text()
public_main=(ROOT/'apps/public-web/src/main.tsx').read_text()
public_boot=(ROOT/'apps/public-web/src/phase14c.js').read_text()
public_css=(ROOT/'apps/public-web/src/phase14h.css').read_text()
carousel_css=(ROOT/'apps/public-web/src/phase14i.css').read_text()
admin_shell=(ROOT/'apps/admin-web/src/phase14h.js').read_text()
admin_ui=(ROOT/'apps/admin-web/src/phase14j.js').read_text()
admin_ui_css=(ROOT/'apps/admin-web/src/phase14j.css').read_text()
admin_icon_runtime=(ROOT/'apps/admin-web/src/phase14x-icon-runtime.js').read_text()
strict_worker=(ROOT/'worker/src/phase14o.js').read_text()
smoke=(ROOT/'scripts/smoke-staging.mjs').read_text()
local=(ROOT/'worker/wrangler.toml').read_text()
staging=(ROOT/'worker/wrangler.staging.toml.template').read_text()
production=(ROOT/'worker/wrangler.production.toml.template').read_text()
assert '/src/phase14c.js' in public_html
assert '/src/phase14h.js' not in public_html
assert '0.14.5-public-boot-r4' in public_html
assert 'void import("./main.tsx")' in public_boot
assert 'import "./phase14h.css"' in public_main
assert 'import "./phase14i.css"' in public_main
assert 'MutationObserver' not in public_main and '.remove()' not in public_main
assert 'CLICK_GUARD_MS=700' in public_main and 'SUBMIT_GUARD_MS=1000' in public_main
assert 'No additional details are available.' in public_main
assert 'function CardImageCarousel' in public_main and 'function DetailImageCarousel' in public_main
assert 'setInterval' in public_main and '4800' in public_main
assert 'onTouchStart' in public_main and 'onTouchEnd' in public_main
assert 'public-card-slide-dots' in public_main and 'public-detail-arrow' in public_main
assert 'React-owned only' in carousel_css and 'public-detail-dot' in carousel_css
assert 'Phase 14J global UI standard' in public_css and 'Icon + Name' in public_css
assert 'import "./phase14j.js"' in admin_shell
assert 'CH-001' in admin_ui and 'Item code' in admin_ui
assert 'Red Mirror Choli' in admin_ui and 'Item name' in admin_ui
assert 'Choli' in admin_ui and 'Category name' in admin_ui
assert 'return_reminder' in admin_ui and 'Template key' in admin_ui
assert 'MENU_GLYPHS' not in admin_ui and 'dataset.uiGlyph' not in admin_ui
assert 'single-owner, idempotent Admin Material icon runtime' in admin_icon_runtime
assert 'setDataset(element, "uiGlyph", icon)' in admin_icon_runtime
assert 'data-ui-icon="1"' in admin_ui_css and 'ui-clean-hidden' in admin_ui_css
configured_main=worker_main(local)
staging_main=worker_main(staging)
production_main=worker_main(production)
assert configured_main==staging_main
assert production_main=='src/phase14at-booking-lifecycle.js'
assert configured_main.startswith('src/') and configured_main.endswith('.js')
assert (ROOT/'worker'/configured_main).is_file()
assert (ROOT/'worker'/production_main).is_file()
configured_worker=(ROOT/'worker'/configured_main).read_text()
assert 'import core from "' in configured_worker
assert "core.fetch(request,env,ctx)" in configured_worker.replace(" ", "")
assert 'INVALID_RENTAL_DATE_RANGE' in strict_worker
assert '0.14.5-public-boot-r4' in smoke and ('Public shell + revision' in smoke or 'Public shell + exact deployed assets' in smoke) and 'Public browser render' in smoke and '--dump-dom' in smoke
print('Phase 14O safe React UI + slideshow + UI standard staging/production split regression: PASS')
