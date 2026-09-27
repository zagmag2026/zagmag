from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

ui = (ANDROID / "BookingWorkspaceScreen5.kt").read_text(encoding="utf-8")
root = (ANDROID / "AdminAppScreen4.kt").read_text(encoding="utf-8")
activity = (ANDROID / "MainActivity.kt").read_text(encoding="utf-8")
vm = (ANDROID / "BookingLifecycleViewModel.kt").read_text(encoding="utf-8")
shared = (ANDROID / "CustomerSharedUi.kt").read_text(encoding="utf-8")

errors = []

def need(body: str, token: str, label: str) -> None:
    if token not in body:
        errors.append(f"{label} missing: {token}")

for token in [
    "onQuickCreate: (String, String) -> Unit",
    "val searchState = viewModel.customerSearchUiState",
    "CustomerSearchUiState.SEARCHING",
    "CustomerSearchUiState.NO_RESULT",
    "val showQuickCreate = canManageCustomers",
    "customerPrefillFromQuery5(trimmedQuery)",
    '"Create new customer"',
    "customerPrefillName = name",
    "customerPrefillMobile = mobile",
    "initialName = customerPrefillName",
    "initialMobile = customerPrefillMobile",
    "viewModel.clearNewCustomerFeedback()",
    "searching = searchState == CustomerSearchUiState.SEARCHING",
]:
    need(ui, token, "Booking customer quick-create UI")

for token in [
    "val query = viewModel.customerSearch",
    "viewModel.setCustomerSearch(value, allowRemoteSearch = canManageCustomers)",
    "customerSearchUiState",
]:
    need(ui, token, "Booking customer authoritative search state")

for token in [
    "enum class CustomerSearchUiState { IDLE, SEARCHING, RESULTS, NO_RESULT, ERROR }",
    "val customerSearchUiState: CustomerSearchUiState",
    "private var customerSearchCompletedState",
    "private var customerSearchJob: Job?",
    "private var customerSearchRequestSerial",
    "delay(300)",
    "requestSerial == customerSearchRequestSerial",
    "customerSearchCompletedState = requested",
]:
    need(vm, token, "Booking customer deterministic search state")

if "var query by rememberSaveable" in ui:
    errors.append("Booking customer search must not keep a second local query state.")

for token in [
    'error.apiCode == "DUPLICATE_MOBILE"',
    "repository.customers(1, primary, 30)",
    "it.mobile.filter(Char::isDigit) == primary",
    "selectBookingCustomer(existing)",
    "private fun selectBookingCustomer(customer: BookingOptionCustomer)",
    "selectedCustomerId = customer.id",
]:
    need(vm, token, "Booking customer duplicate recovery")

for token in [
    'initialName: String = ""',
    'initialMobile: String = ""',
    "var name by rememberSaveable(initialName)",
    "var mobile by rememberSaveable(initialMobile)",
]:
    need(shared, token, "Reusable customer prefill contract")

create_start = vm.find("fun createCustomerForBooking(")
create_end = vm.find("fun validateCustomerStep()", create_start)
if create_start < 0 or create_end < 0:
    errors.append("Could not inspect createCustomerForBooking block.")
else:
    create_block = vm[create_start:create_end]
    if 'customerSearchState = ""' in create_block:
        errors.append("Quick-create must preserve the Booking customer search value after save/select.")

if "trimmedQuery.filter(Char::isDigit)" in ui and 'queryDigits.isNotBlank()' not in ui:
    errors.append("Name search must not become a false mobile match through an empty digit query.")

quick_start = ui.find("val searchState = viewModel.customerSearchUiState")
quick_end = ui.find('LabeledSectionCard(title = "Customer")', quick_start)
if quick_start < 0 or quick_end < 0:
    errors.append("Could not inspect active Booking quick-create visibility block.")
else:
    quick_block = ui[quick_start:quick_end]
    for token in ["CustomerSearchUiState.NO_RESULT", "showQuickCreate", "trimmedQuery.isNotBlank()"]:
        need(quick_block, token, "Active Booking no-result Quick Create")

need(ui, "InlineStatusMessage(", "Booking customer search error feedback")
need(ui, "viewModel.customerSearchError", "Booking customer search error feedback")

if errors:
    print("Booking customer quick-create regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Booking customer quick-create regression PASS")
