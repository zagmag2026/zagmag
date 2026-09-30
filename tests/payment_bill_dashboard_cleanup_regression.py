#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def need(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label}: missing {token}")

classifier = read(ROOT / "worker/src/payment-classifier.js")
screen4 = read(ROOT / "worker/src/phase14be-operational-lifecycle.js")
billing_worker = read(ROOT / "worker/src/phase14br-billing-v1.js")
dashboard_worker = screen4
dashboard_ui = read(ANDROID / "DashboardScreenV2.kt")
billing_ui = read(ANDROID / "Screen11Billing.kt")
billing_pdf = read(ANDROID / "Screen11BillingPdfExporter.kt")
billing_vm = read(ANDROID / "Screen11BillingViewModel.kt")

# Issue 1: one mutually-exclusive authoritative payment classifier everywhere that still
# exposes per-order payment state.
for token in [
    "export function bookingPaymentStatusSql",
    "WHEN ${activeBill} THEN",
    "WHEN ${full} THEN 'FULL_AMOUNT_RECEIVED'",
    "WHEN ${part} THEN 'PART_RECEIVED'",
    "ELSE 'PENDING'",
    "WHEN COALESCE(${b}.advance_amount,0)>0 THEN 'PART_RECEIVED'",
]:
    need(classifier, token, "payment classifier")
if "pb.payment_status" in classifier:
    errors.append("Root-cause regression: classifier must not trust stale stored bill.payment_status.")
for token in [
    'import { bookingPaymentStatusSql } from "./payment-classifier.js";',
    'const paymentStatusSql = bookingPaymentStatusSql("b");',
    "(${paymentStatusSql})='PENDING'",
    "(${paymentStatusSql})='PART_RECEIVED'",
    "(${paymentStatusSql})='FULL_AMOUNT_RECEIVED'",
    "${paymentStatusSql} AS payment_status",
]:
    need(screen4, token, "Booking tab/list classifier reuse")
need(billing_worker, 'import { bookingPaymentStatusSql } from "./payment-classifier.js";', "Active Billing classifier reuse")
need(billing_worker, 'const paymentCase = bookingPaymentStatusSql("b")', "Booking Details classifier reuse")
for legacy in ["activeBillPending", "activeBillPart", "activeBillFull"]:
    if legacy in screen4:
        errors.append(f"Overlapping payment predicate returned: {legacy}")

# Issues 4-5: no nested Balance Due card; one top badge; no duplicate bottom status row.
edit_start = billing_ui.find("private fun BillingEditScreen(")
detail_start = billing_ui.find("private fun BillingDetailScreen(")
edit_amount = billing_ui[edit_start:detail_start]
detail_body = billing_ui[detail_start:]
if "BillingBalanceDue(state.balanceAmount)" not in edit_amount:
    errors.append("Account/Quotation editor Amount Summary must retain the pre-badge Balance Due row.")
if 'BillingAmountRow("Bill Amount", state.netAmount, strong = true)' not in edit_amount:
    errors.append("Account/Quotation editor Amount Summary must retain the pre-badge Bill Amount row.")
if "BillingBalanceDue(bill.balanceAmount)" in detail_body:
    errors.append("Bill Details must replace the Balance Due row with the shared badge.")


detail_start = billing_ui.find("private fun BillingDetailScreen(")
detail_body = billing_ui[detail_start:]
amount_start = detail_body.find('LabeledSectionCard(title = "Amount Summary")')
notes_start = detail_body.find('LabeledSectionCard(title = "Notes")', amount_start)
detail_amount = detail_body[amount_start:notes_start]
summary_start = billing_ui.find("private fun BillingSummaryBadges(")
summary_end = billing_ui.find("private fun BillingPdfActionCard(", summary_start)
summary_badges = billing_ui[summary_start:summary_end]
for token in [
    "horizontalArrangement = Arrangement.End",
    'StatusBadge("Bill Amount ₹$billAmount", BadgeTone.INFO)',
    'StatusBadge("Balance Due ₹$balanceDue"',
    "billingPaymentLabel(paymentStatus)",
]:
    need(summary_badges, token, "Bill Details Amount Summary badges")
status_pos = summary_badges.find("billingPaymentLabel(paymentStatus)")
amount_pos = summary_badges.find('StatusBadge("Bill Amount ₹$billAmount"')
if status_pos < 0 or amount_pos < 0 or status_pos > amount_pos:
    errors.append("Bill Details badge order must be Bill Status + Payment Status first, then Bill Amount + Balance Due.")
for token in [
    'text = "Save Quotation"',
    'text = "Finalize Bill"',
    "SecondaryButton(",
    "PrimaryButton(",
    "Icons.Rounded.Save",
    "Icons.Rounded.CheckCircle",
]:
    need(billing_ui, token, "Quotation global actions")
