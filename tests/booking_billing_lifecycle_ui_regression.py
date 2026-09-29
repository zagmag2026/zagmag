#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")

components = read(ANDROID / "ui/components/Components.kt")
settings = read(ANDROID / "Screen10Settings.kt")
booking = read(ANDROID / "BookingWorkspaceScreen5.kt")
details = read(ANDROID / "BookingDetailsScreen6.kt")
actions = read(ANDROID / "BookingCardActions.kt")
root = read(ANDROID / "AdminAppScreen4.kt")
billing_ui = read(ANDROID / "Screen11Billing.kt")
billing_vm = read(ANDROID / "Screen11BillingViewModel.kt")
billing_repo = read(ANDROID / "Screen11BillingRepository.kt")
booking_vm = read(ANDROID / "BookingLifecycleViewModel.kt")
models = read(ANDROID / "data/BookingLifecycleModels.kt")
worker = read(ROOT / "worker/src/phase14br-billing-v1.js")
core = read(ROOT / "worker/src/index.ts")
migration = read(ROOT / "database/migrations/0024_booking_billing_lifecycle.sql")
finance_migration = read(ROOT / "database/migrations/0025_financial_history_payment_hardening.sql")
sequence_worker = read(ROOT / "worker/src/phase14al-sequential-weekday.js")
customer_worker = read(ROOT / "worker/src/phase14av-customers-screen3.js")
item_ui = read(ANDROID / "Screen5ItemManagement.kt")
billing_pdf = read(ANDROID / "Screen11BillingPdfExporter.kt")
ui_rules = read(ROOT / "docs/GLOBAL_UI_RULES.md")
billing_doc = read(ROOT / "docs/BILLING_MODULE.md")
booking_doc = read(ROOT / "docs/BOOKING_MODULE.md")

errors = []

def need(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label}: missing {token}")

# 1. Tabs: restore the validated Admin Android Run #79 shared tab family.
tab_family = components[components.find("fun AppCompactFixedTabs("):components.find("data class AppFilterOption")]
fixed = tab_family[tab_family.find("fun AppCompactFixedTabs("):tab_family.find("fun AppCompactScrollableTabs(")]
scroll = tab_family[tab_family.find("fun AppCompactScrollableTabs("):]
for token in ["TabRow(", "modifier = modifier.fillMaxWidth()", "Modifier.height(44.dp)", "style = MaterialTheme.typography.labelMedium", "fontSize = 16.sp", "FontWeight.SemiBold", "FontWeight.Normal"]:
    need(fixed, token, "Run 79 fixed tabs")
for token in ["ScrollableTabRow(", "edgePadding = 0.dp", "minTabWidth: androidx.compose.ui.unit.Dp = 96.dp", "Modifier.widthIn(min = minTabWidth).height(44.dp)", "style = MaterialTheme.typography.labelMedium", "fontSize = 16.sp", "FontWeight.SemiBold", "FontWeight.Normal"]:
    need(scroll, token, "Run 79 scrollable tabs")
if ".horizontalScroll(rememberScrollState())" in scroll or "minTabWidth: androidx.compose.ui.unit.Dp = 0.dp" in scroll:
    errors.append("Scrollable tabs must not regress to the post-Run-79 content-width implementation.")
need(settings, "AppCompactScrollableTabs(", "Settings Run 79 scrollable tabs")
need(details, "AppCompactScrollableTabs(", "Booking Details Run 79 scrollable tabs")
need(details, "minTabWidth = 96.dp", "Booking Details Run 79 minimum tab width")
need(read(ANDROID / "CustomerScreenV4.kt"), "AppCompactFixedTabs(", "Customer fixed tabs")
need(read(ANDROID / "Screen5ItemManagement.kt"), "AppCompactFixedTabs(", "Category & Items fixed tabs")
need(read(ANDROID / "Screen8Reports.kt"), "AppCompactScrollableTabs(", "Reports scrollable tabs")
need(root, "AppCompactScrollableTabs(", "Booking lifecycle scrollable tabs")

# 2-4. Numeric replacement + Advance presentation.
for token in [
    "effectiveSelectAllOnFocus = selectAllOnFocus",
    "effectiveKeyboardOptions.keyboardType == KeyboardType.Number",
    "TextRange(0, fieldValue.text.length)",
]:
    need(components, token, "global numeric select-all")
