#!/usr/bin/env python3
from pathlib import Path
import sqlite3
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"
WORKER = ROOT / "worker/src/phase14br-billing-v1.js"
SCREEN4 = ROOT / "worker/src/phase14be-operational-lifecycle.js"
PAYMENT_CLASSIFIER = ROOT / "worker/src/payment-classifier.js"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def need(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

worker = read(WORKER)
screen4 = read(SCREEN4)
payment_classifier = read(PAYMENT_CLASSIFIER)
billing_ui = read(ANDROID / "Screen11Billing.kt")
billing_vm = read(ANDROID / "Screen11BillingViewModel.kt")
billing_repo = read(ANDROID / "Screen11BillingRepository.kt")
billing_models = read(ANDROID / "Screen11BillingModels.kt")
billing_pdf = read(ANDROID / "Screen11BillingPdfExporter.kt")
billing_share = read(ANDROID / "Screen8PdfExporter.kt")
booking_details = read(ANDROID / "BookingDetailsScreen6.kt")
booking_actions = read(ANDROID / "BookingCardActions.kt")
summary_card = read(ANDROID / "BookingSummaryCard.kt")
components = read(ANDROID / "ui/components/Components.kt")
settings = read(ANDROID / "Screen10Settings.kt")
screen5 = read(ANDROID / "Screen5ItemManagement.kt")
reports = read(ANDROID / "Screen8Reports.kt")
customers = read(ANDROID / "CustomerScreenV4.kt")
root_ui = read(ANDROID / "AdminAppScreen4.kt")
rules = read(ROOT / "docs/PROJECT_RULES.md")
billing_doc = read(ROOT / "docs/BILLING_MODULE.md")
ui_rules = read(ROOT / "docs/GLOBAL_UI_RULES.md")
notes_migration = read(ROOT / "database/migrations/0026_billing_notes.sql")

# Billing lifecycle and server authority.
for token in [
    'COALESCE(b.confirmation_state,\'BOOKED\')<>\'RESERVED\'',
    'Confirm the Reserved Order before creating a Bill.',
    "COALESCE(c.address,'') AS customer_address",
    'const receivedAmount = advanceAmount + otherReceivedAmount;',
    'receivedAmount > 0 ? "PART_RECEIVED" : "PENDING"',
    'error:"BOOKING_RETURN_INCOMPLETE"',
    "async function bookingReturnComplete(",
    "SET status='FINAL',finalized_by_user_id=?",
    "Finalized Bill values are immutable.",
    "notes=?",
]:
    need(worker, token, "Billing Worker")

# Booking payment tabs use one authoritative, mutually-exclusive classifier.
need(worker, 'import { bookingPaymentStatusSql } from "./payment-classifier.js";', "Active Billing payment classifier import")
need(worker, 'const paymentCase = bookingPaymentStatusSql("b")', "Active Billing payment classifier reuse")
for token in [
    'import { bookingPaymentStatusSql } from "./payment-classifier.js";',
    'const paymentStatusSql = bookingPaymentStatusSql("b");',
    'view === "PAYMENT_PENDING"',
    'view === "PAYMENT_PART"',
    'view === "PAYMENT_FULL"',
    "(${paymentStatusSql})='PENDING'",
    "(${paymentStatusSql})='PART_RECEIVED'",
    "(${paymentStatusSql})='FULL_AMOUNT_RECEIVED'",
]:
    need(screen4, token, "Booking payment tabs")
for token in [
    "COALESCE(pb.received_amount,0) >= COALESCE(pb.net_amount,0)",
    "COALESCE(pb.received_amount,0) > 0",
    "COALESCE(pb.received_amount,0) < COALESCE(pb.net_amount,0)",
    "WHEN COALESCE(${b}.advance_amount,0)>0 THEN 'PART_RECEIVED'",
]:
    need(payment_classifier, token, "Booking payment classifier")
if "pb.payment_status" in payment_classifier:
    errors.append("Booking payment classifier must derive from authoritative totals, not stored payment_status.")

# Billing navigation/loading regression.
need(billing_vm, "BillingScreen.EDIT -> {", "Billing ViewModel edit back branch")
need(billing_vm, "screen = BillingScreen.LIST", "Billing ViewModel edit back returns to list")
need(billing_ui, "BillingScreen.EDIT -> onBack()", "Billing embedded edit back")
need(billing_ui, "private fun BillingCenteredLoading(", "Billing centered loading")
need(billing_ui, "contentAlignment = Alignment.Center", "Billing centered loading alignment")

# Final Bill direct customer WhatsApp share helper.
for token in [
    'fun shareDirectToCustomerWhatsApp(',
    '.setPackage("com.whatsapp")',
    '.putExtra("jid", "91$digits@s.whatsapp.net")',
]:
    need(billing_share, token, "Final Bill direct WhatsApp share helper")

# Final Bill editor hierarchy.
for token in [
    'LabeledSectionCard(title = "Booking & Customer")',
    'LabeledSectionCard(title = "Items (${state.lines.size})")',
    'LabeledSectionCard(title = "Pickup / Return Status")',
    'LabeledSectionCard(title = "Amount Summary")',
    'LabeledSectionCard(title = "Notes")',
    'label = "Advance Received"',
    'label = "Other Received"',
    'BillingAmountRow("Total Received", state.totalReceivedAmount',
    "BillingBalanceDue(state.balanceAmount)",
    'text = "Save Quotation"',
    'text = "Finalize Bill"',
    'text = "View"',
    'text = "Share"',
    'Screen8PdfExporter.shareDirectToCustomerWhatsApp(',
    'text = "Download"',
    'text = "Print"',
    "BillingCompactDatePicker(",
    'InfoValueRow(Icons.Rounded.Person, bill.customerName',
    'InfoValueRow(Icons.Rounded.Phone, bill.customerMobile',
    '"Bill Amount ₹${bill.netAmount} · Balance Due ₹${bill.balanceAmount}"',
    'InfoValueRow(Icons.Rounded.Person, order.customerName',
    'InfoValueRow(Icons.Rounded.ReceiptLong, order.bookingNo)',
]:
    need(billing_ui, token, "Billing Android UI")

if "RadioButton(" in billing_ui:
    errors.append("Billing payment status must be derived automatically; manual payment radio controls are not allowed.")
if 'text = "Save & Finalize"' in billing_ui:
    errors.append("Draft action label must be Finalize Bill, not Save & Finalize.")
if 'text = "Cancel Bill"' in billing_ui or 'text = "Full Amount Received"' in billing_ui:
    errors.append("Finalized Bill actions must remain View | Share | Download | Print only.")
detail_body = billing_ui[billing_ui.find("private fun BillingDetailScreen("):]
if 'LabeledSectionCard(title = "Pickup / Return Status")' in detail_body:
    errors.append("Bill Details must not contain Pickup / Return Status; PDF retains that lifecycle block.")
for token in ['text = "Order Preview"', "BookingOrderPreviewSheet(detail = detail"]:
    need(billing_ui, token, "Bill list Order Preview")

for token in [
    '"PAYMENT_ADVANCE" -> "Advance Received"',
    '"PAYMENT_CREATE" -> "Bill Draft Created"',
    '"PAYMENT_UPDATE" -> "Payment Updated"',
    '"PAYMENT_FINALIZE" -> "Bill Finalized"',
    'Payment Status ·',
]:
    need(booking_details, token, "Booking payment history")

for token in [
    'actionMap={CREATE:"PAYMENT_CREATE",UPDATE:"PAYMENT_UPDATE",FINALIZE:"PAYMENT_FINALIZE",CANCEL:"PAYMENT_CANCEL",DELETE:"PAYMENT_DELETE"}',
    "(SELECT MAX(pe.pickup_at) FROM pickup_events pe WHERE pe.booking_id=bk.id) AS pickup_at",
    "(SELECT MAX(re.return_at) FROM return_events re WHERE re.booking_id=bk.id) AS return_at",
    "END AS pickup_complete",
]:
    need(worker, token, "Billing Worker history/PDF timing")

if '"${bill.billDate} · Net ₹' in billing_ui:
    errors.append("Billing list must use formatted Bill Date and final Bill Amount / Balance Due terminology.")
lifecycle_block = billing_ui[billing_ui.find("private fun BillingLifecycleStatusBlock("):billing_ui.find("private fun billingPickupDone(")]
if "AppCard(" in lifecycle_block:
    errors.append("Pickup / Return status must not nest AppCard inside its labeled section.")

# Derived Android payment state and persistence.
for token in [
    'val otherReceivedText: String = "0"',
    "val otherReceivedAmount",
    "val totalReceivedAmount",
    'totalReceivedAmount >= netAmount -> "FULL_AMOUNT_RECEIVED"',
    'totalReceivedAmount > 0 -> "PART_RECEIVED"',
    "fun setOtherReceived",
    "fun setNotes",
    "state.totalReceivedAmount > state.netAmount",
]:
    need(billing_vm, token, "Billing ViewModel")
for token in [
    "otherReceivedAmount: Int",
    '.put("otherReceivedAmount", otherReceivedAmount.coerceAtLeast(0))',
    '.put("notes", notes.trim().take(500))',
]:
    need(billing_repo, token, "Billing repository")
for token in ["val notes: String = """, "val pickupDate: String = """, "val returnDate: String = """, "val pickupComplete: Boolean = false"]:
    need(billing_models, token, "Billing models")
need(notes_migration, "ALTER TABLE bills ADD COLUMN notes TEXT", "Billing notes migration")

# Quotation terminology and PDF lifecycle.
for token in [
    'label = "Add Quotation"',
    'AppFilterOption("DRAFT", "Quotation")',
    'text = "Create Quotation"',
    'text = "Save Quotation"',
    'title = if (bill.status == "DRAFT") "Quotation Header" else "Bill Header"',
    'if (bill.status == "DRAFT" || !bill.billNo.isNullOrBlank())',
    'title = if (bill.status == "DRAFT") "Quotation ${bill.bookingNo ?: ""}" else "Final Bill ${bill.billNo}"',
]:
    need(billing_ui, token, "Quotation UI workflow")
for token in [
    'val isQuotation = bill.status == "DRAFT"',
    'canvas.drawText(documentTitle,',
    'Zhagmag_Quotation_$safeNo.pdf',
]:
    need(billing_pdf, token, "Quotation PDF workflow")

# Final PDF hierarchy.
for token in [
    "PdfDocument()",
    'canvas.drawText(documentTitle,',
    '"Booking & Customer"',
    'Items (${detail.items.size})',
    '"Pickup / Return"',
    'amountRow("Item Total"',
    'amountRow("Discount"',
    'amountRow("Bill Amount"',
    'amountRow("Advance Received"',
    'amountRow("Other Received"',
    'amountRow("Total Received"',
    'canvas.drawText("Balance Due"',
    '"Notes"',
    'canvas.drawText("Thank You"',
    "eventTime(bill.pickupAt)",
    "eventTime(bill.returnAt)",
    "bill.pickupComplete",
]:
    need(billing_pdf, token, "Billing PDF")

# Reserved-only edit / Booked+ Bill.
for token in [
    '!detail.booking.confirmationState.equals("RESERVED", true)',
    'listOf("Details", "Pickup", "Return", "Bill", "History")',
    "AppCompactScrollableTabs(",
    "minTabWidth = 96.dp",
]:
    need(booking_details, token, "Booking Details lifecycle actions")
need(booking_actions, 'SoftActionSpec(Icons.Rounded.ReceiptLong, "Bill"', "Shared Bill quick action")

# Shared booking summary metadata.
for token in [
    'title = "Pickup"',
    'title = "Current"',
    'title = "Return"',
    "horizontalAlignment = Alignment.Start",
    "Icons.Rounded.CalendarMonth",
    "Icons.Rounded.EventNote",
]:
    need(summary_card, token, "Booking summary metadata")
if "00:00:00" in summary_card:
    errors.append("Booking summary metadata must not render a fake time row.")

# Run #79 tabs globally.
tab_family = components[components.find("fun AppCompactFixedTabs("):components.find("data class AppFilterOption")]
for token in [
    "TabRow(",
    "ScrollableTabRow(",
    "edgePadding = 0.dp",
    "minTabWidth: androidx.compose.ui.unit.Dp = 96.dp",
    "style = MaterialTheme.typography.labelMedium",
    "fontSize = 16.sp",
    "FontWeight.SemiBold",
    "FontWeight.Normal",
]:
    need(tab_family, token, "Run 79 tab family")
if ".horizontalScroll(rememberScrollState())" in tab_family or "minTabWidth: androidx.compose.ui.unit.Dp = 0.dp" in tab_family:
    errors.append("Run #79 tabs regressed to the later content-width implementation.")
need(customers, "AppCompactFixedTabs(", "Customers fixed tabs")
need(screen5, "AppCompactFixedTabs(", "Category & Items fixed tabs")
need(booking_details, "AppCompactScrollableTabs(", "Booking Details scrollable tabs")
need(booking_details, "minTabWidth = 96.dp", "Booking Details minimum tab width")
need(root_ui, "AppCompactScrollableTabs(", "Bookings scrollable tabs")
need(reports, "AppCompactScrollableTabs(", "Reports scrollable tabs")
if settings.count("AppCompactScrollableTabs(") < 2:
    errors.append("Settings main + WhatsApp Centre must both use Run #79 scrollable tabs.")

# Active docs must describe the approved contract.
for token in [
    "Reserved Orders have **no Bill**",
    "Payment Status is derived automatically",
    "**Bill Details** order is **Bill Header → Booking & Customer → Items (x) → Amount Summary → Notes → Actions**",
    "Finalized Bill monetary/item/customer/date/notes snapshot values are immutable",
]:
    need(rules, token, "Project Rules")
for token in [
    "Reserved Orders do not have Bills",
    "Advance Received + Other Received",
    "Finalized Actions are unboxed shared buttons in two rows: **View | Share** then **Download | Print**.",
    "Finalized values are stored as snapshots",
]:
    need(billing_doc, token, "Billing module")
for token in [
    "96dp minimum tab width",
    "Booking Details uses scrollable tabs",
    "icon + Label → icon + Status → icon + Date → icon + Day",
]:
    need(ui_rules, token, "Global UI rules")

# JS syntax and full migration chain.
for js_path, label in [(WORKER, "Billing Worker"), (SCREEN4, "Booking list Worker")]:
    syntax = subprocess.run(["node", "--check", str(js_path)], cwd=ROOT, text=True, capture_output=True)
    if syntax.returncode != 0:
        errors.append(label + " JS syntax failed: " + (syntax.stderr or syntax.stdout).strip())

conn = None
try:
    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    for migration in sorted((ROOT / "database/migrations").glob("*.sql")):
        conn.executescript(migration.read_text(encoding="utf-8"))
    bill_cols = {row[1] for row in conn.execute("PRAGMA table_info(bills)")}
    for col in ["booking_id","payment_status","received_amount","notes","customer_name_snapshot","booking_no_snapshot"]:
        if col not in bill_cols:
            errors.append("Bill column missing after migrations: " + col)
    if conn.execute("PRAGMA foreign_key_check").fetchall():
        errors.append("Billing migrations introduced foreign-key errors.")
except Exception as exc:
    errors.append("Billing migration chain failed: " + str(exc))
finally:
    if conn is not None:
        conn.close()

if errors:
    print("Phase 14BR Billing V1 regression FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("Phase 14BR Billing V1 regression PASS")
