#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    js = (ROOT / "apps/admin-web/src/phase14x-category-drawer.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14x-category-drawer.css").read_text(encoding="utf-8")
    icon_runtime = (ROOT / "apps/admin-web/src/phase14x-icon-runtime.js").read_text(encoding="utf-8")
    icon_css = (ROOT / "apps/admin-web/src/phase14x-global-icons.css").read_text(encoding="utf-8")
    phase14k = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")

    assert manifest["version"] == "0.14.5" and manifest["phase"] == "14X"
    assert manifest["scope"]["pricing"] is False
    assert manifest["scope"]["operationalCardEveryItemVisible"] is True
    assert 'import "./phase14x-category-drawer.js";' in entry
    assert 'import "./phase14x-category-drawer.css";' in js
    assert 'import "./phase14x-icon-runtime.js";' in phase14k
    assert 'import "./phase14x-global-icons.css";' in icon_runtime
    assert "fetch(" not in icon_runtime and "api(" not in icon_runtime
    assert "fetch(" not in js and "api(" not in js

    for marker in [
        'add.textContent = "Add Category"',
        'document.body.classList.add("phase14x-category-drawer-open")',
        'window.confirm("Discard unsaved category changes?")',
        'CATEGORY_ICONS',
        'setCategoryIcon(add, "add")',
        'setCategoryIcon(refresh, "refresh")',
        'setCategoryIcon(button, "edit")',
        'setCategoryIcon(button, "delete")',
        'ensureSameLayerBackdrop(grid)',
        'event.target === grid',
    ]:
        assert marker in js

    assert 'document.body.appendChild(backdrop)' not in js
    assert 'backdrop-filter' not in css
    for marker in [
        'body:not(.phase14x-category-drawer-open) .manage-grid{display:none!important}',
        'z-index:2147483000!important',
        'background:rgba(35,17,27,.42)!important',
        'body.phase14x-category-drawer-open .manage-grid>.form-card{position:absolute!important',
        '.phase14x-category-drawer-backdrop{display:none!important}',
        '.phase14x-category-icon',
        'opacity:1!important',
        'visibility:visible!important',
        'width:min(500px,100vw)',
    ]:
        assert marker in css

    for marker in ["calendar_month", "assignment_return", "add_circle", "delete", "edit", "tune", "inventory_2", "warning"]:
        assert marker in icon_runtime
    assert "Material Symbols Rounded" in icon_css and "phase14x-category-toolbar" in icon_css
    assert "tests/phase14x_category_drawer_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14X Fix6 category drawer + global icon display regression: PASS")


if __name__ == "__main__":
    main()
