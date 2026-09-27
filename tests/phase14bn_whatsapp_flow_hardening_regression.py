#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def need(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

root = read(ANDROID / "AdminAppScreen4.kt")
vm = read(ANDROID / "CustomerScreen3ViewModel.kt")
workspace = read(ANDROID / "BookingWorkspaceScreen5.kt")
detail = read(ANDROID / "BookingDetailsScreen6.kt")
customers = read(ANDROID / "CustomerScreenV4.kt")
worker = read(ROOT / "worker" / "src" / "phase14bg-settings-whatsapp.js")
registry = read(ROOT / "worker" / "src" / "whatsapp-template-registry.ts")
public = read(ROOT / "apps" / "public-web" / "src" / "main.tsx")
admin = read(ROOT / "apps" / "admin-web" / "src" / "main.tsx")
reports_worker = read(ROOT / "worker" / "src" / "phase14bd-screen8-reports.js")
package = json.loads(read(ROOT / "package.json") or "{}")

# Android: every generic operational WhatsApp tap must finish in chooser/preview/error.
for token in [
    "var whatsappError by remember", "PopupMessage(whatsappError, MessageTone.ERROR)",
    "customerViewModel.prepareWhatsAppTemplates(", "bundle.templates.singleOrNull()",
    "pendingWhatsAppTemplates = PendingWhatsAppTemplateList4(bundle, onReady)",
    'whatsappError = "No active WhatsApp template is available for this order status."',
    "onFailure = { message ->", "contactBusy = whatsappPreparing || customerViewModel.state.actionBusy",
]:
    need(root, token, "Android shared WhatsApp flow")

if "customerViewModel.composeWhatsApp(customerId, bookingId, linkedContext)" in root:
    errors.append("Android root generic WhatsApp flow must not bypass current-status template filtering.")

for token in [
    "onFailure: (String) -> Unit = {}", 'onFailure("Please wait for the current action to finish.")',
    "onFailure(message)", "state = state.copy(actionBusy = true, error = null, notice = null)",
]:
    need(vm, token, "Android WhatsApp failure callback")

need(workspace, "contactBusy: Boolean = false", "Booking workspace contact busy")
need(detail, "busy = viewModel.actionBusy || contactBusy", "Booking Details contact busy")
need(customers, "AppFeedbackHost(", "Customers shared visible WhatsApp feedback host")
need(customers, "errorMessage = contactLaunchError ?: state.error.takeIf", "Customers visible WhatsApp error")
if "busy = bookingViewModel.actionBusy || contactBusy" not in root:
    errors.append("Booking List must include shared customer/WhatsApp busy state.")

# Worker: one invalid template must be skipped without blocking valid siblings.
group_start = worker.find('if (mode === "templates")')
group_end = worker.find("const linkedAction = resolveAction", group_start)
group = worker[group_start:group_end] if group_start >= 0 and group_end > group_start else ""
for token in [
    "const invalidTemplates = []", "if (rendered.error || !rendered.message)",
    "invalidTemplates.push(", "continue;", "if (!renderedTemplates.length)",
    '"TEMPLATE_INVALID"', '"TEMPLATE_MISSING"',
]:
    need(group, token, "Worker status-group resilience")
invalid_pos = group.find("if (rendered.error || !rendered.message)")
continue_pos = group.find("continue;", invalid_pos)
if invalid_pos < 0 or continue_pos < 0:
    errors.append("Invalid status-group template must continue to the next template.")
elif "return apiJson" in group[invalid_pos:continue_pos]:
    errors.append("One invalid status-group template must not immediately fail the whole request.")

for token in [
    '"RESERVED"', '"CONFIRMED_BOOKED"', '"PART_PICKUP"', '"PICKED_UP"',
    '"PART_RETURN"', '"RETURNED"', '"OVERDUE"', '"CANCELLED"', '"OTHER"',
]:
    need(registry, token, "Status-group preservation")

# Public Website: no screen-local hard-coded item WhatsApp message.
for token in [
    "/api/public/whatsapp-inquiry", "prepareWhatsAppInquiry(", "whatsappPreview",
    "Message Preview", "Open WhatsApp", "whatsappError",
]:
    need(public, token, "Public central General Inquiry flow")
if "function inquiryUrl" in public or "Hello, મને" in public:
    errors.append("Public Website must not retain the screen-local hard-coded item WhatsApp message.")
