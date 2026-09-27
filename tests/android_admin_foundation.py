from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android"
SOURCE = ANDROID / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"
DATA = SOURCE / "data"
COMPONENTS = SOURCE / "ui" / "components" / "Components.kt"
CARD_PATTERNS = SOURCE / "ui" / "components" / "CardPatterns.kt"
BOOKING_SUMMARY_CARD = SOURCE / "BookingSummaryCard.kt"
BOOKING_CARD_ACTIONS = SOURCE / "BookingCardActions.kt"

errors = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def require_tokens(path: Path, tokens, label: str):
    text = read(path)
    for token in tokens:
        if token not in text:
            errors.append(f"{label} missing: {token}")
    return text


required = [
    ANDROID / "settings.gradle.kts",
    ANDROID / "build.gradle.kts",
    ANDROID / "app" / "build.gradle.kts",
    ANDROID / "app" / "src" / "main" / "AndroidManifest.xml",
    SOURCE / "MainActivity.kt",
    SOURCE / "AdminEntryScreen.kt",
    SOURCE / "AdminMainChrome.kt",
    SOURCE / "DashboardScreenV2.kt",
    SOURCE / "AdminDataFreshness.kt",
    SOURCE / "CustomerScreenV3.kt",
    SOURCE / "CustomerScreenV4.kt",
    SOURCE / "CustomerSharedUi.kt",
    SOURCE / "CustomerScreen3ViewModel.kt",
    SOURCE / "AdminViewModel.kt",
    SOURCE / "BookingLifecycleViewModel.kt",
    SOURCE / "BookingListViewModel4.kt",
    SOURCE / "AdminAppScreen4.kt",
    SOURCE / "BookingWorkspaceScreen5.kt",
    SOURCE / "BookingDetailsScreen6.kt",
    SOURCE / "Screen5ItemManagement.kt",
    SOURCE / "Screen5ItemManagementModels.kt",
    SOURCE / "Screen5ItemManagementRepository.kt",
    SOURCE / "Screen5ItemManagementViewModel.kt",
    SOURCE / "Screen9Users.kt",
    SOURCE / "Screen9UsersModels.kt",
    SOURCE / "Screen9UsersRepository.kt",
    SOURCE / "Screen9UsersViewModel.kt",
    SOURCE / "Screen10Settings.kt",
    SOURCE / "Screen10SettingsModels.kt",
    SOURCE / "Screen10SettingsRepository.kt",
    SOURCE / "Screen10SettingsViewModel.kt",
    BOOKING_SUMMARY_CARD,
    BOOKING_CARD_ACTIONS,
    COMPONENTS,
    CARD_PATTERNS,
    DATA / "ApiClient.kt",
    DATA / "AdminRepository.kt",
    DATA / "CustomerScreen3Repository.kt",
    DATA / "Models.kt",
    DATA / "BookingLifecycleModels.kt",
    DATA / "Branding.kt",
    DATA / "SecureSessionStore.kt",
    ROOT / "worker" / "src" / "phase14aw-screen4-bookings.js",
    ROOT / "worker" / "src" / "phase14ba-screen5-item-management.js",
    ROOT / "worker" / "src" / "phase14bb-screen9-users.js",
    ROOT / "worker" / "src" / "phase14bc-screen10-settings.js",
    ROOT / "worker" / "src" / "phase14bg-settings-whatsapp.js",
    ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js",
    ROOT / "worker" / "src" / "whatsapp-template-registry.ts",
    ROOT / "database" / "migrations" / "0014_staff_permissions.sql",
    ROOT / "database" / "migrations" / "0015_whatsapp_template_linking.sql",
    ROOT / "database" / "migrations" / "0018_settings_whatsapp_expansion.sql",
    ROOT / "database" / "migrations" / "0020_global_reports_whatsapp_activity.sql",
    ROOT / "worker" / "wrangler.toml",
    ROOT / "docs" / "PROJECT_RULES.md",
    ROOT / "docs" / "GLOBAL_UI_RULES.md",
    ROOT / "docs" / "UI_GUIDELINES.md",
    ROOT / "docs" / "CUSTOMER_MASTER.md",
    ROOT / "docs" / "ADMIN_SCREEN_04_BOOKINGS.md",
    ROOT / "docs" / "ADMIN_ANDROID_SCREEN4_HARDENING.md",
    ROOT / "docs" / "ADMIN_SCREEN_05_ITEM_MANAGEMENT.md",
    ROOT / "docs" / "ADMIN_SCREEN_09_USERS.md",
    ROOT / "docs" / "ADMIN_SCREEN_10_SETTINGS.md",
]

for path in required:
    if not path.exists():
        errors.append(f"Missing required Admin foundation file: {path.relative_to(ROOT)}")

build_text = read(ANDROID / "app" / "build.gradle.kts")
for token, message in [
    ('versionName = "0.14.5"', "Android versionName must remain 0.14.5."),
    ('versionCode = 1', "Android versionCode must remain 1."),
    (
        'providers.gradleProperty("ZHAGMAG_STAGING_BASE_URL")',
        "Staging API URL must be externally supplied through ZHAGMAG_STAGING_BASE_URL.",
    ),
    (
        '"$stagingBaseUrl"',
        "Staging flavor must use the externally supplied staging API URL.",
    ),
    (
        'providers.gradleProperty("ZHAGMAG_PRODUCTION_BASE_URL")',
        "Production API URL must remain externally supplied.",
    ),
]:
    if token not in build_text:
        errors.append(message)
manifest = read(ANDROID / "app" / "src" / "main" / "AndroidManifest.xml")
for token in [
    'android:usesCleartextTraffic="false"',
    'android:allowBackup="false"',
    'android:windowSoftInputMode="adjustResize"',
]:
    if token not in manifest:
        errors.append(f"Android manifest global safety contract missing: {token}")

strings_en = ANDROID / "app" / "src" / "main" / "res" / "values" / "strings.xml"
if not strings_en.exists():
    errors.append("English string resources are required.")
else:
    ET.parse(strings_en)
    if re.search(r"[\u0A80-\u0AFF]", read(strings_en)):
        errors.append("Gujarati text detected in English-only Android strings.")

res_root = ANDROID / "app" / "src" / "main" / "res"
if [p for p in res_root.glob("values-gu*") if p.exists()]:
    errors.append("Gujarati Android resource directories are not allowed.")
locale_text = read(res_root / "xml" / "locales_config.xml")
if 'android:name="en"' not in locale_text or 'android:name="gu"' in locale_text:
    errors.append("Admin Android locale config must remain English-only.")

for path in (ANDROID / "app" / "src" / "main").rglob("*"):
    if not path.is_file() or path.suffix.lower() not in {".kt", ".xml"}:
        continue
    text = read(path)
    if re.search(r"[\u0A80-\u0AFF]", text):
        errors.append(f"Gujarati text detected in Android source: {path.relative_to(ROOT)}")
    if path.suffix == ".kt" and "Color(0x" in text and path.name != "Color.kt":
        errors.append(f"Direct color literal outside design tokens: {path.relative_to(ROOT)}")
    if "fixedRateTimer" in text or re.search(r"while\s*\([^)]*\)\s*\{[^{}]*\bdelay\s*\(", text, re.S):
        errors.append(f"Polling/timer loop detected: {path.relative_to(ROOT)}")

require_tokens(
    SOURCE / "AdminEntryScreen.kt",
    ["DynamicBrandLogo", "Mobile number", "10 digit mobile number", "KeyboardType.Phone", ".imePadding()", "password.length >= 8"],
    "Screen 1 contract",
)
require_tokens(
    DATA / "Branding.kt",
    ['const val DEFAULT_SHOP_NAME = "Your Shop Name"'],
    "Dynamic branding fallback contract",
)
require_tokens(
    DATA / "ApiClient.kt",
    [
        "Unable to connect. Check your internet connection and try again.",
        "Service is temporarily unavailable. Please try again.",
        "catch (network: IOException)",
    ],
    "Global user-facing network error contract",
)
require_tokens(
    SOURCE / "AdminMainChrome.kt",
    [
        "branding.shopName", "Icons.Rounded.Refresh", "dd-MM-yyyy", "EEEE", "Asia/Kolkata",
        "Modifier.padding(horizontal = 6.dp, vertical = 4.dp)",
        "Box(Modifier.size(24.dp)", "Modifier.size(14.dp)",
        "fontSize = 12.sp", "lineHeight = 16.sp",
        "Spacer(Modifier.width(4.dp))", ".height(24.dp)",
    ],
    "Shared signed-in chrome / compact Date Day Time contract",
)

dashboard = require_tokens(
    SOURCE / "DashboardScreenV2.kt",
    [
        'AppScreenTitle("Dashboard"', "CompactNewBookingButton(",
        '"Today Overview"', '"Booking Status"', '"Payment & Billing"', '"Inventory"', '"Customers"', '"Category-wise Inventory"',
        "DashboardKpiCard(", "DashboardCategoryCard(", "BoxWithConstraints(", "maxWidth >= 720.dp", "DashboardKpiAction",
    ],
    "Dashboard KPI contract",
)
for forbidden in [
    "DashboardSection(", "DashboardBookingCard(", "BookingSummaryCard(",
    "CustomerContactConfirmationSheet", "AnimatedVisibility(", "ExpandLess", "ExpandMore",
]:
    if forbidden in dashboard:
        errors.append(f"Dashboard KPI redesign must not restore operational queue/detail UI: {forbidden}")
if 'Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")' in dashboard:
    errors.append("Dashboard page header Refresh action must remain removed.")
