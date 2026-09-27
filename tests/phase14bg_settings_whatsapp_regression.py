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

ui = read(ANDROID / "Screen10Settings.kt")
models = read(ANDROID / "Screen10SettingsModels.kt")
repo = read(ANDROID / "Screen10SettingsRepository.kt")
vm = read(ANDROID / "Screen10SettingsViewModel.kt")
admin_vm = read(ANDROID / "AdminViewModel.kt")
screen4 = read(ANDROID / "AdminAppScreen4.kt")
actions = read(ANDROID / "BookingCardActions.kt")
components = read(ANDROID / "ui" / "components" / "Components.kt")
contexts = read(ANDROID / "data" / "CustomerScreenModels.kt")
core = read(ROOT / "worker" / "src" / "index.ts")
wrapper = read(ROOT / "worker" / "src" / "phase14bg-settings-whatsapp.js")
registry = read(ROOT / "worker" / "src" / "whatsapp-template-registry.ts")
migration = read(ROOT / "database" / "migrations" / "0018_settings_whatsapp_expansion.sql")
public = read(ROOT / "apps" / "public-web" / "src" / "main.tsx")
public_css = read(ROOT / "apps" / "public-web" / "src" / "phase14y-public-uniform.css")
i18n = read(ROOT / "apps" / "public-web" / "src" / "phase14ar-public-i18n.js")
wrangler = read(ROOT / "worker" / "wrangler.toml")
staging = read(ROOT / "worker" / "wrangler.staging.toml.template")
doc = read(ROOT / "docs" / "ADMIN_SCREEN_10_SETTINGS.md")
global_ui = read(ROOT / "docs" / "GLOBAL_UI_RULES.md")

checks = [
    (ui, 'title = "Settings"', "shared Settings back header"),
    (ui, "AppCompactScrollableTabs(", "shared scrollable Settings tabs"),
    (components, "minTabWidth: androidx.compose.ui.unit.Dp = 96.dp", "Run 79 shared compact scroll-tab minimum width"),
    (ui, '"Business Logo"', "Business Logo field"),
    (ui, "ActivityResultContracts.GetContent()", "device image picker"),
    (ui, '"Upload Logo"', "Upload Logo action"),
    (ui, '"Change Logo"', "Change Logo action"),
    (ui, '"Remove Logo"', "Remove Logo action"),
    (ui, '"JPG, PNG or WebP · max 8 MB"', "logo validation helper"),
    (repo, "suspend fun uploadLogo", "logo upload repository"),
    (repo, 'SUPPORTED_LOGO_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")', "logo MIME validation"),
    (repo, "CLIENT_MAX_LOGO_BYTES = 8 * 1024 * 1024", "logo max size"),
    (repo, 'api.post("/api/admin/cloudinary/signature?purpose=logo")', "signed managed logo upload"),
    (admin_vm, "fun applySettingsBranding(shopName: String, logoUrl: String)", "local saved branding apply"),
    (screen4, "onBrandingChanged = adminViewModel::applySettingsBranding", "Settings local branding hook"),
    (ui, '"Global Template Language"', "global WhatsApp language UI"),
    (ui, '"GUJARATI" to "Gujarati"', "Gujarati global option"),
    (ui, '"ENGLISH" to "English"', "English global option"),
    (ui, '"BOTH" to "Both"', "Both global option"),
    (models, 'val whatsappTemplateLanguage: String = "BOTH"', "global language model"),
    (repo, '.put("whatsappTemplateLanguage"', "global language persistence"),
    (ui, 'CompactNewActionButton(', "shared Add Template button"),
    (ui, 'label = "Add Template"', "Add Template label"),
    (ui, 'RESERVATION("Reservation")', "Reservation template section"),
    (ui, 'BOOKING("Booking")', "Booking template section"),
    (ui, 'PICKUP("Pickup")', "Pickup template section"),
    (ui, 'RETURN("Return")', "Return template section"),
    (ui, 'OVERDUE("Overdue")', "Overdue template section"),
    (ui, 'ITEM("Item")', "Item template section"),
    (ui, 'GENERAL("General")', "General template section"),
    (ui, 'LANGUAGE("Language Settings")', "Language Settings final section"),
    (ui, 'Screen10PlaceholderInsertDropdown(', "on-demand placeholder dropdown"),
    (ui, 'Text("Insert Placeholder"', "placeholder insert action"),
    (models, "val placeholderRegistry: Map<String, List<String>>", "placeholder registry model"),
    (vm, "placeholderRegistry = result.placeholderRegistry", "placeholder registry state"),
    (components, "modifier = modifier.wrapContentWidth()", "content-fit global New/Add action"),
    (components, ".padding(horizontal = 10.dp, vertical = 6.dp)", "approved compact New/Add padding"),
    (core, "publicAvailabilityStatus", "central public availability rule"),
    (core, 'if(available<=0)return "NOT_AVAILABLE"', "Not Available threshold"),
    (core, 'if(available<=2)return "FEW_LEFT"', "Few Left threshold"),
    (core, 'if(available<=5)return "LIMITED"', "Limited threshold"),
    (core, 'return "AVAILABLE"', "Available threshold"),
    (public, 'type AvailabilityStatus = "AVAILABLE" | "LIMITED" | "FEW_LEFT" | "NOT_AVAILABLE"', "public status type"),
    (public, 'if(mode==="EXACT" && typeof info.availableQuantity==="number") return `Available: ${info.availableQuantity}`', "Exact Quantity label"),
    (public, 'if(info.status === "LIMITED") return "Limited"', "Limited label"),
    (public_css, ".availability-badge.limited", "Limited visual"),
    (i18n, '"Limited":"મર્યાદિત"', "Limited Gujarati translation"),
    (registry, 'WHATSAPP_GLOBAL_LANGUAGE_MODES = ["GUJARATI", "ENGLISH", "BOTH"]', "global language registry"),
    (registry, "WHATSAPP_PLACEHOLDER_SOURCES", "fixed placeholder sources"),
    (registry, "WHATSAPP_PLACEHOLDERS_BY_ACTION", "action-specific placeholder registry"),
    (registry, "validateTemplatePlaceholders", "placeholder validation"),
    (wrapper, "TEMPLATE_UNRESOLVED", "unresolved placeholder guard"),
    (wrapper, "readableItemSummary", "readable multi-item summary"),
    (wrapper, 'message: `${gu}\\n\\n${en}`', "Both-language rendering"),
    (wrapper, "pickup_time", "pickup-time mapping"),
    (wrapper, "return_time", "return-time mapping"),
    (wrapper, "related_items", "related-items mapping"),
    (wrapper, "today_date", "today-date mapping"),
    (wrapper, "staff_name", "staff-name mapping"),
    (core, "templatePlaceholderRegistry", "placeholder registry bootstrap"),
    (core, "validateTemplatePlaceholders", "template save placeholder validation"),
    (contexts, 'const val RESERVATION_CONFIRMATION = "RESERVATION_CONFIRMATION"', "new Android WhatsApp contexts"),
    (migration, "RESERVATION_CONFIRMATION", "Reservation Confirmation seed"),
    (migration, "RESERVATION_CANCELLED", "Reservation Cancelled seed"),
    (migration, "BOOKING_UPDATED", "Booking Updated seed"),
    (migration, "BOOKING_CANCELLED", "Booking Cancelled seed"),
    (migration, "PICKUP_READY", "Pickup Ready seed"),
    (migration, "PICKUP_DUE_TODAY", "Pickup Due Today seed"),
    (migration, "RETURN_DUE_TODAY", "Return Due Today seed"),
    (migration, "PENDING_PICKUP_REMINDER", "Pending Pickup Reminder seed"),
    (migration, "PENDING_RETURN_REMINDER", "Pending Return Reminder seed"),
    (migration, "OVERDUE_FINAL_REMINDER", "Overdue final seed"),
    (migration, "ITEM_AVAILABILITY_REPLY", "Item Availability Reply seed"),
    (migration, "BOOKING_COMPLETED", "Booking Completed seed"),
    (wrangler, 'main = "src/phase14bq-global-rental-reports.js"', "current local Worker entry"),
    (staging, 'main = "src/phase14bq-global-rental-reports.js"', "current staging Worker entry"),
    (doc, "Global WhatsApp Template Language", "updated Settings documentation"),
    (global_ui, "Global New/Add action", "global New/Add documentation"),
]

