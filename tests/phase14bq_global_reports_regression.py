#!/usr/bin/env python3
from pathlib import Path
import sqlite3
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"
WORKER = ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js"
MIGRATION = ROOT / "database" / "migrations" / "0020_global_reports_whatsapp_activity.sql"
DOC = ROOT / "docs" / "ADMIN_SCREEN_08_REPORTS.md"
RULES = ROOT / "docs" / "PROJECT_RULES.md"
WRANGLER = ROOT / "worker" / "wrangler.toml"
STAGING = ROOT / "worker" / "wrangler.staging.toml.template"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def need(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

models = read(ANDROID / "Screen8ReportsModels.kt")
ui = read(ANDROID / "Screen8Reports.kt")
vm = read(ANDROID / "Screen8ReportsViewModel.kt")
repo = read(ANDROID / "Screen8ReportsRepository.kt")
pdf = read(ANDROID / "Screen8PdfExporter.kt")
worker = read(WORKER)
migration = read(MIGRATION)
doc = read(DOC)
rules = read(RULES)
wrangler = read(WRANGLER)
staging = read(STAGING)

for section in ["OVERVIEW", "OPERATIONS", "INVENTORY", "CUSTOMERS", "ACTIVITY"]:
    need(models, f'Screen8Option("{section}"', "Global report section")

report_types = [
    "OPERATIONS_OVERVIEW",
    "BOOKINGS", "UPCOMING_BOOKINGS", "CANCELLED_BOOKINGS", "PICKUPS", "MISSED_PICKUPS", "RETURNS", "OVERDUE",
    "AVAILABILITY", "CURRENTLY_OUT", "ITEM_UTILIZATION", "LOW_USE_ITEMS", "ITEM_HISTORY", "CATEGORY_STOCK",
    "CUSTOMER_HISTORY", "ACTIVE_RENTALS", "FREQUENT_CUSTOMERS", "NEW_RETURNING_CUSTOMERS", "CUSTOMER_EXCEPTIONS",
    "STAFF_ACTIVITY", "WHATSAPP_ACTIVITY", "AUDIT_REPORT", "EXCEPTIONS",
]
for report_type in report_types:
    need(models, f'"{report_type}"', "Android report catalog")
    need(worker, f'"{report_type}"', "Worker report catalog")

for token in [
    'title = "Reports"', "Screen8SectionTabs(", "AppCompactScrollableTabs(", "Screen8CustomFieldFilter(",
    "AppSingleSelectFilter(", "AppMultiSelectFilter(", "AppDatePickerField(",
    'config.datePreset == "CUSTOM"', '"Clear Filters"',
    "private fun SummaryCard8(", ".padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)",
    "MaterialTheme.typography.titleMedium", "FontWeight.Bold",
    'Screen8Option("1", "Yes")', 'Screen8Option("0", "No")',
    "screen8StatusOptions(type, bootstrap.statuses)", '"Generate Report"', '"Saved Presets"',
    'text = "View"', 'text = "Share"', 'text = "Download"', "LaunchedEffect(state.page, state.rows.size)",
]:
    need(ui, token, "Global Reports UI")

if "private fun Screen8Selector(" in ui or "DropdownMenu(" in ui:
    errors.append("Global Reports must not restore screen-local dropdown controls.")

result_header = ui[ui.find("private fun ReportResultHeader("):ui.find("@Composable\nprivate fun SummaryCard8")]
if result_header:
    view_pos = result_header.find('text = "View"')
    share_pos = result_header.find('text = "Share"')
    download_pos = result_header.find('text = "Download"')
    if not (0 <= view_pos < share_pos < download_pos):
        errors.append("Global Reports PDF actions must remain View -> Share -> Download.")
    download_block = result_header[download_pos:result_header.find("    }\n}", download_pos)] if download_pos >= 0 else ""
    if "modifier = Modifier.fillMaxWidth()" not in download_block:
        errors.append("Global Reports Download must remain full-width on row 2.")
    if "enabled = state.hasRows && !pdfBusy" not in download_block:
        errors.append("Global Reports Download must remain protected by the shared PDF busy state.")
else:
    errors.append("Global Reports result header is missing.")

for token in [
    'val type: String = "OPERATIONS_OVERVIEW"', "Screen8CategoryFieldOption", "customFilters: Map<String, String>",
    "screen8SupportsCustomFields", "screen8StatusOptions", '"MISSED_PICKUP" -> "Missed Pickup"',
    '"OVERDUE" -> "Overdue"', '"FULL_RETURN", "RETURNED" -> "Full Returned"',
]:
    need(models, token, "Global Reports model")

for token in [
    "fun setSection(", "fun setCustomFilter(", "if (state.config.type == \"OPERATIONS_OVERVIEW\"",
    "replaceConfig(", "distinctBy { it.values }", "resolvePresetDates(preset.config)",
    "screen8SupportsDates(config.type)", "customFilters.toSortedMap()",
]:
    need(vm, token, "Global Reports state")

for token in [
    "PdfDocument()", "val landscape = report.columns.size >= 7",
    "fun wrapText(", "fun formatDateLike(", "fun formatPdfValue(",
    "preferredColumnWidths", '"Applied Filters"', '"Summary"', '"Detailed Report"',
    "val summaryColumns = if (landscape) 4 else 3", "screen8StatusLabel(value)",
    "drawContinuationHeader(", "rowIndex % 2 == 1",
]:
    need(pdf, token, "Global Reports PDF presentation")

for token in [
    '"customFilters" to config.customFilters', '"/api/admin/report-generator/bootstrap"',
    '"/api/admin/report-generator"', '"/api/admin/report-presets"',
]:
    need(repo, token, "Global Reports repository")

for token in [
    'import core from "./phase14bg-settings-whatsapp.js"', 'hasStaffPermission(user, "REPORTS")',
    "bi.booked_qty-bi.closed_qty", "bi.given_qty>bi.returned_qty", '"MISSED_PICKUP"', '"OVERDUE"',
    "availabilityReport(", "utilizationReport(", "customerAggregateReport(", "customerExceptionsReport(",
    "staffActivityReport(", "whatsappActivityReport(", "auditReport(", "operationsOverview(",
    'CUSTOMER_HISTORY: {', '["pickup_by", "Given By"]', '["return_by", "Received By"]',
    "item_field_values", "customFilters", "whatsapp_activity_logs",
    'if (user.role !== "OWNER")', "This report has more than 10,000 rows",
    "ctx.waitUntil(recordWhatsAppActivity", "Reporting telemetry must never break the WhatsApp action itself.",
]:
    need(worker, token, "Global Reports Worker")

for forbidden in ["rent amount", "deposit", "discount", "late fee amount", "damage charge", "revenue", "profit"]:
    if forbidden in worker.lower() or forbidden in models.lower() or forbidden in ui.lower():
        errors.append(f"Pricing/payment concept leaked into Global Reports runtime: {forbidden}")

for runtime_body, label in [(worker, "Worker"), (models, "Models"), (ui, "UI")]:
    if "Choli" in runtime_body:
        errors.append(f"{label} must remain category-agnostic; hard-coded Choli found.")

for token in [
    "CREATE TABLE IF NOT EXISTS whatsapp_activity_logs",
    "outcome TEXT NOT NULL CHECK (outcome IN ('PREPARED','FAILED'))",
    "ix_whatsapp_activity_created",
    "ix_pickup_event_items_booking_item",
    "ix_return_event_items_booking_item",
]:
    need(migration, token, "WhatsApp activity migration")

if 'main = "src/phase14br-billing-v1.js"' not in wrangler:
    errors.append("Feature branch Wrangler must use Billing V1 wrapper over Global Reports.")
if 'main = "src/phase14br-billing-v1.js"' not in staging:
    errors.append("Feature branch staging Wrangler must use Billing V1 wrapper over Global Reports.")

for token in [
    "Global Rental Reports", "Overview", "Operations", "Inventory", "Customers", "Activity",
    "Missed Pickup", "Overdue Return", "Availability", "Currently Out", "Item Utilization",
    "WhatsApp Activity", "Audit Report", "dynamic custom", "Summary + Detailed Table",
]:
    need(doc, token, "Global Reports documentation")

need(rules, "Global Rental Reports", "Project Rules Global Reports authority")

if WORKER.exists():
    syntax = subprocess.run(
        ["node", "--check", str(WORKER)],
        cwd=ROOT,
        text=True,
        capture_output=True,
    )
    if syntax.returncode != 0:
        errors.append("Global Reports Worker JS syntax failed: " + (syntax.stderr or syntax.stdout).strip())

try:
    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    for path in sorted((ROOT / "database" / "migrations").glob("*.sql")):
        conn.executescript(path.read_text(encoding="utf-8"))
    columns = {row[1] for row in conn.execute("PRAGMA table_info(whatsapp_activity_logs)")}
    required_columns = {
        "id", "user_id", "customer_id", "booking_id", "item_id", "context",
        "status_group", "outcome", "template_count", "error_code", "error_message", "created_at",
    }
    if not required_columns.issubset(columns):
        errors.append("WhatsApp activity migration columns are incomplete.")
    if conn.execute("PRAGMA foreign_key_check").fetchall():
        errors.append("Global Reports migration introduced foreign-key errors.")
finally:
    try:
        conn.close()
    except Exception:
        pass

if errors:
    print("Phase 14BQ Global Rental Reports regression FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("Phase 14BQ Global Rental Reports regression PASS")