header_title_index = dashboard.find('AppScreenTitle("Dashboard"')
header_new_index = dashboard.find("CompactNewBookingButton(", header_title_index)
if header_title_index < 0 or header_new_index < 0 or header_new_index < header_title_index:
    errors.append("Dashboard header must keep compact New Booking beside the Dashboard title.")

booking_actions = require_tokens(
    BOOKING_CARD_ACTIONS,
    [
        "internal fun CompactNewBookingButton", "internal fun RowScope.BookingCardActions",
        "CompactNewActionButton(", "ResponsiveSoftActionGrid(", "SoftActionSpec(",
        '"View"', '"Edit"', '"Bill"', '"Call"', '"WhatsApp"',
        "ActionTone.INFO", "ActionTone.PURPLE", "ActionTone.BRAND", "ActionTone.SUCCESS", "ActionTone.TEAL",
    ],
    "Shared booking-card action contract",
)
if "internal fun CompactNewActionButton" in booking_actions:
    errors.append("Generic CompactNewActionButton must live in shared ui/components, not BookingCardActions.")
if booking_actions.count("SoftActionSpec(") != 5:
    errors.append("Shared booking-card actions must define View/Edit/Bill/Call/WhatsApp responsive action specs, with Bill optional per screen.")

components = require_tokens(
    COMPONENTS,
    [
        "fun AppCard", "fun CompactSearchField", "height(52.dp)", "fun InfoValueRow", "fun InlineMessage",
        "fun AppScreenTitle(", "fontSize = 22.sp", "lineHeight = 28.sp", "FontWeight.Bold",
        "fun AppBackHeader(", "Modifier.size(20.dp)", "AppScreenTitle(title = title",
        "fun CompactNewActionButton(", "padding(horizontal = 10.dp, vertical = 6.dp)",
        "Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)", "Modifier.size(16.dp)",
        "fun AppButtonContent(", "fun PrimaryButton(", "fun SecondaryButton(", "fun DangerButton(", "fun DangerTextButton(",
        "fun AppFormSheetScaffold(", "showCloseAction: Boolean = true", "expanded: Boolean = false",
        "defaultMinSize(minHeight = 48.dp)", "CircularProgressIndicator(", "strokeWidth = 2.dp",
        "Box(modifier = Modifier.size(18.dp)", "overflow = TextOverflow.Ellipsis",
        "fun AppCompactFixedTabs(", "fun AppCompactScrollableTabs(", "height(44.dp)",
        "TabRow(", "ScrollableTabRow(", "edgePadding = 0.dp", "minTabWidth: androidx.compose.ui.unit.Dp = 96.dp",
        "style = MaterialTheme.typography.labelMedium", "FontWeight.SemiBold", "FontWeight.Normal", "fun AppFeedbackHost(",
        "fun AppSingleSelectFilter(", "fun AppMultiSelectFilter(", "fun AppDatePickerField(",
        "minDate: LocalDate? = null", "maxDate: LocalDate? = null", "showDayOfWeek: Boolean = false",
        'DateTimeFormatter.ofPattern("dd-MM-yyyy"', "datePicker.minDate", "datePicker.maxDate",
        "fun StatusBadge(", "overflow = TextOverflow.Ellipsis",
        "fun rememberReliableKeyboardDismiss()", "keyboardOptions.copy(imeAction = ImeAction.Done)",
        "keyboardActions.onDone?.invoke(this)", "onSearch = {", "onSubmit?.invoke()",
        "effectiveSelectAllOnFocus = selectAllOnFocus", "KeyboardType.Number", "TextRange(0, fieldValue.text.length)",
        "fun InlineStatusMessage(", "fun FeedbackMessage(", "MessageTone.SUCCESS", "MessageTone.ERROR", "MessageTone.WARNING", "MessageTone.INFO",
        "FeedbackDeduper", "durationMillis: Long = 3500L", "AnimatedVisibility(", 'contentDescription = "Dismiss"',
        "Popup(", "PopupProperties(focusable = false)", "WindowInsets.statusBars", "offset = popupOffset", ".fillMaxWidth()",
    ],
    "Global reusable UI component contract",
)
if "Surface(\n        modifier = Modifier.fillMaxWidth()" in components.split("fun InlineMessage", 1)[-1]:
    errors.append("Operational action feedback must not revert to a permanent inline full-width card.")

screen5_sheet_text = read(SOURCE / "Screen5ItemManagement.kt")
if screen5_sheet_text.count("onDismissRequest = requestDismiss") < 3:
    errors.append("Screen 5 Category/Custom Field/Item sheets must use guarded dirty-aware dismiss.")
for token in ["val performDismiss: () -> Unit =", "val requestDismiss: () -> Unit =", "if (!busy && !uploading)", "onDismissRequest = requestDismiss", "onDismiss = requestDismiss"]:
    if token not in screen5_sheet_text:
        errors.append(f"Screen 5 Item editor guarded dismiss missing: {token}")
for token in [
    "AppFormSheetScaffold(", "private fun Screen5ItemViewSheet(", "private fun Screen5RelatedGroupViewSheet(",
    ".fillMaxHeight(0.90f)", ".weight(1f)", 'SecondaryButton("Close", onDismiss, modifier = Modifier.fillMaxWidth())',
]:
    if token not in screen5_sheet_text:
        errors.append(f"Screen 5 bottom-sheet hardening missing: {token}")

screen9_sheet_text = read(SOURCE / "Screen9Users.kt")
for token in [
    "AppFormSheetScaffold(", 'saveLabel = "Reset Password"',
    "onDismissRequest = requestDismiss", "expanded = true",
]:
    if token not in screen9_sheet_text:
        errors.append(f"Users shared form-sheet contract missing: {token}")

screen10_sheet_text = read(SOURCE / "Screen10Settings.kt")
for token in [
    "AppFormSheetScaffold(", "onDismissRequest = requestDismiss",
    'saveLabel = if (existing == null) "Add Template" else "Save"', "expanded = true",
]:
    if token not in screen10_sheet_text:
        errors.append(f"Settings shared form-sheet contract missing: {token}")

for path, label, tokens in [
    (SOURCE / "CustomerScreenV4.kt", "Customers destructive confirmations", ["AppDestructiveConfirmDialog(", 'title = "Archive customer?"', 'requiredPhrase = "DELETE CUSTOMER"', 'confirmLabel = "Delete permanently"']),
    (SOURCE / "BookingDetailsScreen6.kt", "Booking destructive confirmations", ["AppDestructiveConfirmDialog(", 'confirmLabel = "Cancel Booking"', 'text = "Close Remaining Items"']),
    (SOURCE / "Screen5ItemManagement.kt", "Item Management destructive confirmations", ["AppDestructiveConfirmDialog(", 'text = "Delete Field & Data"']),
    (SOURCE / "Screen8Reports.kt", "Reports button hardening", ["DangerButton(", "AppDestructiveConfirmDialog(", "pendingPdfAction == Screen8PdfAction.VIEW", "pendingPdfAction == Screen8PdfAction.SHARE", "pendingPdfAction == Screen8PdfAction.DOWNLOAD", "state.exportLoading || pendingPdfAction != null"]),
    (SOURCE / "Screen9Users.kt", "Users destructive confirmations", ["AppDestructiveConfirmDialog(", 'confirmLabel = "Archive"']),
    (SOURCE / "Screen10Settings.kt", "Settings destructive confirmations", ["AppDestructiveConfirmDialog(", 'confirmLabel = "Discard"', 'confirmLabel = "Delete"']),
]:
    require_tokens(path, tokens, label)

booking_feedback = require_tokens(
    SOURCE / "BookingDetailsScreen6.kt",
    [
        "InlineStatusMessage(", "AppFeedbackHost(",
        "successMessage = viewModel.actionNotice", "errorMessage = contactLaunchError ?: viewModel.actionError", "viewModel.clearActionFeedback()",
        '"Pickup completed."', '"No pickup has been recorded."',
        '"Return completed."', '"No items are ready to return."',
    ],
    "Booking Details passive/action feedback contract",
)

require_tokens(
    SOURCE / "BookingDetailsScreen6.kt",
    [
        "CustomerContactConfirmationSheet(", "customerName = detail.booking.customerName",
        "onCall = { confirmCall = true }",
        'SoftActionSpec(Icons.Rounded.Call, "Call"',
        'SoftActionSpec(Icons.Rounded.Chat, "WhatsApp"',
        "ResponsiveSoftActionGrid(",
    ],
    "Booking Details shared contact contract",
)
if 'text = "Thank You"' in booking_feedback or "onThankYou" in booking_feedback:
    errors.append("Booking Details must not restore the separate Thank You button.")
call_pos = booking_feedback.find('SoftActionSpec(Icons.Rounded.Call, "Call"')
whatsapp_pos = booking_feedback.find('SoftActionSpec(Icons.Rounded.Chat, "WhatsApp"', call_pos)
if call_pos < 0 or whatsapp_pos < 0 or call_pos > whatsapp_pos:
    errors.append("Booking Details contact actions must keep Call before WhatsApp in the responsive action group.")
if 'InlineMessage(\n                if (detail.items.any { it.givenQty > 0 })' in booking_feedback:
    errors.append("Pickup passive state must not use the transient InlineMessage alias.")
if 'InlineMessage(\n                if (detail.booking.displayStatus.equals("FULL_RETURN", true))' in booking_feedback:
    errors.append("Return passive state must not use the transient InlineMessage alias.")

