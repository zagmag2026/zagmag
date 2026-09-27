from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android"
SOURCE = ANDROID / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

required = [
    ANDROID / "settings.gradle.kts",
    ANDROID / "build.gradle.kts",
    ANDROID / "app" / "build.gradle.kts",
    ANDROID / "app" / "src" / "main" / "AndroidManifest.xml",
    SOURCE / "MainActivity.kt",
    SOURCE / "AdminEntryScreen.kt",
    SOURCE / "AdminMainChrome.kt",
    SOURCE / "DashboardScreenV2.kt",
    SOURCE / "CustomerScreenV3.kt",
    SOURCE / "CustomerScreen3ViewModel.kt",
    SOURCE / "AdminViewModel.kt",
    SOURCE / "BookingLifecycleViewModel.kt",
    SOURCE / "AdminApp.kt",
    SOURCE / "AdminAppCompact.kt",
    SOURCE / "AdminAppScreen3.kt",
    SOURCE / "ui" / "components" / "Components.kt",
    SOURCE / "data" / "ApiClient.kt",
    SOURCE / "data" / "AdminRepository.kt",
    SOURCE / "data" / "CustomerScreen3Repository.kt",
    SOURCE / "data" / "Models.kt",
    SOURCE / "data" / "Branding.kt",
    SOURCE / "data" / "BookingLifecycleModels.kt",
    SOURCE / "data" / "SecureSessionStore.kt",
]

errors = []
for path in required:
    if not path.exists():
        errors.append(f"Missing required Android foundation file: {path.relative_to(ROOT)}")

build_text = (ANDROID / "app" / "build.gradle.kts").read_text(encoding="utf-8")
if 'versionName = "0.14.5"' not in build_text:
    errors.append("Android versionName must remain 0.14.5 for this foundation batch.")
if 'versionCode = 1' not in build_text:
    errors.append("Android foundation versionCode must remain 1.")
if 'https://zhagmag-dresses-staging.patelnims26.workers.dev' not in build_text:
    errors.append("Staging flavor must point to the approved staging Worker.")
if 'providers.gradleProperty("ZHAGMAG_PRODUCTION_BASE_URL")' not in build_text:
    errors.append("Production API URL must be externally supplied, not hard-coded.")

manifest = (ANDROID / "app" / "src" / "main" / "AndroidManifest.xml").read_text(encoding="utf-8")
if 'android:usesCleartextTraffic="false"' not in manifest:
    errors.append("Android must reject cleartext HTTP traffic.")
if 'android:allowBackup="false"' not in manifest:
    errors.append("Android admin app backup must stay disabled for session hardening.")

strings_en = ANDROID / "app" / "src" / "main" / "res" / "values" / "strings.xml"
if not strings_en.exists():
    errors.append("English string resources are required.")
else:
    strings_text = strings_en.read_text(encoding="utf-8")
    ET.parse(strings_en)
    if re.search(r"[\u0A80-\u0AFF]", strings_text):
        errors.append("Gujarati text detected in the English-only Android string resources.")

res_root = ANDROID / "app" / "src" / "main" / "res"
gu_resource_dirs = [path for path in res_root.glob("values-gu*") if path.exists()]
if gu_resource_dirs:
    errors.append(f"Gujarati Android resource directories are not allowed: {[path.name for path in gu_resource_dirs]}")

locale_config = res_root / "xml" / "locales_config.xml"
locale_text = locale_config.read_text(encoding="utf-8") if locale_config.exists() else ""
if 'android:name="en"' not in locale_text:
    errors.append("English locale must remain declared in Android locale config.")
if 'android:name="gu"' in locale_text:
    errors.append("Gujarati locale must not be declared in the English-only Android app.")

main_source = ANDROID / "app" / "src" / "main"
for path in main_source.rglob("*"):
    if path.is_file() and path.suffix.lower() in {".kt", ".xml"}:
        text = path.read_text(encoding="utf-8")
        if re.search(r"[\u0A80-\u0AFF]", text):
            errors.append(f"Gujarati text detected in English-only Android source: {path.relative_to(ROOT)}")

