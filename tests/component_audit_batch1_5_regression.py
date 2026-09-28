from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"

def read(name: str) -> str:
    return (ANDROID / name).read_text(encoding="utf-8")

components = read("ui/components/Components.kt")
cards = read("ui/components/CardPatterns.kt")
type_kt = read("ui/theme/Type.kt")
header = read("AdminMainChrome.kt")
root = read("AdminAppScreen4.kt")
booking = read("BookingWorkspaceScreen5.kt")
vm = read("BookingLifecycleViewModel.kt")
screen5 = read("Screen5ItemManagement.kt")
settings = read("Screen10Settings.kt")
contact = read("CustomerSharedUi.kt")
customers = read("CustomerScreenV4.kt")
dashboard = read("DashboardScreenV2.kt")
details = read("BookingDetailsScreen6.kt")
activity = read("MainActivity.kt")
gallery = read("ui/components/ItemImageGallery.kt")

errors = []

def need(body: str, token: str, label: str) -> None:
    if token not in body:
        errors.append(f"{label}: missing {token}")

# Runtime route must prove Screen5, never legacy Screen4.
need(activity, "ZhagmagAdminRootScreen4(", "active root")
need(root, "BookingWorkspaceScreen5(", "active booking route")
if "BookingWorkspaceScreen4(" in root:
    errors.append("active root routes to retired BookingWorkspaceScreen4")

# Batch 1: header/navigation/tabs/cards.
for token in [
    'ZoneId.of("Asia/Kolkata")',
    "delay((60_000L - elapsedInMinute).coerceAtLeast(1_000L))",
    "modifier = Modifier.size(48.dp)",
    "overflow = TextOverflow.Ellipsis",
    "fontSize = 12.sp",
]:
    need(header, token, "header/date hardening")
need(root, "ActionTone.SUCCESS -> StatusSuccessSoft to StatusSuccess", "MoreHub SUCCESS tone")
for token in ['title = "Category & Items"', 'title = "Reports"', 'title = "Bills"', 'title = "WhatsApp Centre"', 'title = "Users"', 'title = "Settings"']:
    need(root, token, "paired More hub modules")

need(cards, "minSideBySideWidth: Dp = 300.dp", "360dp triple breakpoint")
need(cards, "minSideBySideWidth: androidx.compose.ui.unit.Dp = 300.dp", "360dp pair breakpoint")
for body, label in [(root, "root tabs"), (screen5, "screen5 tabs"), (settings, "settings tabs")]:
    if "TabRow(" in body or "ScrollableTabRow(" in body:
        errors.append(f"{label}: raw tab implementation remains")

tab_family = components[components.find("fun AppCompactFixedTabs("):components.find("data class AppFilterOption")]
fixed_tabs = tab_family[tab_family.find("fun AppCompactFixedTabs("):tab_family.find("fun AppCompactScrollableTabs(")]
scroll_tabs = tab_family[tab_family.find("fun AppCompactScrollableTabs("):]
for token in [
    "TabRow(",
    "modifier = modifier.fillMaxWidth()",
    "Modifier.height(44.dp)",
    "style = MaterialTheme.typography.labelMedium",
    "fontSize = 16.sp",
    "FontWeight.SemiBold",
    "FontWeight.Normal",
]:
    need(fixed_tabs, token, "Run 79 equal-width fixed tabs")
for token in [
    "ScrollableTabRow(",
    "edgePadding = 0.dp",
    "minTabWidth: androidx.compose.ui.unit.Dp = 96.dp",
    "Modifier.widthIn(min = minTabWidth).height(44.dp)",
    "style = MaterialTheme.typography.labelMedium",
    "FontWeight.SemiBold",
    "FontWeight.Normal",
]:
    need(scroll_tabs, token, "Run 79 scrollable tabs")
if "AppCompactScrollableTabs(" in fixed_tabs:
    errors.append("Fixed tabs must not delegate to scrollable tabs.")
if ".horizontalScroll(rememberScrollState())" in tab_family or "minTabWidth: androidx.compose.ui.unit.Dp = 0.dp" in tab_family:
    errors.append("Shared tabs regressed to the post-Run-79 content-width implementation.")
need(settings, "AppCompactScrollableTabs(", "Settings main/WhatsApp tabs use Run 79 scrollable tabs")
need(details, "AppCompactScrollableTabs(", "Booking Details uses Run 79 scrollable tabs")
need(screen5, "AppCompactFixedTabs(", "Category & Items uses fixed tabs")

# Batch 2: sections/actions/search and runtime quick-create.
need(cards, "LocalDensity.current.fontScale.coerceIn(1f, 2f)", "font-scale safe section notch")
need(components, "style = MaterialTheme.typography.labelMedium", "SoftActionButton readable label")
need(components, "style = MaterialTheme.typography.bodyMedium", "CompactNewActionButton readable label")
for token in [
    "searching: Boolean = false",
    'contentDescription = "Clear search"',
    "CircularProgressIndicator(",
]:
    need(components, token, "shared compact search")