require_tokens(
    SOURCE / "BookingLifecycleViewModel.kt",
    ["fun clearActionFeedback()", "actionError = null", "actionNotice = null"],
    "Booking transient feedback clear contract",
)

for spacing_path, spacing_label in [
    (SOURCE / "DashboardScreenV2.kt", "Dashboard"),
    (SOURCE / "CustomerScreenV4.kt", "Customers"),
    (SOURCE / "AdminAppScreen4.kt", "Bookings/More"),
    (SOURCE / "BookingWorkspaceScreen5.kt", "Booking editor"),
    (SOURCE / "BookingDetailsScreen6.kt", "Booking details"),
    (SOURCE / "Screen5ItemManagement.kt", "Category & Items"),
    (SOURCE / "Screen8Reports.kt", "Reports"),
    (SOURCE / "Screen9Users.kt", "Users"),
    (SOURCE / "Screen10Settings.kt", "Settings"),
]:
    spacing_text = read(spacing_path)
    if "AppSpacing.xs" not in spacing_text or "MainScreenDateRow(" not in spacing_text:
        errors.append(f"{spacing_label} must preserve the shared compact top spacing/date-row rhythm.")

require_tokens(
    SOURCE / "CustomerScreen3ViewModel.kt",
    ["val loadError: String? = null", "val loadMoreError: String? = null", "loadError = message"],
    "Customer passive load feedback state",
)
require_tokens(
    SOURCE / "CustomerScreen3ViewModel.kt",
    ['"Customer added successfully."', '"Customer updated successfully."'],
    "Customer action-specific success feedback",
)
require_tokens(
    SOURCE / "CustomerScreenV4.kt",
    ["InlineStatusMessage(state.loadError, MessageTone.ERROR)", "AppFeedbackHost(", "successMessage = state.notice", "AppCompactFixedTabs("],
    "Customer passive/action feedback rendering",
)
require_tokens(
    SOURCE / "Screen5ItemManagementViewModel.kt",
    ["val loadError: String? = null", "fun clearActionFeedback()", "loadError = message"],
    "Screen 5 passive/action feedback state",
)
require_tokens(
    SOURCE / "Screen5ItemManagement.kt",
    ["vm.clearActionFeedback()", "InlineStatusMessage(state.loadError, MessageTone.ERROR)", "AppFeedbackHost(", "successMessage = state.notice"],
    "Screen 5 passive/action feedback rendering",
)
require_tokens(
    SOURCE / "Screen8ReportsViewModel.kt",
    [
        "val loadError: String? = null", "loadError = userMessage(error)", "message = null",
        "state = state.copy(\n            loadingReport = true,",
        "state = state.copy(exportLoading = true, error = null)\n        viewModelScope.launch {",
        "state = state.copy(presetBusy = true, error = null, message = null)\n        viewModelScope.launch {",
    ],
    "Reports passive/action feedback state",
)
require_tokens(
    SOURCE / "Screen8Reports.kt",
    ["InlineStatusMessage(state.loadError, MessageTone.ERROR)", "AppFeedbackHost(", "successMessage = state.message", "errorMessage = state.error"],
    "Reports passive/action feedback rendering",
)
require_tokens(
    SOURCE / "Screen9UsersViewModel.kt",
    ["val loadError: String? = null", "val loadMoreError: String? = null", "loadError = message"],
    "Users passive/action feedback state",
)
require_tokens(
    SOURCE / "Screen9Users.kt",
    ["InlineStatusMessage(state.loadError, MessageTone.ERROR)", "AppFeedbackHost(", "successMessage = state.message", "vm.clearFeedback()"],
    "Users passive/action feedback rendering",
)
require_tokens(
    SOURCE / "Screen10SettingsViewModel.kt",
    [
        "val loadError: String? = null", "loadError = userMessage(error)",
        "state = state.copy(actionBusy = true, loadError = null, error = null, message = null)\n        viewModelScope.launch {",
        "state = state.copy(logoUploading = true, error = null, message = null)\n        viewModelScope.launch {",
        'val message = "From Date cannot be after To Date."',
    ],
    "Settings passive/action feedback state",
)
require_tokens(
    SOURCE / "Screen10Settings.kt",
    ["private fun Screen10AuditDateFilters(", "AppDatePickerField(", "ResponsiveCompactPair(", "allowClear = true"],
    "Settings Audit shared date-range UI",
)
require_tokens(
    SOURCE / "Screen10Settings.kt",
    ["InlineStatusMessage(state.loadError, MessageTone.ERROR)", "AppFeedbackHost(", "successMessage = state.message", "vm.clearFeedback()", "tabName = Screen10Tab.entries[index].name"],
    "Settings passive/action feedback rendering",
)
require_tokens(
    ROOT / "docs" / "GLOBAL_UI_RULES.md",
    ["Passive UI activity must never create or replay a transient popup", "InlineStatusMessage", "Pickup completed.", "No items are ready to return."],
    "Global passive feedback documentation",
)

require_tokens(
    COMPONENTS,
    ["fun LoadFailureState(", 'retryLabel: String = "Retry"', "SecondaryButton("],
    "Shared load failure/retry component",
)

require_tokens(
    COMPONENTS,
    [
        "fun AppConfirmDialog(", "fun AppDestructiveConfirmDialog(",
        "requiredPhrase: String? = null", "typedPhrase == requiredPhrase",
        "onDismissRequest = { if (!busy) onDismiss() }",
    ],
    "Shared confirmation dialog contract",
)

booking_detail_load = require_tokens(
    SOURCE / "BookingDetailsScreen6.kt",
    [
        'title = "Booking Details"', "viewModel.detailState.loading -> LoadingState()",
        "LoadFailureState(", "onRetry = viewModel::refreshEditor",
    ],
    "Booking Detail initial load/retry contract",
)
if booking_detail_load.count("BackHandler(enabled = !viewModel.actionBusy)") != 1:
    errors.append("Booking Details must keep exactly one active BackHandler across loading/content states.")

require_tokens(
    SOURCE / "BookingLifecycleViewModel.kt",
    [
        'private var detailRequestId: String? = savedStateHandle["bookingDetailId"]',
        "detailRequestId = id",
        "val id = detailState.data?.booking?.id ?: detailRequestId",
    ],
    "Booking Detail retry target contract",
)

booking_list_vm = require_tokens(
    SOURCE / "BookingListViewModel4.kt",
    ["val hadLoadedContent = state.loaded", "loaded = hadLoadedContent", "loadMoreError == null && page < totalPages", "fun retryLoadMore()"],
    "Booking list failure-preservation contract",
)
if "loaded = true, error = userMessage(error)" in booking_list_vm:
    errors.append("Booking list initial failure must not be marked as a successful empty load.")

customer_load_ui = require_tokens(
    SOURCE / "CustomerScreenV4.kt",
    [
        "LoadFailureState(", "onRetry = onRefresh",
        "state.loaded && state.items.isEmpty() && !state.loading && state.loadError.isNullOrBlank()",
        "LoadingState()",
    ],
    "Customer load/empty/retry contract",
)
if 'Text(if (state.loading) "Loading..." else ""' in customer_load_ui:
    errors.append("Customer load-more must not render an empty placeholder text row.")

booking_list_ui = require_tokens(
    SOURCE / "AdminAppScreen4.kt",
    [
        "LoadFailureState(", "onRetry = listViewModel::refresh",
        "state.loaded && state.items.isEmpty() && !state.loading && state.error.isNullOrBlank()",
    ],
    "Booking list load/empty/retry contract",
)
if 'Text(if (state.loading) "Loading..." else ""' in booking_list_ui:
    errors.append("Booking list load-more must not render an empty placeholder text row.")

require_tokens(
    SOURCE / "CustomerScreen3ViewModel.kt",
    ["loadMoreError == null && page < totalPages", "fun retryLoadMore()"],
    "Customer load-more failure stop contract",
)
require_tokens(
    SOURCE / "Screen5ItemManagementViewModel.kt",
    ["state.copy(loaded = false, loading = false, itemLoading = false, loadError = message)"],
    "Screen 5 initial failure non-empty-safe contract",
)
require_tokens(
    SOURCE / "Screen5ItemManagement.kt",
    ["LoadFailureState(", "onRetry = vm::ensureLoaded", "if (!state.loadError.isNullOrBlank())"],
    "Screen 5 load/retry rendering",
)
require_tokens(
    SOURCE / "Screen8Reports.kt",
    ["LoadFailureState(", "onRetry = viewModel::ensureLoaded"],
    "Reports bootstrap retry rendering",
)
require_tokens(
    SOURCE / "Screen8ReportsViewModel.kt",
    [
        "loadMoreError == null && page < totalPages",
        "fun retryLoadMore()",
        "loadingReport = true,",
        "loadError = null,",
        "error = null,",
    ],
    "Reports load-more failure/recovery contract",
)
require_tokens(
    SOURCE / "Screen9Users.kt",
    [
        "LoadFailureState(", "onRetry = vm::refresh",
        "state.loaded && state.items.isEmpty() && state.loadError.isNullOrBlank()",
        "if (state.loaded) {",
    ],
    "Users load/empty/retry contract",
)
require_tokens(
    SOURCE / "Screen9UsersViewModel.kt",
    ["loadMoreError == null && page < totalPages", "fun retryLoadMore()"],
    "Users load-more failure stop contract",
)
require_tokens(
    SOURCE / "Screen10Settings.kt",
    [
        "LoadFailureState(", "onRetry = vm::refresh", "onRetry = vm::refreshAudit",
        "state.auditLoaded && state.auditItems.isEmpty() && state.loadError.isNullOrBlank()",
    ],
    "Settings/Audit load/empty/retry contract",
)
require_tokens(
    SOURCE / "Screen10SettingsViewModel.kt",
    ["auditLoadMoreError == null && auditPage < auditTotalPages", "fun retryLoadMoreAudit()"],
    "Audit load-more failure stop contract",
)
require_tokens(
    ROOT / "docs" / "GLOBAL_UI_RULES.md",
    [
        "Initial load: Loading → Content / Empty / Error + Retry",
        "Load failure is not an empty result",
        "Refresh keeps already loaded content visible",
    ],
    "Global loading/error/retry documentation",
)

