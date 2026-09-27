from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android"
SOURCE = ANDROID / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"
DATA = SOURCE / "data"
COMPONENTS = SOURCE / "ui" / "components" / "Components.kt"

required = [
    ANDROID / "settings.gradle.kts",
    ANDROID / "build.gradle.kts",
    ANDROID / "app" / "build.gradle.kts",
    ANDROID / "app" / "src" / "main" / "AndroidManifest.xml",
    SOURCE / "MainActivity.kt",
    SOURCE / "AdminEntryScreen.kt",
    SOURCE / "AdminMainChrome.kt",
    SOURCE / "DashboardScreenV2.kt",
    SOURCE / "CustomerScreenV4.kt",
    SOURCE / "CustomerSharedUi.kt",
    SOURCE / "CustomerScreen3ViewModel.kt",
    SOURCE / "AdminViewModel.kt",
    SOURCE / "BookingLifecycleViewModel.kt",
    SOURCE / "BookingListViewModel4.kt",
    SOURCE / "AdminAppScreen4.kt",
    SOURCE / "BookingWorkspaceScreen4.kt",
    COMPONENTS,
    DATA / "ApiClient.kt",
    DATA / "AdminRepository.kt",
    DATA / "CustomerScreen3Repository.kt",
    DATA / "Models.kt",
    DATA / "BookingLifecycleModels.kt",
    DATA / "Branding.kt",
    DATA / "SecureSessionStore.kt",
    ROOT / "worker" / "src" / "phase14aw-screen4-bookings.js",
    ROOT / "worker" / "wrangler.toml",
    ROOT / "docs" / "GLOBAL_UI_RULES.md",
    ROOT / "docs" / "ADMIN_SCREEN_04_BOOKINGS.md",
]

errors = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


for path in required:
    if not path.exists():
        errors.append(f"Missing required Screen 4 foundation file: {path.relative_to(ROOT)}")

build_text = read(ANDROID / "app" / "build.gradle.kts")
if 'versionName = "0.14.5"' not in build_text:
    errors.append("Android versionName must remain 0.14.5.")
if 'versionCode = 1' not in build_text:
    errors.append("Android versionCode must remain 1.")
if 'https://zhagmag-dresses-staging.patelnims26.workers.dev' not in build_text:
    errors.append("Staging flavor must point to the approved staging Worker.")
if 'providers.gradleProperty("ZHAGMAG_PRODUCTION_BASE_URL")' not in build_text:
    errors.append("Production API URL must remain externally supplied.")

manifest_path = ANDROID / "app" / "src" / "main" / "AndroidManifest.xml"
manifest = read(manifest_path)
if 'android:usesCleartextTraffic="false"' not in manifest:
    errors.append("Android must reject cleartext HTTP traffic.")
if 'android:allowBackup="false"' not in manifest:
    errors.append("Android admin app backup must stay disabled.")

strings_en = ANDROID / "app" / "src" / "main" / "res" / "values" / "strings.xml"
if not strings_en.exists():
    errors.append("English string resources are required.")
else:
    ET.parse(strings_en)
    if re.search(r"[\u0A80-\u0AFF]", read(strings_en)):
        errors.append("Gujarati text detected in English-only Android string resources.")

res_root = ANDROID / "app" / "src" / "main" / "res"
if [p for p in res_root.glob("values-gu*") if p.exists()]:
    errors.append("Gujarati Android resource directories are not allowed.")
locale_text = read(res_root / "xml" / "locales_config.xml")
if 'android:name="en"' not in locale_text:
    errors.append("English locale must remain declared.")
if 'android:name="gu"' in locale_text:
    errors.append("Gujarati locale must not be declared in the Admin Android app.")

main_source = ANDROID / "app" / "src" / "main"
for path in main_source.rglob("*"):
    if path.is_file() and path.suffix.lower() in {".kt", ".xml"}:
        if re.search(r"[\u0A80-\u0AFF]", read(path)):
            errors.append(f"Gujarati text detected in English-only Android source: {path.relative_to(ROOT)}")

allowed_delays = {
    "CustomerScreen3ViewModel.kt": {"300"},
    "BookingListViewModel4.kt": {"300"},
    "BookingWorkspaceScreen4.kt": {"300", "2500"},
}
for path in (ANDROID / "app" / "src" / "main" / "java").rglob("*.kt"):
    text = read(path)
    relative = path.relative_to(ROOT)
    if "Color(0x" in text and path.name != "Color.kt":
        errors.append(f"Direct color literal outside design tokens: {relative}")
    if "fixedRateTimer" in text or re.search(r"while\s*\([^)]*\)\s*\{[^{}]*\bdelay\s*\(", text, re.S):
        errors.append(f"Polling/timer loop detected: {relative}")
    delay_calls = set(re.findall(r"\bdelay\s*\(\s*(\d+)\s*\)", text))
    if delay_calls and not delay_calls.issubset(allowed_delays.get(path.name, set())):
        errors.append(f"Unapproved delay/timer detected in {relative}: {sorted(delay_calls)}")
    is_repository_layer = path.parent.name == "data" and path.name.endswith("Repository.kt")
    if "/api/" in text and not is_repository_layer:
        errors.append(f"Direct API endpoint outside repository layer: {relative}")

