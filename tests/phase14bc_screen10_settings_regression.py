from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

errors = []

def text(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def need(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

ui = text(ANDROID / "Screen10Settings.kt")
models = text(ANDROID / "Screen10SettingsModels.kt")
repo = text(ANDROID / "Screen10SettingsRepository.kt")
vm = text(ANDROID / "Screen10SettingsViewModel.kt")
root = text(ANDROID / "AdminAppScreen4.kt")
customer_models = text(ANDROID / "data" / "CustomerScreenModels.kt")
customer_repo = text(ANDROID / "data" / "CustomerScreen3Repository.kt")
dashboard = text(ANDROID / "DashboardScreenV2.kt")
details = text(ANDROID / "BookingDetailsScreen6.kt")
base_worker = text(ROOT / "worker" / "src" / "phase14bc-screen10-settings.js")
current_worker = text(ROOT / "worker" / "src" / "phase14bg-settings-whatsapp.js")
core = text(ROOT / "worker" / "src" / "index.ts")
migration15 = text(ROOT / "database" / "migrations" / "0015_whatsapp_template_linking.sql")
wrangler = text(ROOT / "worker" / "wrangler.toml")
staging = text(ROOT / "worker" / "wrangler.staging.toml.template")
doc = text(ROOT / "docs" / "ADMIN_SCREEN_10_SETTINGS.md")

checks = [
    (ui, 'MainScreenDateRow(businessDate)', "Settings Date/Day/Time row"),
    (ui, 'title = "Settings"', "Settings Back header title"),
    (ui, 'label = "Website"', "Website field"),
    (ui, 'placeholder = "Enter website URL"', "Website placeholder"),
    (ui, 'BASIC("Basic")', "Basic tab"),
    (ui, 'PUBLIC("Public Website")', "Public Website tab"),
    (ui, 'internal fun Screen10WhatsAppManagement(', "standalone WhatsApp Management screen"),
    (root, 'MoreDestination4.WHATSAPP_MANAGEMENT', "More WhatsApp Management destination"),
    (root, 'title = "WhatsApp Centre"', "More WhatsApp Centre card"),
    (ui, 'AUDIT("Audit Log")', "Audit Log tab"),
    (ui, 'LaunchedEffect(tab) { if (tab == Screen10Tab.AUDIT) vm.ensureAuditLoaded() }', "Audit tab first-load on entry"),
    (ui, 'enabled = tab == Screen10Tab.AUDIT', "Audit-only pull-to-refresh scope"),
    (ui, 'onRefresh = vm::refreshAudit', "Audit pull-to-refresh routing"),
    (ui, 'enabled = section != Screen10WhatsAppSection.LANGUAGE', "WhatsApp Language Settings pull-refresh disabled"),
    (vm, 'fun refreshAudit()', "Audit explicit refresh"),
    (vm, 'requestAuditReset()', "Audit page-one reset request"),
    (vm, 'search = snapshot.auditSearch', "Audit refresh preserves search filter"),
    (vm, 'module = snapshot.auditModule', "Audit refresh preserves module filter"),
    (vm, 'action = snapshot.auditAction', "Audit refresh preserves action filter"),
    (vm, 'userId = snapshot.auditUserId', "Audit refresh preserves user filter"),
    (vm, 'fromDate = snapshot.auditFromDate', "Audit refresh preserves from-date filter"),
    (vm, 'toDate = snapshot.auditToDate', "Audit refresh preserves to-date filter"),
    (ui, "AppCompactScrollableTabs(", "shared responsive compact tab row"),
    (ui, 'RESERVATION("Reservation")', "WhatsApp Reservation section"),
    (ui, 'LANGUAGE("Language Settings")', "WhatsApp Language Settings final section"),
    (ui, 'labels = Screen10WhatsAppSection.entries.map { it.label }', "WhatsApp category tabs"),
    (ui, 'Text("Linked Action * · ${initialSection.label}"', "category-scoped Linked Action editor"),
    (ui, "FilterChip(", "compact linked-action chips"),
    (ui, 'label = "Gujarati Message *"', "Gujarati message editor"),
    (ui, 'label = "English Message *"', "English message editor"),
    (ui, '"Global Template Language"', "global template language"),
    (ui, 'placeholderSources = state.placeholderSources', "placeholder source wiring"),
    (ui, "Screen10PlaceholderInsertDropdown(", "placeholder dropdown"),
    (ui, 'Text("Insert Placeholder"', "placeholder insert label"),
    (ui, 'if (expanded) {', "on-demand placeholder menu composition"),
    (ui, 'screen10TemplateKey(linkedAction, name)', "alternative template key generation"),
    (ui, 'CompactNewActionButton(', "shared Add Template action"),
    (repo, '"/api/admin/settings/bootstrap"', "settings bootstrap API"),
    (repo, '.put("websiteUrl", settings.websiteUrl.trim())', "Website setting mutation"),
    (models, 'val websiteUrl: String = ""', "Website settings model"),
    (core, 'websiteUrl: string;', "Website Worker settings type"),
    (core, "websiteUrl:str('websiteUrl',defaults.websiteUrl)", "Website Worker persistence"),
    (repo, '"/api/admin/whatsapp-templates"', "template API"),
    (repo, '.put("whatsappTemplateLanguage"', "global language setting mutation"),
    (repo, '.put("linkedAction"', "linked action mutation"),
    (repo, '.put("messageGu"', "Gujarati mutation"),
    (repo, '.put("messageEn"', "English mutation"),
    (vm, 'linkedAction.isNullOrBlank()', "linked action validation"),
    (vm, 'messageGu.trim().isBlank()', "Gujarati validation"),
    (vm, 'messageEn.trim().isBlank()', "English validation"),
    (root, 'MoreDestination4.SETTINGS', "Settings destination"),
    (root, 'title = "Settings"', "Settings More card"),
    (root, 'Screen10Settings(', "Settings routing"),
    (customer_models, 'object WhatsAppContext', "WhatsApp contexts"),
    (customer_models, 'const val THANK_YOU = "THANK_YOU"', "Thank You context"),
    (customer_repo, '"context" to context?.takeIf { it.isNotBlank() }', "context query parameter"),
    (dashboard, '"Today Overview"', "KPI-only Dashboard replaces operational WhatsApp queue cards"),
    (root, 'composeWhatsApp(booking.customerId, booking.id, null)', "Booking current-status WhatsApp mapping"),
    (root, 'WhatsAppTemplateSelectionSheet(', "shared status-filtered WhatsApp template chooser"),
    (root, 'bundle.templates.singleOrNull()', "single-template preview skip"),
    (details, 'onCall = { confirmCall = true }', "Booking shared Call confirmation"),
    (base_worker, 'import core from "./phase14bb-screen9-users.js"', "legacy Screen 10 wrapper remains in chain"),
    (base_worker, 'composeLinkedWhatsApp', "legacy composer remains available below current wrapper"),
    (current_worker, 'import core from "./phase14be-operational-lifecycle.js"', "current wrapper preserves cumulative lifecycle stack"),
    (current_worker, 'composeLinkedWhatsApp', "current linked WhatsApp composer"),
    (core, 'templatePlaceholderRegistry', "relevant placeholder registry bootstrap"),
    (core, 'templateLanguageModes:WHATSAPP_GLOBAL_LANGUAGE_MODES', "global language options"),
    (core, 'templateLinkedActions:WHATSAPP_LINKED_ACTIONS', "linked action options"),
    (models, 'val whatsappTemplateLanguage: String = "BOTH"', "global language model"),
    (migration15, 'ALTER TABLE whatsapp_templates ADD COLUMN linked_action', "base linked-action schema"),
    (migration15, "ux_whatsapp_templates_active_link", "single active linked template guard"),
    (wrangler, 'main = "src/phase14br-billing-v1.js"', "local current Worker entry"),
    (staging, 'main = "src/phase14br-billing-v1.js"', "staging current Worker entry"),
    (doc, "Global WhatsApp Template Language", "documented global language"),
]

for body, token, label in checks:
    need(body, token, label)

for forbidden, label in [
    ('eyebrow = "Administration"', "old Settings eyebrow"),
    ('subtitle = "Business, website & WhatsApp settings"', "old Settings subtitle"),
    ('"Gujarati · English · All"', "old per-template language summary"),
]:
    if forbidden in ui:
        errors.append(f"{label} must remain removed.")

if 'relevantPlaceholders.joinToString("  ")' in ui:
    errors.append("WhatsApp placeholders must not regress to an unexplained raw token-only list.")
editor_start = ui.find("private fun Screen10TemplateEditorSheet")
editor_end = ui.find("private fun Screen10PlaceholderInsertDropdown", editor_start)
editor_block = ui[editor_start:editor_end] if editor_start >= 0 and editor_end > editor_start else ""
if "relevantPlaceholders.forEach" in editor_block:
    errors.append("WhatsApp template editor must not eagerly compose every placeholder row.")
if 'Screen10Dropdown(\n                    "Linked To"' in editor_block:
    errors.append("WhatsApp Linked To must remain category-scoped chips, not the old 37-action dropdown.")
if 'Text("Add GU")' in editor_block or 'Text("Add EN")' in editor_block:
    errors.append("WhatsApp placeholder insertion must remain on-demand dropdown based.")

if 'distinct().joinToString("_")' in ui:
    errors.append("Alternative WhatsApp template keys must not collapse back to the linked-action key.")

if "fun ensureAuditLoaded()" not in vm or "!state.auditLoaded" not in vm:
    errors.append("Audit tab must load once and reuse cached rows until explicit refresh/filter change.")
settings_start = ui.find("internal fun Screen10Settings(")
settings_end = ui.find("internal fun Screen10WhatsAppManagement(", settings_start)
settings_block = ui[settings_start:settings_end] if settings_start >= 0 and settings_end > settings_start else ui
if "onRefresh = vm::refresh," in settings_block:
    errors.append("Settings editable tabs must not route pull-to-refresh to the Settings bootstrap.")
if "whatsappMessage(" in dashboard:
    errors.append("Dashboard must not retain the old hard-coded WhatsApp message builder.")
if "WhatsAppContext." in dashboard:
    errors.append("Dashboard generic WhatsApp button must remain status-group driven, not hard-coded to a linked action.")
if "onThankYou" in details or 'text = "Thank You"' in details:
    errors.append("Booking Details standalone Thank You action must remain removed.")

if errors:
    print("Screen 10 Settings / WhatsApp regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Screen 10 Settings / WhatsApp regression PASS")
