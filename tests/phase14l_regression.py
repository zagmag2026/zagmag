#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    shell = (ROOT / "apps/admin-web/src/phase14h.js").read_text(encoding="utf-8")
    phase14j = (ROOT / "apps/admin-web/src/phase14j.js").read_text(encoding="utf-8")
    phase14k = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    phase14l = (ROOT / "apps/admin-web/src/phase14l.css").read_text(encoding="utf-8")

    assert 'import "./phase14j.js"' in shell
    assert 'import "./phase14k.js"' in phase14j
    assert 'import "./phase14l.css"' in phase14k

    required = [
        "Phase 14L — ERP Admin UI Batch 1",
        ".topbar",
        ".tabs",
        ".stats",
        ".dashboard-row",
        ".customer-card",
        ".booking-card",
        ".item-editor>.info-card",
        ".pickup-toolbar",
        ".return-lines",
        "@media(max-width:760px)",
    ]
    for marker in required:
        assert marker in phase14l, marker

    # Guard the intended ERP density changes.
    assert "grid-template-columns:repeat(7,minmax(112px,1fr))" in phase14l
    assert "grid-template-columns:1fr!important" in phase14l
    assert "display:none!important" in phase14l

    print("Phase 14L global ERP admin UI regression: PASS")


if __name__ == "__main__":
    main()