source_root = ANDROID / "app" / "src" / "main" / "java"
for path in source_root.rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    relative = path.relative_to(ROOT)
    if "Color(0x" in text and path.name != "Color.kt":
        errors.append(f"Direct color literal outside design tokens: {relative}")
    if "fixedRateTimer" in text or re.search(r"while\s*\([^)]*\)\s*\{[^{}]*\bdelay\s*\(", text, re.S):
        errors.append(f"Polling/timer loop detected in Android source: {relative}")
    delay_calls = re.findall(r"\bdelay\s*\(\s*(\d+)\s*\)", text)
    if delay_calls and not (path.name == "CustomerScreen3ViewModel.kt" and all(value == "350" for value in delay_calls)):
        errors.append(f"Unapproved Android delay/timer detected: {relative}")
    is_repository_layer = path.parent.name == "data" and path.name.endswith("Repository.kt")
    if "/api/" in text and not is_repository_layer:
        errors.append(f"Direct API endpoint outside repository layer: {relative}")

main_activity = (SOURCE / "MainActivity.kt").read_text(encoding="utf-8")
if "ZhagmagAdminRootScreen3(adminViewModel, bookingViewModel, customerViewModel)" not in main_activity:
    errors.append("Screen 3 screen-by-screen Admin root must be active.")

entry_screen = (SOURCE / "AdminEntryScreen.kt").read_text(encoding="utf-8")
for token in ["DynamicBrandLogo", "Mobile number", "10 digit mobile number", "Visibility", "Icons.Rounded.Login", "KeyboardType.Phone"]:
    if token not in entry_screen:
        errors.append(f"Screen 1 entry/login contract missing: {token}")
if "Email or mobile" in entry_screen or "sign_in_subtitle" in entry_screen or "brand_subtitle" in entry_screen:
    errors.append("Screen 1 must remain mobile-only and free of explanatory subtitles.")
if "filter(Char::isDigit).take(10)" not in entry_screen:
    errors.append("Screen 1 mobile field must enforce live 10-digit input filtering.")
if ".imePadding()" not in entry_screen or ".statusBarsPadding()" not in entry_screen:
    errors.append("Screen 1 must remain IME-safe and system-bar safe.")
if "var password by rememberSaveable" in entry_screen or "var passwordVisible by rememberSaveable" in entry_screen:
    errors.append("Screen 1 password and visibility state must never use saveable state restoration.")
for token in ['var password by remember { mutableStateOf("") }', "var passwordVisible by remember { mutableStateOf(false) }"]:
    if token not in entry_screen:
        errors.append(f"Screen 1 non-persistent password policy missing: {token}")

view_model_text = (SOURCE / "AdminViewModel.kt").read_text(encoding="utf-8")
logout_match = re.search(r"fun logout\(\)\s*\{(?P<body>.*?)\n\s*\}\n\n\s*fun loadDashboard", view_model_text, re.S)
if not logout_match:
    errors.append("Admin logout implementation could not be audited.")
else:
    logout_body = logout_match.group("body")
    for token in ["repository.logout()", "finally", "sessionStore.clear()", "authState = AuthState.SignedOut"]:
        if token not in logout_body:
            errors.append(f"Admin logout policy missing: {token}")
for token in ["dashboardRevision", "dashboardRevision += 1", "dashboardState.copy(loading = true, error = null)"]:
    if token not in view_model_text:
        errors.append(f"Screen 2 refresh-state contract missing: {token}")

compact_app = (SOURCE / "AdminAppCompact.kt").read_text(encoding="utf-8")
for token in ["Reserved", "Booked", "Part Pickup", "Full Pickup", "Part Return", "Full Return", "Direct Pickup"]:
    if token not in compact_app:
        errors.append(f"Booking lifecycle UI is missing: {token}")
if "booking_pickup_flow_hint" in compact_app or "auto_search_hint" in compact_app:
    errors.append("Compact active Android UI must not render explanatory helper labels.")
if ".statusBarsPadding()" not in compact_app:
    errors.append("Compact Android header must respect the status bar inset.")
for token in ["AdminMainTopBar", "DashboardScreenV2", "rememberDashboardScreenState", "mutationVersion", "MainScreenDateRow"]:
    if token not in compact_app:
        errors.append(f"Screen 2 signed-in routing contract missing: {token}")