require_tokens(
    SOURCE / "CustomerSharedUi.kt",
    [
        'label = "Name *"', 'label = "Mobile Number *"',
        '"Name is required."', '"Mobile number is required."',
        '"Enter a valid 10-digit mobile number."',
    ],
    "Customer required-field validation contract",
)

require_tokens(
    SOURCE / "Screen9Users.kt",
    [
        'label = "Name *"', 'label = "Mobile Number *"', 'label = "Password *"', 'label = "New Password *"',
        '"Name is required."', '"Mobile number is required."', '"Password is required."',
        '"Password must contain at least 8 characters."',
    ],
    "Users required-field validation contract",
)

screen5_validation = require_tokens(
    SOURCE / "Screen5ItemManagement.kt",
    [
        'label = "Category Name *"', 'label = "Code Prefix *"', 'label = "Website Display Order *"',
        'label = "Field Name *"', 'label = "Field Type *"', 'label = "Options *"', 'label = "Display Order *"',
        'label = "Item Code *"', 'label = "Item Name *"', 'label = "Category *"', 'label = "Total Quantity *"',
        "mutableStateOf(existing?.totalQuantity?.toString().orEmpty())",
        "val quantityNumber = quantity.toIntOrNull()",
        "quantityNumber != null",
        '"Quantity is required. Enter 0 if there is currently no stock."',
        "screen5OptionalValueValid(type, defaultValue, options)",
    ],
    "Screen 5 field-local validation contract",
)
if 'existing?.totalQuantity?.toString() ?: "0"' in screen5_validation:
    errors.append("New Item quantity must not silently default a blank field to zero.")

require_tokens(
    SOURCE / "Screen8Reports.kt",
    [
        'label = "Preset Name *"', '"Preset name is required."',
        "nameTouched && name.trim().isBlank()",
    ],
    "Reports preset required-field validation",
)

require_tokens(
    SOURCE / "Screen10Settings.kt",
    [
        'errorMessage = state.error.takeUnless { showTemplateEditor }',
        "error = state.error",
        'label = "Template Name"', "required = true",
        '"Template name is required."',
        'label = "Gujarati Message *"', '"Gujarati message is required."',
        'label = "English Message *"', '"English message is required."',
        'Text("Linked Action * · ${initialSection.label}"',
        "InlineStatusMessage(error, MessageTone.ERROR)",
    ],
    "WhatsApp template validation/error-placement contract",
)

require_tokens(
    ROOT / "docs" / "GLOBAL_UI_RULES.md",
    [
        "Required fields use a visible `*` marker",
        "Blank required values never silently coerce to a valid persisted value",
        "Field-local validation",
    ],
    "Global form validation documentation",
)

require_tokens(
    ROOT / "worker" / "src" / "phase14av-customers-screen3.js",
    [
        "booking_close_event_items", "booking_close_events",
        'auditStatement(env, user.id, "DELETE_PERMANENT"',
        "bookingHistoryPreserved: true",
        "financialHistoryPreserved: true", "billsPreserved: billCount",
        "booking_id=NULL", "permanently_deleted_at=?",
    ],
    "Customer permanent-delete dependency/atomic audit contract",
)
customer_worker = read(ROOT / "worker" / "src" / "phase14av-customers-screen3.js")
for forbidden in ["bookingsArchivedWithCustomer", "bookingsRestoredWithCustomer"]:
    if forbidden in customer_worker:
        errors.append(f"Misleading customer booking audit flag returned: {forbidden}")

require_tokens(
    CARD_PATTERNS,
    [
        "fun LabeledSectionCard", "fun RoundedItemImage", "RoundedCornerShape(10.dp)",
        "fun AppItemDisplayRow(", '"× $quantity"', "fun CompactExpandCollapseAction(",
        'if (expanded) "Show less" else "+ $hiddenCount more"', "fun CompactMetaBadge(",
        "fun ResponsiveCompactPair(", "data class SoftActionSpec(", "fun ResponsiveSoftActionGrid(",
        "fun ResponsiveActionTriple(", "overflow = TextOverflow.Ellipsis"
    ],
    "Reusable card-pattern contract",
)

summary_card = require_tokens(
    BOOKING_SUMMARY_CARD,
    [
        "internal fun BookingSummaryCard", "shownItems = if (showAllItems || expanded) items else items.take(2)",
        "CompactExpandCollapseAction(", "onToggle = { expanded = !expanded }", "BookingMetaStatusCard(",
        'title = "Pickup"', 'title = "Current"', 'title = "Return"', 'label = "Next"',
        "bookingPickupStatus(displayStatus)", "bookingReturnStatus(displayStatus)",
        "bookingDate.ifBlank { pickupDate }", "AppItemDisplayRow(",
    ],
    "Reusable expandable booking-summary card contract",
)
pickup_index = summary_card.find('title = "Pickup"')
current_index = summary_card.find('title = "Current"')
return_index = summary_card.find('title = "Return"')
items_index = summary_card.find("if (shownItems.isNotEmpty())")
next_index = summary_card.find('label = "Next"')
if min(pickup_index, current_index, return_index, items_index, next_index) < 0 or not (
    pickup_index < current_index < return_index < items_index < next_index
):
    errors.append("Booking summary order must keep Pickup | Current | Return above items and only Next below items.")

customer_shared = require_tokens(
    SOURCE / "CustomerSharedUi.kt",
    [
        "CustomerFormSheet", ".imePadding()", "WindowInsets.navigationBars", ".verticalScroll(",
        "rememberModalBottomSheetState(skipPartiallyExpanded = true)", "sheetState.expand()",
        "rememberReliableKeyboardDismiss", "ui.components.rememberReliableKeyboardDismiss()",
        "ImeAction.Next", "ImeAction.Done", "CustomerContactConfirmationSheet",
        "WhatsAppPreparingSheet", "WhatsAppTemplateSelectionSheet", "Icons.Rounded.Call",
        "launchCustomerCall", "launchCustomerWhatsApp", 'onSave(name.trim(), primaryDigits, "", address.trim())',
    ],
    "Customer shared UI contract",
)
for forbidden in ["Alternative Mobile Number", "Choose number to call", "Choose WhatsApp number"]:
    if forbidden in customer_shared:
        errors.append(f"Removed alternative-mobile UI wording returned: {forbidden}")
if "singleLine = true" not in customer_shared.split('label = "Address"', 1)[-1][:900]:
    errors.append("Address must retain reliable single-line Done behavior in the shared customer form.")

customer_vm = require_tokens(
    SOURCE / "CustomerScreen3ViewModel.kt",
    [
        "AdminDataFreshness.markCustomerMutation()", "scheduleNoticeClear", "scheduleErrorClear",
        "delay(2600)", "delay(5100)", "if (state.notice == message)", "if (state.error == message)",
        "if (state.notice != null || state.error != null)",
        "state = state.copy(actionBusy = true, error = null, notice = null)\n        viewModelScope.launch {",
    ],
    "Customer one-time feedback/freshness contract",
)

customer_v4 = require_tokens(
    SOURCE / "CustomerScreenV4.kt",
    [
        "PullToRefreshBox", "onRefresh = onRefresh", "CompactSearchField", "onLoadMore", "InfoValueRow",
        'AppScreenTitle("Customers"', 'label = "New Customer"', "CompactNewActionButton(",
        "CustomerContactConfirmationSheet", "launchCustomerCall(context, customer.mobile)",
        "launchCustomerWhatsApp(context, customer.mobile, message)",
        '"Total Booking · ${customer.totalBookings}"', '"Active Booking · ${customer.activeBookings}"',
        "CustomerBookingStatBadge(", "TextAlign.Center", 'label = "Archive"',
        "var confirmRestore by remember", 'title = "Restore customer?"',
        "Existing booking history will become visible again.",
    ],
    "Customer Screen V4 contract",
)
for forbidden in ["alternateMobile", "Alternative Mobile Number"]:
    if forbidden in customer_v4:
        errors.append(f"Customer Screen V4 must remain primary-mobile-only; found {forbidden}")
if re.search(r'\bPrevious\b|Page \$\{|\bNext\b', customer_v4):
    errors.append("Customer Screen V4 must not expose Previous/Page/Next pagination controls.")

customer_v3 = read(SOURCE / "CustomerScreenV3.kt")
if "CustomerScreenV4(" not in customer_v3:
    errors.append("Legacy Customer Screen V3 must delegate to the canonical V4 UI.")

freshness = require_tokens(
    SOURCE / "AdminDataFreshness.kt",
    [
        "dashboardRevision", "bookingsRevision", "customersRevision", "bookingBootstrapRevision",
        "fun markBookingMutation()", "fun markCustomerMutation()",
    ],
    "Targeted data-freshness coordinator",
)
if "dashboardRefresh?.invoke()" in freshness or "registerDashboardRefresh" in freshness:
    errors.append("Dashboard freshness must remain revision-based with no off-screen refresh callback.")

