from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


errors = []


def need(body: str, token: str, label: str) -> None:
    if token not in body:
        errors.append(f"{label}: missing {token}")


core = read(ROOT / "worker/src/index.ts")
auth = read(ROOT / "worker/src/auth.ts")
admin_vm = read(ANDROID / "AdminViewModel.kt")
booking_vm = read(ANDROID / "BookingLifecycleViewModel.kt")
booking_ui = read(ANDROID / "BookingWorkspaceScreen5.kt")
booking_details = read(ANDROID / "BookingDetailsScreen6.kt")
components = read(ANDROID / "ui/components/Components.kt")
settings = read(ANDROID / "Screen10Settings.kt")
root = read(ANDROID / "AdminAppScreen4.kt")
legacy_booking = read(ANDROID / "AdminAppScreen3.kt")


# 1. Valid legacy credentials must not do a second expensive PBKDF2 derivation
# in the same login request. New/reset passwords use the currently supported
# 100k PBKDF2 work factor.
need(auth, "const PASSWORD_ITERATIONS = 100_000;", "PBKDF2 current work factor")
need(
    auth,
    "const MIN_SUPPORTED_PBKDF2_ITERATIONS = 100_000;",
    "Legacy password verification",
)

# The obsolete 210k marker must not remain in the active auth implementation.
if "const PASSWORD_ITERATIONS = 210_000;" in auth:
    errors.append(
        "Auth still contains obsolete const PASSWORD_ITERATIONS = 210_000;"
    )

if "passwordNeedsRehash(row.password_hash)" in core:
    errors.append("Login still synchronously rehashes legacy passwords.")

need(
    admin_vm,
    'error.statusCode == 0 -> "Unable to connect.',
    "Android login network error",
)
need(
    admin_vm,
    'error.statusCode >= 500 -> "Server is unavailable.',
    "Android login server error",
)


# 2. Booking customer quick-create must use one authoritative query state.
for token in [
    "private var customerSearchCompletedState",
    "val customerSearchCompletedQuery",
    "private var customerSearchJob: Job?",
    "private var customerSearchRequestSerial",
    "delay(300)",
    "requestSerial == customerSearchRequestSerial",
    "customerSearchCompletedState = requested",
]:
    need(booking_vm, token, "Deterministic customer search")

for token in [
    "val query = viewModel.customerSearch",
    "val searchState = viewModel.customerSearchUiState",
    "searchState == CustomerSearchUiState.NO_RESULT",
    "viewModel.setCustomerSearch(value, allowRemoteSearch = canManageCustomers)",
    "customerPrefillFromQuery5(trimmedQuery)",
    '"Create new customer"',
    "customerPrefillName = name",
    "customerPrefillMobile = mobile",
    "initialName = customerPrefillName",
    "initialMobile = customerPrefillMobile",
]:
    need(booking_ui, token, "Booking quick-create UI")

if "var query by rememberSaveable" in booking_ui:
    errors.append(
        "Booking Step 1 still keeps a duplicate local customer-query state."
    )


# 3-5. Shared tabs/content hierarchy.
for token in [
    "fontSize = 22.sp",
    "lineHeight = 28.sp",
    "fun AppCompactFixedTabs(",
    "fun AppCompactScrollableTabs(",
    "TabRow(",
    "ScrollableTabRow(",
    "edgePadding = 0.dp",
    "minTabWidth: androidx.compose.ui.unit.Dp = 96.dp",
    "style = MaterialTheme.typography.labelMedium",
    "fontSize = 16.sp",
    "FontWeight.SemiBold",
    "FontWeight.Normal",
    "style = MaterialTheme.typography.bodyMedium",
]:
    need(components, token, "Run 79 shared tab/title typography")

if (
    ".horizontalScroll(rememberScrollState())" in components
    or "minTabWidth: androidx.compose.ui.unit.Dp = 0.dp" in components
):
    errors.append(
        "Shared tabs regressed to the later content-width implementation."
    )

need(
    legacy_booking,
    "style = MaterialTheme.typography.bodyMedium",
    "Legacy Booking tab typography",
)

need(
    booking_details,
    "AppCompactScrollableTabs(",
    "Active Booking Details scrollable tab component",
)
need(
    booking_details,
    "minTabWidth = 96.dp",
    "Active Booking Details minimum tab width",
)

need(
    booking_details,
    'listOf("Details", "Pickup", "Return", "Bill", "History")',
    "Active Booking five-tab order",
)


for token in [
    'fontSize = 22.sp',
    'style = MaterialTheme.typography.titleMedium',
    'style = MaterialTheme.typography.bodyMedium',
    'style = MaterialTheme.typography.bodyLarge',
]:
    need(settings, token, "Settings/Audit typography hierarchy")


# 6-9. WhatsApp Centre naming/layout.
need(
    settings,
    'title = "WhatsApp Centre"',
    "WhatsApp Centre screen title",
)

need(
    root,
    'title = "WhatsApp Centre"',
    "WhatsApp Centre More card",
)

need(
    settings,
    'label = "Add Template"',
    "WhatsApp Centre top Add action",
)

need(
    settings,
    "action = {",
    "WhatsApp Centre header action slot",
)

if 'Text("WhatsApp Templates"' in settings:
    errors.append(
        "Large WhatsApp Templates heading must remain removed."
    )

if (
    'title = "WhatsApp Management"' in settings
    or 'title = "WhatsApp Management"' in root
):
    errors.append(
        "Visible WhatsApp Management label remains after WhatsApp Centre rename."
    )


if errors:
    print("UI/Login batch 1-9 regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)


print("UI/Login batch 1-9 regression PASS")