need(worker, 'url.pathname === "/api/public/whatsapp-inquiry"', "Public central WhatsApp endpoint")
need(worker, '"GENERAL_INQUIRY"', "Public General Inquiry template")
need(worker, "JOIN item_related_items rel", "Booking WhatsApp related-item SQL")
need(worker, "FROM item_related_items rel", "Public Item Inquiry related-item SQL")
if "JOIN related_items rel" in worker or "FROM related_items rel" in worker:
    errors.append("WhatsApp related-item SQL must use authoritative item_related_items table.")
need(registry, 'GENERAL_INQUIRY: merge(SHOP, CUSTOMER, ITEMS, AVAILABILITY, ["pickup_date", "return_date"])', "General Inquiry item/date/availability placeholders")

# Phase 14BP: template correctness, reachability and placeholder data safety.
for token in [
    "function applicableTemplateSpecs(", 'pickupDate !== today', 'returnDate !== today',
    '"MISSED_PICKUP_REMINDER"', 'status === "CANCELLED" && confirmation === "RESERVED"',
    '"RESERVATION_CANCELLED"', 'statusGroup === "CONFIRMED_BOOKED"', '"BOOKING_UPDATED"',
    'statusGroup === "RETURNED"', '"BOOKING_COMPLETED"',
    "function referencedPlaceholders(", "const missingValues = referenced.filter",
    "Template placeholder value is unavailable",
    "website_title: settings.websiteTitle", "website_url: settings.websiteUrl",
    "customer_address: customer.address", "booking_notes: booking?.notes", "booked_by: booking?.booked_by",
    "availability_status: itemAvailabilityStatus",
]:
    need(worker, token, "WhatsApp audited correctness")

for token in [
    "website_title:", "website_url:", "customer_address:", "booking_notes:", "booked_by:",
    "availability_status:", "available_qty:", "const AVAILABILITY =",
]:
    need(registry, token, "Expanded placeholder registry")

public_start = worker.find("async function composePublicWhatsAppInquiry")
public_end = worker.find("export default", public_start)
public_block = worker[public_start:public_end] if public_start >= 0 and public_end > public_start else ""
for token in [
    "FROM item_related_items rel", "const relatedRows =", "related_items: relatedItems.join",
    "availability_status: availabilityStatus",
    'available_qty: settings.availabilityMode === "EXACT"',
]:
    need(public_block, token, "Public WhatsApp inquiry context data")
if 'related_items: ""' in public_block:
    errors.append("Public WhatsApp inquiry must not hard-code related_items blank when an item is selected.")

need(worker, "templateName: template.template_name || spec.label", "Configured template name in chooser")

# Admin Web: all supported operational surfaces reuse one central flow.
for token in [
    "function useAdminWhatsAppFlow()", 'new URLSearchParams({mode:"templates"})',
    "Loading Templates…", "WhatsApp Preview", "Open WhatsApp",
    "whatsapp.start(r.customer_id,r.id)", "whatsapp.start(c.id,null)",
    "whatsapp.start(b.customer_id,b.id)", "whatsapp.start(detail.booking.customer_id,detail.booking.id)",
    "whatsapp.start(String(row.customer_id)",
]:
    need(admin, token, "Admin Web shared WhatsApp flow")
if admin.count("https://wa.me/") != 1:
    errors.append("Admin Web must contain exactly one final wa.me launch inside the shared WhatsApp flow.")
if "const wa=" in admin or 'href={wa(' in admin:
    errors.append("Admin Web page-local direct WhatsApp helpers/links must remain removed.")
if admin.count("whatsapp.start(") < 8:
    errors.append("Admin Web Dashboard/Customers/Bookings/Pickup/Return/Reports must use the shared WhatsApp flow.")

for token in [
    "b.customer_id AS customer_id", "b.id AS booking_id,b.customer_id AS customer_id",
    "cu.id AS customer_id",
]:
    need(reports_worker, token, "Admin Web Reports WhatsApp identity")

hardening = package.get("scripts", {}).get("test:hardening", "")
if "phase14bn_whatsapp_flow_hardening_regression.py" not in hardening:
    errors.append("Phase 14BN regression must run in test:hardening.")

if errors:
    print("Phase 14BN WhatsApp flow hardening regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14BN WhatsApp flow hardening regression PASS")