need(booking, 'label = "Advance Amount"', "Booking Advance label")
need(booking, 'placeholder = "Advance Amount"', "Booking Advance placeholder")
need(booking, "selectAllOnFocus = true", "Booking Advance select all")
if "Cash advance · default ₹0" in booking:
    errors.append("Booking Advance helper label must be removed")

# 5. Booking cancellation Advance settlement + linked Bill synchronization.
for token in [
    '"FULL_REFUND", "Full Refund"',
    '"PARTIAL_REFUND", "Partial Refund"',
    '"NO_REFUND", "No Refund"',
    'label = "Refund Amount"',
    "Advance ₹$advance · Refund ₹$refund · Retained",
    "viewModel.cancelBooking(cancelSettlement.ifBlank { null }, refund)",
]:
    need(details, token, "Advance settlement UI")
for token in [
    "advanceSettlementStatus",
    "advanceRefundAmount",
    "advance_settlement_status",
    "advance_refund_amount",
    "cancellation_reason='Order cancelled'",
    "SET status='CANCELLED',cancelled_by_user_id=?",
]:
    need(core, token, "Advance settlement backend")
for token in ["advance_refund_amount", "advance_settlement_status", "advance_settled_at", "advance_settled_by_user_id"]:
    need(migration, token, "Advance settlement migration")

# 6-7. Booking actions and Bill visibility.
overview = details[details.find("private fun BookingDetailOverview6("):details.find("private fun DetailItemSummaryRow6")]
order = [
    'SoftActionSpec(Icons.Rounded.Edit, "Edit"',
    'SoftActionSpec(Icons.Rounded.ReceiptLong, "Bill"',
    'SoftActionSpec(Icons.Rounded.Call, "Call"',
    'SoftActionSpec(Icons.Rounded.Chat, "WhatsApp"',
]
pos = [overview.find(token) for token in order]
if any(i < 0 for i in pos) or pos != sorted(pos):
    errors.append("Booking Details action order must be Edit | Bill | Call | WhatsApp")
need(overview, "minCellWidth = 48.dp", "one-row Booking Details actions")
need(overview, 'val editable = status == "RESERVED"', "Reserved-only Booking Details edit")
need(root, 'editable = booking.displayStatus.equals("RESERVED", true)', "Reserved-only Booking list edit")
need(overview, 'val showBill = status != "RESERVED" && (!cancelled || !booking.billId.isNullOrBlank() || booking.advanceAmount > 0)', "Reserved-no-Bill visibility rule")
need(actions, 'SoftActionSpec(Icons.Rounded.ReceiptLong, "Bill"', "Booking summary Bill action")
need(root, 'billVisible = !booking.displayStatus.equals("RESERVED", true) && (!booking.rawStatus.equals("CANCELLED", true) || !booking.billId.isNullOrBlank() || booking.advanceAmount > 0)', "Booking summary Reserved-no-Bill visibility")

# 8-10. No standalone creation; Select Order; Full Return Draft handoff.
if "fun openStandalone()" in billing_vm:
    errors.append("Standalone/direct Bill creation must remain removed")
for token in [
    "BillingScreen.SELECT_ORDER",
    "fun openOrderSelector()",
    "fun createDraftFromOrder(bookingId: String)",
    'title = "Select Order"',
    'text = "Create Quotation"',
    'placeholder = "Search Order ID, customer or mobile"',
]:
    need(billing_vm + billing_ui, token, "Order-linked Add Bill")
for token in [
    '"/api/admin/billing/eligible-orders"',
    '"/api/admin/billing/ensure-draft/$bookingId"',
]:
    need(billing_repo, token, "Order-linked Billing repository")
for token in [
    '"/api/admin/billing/eligible-orders"',
    "NOT EXISTS (SELECT 1 FROM bills x WHERE x.booking_id=b.id)",
    "if (!bookingId) return { error: \"Select an Order before creating a Bill.\" };",
    "ensureBookingDraft",
    'String(current?.status||"")!=="RETURNED"',
    "billingHandoffBookingId",
]:
    need(worker, token, "Order-linked Billing Worker")