chrome_text = (SOURCE / "AdminMainChrome.kt").read_text(encoding="utf-8")
for token in ["32.dp", "branding.shopName", "Icons.Rounded.Logout", "Log out?", "EEEE, d MMMM yyyy"]:
    if token not in chrome_text:
        errors.append(f"Screen 2 shared chrome contract missing: {token}")

dashboard_text = (SOURCE / "DashboardScreenV2.kt").read_text(encoding="utf-8")
for token in [
    "Available Now", "Booked Pending", "Given Out", "Overdue Returns", "Missed Pickups", "Total Quantity", "Items", "Categories",
    "Today Bookings", "Today Pickups", "Today Returns", "Open Booking", "48.dp", "wa.me", "ACTION_DIAL", "AnimatedVisibility",
    "sectionCounts", "pendingPickupQty", "pendingReturnQty", "Icons.Rounded.Refresh", "New Booking"
]:
    if token not in dashboard_text:
        errors.append(f"Screen 2 Dashboard contract missing: {token}")
if "pullRefresh" in dashboard_text or "PullToRefresh" in dashboard_text:
    errors.append("Screen 2 must use the explicit Refresh action only; pull-to-refresh is not approved.")
if "\\u0aa8" not in dashboard_text or "Hello ${row.customerName}" not in dashboard_text:
    errors.append("Screen 2 WhatsApp templates must include Gujarati + English pre-filled text without Gujarati UI resources.")

booking_vm_text = (SOURCE / "BookingLifecycleViewModel.kt").read_text(encoding="utf-8")
for token in ["mutationVersion", "mutationVersion += 1"]:
    if token not in booking_vm_text:
        errors.append(f"Screen 2 mutation-aware Dashboard refresh missing: {token}")

customer_screen_text = (SOURCE / "CustomerScreenV3.kt").read_text(encoding="utf-8")
for token in ["PullToRefreshBox", "Icons.Rounded.Refresh", "New Customer", "Active", "Archived", "Page ", "Create Booking"]:
    if token not in customer_screen_text:
        errors.append(f"Screen 3 Customers UI contract missing: {token}")

customer_vm_text = (SOURCE / "CustomerScreen3ViewModel.kt").read_text(encoding="utf-8")
for token in ["delay(350)", "PendingCustomerLoad", "pendingLoad", "scheduleSearchLoad", "drainPendingLoad"]:
    if token not in customer_vm_text:
        errors.append(f"Screen 3 request coordination contract missing: {token}")
if re.search(r"while\s*\([^)]*\)\s*\{[^{}]*\bdelay\s*\(", customer_vm_text, re.S):
    errors.append("Screen 3 must not poll while waiting for another customer request.")

models_text = (SOURCE / "data" / "Models.kt").read_text(encoding="utf-8")
for token in ["DashboardSectionCounts", "DashboardItem", "displayStatus", "pendingPickupQty", "pendingReturnQty", "sectionCounts"]:
    if token not in models_text:
        errors.append(f"Screen 2 Dashboard data contract missing: {token}")

repo_text = (SOURCE / "data" / "AdminRepository.kt").read_text(encoding="utf-8")
allowed_prefixes = (
    "/api/auth/",
    "/api/public/bootstrap",
    "/api/admin/dashboard",
    "/api/admin/customers",
    "/api/admin/bookings",
    "/api/admin/pickups/",
    "/api/admin/returns/",
)
found = set(re.findall(r'"(/api/[^"?]+)', repo_text))
unknown = {endpoint for endpoint in found if not endpoint.startswith(allowed_prefixes)}
if unknown:
    errors.append(f"Unexpected Android API endpoints: {sorted(unknown)}")
for token in ["confirmationState", "/confirm", "savePickup", "saveReturn"]:
    if token not in repo_text:
        errors.append(f"Booking lifecycle repository contract missing: {token}")

