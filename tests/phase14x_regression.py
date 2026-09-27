#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def main():
    manifest = json.loads((ROOT / "project-manifest.json").read_text(encoding="utf-8"))
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    polish = (ROOT / "apps/admin-web/src/phase14s-polish.js").read_text(encoding="utf-8")
    js = (ROOT / "apps/admin-web/src/phase14x.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14x.css").read_text(encoding="utf-8")

    assert manifest["version"] == "0.14.5" and manifest["phase"] == "14X"
    assert manifest["scope"]["pricing"] is False
    assert manifest["scope"]["operationalCardEveryItemVisible"] is True
    for flag in [
        "adminSettledEmptyStateClarity",
        "adminLoadingShimmerSettledFix",
        "adminZeroResultPaginationSuppression",
        "adminQueueSpecificEmptyCopy",
        "dashboardKpiCardsActionable",
        "dashboardKpiCardIcons",
        "zeroExtraPhase14xApiCalls",
    ]:
        assert manifest["scope"].get(flag) is True

    assert 'import "./phase14x.js";' in entry
    assert 'import "./phase14x.css";' in js
    assert "characterData:true" in polish
    assert "fetch(" not in js and "api(" not in js
    for marker in [
        '"Categories": { glyph:',
        '"Booked Pending": { glyph:',
        '"Overdue Returns": { glyph:',
        'setAttribute("role", "button")',
        'setAttribute("tabindex", "0")',
        'document.querySelectorAll(".list-card,.dashboard-panel")',
        'pager.hidden = true',
        'phase14s-loading',
        'phase14u-stale-copy',
        'No returns due today.',
        'No matching return records found.',
        'No pickups due today.',
    ]:
        assert marker in js
    for marker in [
        '.stat.phase14x-kpi',
        'content:attr(data-phase14x-glyph)',
        '.empty.phase14x-empty-state',
        '.pager[hidden]',
    ]:
        assert marker in css
    assert "+N more" not in js and "+N more" not in css
    assert "tests/phase14x_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14X list-state clarity + actionable KPI regression: PASS")

if __name__ == "__main__":
    main()