entry = read(SOURCE / "AdminEntryScreen.kt")
for token in [
    "DynamicBrandLogo", "Mobile number", "10 digit mobile number", "Visibility",
    "Icons.Rounded.Login", "KeyboardType.Phone", ".imePadding()", ".statusBarsPadding()",
    "filter(Char::isDigit).take(10)"
]:
    if token not in entry:
        errors.append(f"Screen 1 entry/login contract missing: {token}")
if "Email or mobile" in entry or "sign_in_subtitle" in entry or "brand_subtitle" in entry:
    errors.append("Screen 1 must remain mobile-only and free of helper subtitles.")
if "var password by rememberSaveable" in entry or "var passwordVisible by rememberSaveable" in entry:
    errors.append("Screen 1 password state must not be saveable.")

chrome = read(SOURCE / "AdminMainChrome.kt")
for token in [
    "branding.shopName", "Icons.Rounded.Refresh", "dd-MM-yyyy", "EEEE",
    "Icons.Rounded.DateRange", "Icons.Rounded.Event", "Icons.Rounded.Schedule", "Asia/Kolkata"
]:
    if token not in chrome:
        errors.append(f"Shared signed-in chrome contract missing: {token}")

admin_vm = read(SOURCE / "AdminViewModel.kt")
logout_match = re.search(r"fun logout\(\)\s*\{(?P<body>.*?)\n\s*\}\n\n\s*fun loadDashboard", admin_vm, re.S)
if not logout_match:
    errors.append("Admin logout implementation could not be audited.")
else:
    logout_body = logout_match.group("body")
    for token in ["repository.logout()", "finally", "sessionStore.clear()", "authState = AuthState.SignedOut"]:
        if token not in logout_body:
            errors.append(f"Admin logout policy missing: {token}")

dashboard = read(SOURCE / "DashboardScreenV2.kt")
for token in [
    "Available Now", "Booked Pending", "Given Out", "Overdue Returns", "Missed Pickups",
    "Today Bookings", "Today Pickups", "Today Returns", "Open Booking", "New Booking",
    "pendingPickupQty", "pendingReturnQty", "AnimatedVisibility"
]:
    if token not in dashboard:
        errors.append(f"Screen 2 Dashboard contract missing: {token}")

booking_vm = read(SOURCE / "BookingLifecycleViewModel.kt")
for token in [
    "mutationVersion", "mutationVersion += 1", "loadAvailabilityForSelectedDates",
    "fillAllPickup", "fillAllReturn", "directPickup", "confirmReserved", "savePickup", "saveReturn"
]:
    if token not in booking_vm:
        errors.append(f"Booking lifecycle ViewModel contract missing: {token}")

main_activity = read(SOURCE / "MainActivity.kt")
for token in [
    "BookingListViewModel4.Factory", "CustomerScreen3ViewModel.Factory", "ZhagmagAdminRootScreen4",
    "bookingListViewModel", "customerViewModel"
]:
    if token not in main_activity:
        errors.append(f"Screen 4 MainActivity routing missing: {token}")

components = read(COMPONENTS)
for token in ["fun AppCard", "fun CompactSearchField", "height(52.dp)", "fun PrimaryButton", "fun SecondaryButton", "busyLabel"]:
    if token not in components:
        errors.append(f"Global reusable UI component contract missing: {token}")
primary_block = components.split("fun PrimaryButton", 1)[-1].split("fun SecondaryButton", 1)[0]
if "CircularProgressIndicator" in primary_block:
    errors.append("PrimaryButton must not render a circular loading spinner.")
secondary_block = components.split("fun SecondaryButton", 1)[-1].split("enum class ActionTone", 1)[0]
if "CircularProgressIndicator" in secondary_block:
    errors.append("SecondaryButton must not render a circular loading spinner.")

customer_shared = read(SOURCE / "CustomerSharedUi.kt")
for token in [
    "Choose number to call", "Choose WhatsApp number", "Primary", "Alternative",
    "CustomerFormSheet", "Alternative Mobile Number", "usableAlternateMobile"
]:
    if token not in customer_shared:
        errors.append(f"Global customer contact/form contract missing: {token}")

customer_v4 = read(SOURCE / "CustomerScreenV4.kt")
for token in [
    "New Customer", "Active", "Archived", "CompactSearchField", "onLoadMore",
    "CustomerContactChooserSheet", "CustomerFormSheet", "Total Booking", "Active Booking"
]:
    if token not in customer_v4:
        errors.append(f"Customer Screen V4 contract missing: {token}")
if re.search(r'\bPrevious\b|Page \$\{|\bNext\b', customer_v4):
    errors.append("Customer Screen V4 must not expose Previous/Page/Next pagination controls.")

