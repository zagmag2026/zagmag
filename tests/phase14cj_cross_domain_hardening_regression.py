from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

errors = []

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""

def need(body: str, token: str, label: str) -> None:
    if token not in body:
        errors.append(f"{label} missing: {token}")

freshness = read(ANDROID / "AdminDataFreshness.kt")
for token in [
    "bookingsRevision", "customersRevision", "inventoryRevision", "bookingBootstrapRevision",
    "reportsMetadataRevision", "reportsDataRevision", "usersRevision",
    "fun markInventoryMutation()", "fun markUserMutation()", "fun markSessionBoundary()",
]:
    need(freshness, token, "Freshness coordinator")

booking_list = read(ANDROID / "BookingListViewModel4.kt")
for token in [
    "PendingBookingListRequest4", "private var pendingLoad", "searchJob?.cancel()",
    "pendingLoad = request", "val next = pendingLoad", "if (next != null && next != request) load(next)",
]:
    need(booking_list, token, "Booking list latest-request coordination")

api = read(ANDROID / "data" / "ApiClient.kt")
admin_vm = read(ANDROID / "AdminViewModel.kt")
for token in ["object ApiSessionEvents", "status == 401", "sessionStore.clear()", "notifySessionExpired()"]:
    need(api, token, "Central 401 handling")
for token in ["registerSessionExpiredHandler", "expireSessionLocally()", "markSessionBoundary()"]:
    need(admin_vm, token, "Admin auth/session boundary")

lifecycle = read(ANDROID / "BookingLifecycleViewModel.kt")
for token in [
    "customerSearchError", "allowRemoteSearch", "customerOptions = listOfNotNull(selected)",
    "AdminDataFreshness.markBookingMutation()", "private fun selectedCustomerOption()",
]:
    need(lifecycle, token, "Booking lifecycle hardening")
if "block()\n                AdminDataFreshness.markBookingMutation()" in lifecycle:
    errors.append("Booking mutation freshness must not wait for the post-mutation detail reload.")

workspace = read(ANDROID / "BookingWorkspaceScreen5.kt")
for token in [
    "canManageCustomers", "canPickup", "customerSearchError", "LoadFailureState(",
    "var visibleCount by remember(search, category)", 'key = "booking-item-more-$visibleCount"',
]:
    need(workspace, token, "Booking workspace permission/loading/pagination")

details = read(ANDROID / "BookingDetailsScreen6.kt")
for token in [
    "canContactCustomers", "canPickup", "canReturn", "actionAllowed",
    "Pickup access is not enabled for this account.", "Return access is not enabled for this account.",
]:
    need(details, token, "Booking details permission gating")

screen5_vm = read(ANDROID / "Screen5ItemManagementViewModel.kt")
for token in [
    "seenInventoryRevision", "AdminDataFreshness.markInventoryMutation()",
    "Changes were saved, but latest data could not be loaded. Refresh to sync.",
]:
    need(screen5_vm, token, "Inventory freshness/two-phase mutation")

screen5 = read(ANDROID / "Screen5ItemManagement.kt")
for token in [
    "state.canLoadMoreItems", "relationGroups.take(relatedVisibleLimit)",
    "related-group-more-$relatedVisibleLimit", "if (relatedEditorOpen)",
]:
    need(screen5, token, "Inventory local pagination/nested Back")

users = read(ANDROID / "Screen9UsersViewModel.kt")
for token in ["seenUsersRevision", "AdminDataFreshness.markUserMutation()"]:
    need(users, token, "Users freshness")

settings = read(ANDROID / "Screen10SettingsViewModel.kt")
if settings.count("auditSearchJob?.cancel()") < 6:
    errors.append("Audit search debounce must be cancelled by refresh/filter/date/reset actions.")

reports_vm = read(ANDROID / "Screen8ReportsViewModel.kt")
for token in [
    "reportRequestSerial", "exportRequestSerial", "currentBusinessDate()",
    "AdminDataFreshness.reportsMetadataRevision", "AdminDataFreshness.reportsDataRevision",
    'cacheKey() + "|dataRevision="', "loadError = userMessage(error)",
    "Preset was saved, but latest report metadata could not be loaded. Refresh to sync.",
]:
    need(reports_vm, token, "Reports request/freshness hardening")

root = read(ANDROID / "AdminAppScreen4.kt")
for token in [
    "canPickups", "canReturns", "moreNestedOpen", "rememberSaveableStateHolder()",
    "SaveableStateProvider(destination.name)", "customerListState", "bookingListState",
    "if (!editorOpen && !moreNestedOpen && !dashboardDrilldownActive)",
]:
    need(root, token, "Root permission/navigation/state preservation")

chrome = read(ANDROID / "AdminMainChrome.kt")
need(chrome, "supplied.isBefore(currentBusinessDate)", "Business-date rollover display")
components = read(ANDROID / "ui" / "components" / "Components.kt")
need(components, 'ZoneId.of("Asia/Kolkata")', "Business-zone platform date picker")
need(components, "val pickerZone = zone", "Date picker min/max zone consistency")

phase14o = read(ROOT / "worker" / "src" / "phase14o.js")
for token in [
    "strictDateValidation", "PAST_PICKUP_DATE", "INVALID_RENTAL_DATE",
    "date.toISOString().slice(0, 10) === raw", "returnDate <= pickupDate",
]:
    need(phase14o, token, "Worker booking-date authority")

core = read(ROOT / "worker" / "src" / "index.ts")
for token in [
    "pickupDate<businessToday()", "returnDate<=pickupDate", "permissionsChanged",
    "nextRole!==currentRole || permissionsChanged",
]:
    need(core, token, "Core booking-date/permission-session authority")

reports_worker = read(ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js")
for token in [
    "pe.pickup_at>=?", "re.return_at>=?",
    "istDayStartUtc(filters.fromDate)", "istDayStartUtc(addIsoDays(filters.toDate, 1))",
    "datetime(w.created_at,'+5 hours','+30 minutes') AS activity_at",
]:
    need(reports_worker, token, "Reports IST activity boundaries")
for forbidden in ["substr(pe.pickup_at,1,10) BETWEEN", "substr(re.return_at,1,10) BETWEEN", "substr(w.created_at,1,10)>=?", "substr(w.created_at,1,10)<=?"]:
    if forbidden in reports_worker:
        errors.append(f"UTC calendar-date report filter restored: {forbidden}")

for js in [ROOT / "worker" / "src" / "phase14o.js", ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js"]:
    syntax = subprocess.run(["node", "--check", str(js)], capture_output=True, text=True)
    if syntax.returncode != 0:
        errors.append(f"JavaScript syntax failed for {js.name}: {(syntax.stderr or syntax.stdout).strip()}")

if errors:
    print("Phase 14CJ cross-domain hardening regression FAILED")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Phase 14CJ cross-domain hardening regression PASS")
