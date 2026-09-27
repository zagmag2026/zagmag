#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    runtime = (ROOT / "apps/admin-web/src/phase14x-operational-status.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14x-operational-status.css").read_text(encoding="utf-8")

    assert 'import "./phase14x-operational-status.js";' in entry
    assert 'import "./phase14x-operational-status.css";' in runtime
    assert "fetch(" not in runtime and "api(" not in runtime
    assert 'timeZone: "Asia/Kolkata"' in runtime
    assert '"MISSED PICKUP' in runtime
    assert '"TODAY PICKUP"' in runtime
    assert '"PICKUP COMPLETED"' in runtime
    assert 'operationalTone' in runtime
    assert 'MutationObserver(schedule)' in runtime
    assert 'queueMicrotask' in runtime

    for selector in [
        ".booking-card",
        ".pickup-card",
        ".dashboard-row",
        ".history-list article",
        ".report-row",
    ]:
        assert selector in css

    for tone in ["info", "danger", "warning", "success", "neutral", "accent"]:
        assert f'[data-operational-tone="{tone}"]' in css

    assert 'tests/phase14x_operational_status_regression.py' in package["scripts"]["test:hardening"]
    print("Phase 14X Fix9 operational semantic card status regression: PASS")


if __name__ == "__main__":
    main()