admin_vm_freshness = require_tokens(
    SOURCE / "AdminViewModel.kt",
    ["fun ensureDashboard()", "seenDashboardFreshnessRevision", "if (dashboardState.loading) return", "repository.dashboard()"],
    "Dashboard freshness wiring",
)
if "AdminDataFreshness.registerDashboardRefresh" in admin_vm_freshness:
    errors.append("AdminViewModel must not restore the retired immediate Dashboard refresh callback.")

booking_vm = require_tokens(
    SOURCE / "BookingLifecycleViewModel.kt",
    [
        "mutationVersion", "loadAvailabilityForSelectedDates", "fillAllPickup", "fillAllReturn", "directPickup", "savePickup", "saveReturn",
        "fun openNew(preselectedCustomer: BookingOptionCustomer? = null)", "selectedCustomerId = preselectedCustomer?.id.orEmpty()",
        "AdminDataFreshness.markBookingMutation()",
        "private suspend fun reloadDetailAwaited(id: String)",
        "detailState = detailState.copy(loading = true, error = null)",
        "detailRefreshBlocked = true",
        "detailRefreshBlocked = false",
        "reloadDetailAwaited(detail.booking.id)",
        "actionBusy = true\n        actionError = null\n        actionNotice = null\n        viewModelScope.launch {",
        "finally {\n                actionBusy = false",
    ],
    "Booking lifecycle ViewModel contract",
)

pickup_block = booking_vm[booking_vm.find("fun savePickup()"):booking_vm.find("fun saveReturn(")]
if pickup_block.find("reloadDetailAwaited(detail.booking.id)") < 0 or pickup_block.find("actionNotice = message") < 0:
    errors.append("Pickup must await refreshed detail before success feedback.")
elif pickup_block.find("reloadDetailAwaited(detail.booking.id)") > pickup_block.find("actionNotice = message"):
    errors.append("Pickup success feedback must not appear before refreshed detail is applied.")

return_block = booking_vm[booking_vm.find("fun saveReturn("):booking_vm.find("private fun createBooking(")]
return_reload = return_block.find("reloadDetailAwaited(detail.booking.id)")
return_notice = return_block.find("actionNotice =")
if return_reload < 0 or return_notice < 0:
    errors.append("Return must await refreshed detail before success feedback.")
elif return_reload > return_notice:
    errors.append("Return success feedback must not appear before refreshed detail is applied.")

awaited_detail_reload = booking_vm[booking_vm.find("private suspend fun reloadDetailAwaited"):booking_vm.find("private fun reloadDetail(", booking_vm.find("private suspend fun reloadDetailAwaited"))]
if "detailState = DataState(loading = true)" in awaited_detail_reload:
    errors.append("Post-mutation Booking Detail refresh must retain current detail instead of blanking the screen.")
if "detailState = detailState.copy(loading = true, error = null)" not in awaited_detail_reload:
    errors.append("Post-mutation Booking Detail refresh must preserve loaded data while marking refresh loading.")

require_tokens(
    SOURCE / "BookingDetailsScreen6.kt",
    [
        "viewModel.detailRefreshBlocked", 'text = "Retry Refresh"',
        "val mutationBlocked = viewModel.actionBusy || viewModel.detailRefreshBlocked",
        "enabled = !mutationBlocked",
    ],
    "Booking Detail stale-refresh safety contract",
)

main_activity = read(SOURCE / "MainActivity.kt")
for token in ["BookingListViewModel4.Factory", "CustomerScreen3ViewModel.Factory", "ZhagmagAdminRootScreen4"]:
    if token not in main_activity:
        errors.append(f"MainActivity Screen 4 routing missing: {token}")

screen4 = require_tokens(
    SOURCE / "AdminAppScreen4.kt",
    [
        "BookingListFilter4.values().filterNot { it.dashboardOnly }", 'placeholder = "Search"', "AppCompactScrollableTabs(", "listViewModel.loadMore()",
        "BookingListCard4", "BookingWorkspaceScreen5", "ZhagmagAdminRootScreen4", "PendingWhatsAppPreview4",
        "PendingWhatsAppTemplateList4", "WhatsAppPreparingSheet", "WhatsAppTemplateSelectionSheet",
        "var whatsappError by remember", "PopupMessage(whatsappError, MessageTone.ERROR)",
        "contactBusy = whatsappPreparing || customerViewModel.state.actionBusy",
        "CustomerContactConfirmationSheet", "BookingOptionCustomer(customer.id, customer.name, customer.mobile)",
        "BookingSummaryCard(", "BookingCardActions(", "CompactNewBookingButton(", 'AppScreenTitle("Bookings"',
        "booking.itemPreviews.map", "totalItemCount = booking.itemCount", "onEdit = { bookingViewModel.openBooking(booking.id, edit = true) }",
    ],
    "Screen 4 list/root contract",
)
if re.search(r'\bPrevious\b|Page \$\{|Page 1|Page X', screen4):
    errors.append("Screen 4 list must not expose Previous/Page/Next controls.")
name_index = screen4.find("customerName = booking.customerName")
mobile_index = screen4.find("customerMobile = booking.customerMobile", name_index)
number_index = screen4.find("bookingNo = booking.bookingNo", mobile_index)
if min(name_index, mobile_index, number_index) < 0 or not (name_index < mobile_index < number_index):
    errors.append("Booking summary identity order must be Customer Name -> Mobile Number -> Booking Number.")

list_vm = require_tokens(
    SOURCE / "BookingListViewModel4.kt",
    [
        "pageSize = 10", "loadMore", "canLoadMore", "BookingListFilter4.CANCELLED", "defensiveItems",
        'it.rawStatus.equals("CANCELLED", true)', 'it.displayStatus.equals("CANCELLED", true)',
        "AdminDataFreshness.bookingsRevision",
    ],
    "Booking list filter/freshness contract",
)
for label in ["All", "Reserved", "Booked", "Picked Up", "Returned", "Overdue", "Cancelled"]:
    if label not in list_vm:
        errors.append(f"Booking filter missing: {label}")

workspace5 = require_tokens(
    SOURCE / "BookingWorkspaceScreen5.kt",
    [
        "fun BookingWorkspaceScreen5", "contactBusy: Boolean = false", "BookingDetailsScreen6(",
        "AppScreenTitle(", 'label = "New Customer"', "CompactNewActionButton(", 'LabeledSectionCard(title = "Customer")',
        "viewModel.clearSelectedCustomer()", 'contentDescription = "Clear selected customer"',
        'label = "Pickup Date"', 'label = "Return Date"', "AppDatePickerField(",
        "showDayOfWeek = true", "minDate = LocalDate.now",
        'LabeledSectionCard(title = "Items")', "AppSingleSelectFilter(", 'title = "Filter Items"', "visibleCount",
        'AppFilterOption("ALL", "All Categories")', "BookingItemResultCard5", '"+ Select"', 'Text("Selected"',
        'LabeledSectionCard(title = "Selected Items (${viewModel.draftLines.size})")', "EditableQuantityControl5",
        "TextFieldValue", "TextRange(0, text.length)", "KeyboardType.Number", "ImeAction.Done", "dismissKeyboard()",
        "AppItemDisplayRow(", "RelatedSuggestionRow5(", "viewModel.addRelatedItem(item.id, relatedItem.id)",
        'text = "Reserve"', 'text = "Confirm"', 'text = "Directly Pickup"', "viewModel.directPickup()",
    ],
    "Screen 5 New/Edit Booking hardening contract",
)
selected_index = workspace5.find('LabeledSectionCard(title = "Selected Items')
result_index = workspace5.find("items(shown.take(visibleCount)")
if selected_index < 0 or result_index < 0 or selected_index < result_index:
    errors.append("Selected Items must render after item search results in New Booking Step 3.")
for forbidden in ["horizontalScroll(rememberScrollState())", 'text = "Add"']:
    if forbidden in workspace5:
        errors.append(f"New Booking Step 3 restored obsolete selection/filter pattern: {forbidden}")

booking_detail = require_tokens(
    SOURCE / "BookingDetailsScreen6.kt",
    [
        "fun BookingDetailsScreen6", "contactBusy: Boolean = false", "busy = viewModel.actionBusy || contactBusy", 'listOf("Details", "Pickup", "Return", "Bill", "History")',
        "BackHandler(enabled = !viewModel.actionBusy)", "viewModel::backFromEditor",
        "AppBackHeader(", 'title = "Booking Details"',
        "booking.customerName", "booking.customerMobile", "booking.bookingNo", "BookingSummaryCard(", "paymentStatus = booking.paymentStatus",
        'if (status == "RESERVED")', 'text = "Cancel Booking"', 'text = "Confirm"', 'text = "Directly Pickup"', "AppItemDisplayRow(",
        "OutlinedTextField(", "TextRange(0, text.length)", "KeyboardType.Number", "ImeAction.Done", "dismissKeyboard()",
        '"RESERVE_BOOKING" -> "Reserved"', '"CONFIRM_BOOKING" -> "Confirmed"',
        '"PARTIALLY_GIVEN" -> "Part Picked Up"', '"GIVEN", "RETURNED" -> "Full Picked Up"',
        '"PARTIALLY_RETURNED" -> "Part Return"', '"RETURNED" -> "Full Returned"',
        "JSONObject(event.newValueJson.orEmpty())", 'DateTimeFormatter.ofPattern("dd-MM-yyyy · EEEE · h:mm a"',
    ],
    "Booking Details/history hardening contract",
)
detail_name = booking_detail.find("booking.customerName")
detail_mobile = booking_detail.find("booking.customerMobile", detail_name)
detail_no = booking_detail.find("booking.bookingNo", detail_mobile)
if min(detail_name, detail_mobile, detail_no) < 0 or not (detail_name < detail_mobile < detail_no):
    errors.append("Booking Details identity order must be Customer Name -> Mobile Number -> Booking Number.")

