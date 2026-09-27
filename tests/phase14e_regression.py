#!/usr/bin/env python3
from pathlib import Path
import sqlite3,json

ROOT=Path(__file__).resolve().parents[1]

def worker_main(config: str) -> str:
    for raw in config.splitlines():
        line=raw.strip()
        if line.startswith("main = "):
            value=line.split("=",1)[1].strip()
            assert len(value)>=2 and value[0]=='"' and value[-1]=='"'
            return value[1:-1]
    raise AssertionError("Wrangler main entry is missing")

admin=(ROOT/'apps/admin-web/src/phase14e.js').read_text()
admin_css=(ROOT/'apps/admin-web/src/phase14e.css').read_text()
worker=(ROOT/'worker/src/phase14e.js').read_text()
strict_worker=(ROOT/'worker/src/phase14o.js').read_text()
shell_worker=(ROOT/'worker/src/phase14f.js').read_text()
admin_html=(ROOT/'apps/admin-web/index.html').read_text()
manifest=json.loads((ROOT/'project-manifest.json').read_text())
assert '/src/phase14e.js' in admin_html
assert '/api/admin/cloudinary/signature' in admin
assert 'https://api.cloudinary.com/v1_1/' in admin
assert 'No image selected. Item will still show with a clean placeholder.' in admin
assert 'cloudinaryAssets' in admin
assert 'authoritativeImageUrls' in admin
assert 'uploaderState' in admin and 'effectiveUrls' in admin
assert 'body.imageUrls = authoritativeImageUrls' in admin
assert 'itemMutation && response.ok' in admin
assert 'function labelText' not in admin
assert 'data-zhagmag-item-photos="1"' in admin
assert 'textarea.dataset.zhagmagItemPhotos = "1"' in admin
assert 'activeLabelObserver' in admin
assert 'if (!shell.isConnected)' in admin
assert 'mountShell(textarea, shell)' in admin
assert 'activeLabelObserver.observe(label, { childList: true })' in admin
assert 'setInterval(scan' not in admin
assert 'activeMainObserver.observe(main, { childList: true })' in admin
assert 'CLIENT_MAX_IMAGE_BYTES = 8 * 1024 * 1024' in admin
assert 'validateFileBeforeSigning(file)' in admin
assert 'validateSignaturePayload(signed)' in admin
assert 'phase14e-error' in admin
assert 'window.alert' not in admin
assert 'setReactTextarea(textarea, current.slice(0, 8))' in admin
assert '.phase14e-error' in admin_css
assert 'cloudinary_item_assets' in worker
assert 'destroyCloudinary' in worker
assert 'env.ASSETS.fetch' in shell_worker
assert 'x-zhagmag-shell' in shell_worker
assert 'no-store, no-cache, must-revalidate, max-age=0' in shell_worker
assert 'INVALID_RENTAL_DATE_RANGE' in strict_worker
assert manifest['version']=='0.14.5'
assert manifest['scope'].get('cloudinaryItemImageUpload') is True
assert manifest['scope'].get('workerStrictDateBoundary') is True
base=(ROOT/'worker'/'wrangler.toml').read_text()
staging=(ROOT/'worker'/'wrangler.staging.toml.template').read_text()
production=(ROOT/'worker'/'wrangler.production.toml.template').read_text()
configured_main=worker_main(base)
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
assert 'keep_vars = true' in base
worker_first='run_worker_first = ["/api/*", "/", "/index.html", "/admin", "/admin/", "/admin/index.html"]'
for text in [staging,production]:
    assert 'keep_vars = true' in text
    assert worker_first in text
conn=sqlite3.connect(':memory:')
for migration in sorted((ROOT/'database/migrations').glob('*.sql')):
    conn.executescript(migration.read_text())
tables={r[0] for r in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")}
assert 'cloudinary_item_assets' in tables
print('Phase 14E Fix11 React-safe Cloudinary upload lifecycle + staging/production Worker split regression: PASS')
