#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    js = (ROOT / "apps/admin-web/src/phase14w.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14w.css").read_text(encoding="utf-8")

    assert manifest["version"] == "0.14.5" and manifest["phase"] in {"14W", "14X"}
    assert manifest["scope"]["pricing"] is False
    assert manifest["scope"]["operationalCardEveryItemVisible"] is True
    for flag in [
        "dashboardCompactOperationalCards",
        "dashboardMobileStatusInline",
        "dashboardCompactIndividualItemRows",
        "dashboardCompactQuickActions",
        "zeroExtraPhase14wApiCalls",
    ]:
        assert manifest["scope"].get(flag) is True

    assert 'import "./phase14w.js";' in entry
    assert 'import "./phase14w.css";' in js
    assert "fetch(" not in js
    for marker in [
        ".dashboard-row-head",
        "grid-template-columns:minmax(0,1fr) auto",
        ".dashboard-row>.phase14d-item-grid",
        ".dashboard-row .phase14d-item-card",
        ".dashboard-row .phase14d-item-thumb",
        ".dashboard-actions",
        "repeat(3,minmax(0,1fr))",
        "@media(max-width:700px)",
    ]:
        assert marker in css
    assert "+N more" not in js and "+N more" not in css
    assert "tests/phase14w_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14W compact dashboard card UI regression: PASS")


if __name__ == "__main__":
    main()