worker = require_tokens(
    ROOT / "worker" / "src" / "phase14aw-screen4-bookings.js",
    ['pageSize = Math.min(10', 'view === "CANCELLED"', "b.status='CANCELLED'", 'view === "OVERDUE"', "LIMIT 30", "getSessionUser"],
    "Screen 4 compatibility Worker list contract",
)
active_worker = require_tokens(
    ROOT / "worker" / "src" / "phase14be-operational-lifecycle.js",
    [
        'requirePermission(request, env, "DASHBOARD")',
        'requirePermission(request, env, "BOOKINGS")',
        'import { bookingPaymentStatusSql } from "./payment-classifier.js";',
        'view === "TODAY_PICKUP"', 'view === "TODAY_RETURN"', 'view === "MISSED_PICKUP"',
        'view === "ACTIVE_RENTAL"', 'view === "PAYMENT_PENDING"', 'view === "PAYMENT_PART"', 'view === "PAYMENT_FULL"',
        "One bundled query owns all 34 fixed KPIs",
    ],
    "Active Dashboard/Booking Worker contract",
)
require_tokens(
    ROOT / "worker" / "src" / "phase14br-billing-v1.js",
    ['import core from "./phase14bq-global-rental-reports.js";', 'const paymentCase = bookingPaymentStatusSql("b")'],
    "Active Billing wrapper contract",
)

screen5_worker = require_tokens(
    ROOT / "worker" / "src" / "phase14ba-screen5-item-management.js",
    [
        'import core from "./phase14aw-screen4-bookings.js"',
        'url.pathname === "/api/admin/item-management/bootstrap"',
        '/^\\/api\\/admin\\/related-items\\/([^/]+)$/',
        "return core.fetch(request, env, ctx)",
    ],
    "Screen 5 cumulative Worker wrapper contract",
)

require_tokens(
    ROOT / "docs" / "GLOBAL_UI_RULES.md",
    [
        "Every normal editable textbox/input field",
        "Placeholders must never contain actual, sample or business-specific example data",
        "NUMBER, DATE, DROPDOWN, MULTI_SELECT and YES_NO values use their proper controls",
        "Closed dropdown state shows a generic prompt before selection",
        "selected option is visibly identified in the menu with a check/highlight",
        "Global filter pattern", "Cancel | Clear/Reset | Apply",
    ],
    "Global textbox/dropdown UI rules",
)

require_tokens(
    SOURCE / "Screen5ItemManagement.kt",
    [
        'title = "Category & Items"', "AppCompactFixedTabs(", "ActivityResultContracts.GetMultipleContents()",
        'Text("Item Photos"', 'Text("Make Primary")', 'contentDescription = "Remove photo"',
        "private const val MAX_ITEM_PHOTOS = 8",
        '"Related Groups (', 'Screen5Tab.RELATED -> "New Group"', "Screen5RelatedGroupCard(",
        'label = "View"', 'label = "Edit"', 'label = "Delete"', "Screen5RelatedGroupViewSheet(",
        'title = "Delete related group?"', "vm.replaceRelatedItems(sourceId, emptyList())",
        'title = "Main Item"', "Screen5MainItemPickerSheet(", "Screen5RelationSectionHeader(",
        "Screen5RelatedItemsPickerSheet(", 'Text("Add Related Items"', 'text = "Add Selected"',
        'Screen5Tab.CATEGORIES -> "New Category"', 'Screen5Tab.ITEMS -> "New Item"',
        'label = "New Custom Field"', "CompactNewActionButton(",
        'label = "Website Display Order *"', '(categories.maxOfOrNull { it.displayOrder } ?: 0) + 1',
        "Screen5DropdownField(", 'prompt = "Select Field Type"', "Screen5MultiSelectField(",
        "Screen5DateField(", "AppDatePickerField(", "Screen5DynamicFieldInput(",
        "screen5NormalizeFieldValues(fields, fieldValues)", "requiredFieldsValid",
        'placeholder = "Enter category name"', 'placeholder = "Enter item name"', 'placeholder = "Enter quantity"',
        'StatusBadge("Category · $selectedCategoryName"', "AppSingleSelectFilter(",
        "AppItemDisplayRow(", '"Available ${item.availableQuantity} · Booked ${item.bookedQuantity} · Given ${item.givenQuantity}"',
        'text = "Save Related Items"', "AppBackHeader(", "onRemoveSelection = if (canEdit)",
        'text = "Remove Data & Delete Field"', 'strongDeleteText == "DELETE FIELD"', 'Text("Clear Value")',
    ],
    "Screen 5 item-management UI parity",
)
require_tokens(
    SOURCE / "Screen5ItemManagementRepository.kt",
    [
        'api.post("/api/admin/cloudinary/signature")', 'https://api.cloudinary.com/v1_1/$cloudName/image/upload',
        '.put("cloudinaryAssets", assetPayload)', "const val CLIENT_MAX_IMAGE_BYTES = 8 * 1024 * 1024",
        '"/api/admin/category-fields/$id/delete-action"', '.put("clearFieldIds", JSONArray(clearFieldIds.toList()))',
        'api.request("DELETE", "/api/admin/items/$id")',
    ],
    "Screen 5 Cloudinary upload contract",
)
require_tokens(
    SOURCE / "Screen5ItemManagementModels.kt",
    ["internal data class Screen5UploadedAsset", "val publicId: String = \"\"", "val valueCount: Int", "val linkedItemCount: Int"],
    "Screen 5 uploaded asset model",
)
phase14e_text = require_tokens(
    ROOT / "worker" / "src" / "phase14e.js",
    [
        'url.pathname === "/api/admin/cloudinary/signature"', 'url.pathname === "/api/admin/cloudinary/discard"',
        'maxFiles: purpose === "logo" ? 1 : 8', "maxBytes: MAX_IMAGE_BYTES", "validateUploadedAssets",
        "destroyUnmappedAssets", "body.cloudinaryAssets",
    ],
    "Existing Website/Worker Cloudinary contract",
)
if "syncAssetMap(" in phase14e_text:
    errors.append("Cloudinary asset ownership mapping must not return to a post-save wrapper phase.")
require_tokens(
    ROOT / "worker" / "src" / "index.ts",
    ["INSERT INTO cloudinary_item_assets", "DELETE FROM cloudinary_item_assets", "parsed.cloudinaryAssets"],
    "Atomic Item/Cloudinary asset-map contract",
)
require_tokens(
    ROOT / "docs" / "ADMIN_SCREEN_05_ITEM_MANAGEMENT.md",
    ["Visible page heading is `Category & Items`.", "existing signed Cloudinary upload flow"],
    "Screen 5 image-upload documentation",
)

models_text = require_tokens(
    DATA / "Models.kt",
    [
        "enum class UserRole { OWNER, STAFF }",
        "object StaffAccess",
        "staffPermissions: Set<String>",
        "fun SessionUser.hasAccess(permission: String)",
    ],
    "Owner/Staff session permission contract",
)

screen9 = require_tokens(
    SOURCE / "Screen9Users.kt",
    [
        "MainScreenDateRow(businessDate)", 'title = "Users"', "CompactNewActionButton(",
        'label = "Add User"', 'placeholder = "Search name or mobile"',
        '"OWNER" to "Owner"', '"STAFF" to "Staff"', '"Active"', '"Archived"',
        'label = "Mobile Number *"', 'label = "Password *"',
        '"Staff Access"', '"Select All"', '"Clear All"', 'text = "Reset Password"',
        '"Archive user?"', "permissionOptions.toSet()", "StaffAccess.REPORTS",
        "LaunchedEffect(state.page, state.items.size) { vm.loadMore() }",
    ],
    "Screen 9 Users UI contract",
)
for forbidden in ['eyebrow = "Access Control"', '"Owner and Staff accounts"', 'Icons.Rounded.ArrowBack']:
    if forbidden in screen9:
        errors.append(f"Screen 9 compact Users header restored removed UI: {forbidden}")

require_tokens(
    SOURCE / "Screen9UsersRepository.kt",
    [
        '"/api/admin/users"', '"pageSize" to pageSize.coerceIn(1, 10).toString()',
        '.put("mobile", mobile.filter(Char::isDigit))', '.put("staffPermissions"',
        '"/api/admin/users/$id/password"', '"/api/admin/users/$id/archive"', '"/api/admin/users/$id/restore"',
    ],
    "Screen 9 Users repository contract",
)

require_tokens(
    SOURCE / "Screen9UsersViewModel.kt",
    [
        "val canLoadMore", "fun loadMore()", "load(reset = true)", "delay(350)", "pendingReset", "distinctBy { it.id }",
        "repository.create(", "repository.update(", "repository.resetPassword(",
        "state = state.copy(actionBusy = true, loadError = null, error = null, message = null)\n        viewModelScope.launch {",
    ],
    "Screen 9 Users incremental state contract",
)

