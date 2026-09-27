from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"
WORKER = ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js"
MIGRATION = ROOT / "database" / "migrations" / "0016_report_presets.sql"
DOC = ROOT / "docs" / "ADMIN_SCREEN_08_REPORTS.md"
WRANGLER = ROOT / "worker" / "wrangler.toml"
STAGING = ROOT / "worker" / "wrangler.staging.toml.template"
MANIFEST = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "AndroidManifest.xml"

errors = []

def text(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

ui = text(ANDROID / "Screen8Reports.kt")
models = text(ANDROID / "Screen8ReportsModels.kt")
repo = text(ANDROID / "Screen8ReportsRepository.kt")
vm = text(ANDROID / "Screen8ReportsViewModel.kt")
pdf = text(ANDROID / "Screen8PdfExporter.kt")
root = text(ANDROID / "AdminAppScreen4.kt")
worker = text(WORKER)
migration = text(MIGRATION)
doc = text(DOC)
wrangler = text(WRANGLER)
staging = text(STAGING)
manifest = text(MANIFEST)

checks = [
    (ui, 'MainScreenDateRow(businessDate)', "Report Date/Day/Time row"),
    (ui, 'title = "Reports"', "Global Reports Back header title"),
    (models, 'Screen8Option("OVERVIEW", "Overview")', "Overview section"),
    (models, 'Screen8Option("OPERATIONS", "Operations")', "Operations section"),
    (models, 'Screen8Option("INVENTORY", "Inventory")', "Inventory section"),
    (models, 'Screen8Option("CUSTOMERS", "Customers")', "Customers section"),
    (models, 'Screen8Option("ACTIVITY", "Activity")', "Activity section"),
    (ui, 'LabeledSectionCard(title = "Report Options")', "Report Options labeled section"),
    (ui, '"Generate Report"', "Generate Report action"),
    (ui, '"Saved Presets"', "Saved Presets section"),
    (ui, 'text = "View"', "View PDF action"),
    (ui, '"Download"', "Download PDF action"),
    (ui, '"Share"', "Share PDF action"),
    (ui, 'screen8GroupingOptions', "dynamic grouping"),
    (ui, 'LaunchedEffect(state.page, state.rows.size)', "incremental +10 loading"),
    (models, '"BOOKINGS"', "Bookings report type"),
    (models, '"PICKUPS"', "Pickups report type"),
    (models, '"RETURNS"', "Returns report type"),
    (models, '"OVERDUE"', "Overdue report type"),
    (models, '"ITEM_HISTORY"', "Item history report type"),
    (models, '"CUSTOMER_HISTORY"', "Customer history report type"),
    (models, '"CATEGORY_STOCK"', "Category stock report type"),
    (models, '"STAFF_ACTIVITY"', "Staff activity report type"),
    (models, '"OPERATIONS_OVERVIEW"', "Operations overview type"),
    (models, '"AVAILABILITY"', "Availability report type"),
    (models, '"WHATSAPP_ACTIVITY"', "WhatsApp activity report type"),
    (models, '"AUDIT_REPORT"', "Audit report type"),
    (repo, '"/api/admin/report-generator/bootstrap"', "report bootstrap API"),
    (repo, '"/api/admin/report-generator"', "report page API"),
    (repo, '"export" to if (exportAll) "1" else null', "PDF full-export request"),
    (repo, '"/api/admin/report-presets"', "report preset API"),
    (vm, "fun loadMore()", "incremental report loading"),
    (vm, "fun requestExport()", "export request"),
    (vm, "fun savePreset(", "preset save"),
    (vm, '"THIS_MONTH"', "relative date preset"),
    (pdf, "PdfDocument()", "device-side PDF"),
    (pdf, "val landscape = report.columns.size >= 7", "automatic PDF orientation"),
    (pdf, "MediaStore.Downloads", "PDF download"),
    (pdf, "FileProvider.getUriForFile", "PDF view/share URI"),
    (root, "MoreDestination4.REPORTS", "Reports More destination"),
    (root, "user.hasAccess(StaffAccess.REPORTS)", "Reports permission visibility"),
    (root, "Screen8Reports(", "Reports routing"),
    (worker, 'import core from "./phase14bg-settings-whatsapp.js"', "cumulative Global Reports wrapper"),
    (worker, '"STAFF_ACTIVITY"', "Staff Activity backend"),
    (worker, '"/api/admin/report-generator"', "report generator backend"),
    (worker, "report-presets", "preset backend"),
    (worker, 'hasStaffPermission(user, "REPORTS")', "Reports permission guard"),
    (worker, "This report has more than 10,000 rows", "bounded export"),
    (migration, "CREATE TABLE IF NOT EXISTS report_presets", "preset schema"),
    (migration, "visibility TEXT NOT NULL DEFAULT 'MY'", "preset visibility"),
    (wrangler, 'main = "src/phase14bq-global-rental-reports.js"', "local Global Reports entry"),
    (staging, 'main = "src/phase14bq-global-rental-reports.js"', "staging Global Reports entry"),
    (manifest, '${applicationId}.fileprovider', "PDF FileProvider authority"),
    (doc, "Global Rental Reports", "Screen 8 documentation"),
    (doc, "Summary + Detailed Table", "PDF format documentation"),
]

for body, token, label in checks:
    if token not in body:
        errors.append(f"{label} missing: {token}")

for forbidden in ['eyebrow = "Reports"', 'subtitle = "Generate filtered reports and export PDF"', 'SecondaryButton("Back", onBack)', 'AppCard { EmptyState("No records found for the selected filters.") }']:
    if forbidden in ui:
        errors.append(f"Reports compact header restored removed UI: {forbidden}")

if "Screen 8 Reports — intentionally skipped" in doc:
    errors.append("Screen 8 documentation must not remain marked skipped.")

if errors:
    print("Screen 8 Reports regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Screen 8 Reports regression PASS")
