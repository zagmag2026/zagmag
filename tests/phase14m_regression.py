#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    css = (ROOT / "apps/admin-web/src/phase14m.css").read_text(encoding="utf-8")
    main_tsx = (ROOT / "apps/admin-web/src/main.tsx").read_text(encoding="utf-8")

    assert 'import "./phase14m.css"' in entry

    # Reports ERP surface.
    for marker in [".report-tabs", ".report-filters", ".report-row", ".report-metrics", ".report-actions"]:
        assert marker in css
    for marker in ["report-shell", "report-tabs", "report-filters", "report-row"]:
        assert marker in main_tsx

    # User management ERP rows + modal consistency.
    for marker in [".users-toolbar", ".users-grid", ".user-card", ".user-stats", ".user-actions", ".user-modal", ".password-modal"]:
        assert marker in css
    for marker in ["users-shell", "users-toolbar", "users-grid", "user-card"]:
        assert marker in main_tsx

    # Settings, templates and audit density.
    for marker in [".settings-tabs", ".settings-section", ".settings-form-grid", ".template-grid", ".audit-filters", ".audit-list", ".audit-diff"]:
        assert marker in css
    for marker in ["settings-shell", "settings-tabs", "settings-section", "audit-filters"]:
        assert marker in main_tsx

    # Shared operational drawer/detail UI stays present and gets ERP treatment.
    for marker in [".history-backdrop", ".history-panel", ".history-title", ".detail-meta", ".detail-items", ".timeline"]:
        assert marker in css

    # Mobile and tablet breakpoints are mandatory for this visual layer.
    assert "@media(max-width:850px)" in css
    assert "@media(max-width:650px)" in css
    assert "@media(max-width:430px)" in css

    print("Phase 14M ERP reports/users/settings/detail UI regression: PASS")


if __name__ == "__main__":
    main()