screen4_more = require_tokens(
    SOURCE / "AdminAppScreen4.kt",
    [
        "private enum class MoreDestination4 { HUB, ITEM_MANAGEMENT, BILLING, REPORTS, USERS, SETTINGS, WHATSAPP_MANAGEMENT }",
        "private fun Screen4MoreHub(", "businessDate: String", "MainScreenDateRow(businessDate)", 'AppScreenTitle("More")', 'title = "Category & Items"',
        'title = "Category & Items"', 'title = "Reports"', 'title = "Bills"', 'title = "WhatsApp Centre"', 'title = "Users"', 'title = "Settings"', "modifier = Modifier.weight(1f)",
        "user.hasAccess(StaffAccess.ITEMS)", "user.hasAccess(StaffAccess.BOOKINGS)", "user.role == UserRole.OWNER",
        'title = "Bills"', "MoreDestination4.BILLING",
        "canDashboard = user.hasAccess(StaffAccess.DASHBOARD)",
        "canCustomers = user.hasAccess(StaffAccess.CUSTOMERS)",
        "canBookings = user.hasAccess(StaffAccess.BOOKINGS)",
        "var moreHubRequest by rememberSaveable(user.id) { mutableStateOf(0) }",
        "moreHubRequest += 1", "hubRequest = moreHubRequest", "LaunchedEffect(hubRequest, directDestination)",
        "if (hubRequest > 0 && directDestination.isNullOrBlank())",
        "dashboardDrilldown", "onDashboardBack",
    ],
    "Permission-aware More/navigation contract",
)

screen9_worker = require_tokens(
    ROOT / "worker" / "src" / "phase14bb-screen9-users.js",
    [
        'import core from "./phase14ba-screen5-item-management.js"',
        'pathname.startsWith("/api/admin/users")',
        'pathname.startsWith("/api/admin/settings")',
        'pathname.startsWith("/api/admin/whatsapp-templates")',
        'pathname.startsWith("/api/admin/audit-logs")',
        '"DASHBOARD"', '"ITEMS"', '"CUSTOMERS"', '"BOOKINGS"', '"PICKUPS"', '"RETURNS"', '"REPORTS"',
        "hasStaffPermission(user, permission)",
    ],
    "Screen 9 Worker permission guard",
)

require_tokens(
    ROOT / "database" / "migrations" / "0014_staff_permissions.sql",
    [
        "staff_permissions_json", "WHERE role='ADMIN'", "SET role='STAFF'", "WHERE role='OWNER'",
    ],
    "Screen 9 role/permission migration",
)

require_tokens(
    ROOT / "docs" / "ADMIN_SCREEN_09_USERS.md",
    [
        "Roles are exactly **OWNER** and **STAFF**.", "Email is removed from User Management UI",
        "Staff Access checkboxes", "two-column pastel card-grid", "Search input is debounced",
        "Screen 6 Pickup and Screen 7 Returns are intentionally skipped",
    ],
    "Screen 9 documentation",
)

screen10 = require_tokens(
    SOURCE / "Screen10Settings.kt",
    [
        "MainScreenDateRow(businessDate)", 'title = "Settings"', '"Basic"', '"Public Website"', '"Audit Log"',
        'internal fun Screen10WhatsAppManagement(', 'title = "WhatsApp Centre"',
        'label = "Website"', 'placeholder = "Enter website URL"',
        "AppCompactScrollableTabs(",
        '"Business Logo"', '"Upload Logo"', '"Change Logo"', '"Remove Logo"',
        'Text("Working Hours"', '"Copy Mon → Weekdays"', '"Copy Mon → All"', 'rememberTimePickerState(',
        '"Global Template Language"', '"Gujarati"','"English"','"Both"',
        'LANGUAGE("Language Settings")', 'Text("Linked Action * · ${initialSection.label}"', 'FilterChip(',
        'Text("Insert Placeholder"', '"Gujarati Message *"', '"English Message *"',
        '"RESERVATION_CONFIRMATION" to "Reservation Confirmation"',
        '"BOOKING_UPDATED" to "Booking Updated"', '"PICKUP_READY" to "Pickup Ready"',
        '"RETURN_DUE_TODAY" to "Return Due Today"', '"ITEM_AVAILABILITY_REPLY" to "Item Availability Reply"',
        '"BOOKING_COMPLETED" to "Booking Completed"', '"THANK_YOU" to "Thank You"',
        'CompactNewActionButton(', 'label = "Add Template"',
    ],
    "Screen 10 Settings UI contract",
)
if 'eyebrow = "Administration"' in screen10 or 'subtitle = "Business, website & WhatsApp settings"' in screen10:
    errors.append("Settings must keep the simplified title-only header.")
if '"Gujarati · English · All"' in screen10:
    errors.append("Settings must not restore per-template language summary.")
screen10_editor_start = screen10.find("private fun Screen10TemplateEditorSheet")
screen10_editor_end = screen10.find("private fun Screen10PlaceholderInsertDropdown", screen10_editor_start)
screen10_editor = screen10[screen10_editor_start:screen10_editor_end] if screen10_editor_start >= 0 and screen10_editor_end > screen10_editor_start else ""
if "relevantPlaceholders.forEach" in screen10_editor:
    errors.append("Settings template editor must not eagerly render all placeholder rows.")
if 'Screen10Dropdown(\n                    "Linked To"' in screen10_editor:
    errors.append("Settings template editor must not restore the full Linked To dropdown.")
if 'Text("Add GU")' in screen10_editor or 'Text("Add EN")' in screen10_editor:
    errors.append("Settings template editor must keep placeholder insertion in the on-demand dropdown.")


require_tokens(
    SOURCE / "Screen10SettingsRepository.kt",
    [
        '"/api/admin/settings/bootstrap"', '"/api/admin/settings"', '"/api/admin/whatsapp-templates"',
        '"/api/admin/audit-logs"', '.put("websiteUrl", settings.websiteUrl.trim())', '.put("whatsappTemplateLanguage"', '.put("languageMode", "ALL")',
        '.put("linkedAction"', '.put("messageGu"', '.put("messageEn"', 'suspend fun uploadLogo',
    ],
    "Screen 10 Settings repository contract",
)

require_tokens(
    ROOT / "worker" / "src" / "phase14bg-settings-whatsapp.js",
    [
        'import core from "./phase14be-operational-lifecycle.js"', "composeLinkedWhatsApp",
        "whatsappTemplateLanguage", "TEMPLATE_UNRESOLVED", "readableItemSummary",
        "pickup_time", "return_time", "related_items", "staff_name",
    ],
    "Screen 10 current linked WhatsApp Worker contract",
)
require_tokens(
    ROOT / "worker" / "src" / "whatsapp-template-registry.ts",
    [
        'WHATSAPP_GLOBAL_LANGUAGE_MODES = ["GUJARATI", "ENGLISH", "BOTH"]',
        "WHATSAPP_PLACEHOLDER_SOURCES", "WHATSAPP_PLACEHOLDERS_BY_ACTION",
        "validateTemplatePlaceholders", "WHATSAPP_STATUS_GROUPS", "WHATSAPP_TEMPLATES_BY_STATUS_GROUP",
        '"CONFIRMED_BOOKED"', '"PART_PICKUP"', '"PICKED_UP"', '"PART_RETURN"', '"RETURNED"', '"OVERDUE"', '"CANCELLED"', '"OTHER"',
        '"ITEM_AVAILABILITY_REPLY"', '"BOOKING_COMPLETED"',
    ],
    "WhatsApp registry contract",
)

require_tokens(
    ROOT / "database" / "migrations" / "0015_whatsapp_template_linking.sql",
    [
        "language_mode", "linked_action", "PICKUP_DONE", "PART_PICKUP_DONE",
        "RETURN_DONE", "PART_RETURN_DONE", "THANK_YOU", "ux_whatsapp_templates_active_link",
    ],
    "Screen 10 base WhatsApp template migration",
)
require_tokens(
    ROOT / "database" / "migrations" / "0018_settings_whatsapp_expansion.sql",
    [
        "RESERVATION_CONFIRMATION", "RESERVATION_CANCELLED", "BOOKING_UPDATED", "BOOKING_CANCELLED",
        "PICKUP_READY", "PICKUP_DUE_TODAY", "RETURN_DUE_TODAY", "PENDING_PICKUP_REMINDER",
        "PENDING_RETURN_REMINDER", "OVERDUE_FINAL_REMINDER", "ITEM_AVAILABILITY_REPLY", "BOOKING_COMPLETED",
    ],
    "Screen 10 expanded WhatsApp template migration",
)

require_tokens(
    ROOT / "database" / "migrations" / "0019_whatsapp_status_groups.sql",
    [
        "RESERVATION_REMINDER", "RESERVATION_EXPIRY_REMINDER", "BOOKING_DETAILS",
        "RETURN_DATE_TIME_UPDATE", "RETURN_THANK_YOU", "FEEDBACK_REVIEW",
        "OVERDUE_URGENT_REMINDER", "OVERDUE_FOLLOW_UP_REMINDER", "CANCELLATION_DETAILS",
        "SHOP_ADDRESS", "WORKING_HOURS", "HOLIDAY_SHOP_CLOSED", "CONTACT_US", "CUSTOM_GENERAL_MESSAGE",
    ],
    "Phase 14BM WhatsApp status-group template migration",
)

wrangler = read(ROOT / "worker" / "wrangler.toml")
if 'main = "src/phase14bq-global-rental-reports.js"' not in wrangler:
    errors.append("Wrangler entry must use the cumulative Phase 14BQ Global Rental Reports wrapper.")

