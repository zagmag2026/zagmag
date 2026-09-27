from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")

components = read(ANDROID / "ui/components/Components.kt")
booking_vm = read(ANDROID / "BookingLifecycleViewModel.kt")
booking_ui = read(ANDROID / "BookingWorkspaceScreen5.kt")
customer_ui = read(ANDROID / "CustomerSharedUi.kt")
customer_vm = read(ANDROID / "CustomerScreen3ViewModel.kt")
booking_list_vm = read(ANDROID / "BookingListViewModel4.kt")
screen5 = read(ANDROID / "Screen5ItemManagement.kt")
screen5_vm = read(ANDROID / "Screen5ItemManagementViewModel.kt")
screen9 = read(ANDROID / "Screen9Users.kt")
screen9_vm = read(ANDROID / "Screen9UsersViewModel.kt")
screen10 = read(ANDROID / "Screen10Settings.kt")
screen10_vm = read(ANDROID / "Screen10SettingsViewModel.kt")
reports = read(ANDROID / "Screen8Reports.kt")
reports_vm = read(ANDROID / "Screen8ReportsViewModel.kt")
billing = read(ANDROID / "Screen11Billing.kt")
billing_vm = read(ANDROID / "Screen11BillingViewModel.kt")
billing_models = read(ANDROID / "Screen11BillingModels.kt")
item_worker = read(ROOT / "worker/src/phase14ba-screen5-item-management.js")
billing_worker = read(ROOT / "worker/src/phase14br-billing-v1.js")
global_rules = read(ROOT / "docs/GLOBAL_UI_RULES.md")
item_doc = read(ROOT / "docs/ADMIN_SCREEN_05_ITEM_MANAGEMENT.md")
settings_doc = read(ROOT / "docs/ADMIN_SCREEN_10_SETTINGS.md")
billing_doc = read(ROOT / "docs/BILLING_MODULE.md")

checks = [
    (components, "fun AppUnsavedChangesDialog(", "shared unsaved dialog"),
    (components, "fun InlineRetryMessage(", "shared inline retry"),

    (booking_vm, "fun hasUnsavedEditorChanges()", "Booking editor baseline dirty check"),
    (booking_ui, "if (viewModel.hasUnsavedEditorChanges()) showDiscardChanges = true", "Booking exit guard"),
    (customer_ui, "val dirty = name != initialName", "Customer dirty form guard"),
    (customer_ui, "onDismissRequest = requestDismiss", "Customer guarded sheet dismiss"),

    (screen5, "val relatedEditorDirty = relatedSelectionDirty || sourceItemId != relatedInitialSourceId", "Related Group dirty guard"),
    (screen5, "AppUnsavedChangesDialog(", "Item Management dirty dialogs"),
    (screen5_vm, "fun retryItems()", "Items first-page retry"),
    (screen5_vm, "fun retryLoadMoreItems()", "Items next-page retry"),
    (screen5, "retryLabel = \"Retry loading more\"", "Items load-more retry UI"),

    (screen9, "onDismissRequest = requestDismiss", "Users guarded sheet dismiss"),
    (screen9, "if (password.isNotBlank()) showDiscardChanges = true", "Reset Password dirty guard"),
    (screen9_vm, "fun retryLoadMore()", "Users next-page retry"),

    (screen10, "LazyRow(", "WhatsApp Linked Action LazyRow"),
    (screen10, "actionListState.animateScrollToItem(index)", "WhatsApp selected action auto-scroll"),
    (screen10, 'Text("Hide Keyboard")', "WhatsApp multiline keyboard close action"),
    (screen10, "onDismissRequest = requestDismiss", "WhatsApp Template dirty dismiss"),
    (screen10_vm, "fun retryLoadMoreAudit()", "Audit next-page retry"),

    (customer_vm, "fun retryLoadMore()", "Customers next-page retry"),
    (booking_list_vm, "fun retryLoadMore()", "Bookings next-page retry"),
    (reports_vm, "fun retryLoadMore()", "Reports next-page retry"),
    (reports, 'retryLabel = "Retry loading more"', "Reports load-more retry UI"),

    (item_worker, "const imagesByItem = new Map()", "Related Item full gallery URL metadata"),
    (item_worker, "images: imagesByItem.get(String(row.id)) || []", "Related Item gallery list"),

    (billing_worker, "async function attachBillingItemImages(", "Billing full image URL enrichment"),
    (billing_worker, "image_urls:", "Billing image URL payload"),
    (billing_models, "val imageUrls: List<String> = emptyList()", "Billing image URL models"),
    (billing, "imageUrls = line.imageUrls", "Billing shared gallery parity"),

    (billing_vm, "val listLoadError: String? = null", "Bills initial error state"),
    (billing_vm, "val listLoadMoreError: String? = null", "Bills load-more error state"),
    (billing_vm, "val eligibleLoadError: String? = null", "Select Order initial error state"),
    (billing_vm, "val detailLoadError: String? = null", "Bill detail/embedded error state"),
    (billing_vm, "fun retryLoadMore()", "Bills load-more retry"),
    (billing_vm, "fun retryLoadMoreEligibleOrders()", "Select Order load-more retry"),
    (billing_vm, "fun retryDetail()", "Bill Detail retry"),
    (billing_vm, "fun retryBookingBill(bookingId: String)", "Embedded Bill retry"),
    (billing_vm, "fun hasUnsavedEditorChanges()", "Bill editor dirty check"),
    (billing, "BillingLoadFailureScreen(", "Billing explicit failure surface"),
    (billing, "AppUnsavedChangesDialog(", "Billing dirty Back guard"),
    (billing, "keyboardActions = KeyboardActions(onNext = { rentFocus.requestFocus() })", "Billing Qty to Rent IME flow"),
    (billing, "discountFocus", "Billing sequential amount focus"),
    (billing, "dismissKeyboard()", "Billing commit keyboard dismissal"),
    (billing, 'retryLabel = "Retry loading more"', "Billing page retry UI"),

    (global_rules, "Editable forms use one reusable dirty-state Back/dismiss rule", "Global dirty rule docs"),
    (item_doc, "Retry Items", "Item retry docs"),
    (settings_doc, "automatically scrolls the currently selected Linked Action into view", "WhatsApp action-scroll docs"),
    (billing_doc, "Embedded Booking Details → Bill never remains on an indefinite opening spinner", "Embedded Billing failure docs"),
]

errors = [label for body, token, label in checks if token not in body]

if billing.count("imageUrls = line.imageUrls") < 2:
    errors.append("Billing Edit and Detail must both pass full imageUrls.")
if screen5.count("AppUnsavedChangesDialog(") < 4:
    errors.append("Screen 5 Category/Field/Item/Related dirty guards are incomplete.")
if 'onDismissRequest = { if (!busy) onDismiss() }' in screen9:
    errors.append("Users must not restore direct dirty-form dismiss.")
if "state.listLoaded && !state.loading && state.list.bills.isEmpty()" not in billing:
    errors.append("Bills Empty state must require a successful list load.")
if "state.eligibleLoaded && !state.loading && state.eligibleOrders.orders.isEmpty()" not in billing:
    errors.append("Select Order Empty state must require a successful eligible-order load.")

if errors:
    raise SystemExit("Phase14CM Step 3-7 hardening regression FAILED: " + "; ".join(errors))
print("Phase14CM Step 3-7 hardening regression PASS")