for token in [
    "selectAllOnFocus: Boolean = false",
    "effectiveSelectAllOnFocus = selectAllOnFocus",
    "effectiveKeyboardOptions.keyboardType == KeyboardType.Number",
    "effectiveKeyboardOptions.keyboardType == KeyboardType.Decimal",
    "TextRange(0, fieldValue.text.length)",
]:
    need(components, token, "shared numeric select-all contract")
need(booking, 'placeholder = "Advance Amount"', "Booking Advance placeholder")
if 'supportingText = "Cash advance · default ₹0"' in booking:
    errors.append("Booking Advance old helper text must remain removed")
for token in [
    "enum class CustomerSearchUiState { IDLE, SEARCHING, RESULTS, NO_RESULT, ERROR }",
    "val customerSearchUiState: CustomerSearchUiState",
    "customerSearchCompletedQuery != query",
    "requestSerial == customerSearchRequestSerial",
]:
    need(vm, token, "explicit deterministic customer search")
for token in [
    "val searchState = viewModel.customerSearchUiState",
    "searching = searchState == CustomerSearchUiState.SEARCHING",
    "searchState == CustomerSearchUiState.NO_RESULT",
    "val showQuickCreate = canManageCustomers",
    "customerPrefillFromQuery5(trimmedQuery)",
    '"Create new customer"',
]:
    need(booking, token, "active quick-create")
need(booking, "return trimmed to", "typed-name prefill")
need(booking, 'return "" to digits', "typed-mobile prefill")

# Batch 3: canonical selectors, date timezone, item rows.
for token in ["fun AppSelectField(", "fun AppMultiSelectField(", '"No options found."']:
    need(components, token, "shared selector family")
need(screen5, "AppSelectField(", "Screen5 shared selector")
need(screen5, "AppMultiSelectField(", "Screen5 shared multi selector")
need(settings, "AppSelectField(", "Settings shared selector")
need(components, "val pickerZone = zone", "business timezone date bounds")
if "showDatePicker4" in booking or "DatePickerField4" in booking:
    errors.append("active Booking still contains legacy date picker")
need(booking, "AppDatePickerField(", "active shared date picker")
need(booking, "AppItemDisplayRow(", "active canonical item row")
need(booking, "imageUrls = item.imageUrls", "active item gallery bundle")

# Batch 4: external launch feedback and shared dialogs.
for body, label in [(root, "Bookings"), (customers, "Customers"), (details, "Booking Details")]:
    need(body, "Unable to open", f"{label} external launch feedback")
if "CustomerContactConfirmationSheet" in dashboard or "launchCustomerWhatsApp" in dashboard:
    errors.append("KPI Dashboard must not retain operational contact actions.")
need(root, "AppConfirmDialog(", "shared exit confirmation")
need(header, "AppConfirmDialog(", "shared logout confirmation")
need(contact, "Primary-mobile-only contact surface", "primary-only runtime contact")
if "alternate = booking.customerAlternateMobile" in root:
    errors.append("active booking contact still wires alternate mobile")

# Batch 5: empty/loading/typography/responsive layout.
for token in [
    "enum class EmptyStateVariant { NORMAL, SEARCH_NO_RESULT, FILTERED_NO_RESULT }",
    'fun LoadingState(label: String = "Loading…")',
    "rememberSaveable(selected)",
    'rememberSaveable { mutableStateOf("") }',
]:
    need(components, token, "empty/loading/filter state")
need(booking, "ResponsiveCompactPair(", "Booking narrow search/filter adaptation")
need(screen5, "ResponsiveCompactPair(", "Screen5 narrow search/filter adaptation")
if screen5.count("expanded = true") < 2:
    errors.append("Screen5 Custom Field and Item long forms must use expanded IME-safe sheet scaffolds.")
if settings.count("Modifier.fillMaxSize().imePadding()") < 2:
    errors.append("Settings and WhatsApp Centre long forms must keep IME-padded scrolling roots.")
for token in [
    "headlineMedium = TextStyle(fontSize = 22.sp",
    "headlineSmall = TextStyle(fontSize = 20.sp",
    "titleMedium = TextStyle(fontSize = 16.sp",
    "bodyMedium = TextStyle(fontSize = 14.sp",
    "bodySmall = TextStyle(fontSize = 12.sp",
    "labelMedium = TextStyle(fontSize = 13.sp",
    "labelSmall = TextStyle(fontSize = 11.sp",
]:
    need(type_kt, token, "semantic typography")

# Existing full-screen viewer contract must remain intact.
for token in [
    "HorizontalPager(",
    "rememberPagerState(",
    "graphicsLayer",
    "awaitEachGesture",
    "pressedPointers >= 2",
    "transformOwnsGesture = scale > 1.01f",
    "event.calculateZoom()",
    "event.calculatePan()",
    "detectTapGestures",
    "Color.Black",
    "ContentScale.Fit",
]:
    need(gallery, token, "full-screen item viewer")

if errors:
    print("Component Audit Batch 1-5 regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Component Audit Batch 1-5 regression PASS")
