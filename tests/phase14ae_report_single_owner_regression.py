from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PHASE14G = (ROOT / "apps/admin-web/src/phase14g.js").read_text()
PHASE14D = (ROOT / "apps/admin-web/src/phase14d.js").read_text()


def require(value: bool, message: str) -> None:
    if not value:
        raise AssertionError(message)


# Reports must have exactly one visual-item renderer owner. Phase14G remains
# responsible for dashboard/booking/pickup/history direct item decoration, but
# must never create report-row item grids again.
require("function renderReport(" not in PHASE14G,
        "Legacy Phase14G report renderer returned")
require('document.querySelectorAll(".report-row").forEach(renderReport)' not in PHASE14G,
        "Phase14G still scans report rows")
require('node.closest(".report-row")' not in PHASE14G,
        "Phase14G still contains report-specific rendering mode logic")

# Phase14D is the canonical report visual-item owner.
require('document.querySelectorAll(".report-row").forEach(node=>{' in PHASE14D,
        "Canonical Phase14D report renderer missing")
require('renderBookingItems(primary,row' in PHASE14D,
        "Phase14D report rows no longer use canonical renderBookingItems")
require('data-phase14d-visual' in PHASE14D,
        "Phase14D visual-grid ownership marker missing")
require('uniqueLines(lines)' in PHASE14D,
        "Phase14D report line dedupe safeguard missing")

# The legacy direct-item class may remain for non-report cards, but no report
# function may append that class into report-primary.
report_section = PHASE14G[PHASE14G.find("function scan()"):] if "function scan()" in PHASE14G else ""
require("phase14g-direct-items" not in report_section,
        "Report scan can still create Phase14G direct item grids")

print("Phase 14AE Reports single visual-item owner regression: PASS")