for token in ["billingHandoffBookingId", "billingDraftError"]:
    need(models + booking_vm, token, "Full Return Billing handoff")
need(details, "LaunchedEffect(viewModel.billingHandoffBookingId)", "Full Return opens linked Billing")

# Read-only settlement surface for cancelled Order with Advance/no Bill.
for token in ['title = "Advance Settlement"', '"Advance Received"', '"Refund Amount"', '"Retained Amount"']:
    need(billing_ui, token, "Advance settlement history")
need(billing_vm, "BillingScreen.SETTLEMENT", "Advance settlement route")

# Active docs lock the same contracts.
for token in ["16sp / 20sp", "divides it **equally**", "select the entire current value", "placeholder is exactly **Advance Amount**"]:
    need(ui_rules, token, "Global UI rules")
for token in ["Order-linked", "Select Order", "Full Return Billing handoff", "Full Refund", "Partial Refund", "No Refund"]:
    need(billing_doc, token, "Billing rules")
for token in ["Advance settlement when cancelling before pickup", "Booking → Bill handoff", "daily and atomic", "Pickup | Current | Return"]:
    need(booking_doc, token, "Booking rules")

# 11. Rent badge, financial-history-safe deletion, sequential Orders, direct Bill target, payment radio and compact Billing UI.
need(item_ui, 'StatusBadge("Rent ₹${item.rentAmount}", BadgeTone.INFO)', "Item Rent badge")
for token in ["permanently_deleted_at", "received_amount", "ix_bills_received_amount"]:
    need(finance_migration, token, "financial-history/payment migration")
for token in [
    "financialHistoryPreserved: true",
    "billsPreserved: billCount",
    "booking_id=NULL",
    "permanently_deleted_at=?",
    "Customer permanently deleted. Billing history was preserved.",
]:
    need(customer_worker, token, "Customer financial-history-safe delete")
for token in [
    "UPDATE bill_items SET item_id=NULL WHERE item_id=?",
    "billRowsPreserved",
    "Item deleted. Billing history was preserved.",
]:
    need(core, token, "Item financial-history-safe delete")
for token in [
    "confirmation_state,advance_amount",
    "booking_daily_sequences",
    "'BK-' || REPLACE(?, '-', '') || '-' || printf('%03d', sequence_value)",
]:
    need(sequence_worker, token, "modern sequential Order number")
need(actions, "maxColumns = 5", "five Booking summary actions one row")
need(actions, "minCellWidth = 48.dp", "five Booking summary actions compact")
summary_card = read(ANDROID / "BookingSummaryCard.kt")
for token in [
    'title = "Pickup"',
    'title = "Current"',
    'title = "Return"',
    "bookingPickupStatus(displayStatus)",
    "bookingReturnStatus(displayStatus)",
    "bookingDate.ifBlank { pickupDate }",
    '"FULL_PICKUP", "PART_RETURN", "FULL_RETURN" -> "Done"',
]:
    need(summary_card, token, "Booking Pickup Current Return metadata")
if 'Text("00:00:00"' in summary_card or "Icons.Rounded.Schedule" in summary_card:
    errors.append("Booking Pickup/Current/Return metadata must not render time rows")
if 'label = "Current"' in read(ANDROID / "BookingSummaryCard.kt"):
    errors.append("lower Current lifecycle badge must remain removed")
for token in [
    "if (!initialBookingId.isNullOrBlank() && state.screen == BillingScreen.LIST)",
    'label = "Advance Received"',
    'label = "Other Received"',
    'BillingAmountRow("Total Received", state.totalReceivedAmount',
    "BillingBalanceDue(state.balanceAmount)",
    "billingPaymentLabel",
    'LabeledSectionCard(title = "Pickup / Return Status")',
]:
    need(billing_ui, token, "Billing final amount/status UI")
if "RadioButton(" in billing_ui or 'label = "Part Received"' in billing_ui:
    errors.append("Billing payment status must be derived automatically, not manually selected.")
for token in [
    'val otherReceivedText: String = "0"',
    "val otherReceivedAmount",
    "val totalReceivedAmount",
    'totalReceivedAmount >= netAmount -> "FULL_AMOUNT_RECEIVED"',
    'totalReceivedAmount > 0 -> "PART_RECEIVED"',
    "fun setOtherReceived",
]:
    need(billing_vm, token, "Billing derived payment state")