for body, token, label in checks:
    require(body, token, label)

required_placeholders = [
    "shop_name","shop_phone","shop_whatsapp","shop_address","customer_name","customer_mobile",
    "booking_no","booking_status","booking_date","pickup_date","return_date","pickup_time",
    "return_time","item_name","item_code","category_name","qty","pending_qty","picked_qty",
    "returned_qty","overdue_days","related_items","today_date","staff_name"
]
for key in required_placeholders:
    if f"{key}:" not in registry:
        errors.append(f"Placeholder registry missing fixed source: {key}")

for forbidden, label in [
    ('eyebrow = "Administration"', "old Administration label"),
    ('subtitle = "Business, website & WhatsApp settings"', "old Settings subtitle"),
    ('"Gujarati · English · All"', "old per-template language summary"),
    ('label = "Language"', "per-template Language selector"),
    ('"Logo URL"', "raw Logo URL field"),
    ('"Few Left Threshold"', "editable legacy threshold field"),
]:
    if forbidden in ui:
        errors.append(f"Retired Settings UI present: {label}")

editor_start = ui.find("private fun Screen10TemplateEditorSheet")
editor_end = ui.find("private fun Screen10PlaceholderInsertDropdown", editor_start)
editor_block = ui[editor_start:editor_end] if editor_start >= 0 and editor_end > editor_start else ""
if "relevantPlaceholders.forEach" in editor_block:
    errors.append("Template editor must not eagerly render all relevant placeholder rows.")
if 'Screen10Dropdown(\n                    "Linked To"' in editor_block:
    errors.append("Template editor must not regress to the old full Linked To dropdown.")
if 'Text("Add GU")' in editor_block or 'Text("Add EN")' in editor_block:
    errors.append("Placeholder insertion must use the compact on-demand dropdown.")

if "availabilityMode==='EXACT'?{itemId:String(r.id),status,availableQuantity:available}:{itemId:String(r.id),status}" not in core:
    errors.append("Status Only must omit exact quantity while Exact Quantity returns it.")

# Billing V1 is the explicit pricing/payment exception. Screen 10's legacy
# non-billing template surface must still reject unrelated pricing concepts.
for forbidden in ["price", "deposit"]:
    if forbidden in registry.lower() or forbidden in migration.lower():
        errors.append(f"Unsupported pricing concept must not be added to WhatsApp registry/migration: {forbidden}")

for billing_token in ["PAYMENT_PENDING", "FULL_AMOUNT_RECEIVED", "balance_amount", "payment_status"]:
    if billing_token not in registry:
        errors.append(f"Billing V1 WhatsApp exception missing: {billing_token}")

if "onBrandingChanged = adminViewModel::refreshPublicBranding" in screen4:
    errors.append("Settings save must not restore the extra public-branding network refetch.")

if errors:
    print("Phase 14BG Settings/WhatsApp regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14BG Settings/WhatsApp regression PASS")
