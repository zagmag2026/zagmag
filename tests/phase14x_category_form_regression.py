#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def main():
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14x-category-form.css").read_text(encoding="utf-8")

    assert manifest["version"] == "0.14.5" and manifest["phase"] == "14X"
    assert manifest["scope"]["pricing"] is False
    assert manifest["scope"]["operationalCardEveryItemVisible"] is True
    assert 'import "./phase14x-category-form.css";' in entry
    for marker in [
        '.manage-grid>.form-card{padding:13px!important',
        '.manage-grid>.form-card>.form-grid{grid-template-columns:',
        '.manage-grid>.form-card input{min-height:39px!important',
        '.manage-grid>.form-card>.switch-row .check',
        '.manage-grid>.form-card>.primary.full',
        '@media(max-width:650px)',
        '@media(max-width:430px)',
    ]:
        assert marker in css
    assert "fetch(" not in css and "api(" not in css
    assert "+N more" not in css
    assert "tests/phase14x_category_form_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14X Fix1 New Category/Add Item form parity regression: PASS")

if __name__ == "__main__":
    main()