screen8 = require_tokens(
    SOURCE / "Screen8Reports.kt",
    [
        'title = "Reports"', "MainScreenDateRow(businessDate)", '"Generate Report"', '"Saved Presets"',
        'text = "View"', 'text = "Share"', 'text = "Download"', "Screen8PdfExporter",
        "Screen8SectionTabs(", "AppCompactScrollableTabs(", "Screen8CustomFieldFilter(",
        "AppSingleSelectFilter(", "AppMultiSelectFilter(", "AppDatePickerField(",
        'config.datePreset == "CUSTOM"', '"Clear Filters"', "resetCurrentFilters",
        "private fun SummaryCard8(", ".padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)",
        "MaterialTheme.typography.titleMedium", "FontWeight.Bold",
        "LaunchedEffect(state.page, state.rows.size)", "Unable to create PDF. Please try again.",
    ],
    "Screen 8 Global Rental Reports UI contract",
)
if "pdfMessage = error.message" in screen8:
    errors.append("Reports PDF errors must not expose raw exception messages.")
if "ResponsiveActionTriple(" not in screen8 or "ResponsiveCompactPair(" not in screen8:
    errors.append("Reports must keep responsive preset actions and Custom From/To dates.")
if "private fun Screen8Selector(" in screen8 or "DropdownMenu(" in screen8:
    errors.append("Reports must reuse shared filter-sheet controls instead of local dropdown implementations.")

for local_date_picker_path, local_date_picker_label in [
    (SOURCE / "BookingWorkspaceScreen5.kt", "Booking editor"),
    (SOURCE / "Screen5ItemManagement.kt", "Category & Items"),
    (SOURCE / "Screen10Settings.kt", "Settings"),
]:
    local_date_picker_text = read(local_date_picker_path)
    if "DatePickerDialog(" in local_date_picker_text:
        errors.append(f"{local_date_picker_label} must reuse AppDatePickerField instead of a screen-local DatePickerDialog.")

result_header = screen8[screen8.find("private fun ReportResultHeader("):screen8.find("@Composable\nprivate fun SummaryCard8")]
if result_header:
    view_pos = result_header.find('text = "View"')
    share_pos = result_header.find('text = "Share"')
    download_pos = result_header.find('text = "Download"')
    if not (0 <= view_pos < share_pos < download_pos):
        errors.append("Reports PDF actions must be ordered View -> Share -> Download.")
    if 'modifier = Modifier.fillMaxWidth(),\n            enabled = state.hasRows && !pdfBusy,\n            loading = pendingPdfAction == Screen8PdfAction.DOWNLOAD,\n            icon = { Icon(Icons.Rounded.Download' not in result_header:
        errors.append("Reports Download action must be full-width on the second row.")
else:
    errors.append("Reports result header contract is missing.")

require_tokens(
    SOURCE / "Screen8ReportsModels.kt",
    [
        '"PARTIALLY_GIVEN" -> "Part Picked Up"',
        '"GIVEN" -> "Full Picked Up"',
        '"RETURNED" -> "Full Returned"',
        '"OPERATIONS_OVERVIEW"', '"MISSED_PICKUPS"', '"AVAILABILITY"',
        '"CURRENTLY_OUT"', '"ITEM_UTILIZATION"', '"LOW_USE_ITEMS"',
        '"ACTIVE_RENTALS"', '"FREQUENT_CUSTOMERS"', '"NEW_RETURNING_CUSTOMERS"',
        '"CUSTOMER_EXCEPTIONS"', '"WHATSAPP_ACTIVITY"', '"AUDIT_REPORT"', '"EXCEPTIONS"',
        "screen8Sections", "screen8StatusOptions", "Screen8CategoryFieldOption",
    ],
    "Screen 8 global report catalog contract",
)

require_tokens(
    SOURCE / "Screen8ReportsRepository.kt",
    [
        '"/api/admin/report-generator/bootstrap"', '"/api/admin/report-generator"',
        '"/api/admin/report-presets"', '"export" to if (exportAll) "1" else null',
    ],
    "Screen 8 Reports repository contract",
)

require_tokens(
    SOURCE / "Screen8ReportsViewModel.kt",
    [
        "fun loadMore()", "fun requestExport()", "fun savePreset(", '"THIS_MONTH"',
        "resolvePresetDates(preset.config)", "generatedConfig = null",
        "columns = emptyList()", "summary = emptyList()",
    ],
    "Screen 8 Reports state contract",
)

require_tokens(
    SOURCE / "Screen8PdfExporter.kt",
    [
        "PdfDocument()", "val landscape = report.columns.size >= 7",
        "fun wrapText(", "fun formatDateLike(", "fun formatPdfValue(",
        "preferredColumnWidths", '"Applied Filters"', '"Summary"', '"Detailed Report"',
        "val summaryColumns = if (landscape) 4 else 3", "screen8StatusLabel(value)",
        "drawContinuationHeader(", "rowIndex % 2 == 1",
        "MediaStore.Downloads", "FileProvider.getUriForFile",
    ],
    "Screen 8 PDF contract",
)

require_tokens(
    ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js",
    [
        'import core from "./phase14bg-settings-whatsapp.js"', '"OPERATIONS_OVERVIEW"',
        '"MISSED_PICKUPS"', '"AVAILABILITY"', '"CURRENTLY_OUT"', '"ITEM_UTILIZATION"',
        '"WHATSAPP_ACTIVITY"', '"AUDIT_REPORT"', '"/api/admin/report-generator"',
        "report-presets", 'hasStaffPermission(user, "REPORTS")',
        "whatsapp_activity_logs", "customFilters", "This report has more than 10,000 rows",
    ],
    "Screen 8 Global Rental Reports Worker contract",
)

require_tokens(
    ROOT / "database" / "migrations" / "0016_report_presets.sql",
    ["CREATE TABLE IF NOT EXISTS report_presets", "visibility TEXT NOT NULL DEFAULT 'MY'"],
    "Screen 8 preset migration",
)

require_tokens(
    ROOT / "docs" / "ADMIN_SCREEN_08_REPORTS.md",
    ["Global Rental Reports", "Overview", "Operations", "Inventory", "Customers", "Activity",
     "Summary + Detailed Table", "first row: **View | Share**", "full-width **Download**"],
    "Screen 8 documentation",
)

hardening_doc = require_tokens(
    ROOT / "docs" / "ADMIN_ANDROID_SCREEN4_HARDENING.md",
    [
        "transient and one-time", "full-width placement immediately below the Main Header",
        "InputMethodManager.hideSoftInputFromWindow", "always vertical", "Clear selected customer", "editable quantity",
        "exact `10.dp` corner radius", "Booking List and Dashboard operational queues reuse the same `BookingSummaryCard` structure",
        "reuse the same booking-card action component", "shared Call confirmation popup", "shared WhatsApp preview/confirmation popup",
        "tapping `+ N more` expands the same card", "visible Back action and Android system/gesture Back",
        "Customer Name → Mobile Number → Booking Number", "Part Picked Up", "Full Picked Up", "Part Return", "Full Returned",
        "defensively refuses to render a non-cancelled row", "Successful mutations mark only affected data domains stale",
        "staging APK only", "0.14.5 / versionCode 1",
    ],
    "Admin Android Screen 4 hardening documentation",
)


require_tokens(
    SOURCE / "CustomerSharedUi.kt",
    ["ui.components.rememberReliableKeyboardDismiss()"],
    "Shared keyboard-dismiss delegation contract",
)
require_tokens(
    SOURCE / "Screen9Users.kt",
    ["ImeAction.Next", "ImeAction.Done", "keyboardType = KeyboardType.Password"],
    "Users IME navigation contract",
)
require_tokens(
    SOURCE / "Screen5ItemManagement.kt",
    ["ImeAction.Next", "ImeAction.Done", 'label = "Website Display Order *"', 'label = "Total Quantity *"'],
    "Category & Items IME navigation contract",
)


require_tokens(
    SOURCE / "AdminAppScreen4.kt",
    ["BackHandler {", "editorOpen -> Unit", "onBack = { destination = MoreDestination4.HUB }"],
    "Global Back fall-through guard",
)
require_tokens(
    SOURCE / "BookingWorkspaceScreen5.kt",
    ["BackHandler(enabled = viewModel.editorStartsInEditMode && viewModel.detailState.data == null)", "viewModel.backFromEditor()"],
    "Booking detail loading Back guard",
)
require_tokens(
    SOURCE / "Screen5ItemManagement.kt",
    ["onBack: () -> Unit", "AppBackHeader(", 'title = "Category & Items"', "BackHandler(enabled = relatedEditorOpen)"],
    "Screen 5 Back navigation contract",
)
require_tokens(
    SOURCE / "Screen8Reports.kt",
    ["onBack: () -> Unit", "AppBackHeader(", 'title = "Reports"', "pendingPdfAction == null"],
    "Screen 8 Back navigation contract",
)
require_tokens(
    SOURCE / "Screen9Users.kt",
    ["onBack: () -> Unit", "AppBackHeader(", 'title = "Users"', "onDismissRequest = requestDismiss", "AppUnsavedChangesDialog("],
    "Screen 9 Back navigation contract",
)
require_tokens(
    SOURCE / "Screen10Settings.kt",
    ["hasUnsavedSettings", "Discard unsaved changes?", "AppBackHeader(", 'title = "Settings"', "onDismissRequest = requestDismiss", "AppUnsavedChangesDialog("],
    "Screen 10 Back/unsaved navigation contract",
)
if errors:
    print("Android Admin cumulative foundation audit FAILED")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Android Admin cumulative foundation audit passed")
