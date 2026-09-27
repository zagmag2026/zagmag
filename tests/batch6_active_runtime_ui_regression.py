from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"

def read(name: str) -> str:
    return (ANDROID / name).read_text(encoding="utf-8")

activity = read("MainActivity.kt")
root = read("AdminAppScreen4.kt")
booking = read("BookingWorkspaceScreen5.kt")
details = read("BookingDetailsScreen6.kt")
cards = read("ui/components/CardPatterns.kt")
vm = read("BookingLifecycleViewModel.kt")

errors = []

def need(body: str, token: str, label: str) -> None:
    if token not in body:
        errors.append(f"{label}: missing {token}")

need(activity, "ZhagmagAdminRootScreen4(", "Active Admin root")
need(root, "BookingWorkspaceScreen5(", "Active Booking workspace wiring")
need(booking, "val query = viewModel.customerSearch", "Single authoritative Booking customer query")
need(booking, "viewModel.setCustomerSearch(value, allowRemoteSearch = canManageCustomers)", "Active customer search dispatch")
need(booking, "val searchState = viewModel.customerSearchUiState", "Deterministic explicit customer search state")
need(booking, '"Create new customer"', "Active quick-create action")
need(booking, "initialName = customerPrefillName", "Typed name prefill")
need(booking, "initialMobile = customerPrefillMobile", "Typed mobile prefill")
need(booking, '"Searching customers…"', "Visible customer searching state")
need(booking, ".imePadding()", "Booking wizard IME protection")
need(details, ".imePadding()", "Booking detail IME protection")
need(cards, "minSideBySideWidth: Dp = 300.dp", "Responsive triple breakpoint")
need(cards, "minSideBySideWidth: androidx.compose.ui.unit.Dp = 300.dp", "Responsive pair breakpoint")
need(vm, "enum class CustomerSearchUiState { IDLE, SEARCHING, RESULTS, NO_RESULT, ERROR }", "Explicit search-state model")
need(vm, 'savedStateHandle["bookingCustomerSearch"]', "Customer query process restoration")
need(vm, 'savedStateHandle["bookingDraftLines"]', "Booking draft process restoration")

if "var query by rememberSaveable" in booking:
    errors.append("Active Booking customer search has duplicate local query state.")
if "BookingWorkspaceScreen4(" in root:
    errors.append("Active root unexpectedly routes to retired BookingWorkspaceScreen4.")

if errors:
    print("Batch 6 active runtime UI regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Batch 6 active runtime UI regression PASS")