for token in [
    "otherReceivedAmount: Int",
    '.put("otherReceivedAmount", otherReceivedAmount.coerceAtLeast(0))',
    '.put("notes", notes.trim().take(500))',
]:
    need(billing_repo, token, "Billing received amount repository")
for token in [
    '"PART_RECEIVED"',
    "const receivedAmount = advanceAmount + otherReceivedAmount;",
    'receivedAmount > 0 ? "PART_RECEIVED" : "PENDING"',
    "billingRow(bill)",
    'Confirm the Reserved Order before creating a Bill.',
]:
    need(worker, token, "Billing derived payment Worker")
for token in [
    'amountRow("Advance Received", bill.advanceAmount)',
    'amountRow("Other Received", otherReceived)',
    'amountRow("Total Received", bill.receivedAmount',
    'canvas.drawText("Balance Due"',
]:
    need(billing_pdf, token, "Bill PDF received totals")


# 12. Five-step Booking flow, payment Booking tabs and inline fourth-position Bill tab.
for token in [
    'listOf("Customer", "Details", "Items", "Preview", "Payment")',
    "private fun BookingPreviewStep5(",
    "private fun BookingPaymentStep5(",
    'LabeledSectionCard(title = "Payment")',
]:
    need(booking, token, "five-step Booking flow")
preview_body = booking[booking.find("private fun BookingPreviewStep5("):booking.find("private fun BookingPaymentStep5(")]
if 'label = "Advance Amount"' in preview_body or "viewModel.reserve()" in preview_body:
    errors.append("Preview step must remain review-only; Advance/actions belong to Payment")
payment_body = booking[booking.find("private fun BookingPaymentStep5("):]
for token in ['label = "Advance Amount"', "viewModel.reserve()", "viewModel.createConfirmed()", "viewModel.directPickup()"]:
    need(payment_body, token, "Booking Payment step")

booking_list_vm = read(ANDROID / "BookingListViewModel4.kt")
for token in [
    'PAYMENT_PENDING("PAYMENT_PENDING", "Pending Payment")',
    'PAYMENT_PART("PAYMENT_PART", "Part Payment")',
    'PAYMENT_FULL("PAYMENT_FULL", "Full Payment")',
]:
    need(booking_list_vm, token, "Booking payment tabs")
screen4_worker = read(ROOT / "worker" / "src" / "phase14be-operational-lifecycle.js")
payment_classifier = read(ROOT / "worker" / "src" / "payment-classifier.js")
for token in [
    'import { bookingPaymentStatusSql } from "./payment-classifier.js";',
    'const paymentStatusSql = bookingPaymentStatusSql("b");',
    'view === "PAYMENT_PENDING"',
    'view === "PAYMENT_PART"',
    'view === "PAYMENT_FULL"',
    "(${paymentStatusSql})='PENDING'",
    "(${paymentStatusSql})='PART_RECEIVED'",
    "(${paymentStatusSql})='FULL_AMOUNT_RECEIVED'",
    "${paymentStatusSql} AS payment_status",
]:
    need(screen4_worker, token, "Booking payment tab backend")
for token in [
    "export function bookingPaymentStatusSql",
    "WHEN ${activeBill} THEN",
    "WHEN ${full} THEN 'FULL_AMOUNT_RECEIVED'",
    "WHEN ${part} THEN 'PART_RECEIVED'",
    "ELSE 'PENDING'",
    "WHEN COALESCE(${b}.advance_amount,0)>0 THEN 'PART_RECEIVED'",
]:
    need(payment_classifier, token, "authoritative payment classifier")
if "pb.payment_status" in payment_classifier:
    errors.append("Payment classifier must not trust stored bill.payment_status; authoritative totals must be mutually exclusive.")
for legacy in ["activeBillPending", "activeBillPart", "activeBillFull"]:
    if legacy in screen4_worker:
        errors.append(f"Booking payment tabs must not restore overlapping classifier: {legacy}")

