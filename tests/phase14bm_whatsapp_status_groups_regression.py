#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def require(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

registry = read(ROOT / "worker" / "src" / "whatsapp-template-registry.ts")
worker = read(ROOT / "worker" / "src" / "phase14bg-settings-whatsapp.js")
migration = read(ROOT / "database" / "migrations" / "0019_whatsapp_status_groups.sql")
models = read(ANDROID / "data" / "CustomerScreenModels.kt")
repo = read(ANDROID / "data" / "CustomerScreen3Repository.kt")
vm = read(ANDROID / "CustomerScreen3ViewModel.kt")
shared = read(ANDROID / "CustomerSharedUi.kt")
root = read(ANDROID / "AdminAppScreen4.kt")
dashboard = read(ANDROID / "DashboardScreenV2.kt")
customers = read(ANDROID / "CustomerScreenV4.kt")
detail = read(ANDROID / "BookingDetailsScreen6.kt")
settings = read(ANDROID / "Screen10Settings.kt")

for token in [
    "WHATSAPP_STATUS_GROUPS", "WHATSAPP_TEMPLATES_BY_STATUS_GROUP",
    '"RESERVED"', '"CONFIRMED_BOOKED"', '"PART_PICKUP"', '"PICKED_UP"',
    '"PART_RETURN"', '"RETURNED"', '"OVERDUE"', '"CANCELLED"', '"OTHER"',
    '"Reservation Confirmation"', '"Reservation Reminder"', '"Reservation Expiry Reminder"',
    '"Booking Confirmation"', '"Booking Details"', '"Pickup Reminder"', '"Pickup Today"', '"Pickup Ready"',
    '"Partial Pickup Confirmation"', '"Remaining Pickup Reminder"', '"Remaining Pickup Today"', '"Remaining Pickup Ready"',
    '"Pickup Confirmation"', '"Return Reminder"', '"Return Today"', '"Return Date/Time Update"',
    '"Partial Return Confirmation"', '"Remaining Return Reminder"', '"Remaining Return Today"', '"Pending Return Reminder"',
    '"Return Confirmation"', '"Thank You"', '"Feedback / Review"',
    '"Overdue Reminder"', '"Urgent Reminder"', '"Follow-up Reminder"', '"Final Reminder"',
    '"Cancellation Confirmation"', '"Cancellation Details"',
    '"Shop Address"', '"Working Hours"', '"General Information"', '"Holiday / Shop Closed"', '"Contact Us"', '"Custom General Message"',
]:
    require(registry, token, "status-group registry")

for token in ["business_name:", "booking_id:", "items:", "status:", "matchAll(/(?:\\{\\{", "Unknown WhatsApp placeholder: {{"]:
    require(registry, token, "double-brace placeholder registry")

for token in [
    'const mode = text(url.searchParams.get("mode"), 30).toLowerCase()',
    'if (mode === "templates")', "resolveStatusGroup", "statusGroupLabel",
    "WHATSAPP_TEMPLATES_BY_STATUS_GROUP[statusGroup]", "renderedTemplates",
    "business_name: settings.shopName", "booking_id: booking?.booking_no", "items: readableItems", "status: friendlyStatus",
]:
    require(worker, token, "status-filtered WhatsApp worker")

for token in [
    "RESERVATION_REMINDER", "RESERVATION_EXPIRY_REMINDER", "BOOKING_DETAILS",
    "RETURN_DATE_TIME_UPDATE", "RETURN_THANK_YOU", "FEEDBACK_REVIEW",
    "OVERDUE_URGENT_REMINDER", "OVERDUE_FOLLOW_UP_REMINDER", "CANCELLATION_DETAILS",
    "SHOP_ADDRESS", "WORKING_HOURS", "HOLIDAY_SHOP_CLOSED", "CONTACT_US", "CUSTOM_GENERAL_MESSAGE",
]:
    require(migration, token, "status-group seed migration")

for body, token, label in [
    (models, "data class WhatsAppTemplateBundle", "Android template bundle"),
    (models, "data class WhatsAppTemplateOption", "Android template option"),
    (repo, '"mode" to "templates"', "Android template request"),
    (vm, "fun prepareWhatsAppTemplates(", "Android template loader"),
    (shared, "fun WhatsAppPreparingSheet()", "WhatsApp immediate loading UI"),
    (shared, "fun WhatsAppTemplateSelectionSheet(", "WhatsApp template selection UI"),
    (root, "bundle.templates.singleOrNull()", "single-template selection skip"),
    (root, "PendingWhatsAppTemplateList4", "multi-template root state"),
    (root, "WhatsAppTemplateSelectionSheet(", "root template chooser"),
    (dashboard, '"Category-wise Inventory"', "KPI-only Dashboard replaces operational WhatsApp queue actions"),
    (root, "composeWhatsApp(booking.customerId, booking.id, null)", "Booking current-status flow"),
    (customers, "onWhatsApp(customer.id, null, null)", "Customer Other-group flow"),
    (detail, "onCall = { confirmCall = true }", "Booking Details confirm-call flow"),
    (detail, "customerName = detail.booking.customerName", "Booking Details call customer name"),
    (shared, "icon = if (action == CustomerContactAction.CALL) Icons.Rounded.Call else Icons.Rounded.Phone", "shared call mobile icon"),
    (settings, '"RESERVATION_REMINDER" to "Reservation Reminder"', "Settings new linked actions"),
    (settings, 'val placeholderKey = raw.removePrefix("{").removeSuffix("}")', "Settings placeholder normalization"),
    (settings, 'val token = "{{$placeholderKey}}"', "Settings double-brace placeholder display"),
    (settings, 'placeholderSources[placeholderKey]', "Settings placeholder source display"),
    (settings, 'Screen10PlaceholderInsertDropdown(', "Settings placeholder dropdown"),
    (settings, 'Text("Insert Placeholder"', "Settings placeholder insert action"),
    (settings, 'LANGUAGE("Language Settings")', "Settings final language section"),
    (settings, 'FilterChip(', "Settings linked-action chips"),
]:
    require(body, token, label)

if "onThankYou" in detail or 'text = "Thank You"' in detail:
    errors.append("Booking Details must not contain the removed standalone Thank You action.")

call_pos = detail.find('SoftActionSpec(Icons.Rounded.Call, "Call"')
whatsapp_pos = detail.find('SoftActionSpec(Icons.Rounded.Chat, "WhatsApp"', call_pos)
if call_pos < 0 or whatsapp_pos < 0 or call_pos > whatsapp_pos:
    errors.append("Booking Details must keep Call left of WhatsApp.")

if "WhatsAppContext." in dashboard:
    errors.append("Dashboard generic WhatsApp button must not hard-code a status-specific template context.")
if "WhatsAppContext.GENERAL_INQUIRY" in customers:
    errors.append("Customer generic WhatsApp button must use the Other template group instead of one hard-coded message.")

for forbidden in ["price", "payment", "deposit", "balance"]:
    if forbidden in migration.lower():
        errors.append(f"Out-of-scope financial concept added to Phase 14BM migration: {forbidden}")

if errors:
    print("Phase 14BM WhatsApp status-group/contact regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14BM WhatsApp status-group/contact regression PASS")