customer_repo_text = (SOURCE / "data" / "CustomerScreen3Repository.kt").read_text(encoding="utf-8")
for token in [
    '"pageSize" to "10"', '"sort" to sort', '"archived" to if (archived) "1" else "0"',
    '"/api/admin/customers"', '"/api/admin/customers/$id/archive"', '"/api/admin/customers/$id/restore"',
    '"DELETE"', '"/api/admin/customers/$customerId/whatsapp"'
]:
    if token not in customer_repo_text:
        errors.append(f"Screen 3 Customers repository contract missing: {token}")

branding_text = (SOURCE / "data" / "Branding.kt").read_text(encoding="utf-8")
for token in ["AppBranding", "BrandingStore", "shopNameEn", "logoUrl"]:
    if token not in branding_text:
        errors.append(f"Dynamic entry branding contract missing: {token}")

session_text = (SOURCE / "data" / "SecureSessionStore.kt").read_text(encoding="utf-8")
for required_token in ["AndroidKeyStore", "AES/GCM/NoPadding", "MODE_PRIVATE"]:
    if required_token not in session_text:
        errors.append(f"Secure session storage missing requirement: {required_token}")

worker_dashboard = ROOT / "worker" / "src" / "phase14au-dashboard-screen2.js"
if not worker_dashboard.exists():
    errors.append("Screen 2 bundled Dashboard Worker wrapper is missing.")
else:
    worker_dashboard_text = worker_dashboard.read_text(encoding="utf-8")
    for token in ["sectionCounts", "missed_pickups_json", "items_json", "item_images", "LIMIT 50", "getSessionUser"]:
        if token not in worker_dashboard_text:
            errors.append(f"Screen 2 bundled Dashboard Worker contract missing: {token}")

worker_customers = ROOT / "worker" / "src" / "phase14av-customers-screen3.js"
if not worker_customers.exists():
    errors.append("Screen 3 Customers Worker wrapper is missing.")
else:
    worker_customers_text = worker_customers.read_text(encoding="utf-8")
    if 'import core from "./phase14au-dashboard-screen2.js";' not in worker_customers_text:
        errors.append("Screen 3 Worker must preserve the cumulative Screen 2 Dashboard chain.")
    for token in ["pageSize", "alternate_mobile", "active_bookings", "DUPLICATE_MOBILE", "ACTIVE_BOOKINGS", "audit_logs"]:
        if token not in worker_customers_text:
            errors.append(f"Screen 3 Customers Worker contract missing: {token}")

wrangler_text = (ROOT / "worker" / "wrangler.toml").read_text(encoding="utf-8")
if 'main = "src/phase14av-customers-screen3.js"' not in wrangler_text:
    errors.append("Wrangler must activate the cumulative Screen 3 Customers wrapper for staging.")

staging_workflow = (ROOT / ".github" / "workflows" / "deploy-staging.yml").read_text(encoding="utf-8")
if '".github/staging-run-trigger"' not in staging_workflow:
    errors.append("Cloudflare staging deploy trigger contract changed unexpectedly.")
if "apps/admin-android" in staging_workflow:
    errors.append("Android-only changes must not trigger the Cloudflare staging deploy workflow.")

if errors:
    print("Android Admin Foundation Audit: FAILED")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Android Admin Foundation Audit: PASS")
print("- Version 0.14.5 / versionCode 1 preserved")
print("- Production endpoint remains externally configured")
print("- English-only Android resources enforced")
print("- Screen 1 uses cached dynamic branding + mobile-only live validation")
print("- Screen 1 raw password state is non-saveable and logout always clears local session")
print("- Screen 2 Dashboard uses bundled sections + per-item image/name/quantity data")
print("- Screen 2 refresh is explicit, mutation-aware and non-polling")
print("- Screen 2 WhatsApp reminders are Gujarati + English pre-filled messages")
print("- Screen 3 Customers root, repository, refresh controls and cumulative Worker are active")
print("- Screen 3 search uses approved 350ms debounce without polling loops")
print("- Compact active UI has no explanatory helper labels")
print("- Single booking lifecycle: Reserved → Booked → Pickup → Return")
print("- API calls centralized in repository-layer classes")
print("- No polling/timer loop detected")
print("- Session cookie storage uses Android Keystore + AES-GCM")
print("- Android changes do not alter Cloudflare staging deploy trigger scope")