edit_actions = billing_ui[billing_ui.find("private fun BillingEditScreen("):billing_ui.find("private fun BillingDetailScreen(")]
if "SoftActionButton(" in edit_actions:
    errors.append("Save Quotation / Finalize Bill must not use SoftActionButton.")
for token in [
    "state.balanceAmount > 0",
    "BILL_BALANCE_DUE",
    "COALESCE(received_amount,0) >= COALESCE(net_amount,0)",
]:
    need(billing_ui + billing_vm + billing_worker, token, "Billing balance guard")
if 'Text("Payment Status"' in detail_amount:
    errors.append("Bill Details Amount Summary must not duplicate Payment Status at the bottom.")
for label in ['"Pending Payment"', '"Part Payment"', '"Full Payment"']:
    need(billing_ui, label, "shared payment badge label")

# Issue 3: finalized details actions reuse shared SoftActionButton controls in one horizontal row.
actions_start = detail_body.find('if (bill.status == "DRAFT" || !bill.billNo.isNullOrBlank())')
actions_body = detail_body[actions_start:]
if 'LabeledSectionCard(title = "Actions")' in actions_body:
    errors.append("Finalized Bill Details actions must not be wrapped in an outer Actions card.")
if actions_body.count("SoftActionButton(") < 4:
    errors.append("Bill Details actions must expose four shared SoftActionButton controls.")
for token in ['label = "View"', 'label = "Share"', 'label = "Download"', 'label = "Print"', "SoftActionButton("]:
    need(actions_body, token, "Bill Details actions")
if "ResponsiveCompactPair(" in actions_body:
    errors.append("Bill Details actions must be a single horizontal shared-button row, not ResponsiveCompactPair rows.")

# Issue 2: finalized PDF fields/spacing and actual-time-only rule.
for token in [
    'drawSectionTitle(canvas, "Booking & Customer"',
    '"Items (${detail.items.size})"',
    'drawSectionTitle(canvas, "Pickup / Return"',
    'drawSectionTitle(canvas, "Amount Summary"',
    'lifecycleLine("Status"',
    'lifecycleLine("Date"',
    'lifecycleLine("Day"',
    'lifecycleLine("Time"',
    "eventTime(bill.pickupAt).takeIf { it.isNotBlank() }",
    "eventTime(bill.returnAt).takeIf { it.isNotBlank() }",
    'drawSectionTitle(canvas, "Notes"',
    'canvas.drawText("Thank You"',
]:
    need(billing_pdf, token, "Bill PDF")
if "00:00:00" in billing_pdf:
    errors.append("Bill PDF must never render fake time 00:00:00.")

# Issue 7 / Issue 6 continuation: active Dashboard is KPI-only and never reloads queue detail JSON.
for token in [
    "One bundled query owns all 34 fixed KPIs",
    "kpis: visibleKpis",
    "categoryInventory:",
    "missedPickups: []",
    "todayBookings: []",
    "todayPickups: []",
    "todayReturns: []",
    "overdueReturns: []",
]:
    need(dashboard_worker, token, "Active KPI Dashboard payload")
for forbidden in [
    "missed_pickups_json",
    "today_pickups_json",
    "today_returns_json",
    "overdue_returns_json",
]:
    if forbidden in dashboard_worker:
        errors.append(f"Dashboard must not load operational detail JSON: {forbidden}")

for token in [
    '"Today Overview"', '"Booking Status"', '"Payment & Billing"',
    '"Inventory"', '"Customers"', '"Category-wise Inventory"',
    "DashboardKpiCard(", "DashboardCategoryCard(", "maxWidth >= 720.dp",
]:
    need(dashboard_ui, token, "KPI Dashboard UI")
for forbidden in ["DashboardSection(", "DashboardBookingCard(", "BookingSummaryCard("]:
    if forbidden in dashboard_ui:
        errors.append(f"Dashboard must not render retired operational queue UI: {forbidden}")

# Syntax-check all changed Worker JS surfaces.
for rel in [
    "worker/src/payment-classifier.js",
    "worker/src/phase14be-operational-lifecycle.js",
    "worker/src/phase14bq-global-rental-reports.js",
    "worker/src/phase14br-billing-v1.js",
]:
    proc = subprocess.run(["node", "--check", str(ROOT / rel)], cwd=ROOT, text=True, capture_output=True)
    if proc.returncode != 0:
        errors.append(f"{rel} JS syntax failed: {(proc.stderr or proc.stdout).strip()}")

if errors:
    print("Payment/Bill/Dashboard cleanup regression FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("Payment/Bill/Dashboard cleanup regression PASS")
