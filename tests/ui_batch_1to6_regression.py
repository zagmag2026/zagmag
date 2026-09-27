from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")

gallery = read(ANDROID / "ui" / "components" / "ItemImageGallery.kt")
cards = read(ANDROID / "ui" / "components" / "CardPatterns.kt")
screen5 = read(ANDROID / "Screen5ItemManagement.kt")
screen5_models = read(ANDROID / "Screen5ItemManagementModels.kt")
booking_models = read(ANDROID / "data" / "BookingLifecycleModels.kt")
booking_ui = read(ANDROID / "BookingWorkspaceScreen5.kt")
booking_summary = read(ANDROID / "BookingSummaryCard.kt")
booking_details = read(ANDROID / "BookingDetailsScreen6.kt")
root = read(ANDROID / "AdminAppScreen4.kt")
dashboard = read(ANDROID / "DashboardScreenV2.kt")
dashboard_models = read(ANDROID / "data" / "Models.kt")
settings = read(ANDROID / "Screen10Settings.kt")
customers = read(ANDROID / "CustomerScreenV4.kt")
customer_vm = read(ANDROID / "CustomerScreen3ViewModel.kt")
operational = read(ROOT / "worker" / "src" / "phase14be-operational-lifecycle.js")
core = read(ROOT / "worker" / "src" / "index.ts")

errors = []

def need(body: str, token: str, label: str) -> None:
    if token not in body:
        errors.append(f"{label} missing: {token}")

# 1. One shared Item gallery + all-image bundled plumbing.
for token in [
    "fun ItemImageGalleryDialog(",
    "HorizontalPager(",
    "rememberPagerState(",
    "dismissOnBackPress = true",
    "Icons.Rounded.Close",
    'text = "${pagerState.currentPage + 1} / ${images.size}"',
]:
    need(gallery, token, "Shared Item image gallery")

for token in [
    "galleryUrls: List<String> = emptyList()",
    'onClickLabel = "View item images"',
    "ItemImageGalleryDialog(",
    "imageUrls: List<String> = emptyList()",
]:
    need(cards, token, "Shared Item thumbnail gallery hook")

for body, token, label in [
    (screen5_models, "val imageUrls: List<String>", "Screen 5 all-image model"),
    (screen5, "imageUrls = item.imageUrls", "Screen 5 Item card gallery"),
    (screen5, "imageUrls = sourceItem.imageUrls", "Screen 5 Related Item gallery"),
    (screen5, "galleryUrls = photos.map { it.url }", "Screen 5 editor gallery"),
    (booking_models, "val imageUrls: List<String> = emptyList()", "Booking all-image models"),
    (booking_models, 'stringOrNull("images_json")', "Booking images parser"),
    (booking_ui, "imageUrls = item.imageUrls", "Booking editor/detail shared Item row gallery"),
    (booking_summary, "imageUrls = item.imageUrls", "Booking summary image gallery"),
    (booking_details, "imageUrls = item.imageUrls", "Booking detail image gallery"),
    (root, "imageUrls = item.imageUrls", "Booking list preview gallery"),
    (dashboard, '"Category-wise Inventory"', "Dashboard category inventory KPI section"),
    (dashboard, "DashboardCategoryCard(", "Dashboard category drilldown card"),
    (operational, "'images_json',x.images_json", "Active Worker bundled gallery payload"),
    (operational, "json_group_array(image_url)", "Active Worker bundled gallery query"),
    (core, "AS images_json", "Core booking bundled gallery payload"),
]:
    need(body, token, label)

# 2. WhatsApp management is outside Settings and OWNER-only through More.
if 'WHATSAPP("WhatsApp Templates")' in settings:
    errors.append("WhatsApp Templates must not remain a Settings tab.")
for token in [
    "internal fun Screen10WhatsAppManagement(",
    'title = "WhatsApp Centre"',
    "currentUser.role != UserRole.OWNER",
    "screen10WhatsAppItems(",
    '"Global Template Language"',
]:
    need(settings, token, "Standalone WhatsApp Centre")
for token in [
    "WHATSAPP_MANAGEMENT",
    "Screen10WhatsAppManagement(",
    'title = "WhatsApp Centre"',
    'subtitle = "Templates, actions & language"',
]:
    need(root, token, "More WhatsApp Centre navigation")

# 3. Booking no-result Quick Create is verified on the runtime-active Screen 5 path.
for token in [
    "val searchState = viewModel.customerSearchUiState",
    "CustomerSearchUiState.SEARCHING",
    "CustomerSearchUiState.NO_RESULT",
    "val showQuickCreate = canManageCustomers",
    '"Create new customer"',
    "customerPrefillFromQuery5(trimmedQuery)",
    "viewModel.customerSearchError",
    "InlineStatusMessage(",
]:
    need(booking_ui, token, "Active Booking Quick Create")

if "BookingWorkspaceScreen4.kt" in str(booking_ui):
    errors.append("Legacy BookingWorkspaceScreen4 must not be used as runtime proof.")

# 4. Archived permanent delete keeps Archived until the authoritative global total reaches zero.
delete_start = customer_vm.find("fun deletePermanently(id: String)")
delete_end = customer_vm.find("fun composeWhatsApp(", delete_start)
if delete_start < 0 or delete_end < 0:
    errors.append("Could not inspect permanent-delete tab retention flow.")
else:
    delete_block = customer_vm[delete_start:delete_end]
    for token in [
        "archivedResponse.summary.archivedCustomers > 0",
        "archived = true",
        "search = snapshot.search",
        "sort = snapshot.sort",
        "archived = false",
        'search = ""',
    ]:
        need(delete_block, token, "Archived delete tab retention")
    if "mutateAndReset" in delete_block:
        errors.append("Permanent delete must not use the old unconditional Active-tab reset.")

# 5. Customer booking badges are one fixed 50/50 centered row.
for token in [
    "CustomerBookingStatBadge(",
    '"Total Booking · ${customer.totalBookings}"',
    '"Active Booking · ${customer.activeBookings}"',
    "textAlign = TextAlign.Center",
]:
    need(customers, token, "Customer booking stat row")
card_start = customers.find("private fun CustomerCard4(")
if card_start >= 0:
    card_block = customers[card_start:]
    if card_block.count("modifier = Modifier.weight(1f)") < 7:
        errors.append("Customer card must retain equal-width booking badges/actions.")

# 6. Active Customer actions stay on one line in exact fixed order.
if "ResponsiveSoftActionGrid(" in customers:
    errors.append("Customer card actions must not use the wrapping responsive grid.")
action_labels = [
    'label = "Edit"',
    'label = "Archive"',
    'label = "Booking"',
    'label = "Call"',
    'label = "WhatsApp"',
]
positions = [customers.find(token, card_start) for token in action_labels]
if any(pos < 0 for pos in positions) or positions != sorted(positions):
    errors.append("Customer action order must be Edit | Archive | Booking | Call | WhatsApp.")

if errors:
    print("UI batch 1-6 regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("UI batch 1-6 regression PASS")
