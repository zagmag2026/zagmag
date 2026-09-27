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
balance_start = billing_ui.find("private fun BillingBalanceDue")
balance_end = billing_ui.find("private fun billingDisplayDay", balance_start)
balance_body = billing_ui[balance_start:balance_end]
need(balance_body, 'Text("Balance Due"', "Balance Due row")
if "AppCard(" in balance_body:
    errors.append("Balance Due must be a normal emphasized row, not an inner card.")

detail_start = billing_ui.find("private fun BillingDetailScreen(")
detail_body = billing_ui[detail_start:]
amount_start = detail_body.find('LabeledSectionCard(title = "Amount Summary")')
notes_start = detail_body.find('LabeledSectionCard(title = "Notes")', amount_start)
detail_amount = detail_body[amount_start:notes_start]
for token in [
    "horizontalArrangement = Arrangement.End",
    "StatusBadge(billingPaymentLabel(bill.paymentStatus)",
    'BillingAmountRow("Total Received", bill.receivedAmount, strong = true)',
    "AppItemListDivider()",
    "BillingBalanceDue(bill.balanceAmount)",
]:
    need(detail_amount, token, "Bill Details Amount Summary")
if 'Text("Payment Status"' in detail_amount:
    errors.append("Bill Details Amount Summary must not duplicate Payment Status at the bottom.")
for label in ['"Pending Payment"', '"Part Payment"', '"Full Payment"']:
    need(billing_ui, label, "shared payment badge label")

# Issue 3: finalized details actions are unboxed and remain exactly the two shared rows.
actions_start = detail_body.find("if (!bill.billNo.isNullOrBlank())")
actions_body = detail_body[actions_start:]
if 'LabeledSectionCard(title = "Actions")' in actions_body:
    errors.append("Finalized Bill Details actions must not be wrapped in an outer Actions card.")
for token in ['text = "View"', 'text = "Share"', 'text = "Download"', 'text = "Print"', "ResponsiveCompactPair("]:
    need(actions_body, token, "Bill Details actions")

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
