#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14n.css").read_text(encoding="utf-8")
    main_tsx = (ROOT / "apps/admin-web/src/main.tsx").read_text(encoding="utf-8")

    assert 'import "./phase14n.css"' in entry

    # Category Master + field builder final ERP polish.
    for marker in [
        ".mini-stats", ".manage-grid", ".category-main", ".fields-zone",
        ".field-layout", ".field-row", ".field-form"
    ]:
        assert marker in css
    for marker in ["mini-stats", "manage-grid", "category-main", "fields-zone", "field-layout", "field-row", "field-form"]:
        assert marker in main_tsx

    # Login / Owner Setup remains functional while receiving compact UI treatment.
    for marker in [".auth-page", ".auth-brand", ".brand-mark", ".auth-card", ".link-button"]:
        assert marker in css
    for marker in ["auth-page", "auth-brand", "brand-mark", "auth-card", "Initial Owner Setup"]:
        assert marker in main_tsx

    # Final accessibility, overflow and mobile/IME hardening gates.
    assert ":focus-visible" in css
    assert "prefers-reduced-motion:reduce" in css
    assert "scroll-margin-bottom:150px" in css
    assert "env(safe-area-inset-bottom)" in css
    assert "font-size:16px!important" in css
    assert "overflow-wrap:anywhere" in css

    # Mandatory responsive breakpoints and mobile touch target treatment.
    assert "@media(max-width:900px)" in css
    assert "@media(max-width:650px)" in css
    assert "@media(max-width:430px)" in css
    assert "min-height:42px!important" in css

    print("Phase 14N final admin UI consistency + mobile polish regression: PASS")


if __name__ == "__main__":
    main()