customer_vm = read(SOURCE / "CustomerScreen3ViewModel.kt")
for token in ["delay(300)", "loadMore", "canLoadMore", "state.items + response.items.filter"]:
    if token not in customer_vm:
        errors.append(f"Customer incremental list contract missing: {token}")

screen4 = read(SOURCE / "AdminAppScreen4.kt")
for token in [
    "BookingListFilter4.values().toList()", 'placeholder = "Search"', "ScrollableTabRow",
    "listViewModel.loadMore()", "BookingListCard4", "customerAlternateMobile",
    "CustomerContactChooserSheet", "BookingWorkspaceScreen4", "ZhagmagAdminRootScreen4"
]:
    if token not in screen4:
        errors.append(f"Screen 4 list/root contract missing: {token}")
if re.search(r'\bPrevious\b|Page \$\{|Page 1|Page X', screen4):
    errors.append("Screen 4 list must not expose Previous/Page/Next pagination controls.")

list_vm = read(SOURCE / "BookingListViewModel4.kt")
for label in ["All", "Reserved", "Booked", "Picked Up", "Returned", "Overdue", "Cancelled"]:
    if label not in list_vm:
        errors.append(f"Screen 4 booking filter missing: {label}")
for token in ["pageSize = 10", "loadMore", "canLoadMore", "filter: BookingListFilter4 = BookingListFilter4.ALL", "OVERDUE", "CANCELLED"]:
    if token not in list_vm:
        errors.append(f"Booking +10 list ViewModel contract missing: {token}")

workspace = read(SOURCE / "BookingWorkspaceScreen4.kt")
for token in [
    'listOf("Details", "Pickup", "Return", "History")', 'Text("Booking Details"',
    'text = "Reserve"', 'text = "Confirm"', 'text = "Pickup"', 'text = "Back"', 'text = "Cancel"',
    'onBack = { if (!viewModel.actionBusy) step = 3 }', "Selected Items (", 'category == "ALL"',
    "fillAllPickup", "fillAllReturn", "BookingHistoryTab4", "formatTimestamp4",
    'DateTimeFormatter.ofPattern("dd-MM-yyyy · EEEE · h:mm a"'
]:
    if token not in workspace:
        errors.append(f"Screen 4 booking workspace contract missing: {token}")
if 'listOf("Details", "Items", "Pickup"' in workspace:
    errors.append("Booking Details must not restore the removed Items tab.")
if "Reserved Done" in screen4 or "Reserved Done" in workspace:
    errors.append("Deprecated 'Reserved Done' wording must not appear in active Screen 4 UI.")

booking_models = read(DATA / "BookingLifecycleModels.kt")
for token in ["customerAlternateMobile", "BookingTimelineEvent", "timeline", "createdAt", "updatedAt"]:
    if token not in booking_models:
        errors.append(f"Booking Screen 4 data model contract missing: {token}")

repo_text = read(DATA / "AdminRepository.kt")
allowed_prefixes = (
    "/api/auth/", "/api/public/bootstrap", "/api/admin/dashboard", "/api/admin/customers",
    "/api/admin/bookings", "/api/admin/pickups/", "/api/admin/returns/",
)
found = set(re.findall(r'"(/api/[^"?]+)', repo_text))
unknown = {endpoint for endpoint in found if not endpoint.startswith(allowed_prefixes)}
if unknown:
    errors.append(f"Unexpected Android API endpoints: {sorted(unknown)}")
for token in ["confirmationState", "/confirm", "savePickup", "saveReturn", "pageSize.coerceIn(1, 10)"]:
    if token not in repo_text:
        errors.append(f"Booking repository contract missing: {token}")

worker = read(ROOT / "worker" / "src" / "phase14aw-screen4-bookings.js")
for token in [
    'import core from "./phase14av-customers-screen3.js"', 'pageSize = Math.min(10',
    'view === "CANCELLED"', 'view === "OVERDUE"', "customer_alternate_mobile",
    "json_group_array", "LIMIT 30", "display_status", "getSessionUser"
]:
    if token not in worker:
        errors.append(f"Screen 4 Worker list contract missing: {token}")

wrangler = read(ROOT / "worker" / "wrangler.toml")
if 'main = "src/phase14aw-screen4-bookings.js"' not in wrangler:
    errors.append("Staging Wrangler entry must point to the cumulative Screen 4 wrapper.")

screen4_doc = read(ROOT / "docs" / "ADMIN_SCREEN_04_BOOKINGS.md")
global_doc = read(ROOT / "docs" / "GLOBAL_UI_RULES.md")
for token in [
    "All | Reserved | Booked | Picked Up | Returned | Overdue | Cancelled",
    "Details | Pickup | Return | History",
    "Reserve | Confirm | Pickup",
]:
    if token not in screen4_doc:
        errors.append(f"Screen 4 documentation missing: {token}")
for token in ["maximum 10 rows", "Action...", "Choose number to call", "DD-MM-YYYY"]:
    if token not in global_doc:
        errors.append(f"Global UI documentation missing: {token}")

if errors:
    print("Android Admin Screen 4 foundation audit FAILED")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Android Admin Screen 4 foundation audit passed")