for token in [
    'listOf("Details", "Pickup", "Return", "Bill", "History")',
    "private const val BILL6 = 3",
    "tab = BILL6",
    "Screen11Billing(",
    "embedded = true",
]:
    need(details, token, "inline Booking Bill tab")

# Bill opened from Booking Details must return to the same Booking Details tab
# when the Bill screen header Back action is used.
need(
    details,
    'if (tab == BILL6) tab = DETAIL6 else viewModel.backFromEditor()',
    "Booking Details Bill header back navigation",
)
for token in ["embedded: Boolean = false", '"billing-embedded-', "if (!embedded)", '"Generate Bill"']:
    need(billing_ui, token, "embedded Billing workspace")

# Shared Order card/payment badge, Order Preview and merged Payment History.
for token in [
    "paymentStatus: String? = null",
    "bookingPaymentLabel(paymentStatus)",
    "bookingCurrentColors(displayStatus)",
    "BookingOrderPreviewSheet(",
    "showAllItems = true",
]:
    need(summary_card, token, "shared payment-aware Order card")
for token in [
    "paymentStatus = booking.paymentStatus",
    "BookingSummaryCard(",
    '"PAYMENT_CREATE" -> "Bill Draft Created"',
    '"PAYMENT_UPDATE" -> "Payment Updated"',
    '"PAYMENT_FINALIZE" -> "Bill Finalized"',
    "paymentHistoryLines6(event)",
]:
    need(details, token, "Booking Details shared card/payment history")
for token in [
    'text = "Order Preview"',
    "onPreviewOrder = vm::openOrderPreview",
    "BookingOrderPreviewSheet(detail = detail",
]:
    need(billing_ui, token, "Billing shared Order Preview")
if billing_ui[billing_ui.find("private fun BillingDetailScreen("):].count('LabeledSectionCard(title = "Pickup / Return Status")') > 0:
    errors.append("Bill Details must not render Pickup / Return Status; that data belongs in the Bill PDF.")

# 13. Edit Booking save, compact Bill UI/detail parity and full-return-only finalization.
need(
    core,
    "SELECT ?,?,?,?,0,0 WHERE EXISTS (SELECT 1 FROM bookings WHERE id=? AND updated_at=?)",
    "Edit Booking item reinsert SQL arity",
)
if "SELECT ?,?,?,?,?,0,0 WHERE EXISTS (SELECT 1 FROM bookings WHERE id=? AND updated_at=?)" in core:
    errors.append("Edit Booking must not regress to seven values for six booking_items columns")

for token in [
    'title = if (bill.status == "DRAFT") "Quotation Header" else "Bill Header"',
    'LabeledSectionCard(title = "Booking & Customer")',
    'LabeledSectionCard(title = "Items (${state.lines.size})")',
    'LabeledSectionCard(title = "Pickup / Return Status")',
    'LabeledSectionCard(title = "Amount Summary")',
    'LabeledSectionCard(title = "Notes")',
    'LabeledSectionCard(title = "Actions")',
    "imageUrl = line.imageUrl",
    'label = "Qty"',
    'label = "Rent Rate"',
    'label = "Discount"',
    'label = "Advance Received"',
    'label = "Other Received"',
    'text = "Save Quotation"',
    'text = "Finalize Bill"',
    'text = "Print"',
]:
    need(billing_ui, token, "final compact Bill hierarchy")

for token in [
    "async function bookingReturnComplete(",
    'error:"BOOKING_RETURN_INCOMPLETE"',
    "pickup_pending",
    "return_pending",
]:
    need(worker, token, "Worker full-return finalize guard")

for token in [
    "returnComplete",
    "state.bootstrap?.booking?.returnComplete != true",
    "!bill.returnComplete",
    'Bill can be finalized only after all picked-up items are fully returned.',
]:
    need(billing_vm, token, "Android full-return finalize guard")

for token in [
    "Reserved Orders do not have Bills",
    "saved before Return completion",
    "authoritative 100% Return completion",
    "Payment Status is derived automatically",
    "Finalized values are stored as snapshots",
]:
    need(billing_doc, token, "Billing final lifecycle rules")

if errors:
    print("Booking/Billing lifecycle + UI consistency regression FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

print("Booking/Billing lifecycle + UI consistency regression PASS")
