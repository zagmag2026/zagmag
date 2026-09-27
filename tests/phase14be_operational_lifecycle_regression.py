from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(text: str, tokens: list[str], label: str) -> None:
    missing = [token for token in tokens if token not in text]
    if missing:
        raise AssertionError(f"{label} missing: {missing}")


migration = read("database/migrations/0017_operational_lifecycle.sql")
worker = read("worker/src/phase14be-operational-lifecycle.js")
wrangler = read("worker/wrangler.toml")
staging = read("worker/wrangler.staging.toml.template")
list_vm = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/BookingListViewModel4.kt")
detail = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/BookingDetailsScreen6.kt")
lifecycle_vm = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/BookingLifecycleViewModel.kt")
models = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/data/BookingLifecycleModels.kt")
dashboard = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/DashboardScreenV2.kt")
screen4 = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/AdminAppScreen4.kt")
reports = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/Screen8Reports.kt")

require(migration, [
    "ADD COLUMN closed_qty",
    "CREATE TABLE IF NOT EXISTS booking_close_events",
    "CREATE TABLE IF NOT EXISTS booking_close_event_items",
    "DROP TRIGGER IF EXISTS trg_booking_status_sync_quantities",
    "given_qty < (booked_qty - closed_qty)",
    "UPDATE bookings",
    "WHERE status <> 'CANCELLED'",
], "0017 migration")

require(worker, [
    'import core from "./phase14bd-screen8-reports.js"',
    'requirePermission(request, env, "DASHBOARD")',
    'requirePermission(request, env, "BOOKINGS")',
    'requirePermission(request, env, "PICKUPS")',
    'requirePermission(request, env, "RETURNS")',
    "COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED'",
    "pickup_pending>0",
    "return_pending>0",
    'view === "PART_PICKUP"',
    'view === "FULL_PICKUP"',
    'view === "PART_RETURN"',
    'view === "FULL_RETURN"',
    '"PENDING_PICKUP_DECISION_REQUIRED"',
    '"KEEP_OPEN"',
    '"CLOSE_REMAINING"',
    '"CLOSE_REMAINING_ITEMS"',
    "booking_close_event_items",
    "movementStatus: pickupEventStatus",
    "movementStatus: returnEventStatus",
    "original_booked_qty",
    "closed_qty",
    "booked_qty-closed_qty",
], "Phase 14BE worker")

if "Pickup cannot be changed after return has started." in worker:
    raise AssertionError("Phase 14BE must allow remaining pickup after an earlier return when the order stays open.")

require(wrangler, ['main = "src/phase14br-billing-v1.js"', 'phase14be-operational-lifecycle.js'], "local cumulative worker entry")
require(staging, ['main = "src/phase14br-billing-v1.js"', 'phase14be-operational-lifecycle.js'], "staging cumulative worker entry")

require(list_vm, [
    'PART_PICKUP("PART_PICKUP", "Part Picked Up")',
    'FULL_PICKUP("FULL_PICKUP", "Full Picked Up")',
    'PART_RETURN("PART_RETURN", "Part Return")',
    'FULL_RETURN("FULL_RETURN", "Full Returned")',
], "Booking tabs")

require(screen4, [
    '"PART_PICKUP" -> "Part Picked Up"',
    '"FULL_PICKUP" -> "Full Picked Up"',
    '"PART_RETURN" -> "Part Return"',
    '"FULL_RETURN" -> "Full Returned"',
], "Visible lifecycle labels")

require(detail, [
    'text = "Directly Pickup"',
    "confirmReservedForPickup",
    '"Keep Order Open"',
    '"Close Remaining Items"',
    'viewModel.saveReturn("KEEP_OPEN")',
    'viewModel.saveReturn("CLOSE_REMAINING")',
    '"Remaining Items Closed"',
    '"• ${item.itemName} × ${item.quantity}"',
], "Booking details operational flow")

require(lifecycle_vm, [
    "fun confirmReservedForPickup",
    "fun saveReturn(pendingPickupAction: String? = null)",
    "private suspend fun reloadDetailAwaited(id: String)",
    "detailState = detailState.copy(loading = true, error = null)",
    "detailRefreshBlocked = true",
    "detailRefreshBlocked = false",
    "reloadDetailAwaited(detail.booking.id)",
    "reloadDetailAwaited(id)",
    "actionBusy = true",
    "finally {",
    "actionBusy = false",
], "Booking lifecycle ViewModel")

pickup_body = lifecycle_vm[lifecycle_vm.find("fun savePickup()"):lifecycle_vm.find("fun saveReturn(")]
if not (pickup_body.find("reloadDetailAwaited(detail.booking.id)") < pickup_body.find("actionNotice = message")):
    raise AssertionError("Pickup success must refresh/apply detail before success feedback.")

return_body = lifecycle_vm[lifecycle_vm.find("fun saveReturn("):lifecycle_vm.find("private fun createBooking(")]
return_reload = return_body.find("reloadDetailAwaited(detail.booking.id)")
return_notice = return_body.find("actionNotice =")
if return_reload < 0 or return_notice < 0 or return_reload > return_notice:
    raise AssertionError("Return success must refresh/apply detail before success feedback.")

awaited_reload = lifecycle_vm[lifecycle_vm.find("private suspend fun reloadDetailAwaited"):lifecycle_vm.find("private fun reloadDetail(", lifecycle_vm.find("private suspend fun reloadDetailAwaited"))]
if "detailState = DataState(loading = true)" in awaited_reload:
    raise AssertionError("Awaited Booking Detail refresh must preserve the current detail instead of blanking the screen.")
if "detailState = detailState.copy(loading = true, error = null)" not in awaited_reload:
    raise AssertionError("Awaited Booking Detail refresh must mark loading while retaining existing data.")
if "detailRefreshBlocked = true" not in awaited_reload:
    raise AssertionError("Failed post-mutation detail refresh must block stale booking mutations.")

require(models, [
    "val effectiveBookedQty: Int",
    "val closedQty: Int",
    "val remainingToGive: Int get() = (effectiveBookedQty - givenQty)",
    "data class BookingHistoryItem",
    "val items: List<BookingHistoryItem>",
], "Booking lifecycle models")

for token in [
    '"Today Overview"', '"Booking Status"', '"Payment & Billing"',
    '"Inventory"', '"Customers"', '"Category-wise Inventory"',
    "DashboardKpiCard(", "DashboardCategoryCard(", "maxWidth >= 720.dp",
]:
    if token not in dashboard:
        raise AssertionError(f"KPI Dashboard contract missing: {token}")
for forbidden in ["DashboardSection(", "DashboardBookingCard(", "BookingSummaryCard("]:
    if forbidden in dashboard:
        raise AssertionError(f"KPI Dashboard must not render retired operational UI: {forbidden}")
for token in [
    "One bundled query owns all 34 fixed KPIs",
    "category_inventory_json",
    "missedPickups: []",
    "todayPickups: []",
    "todayReturns: []",
    "overdueReturns: []",
]:
    if token not in worker:
        raise AssertionError(f"Active Phase 14BE KPI worker missing: {token}")

require(reports, [
    "import kotlinx.coroutines.CancellationException",
    "catch (cancelled: CancellationException)",
    "throw cancelled",
    "finally {",
    "if (pendingPdfAction == action) pendingPdfAction = null",
], "Screen 8 cancellation safety")

bad = reports.find("pendingPdfAction = null\n        runCatching")
if bad >= 0:
    raise AssertionError("Screen 8 must not clear its LaunchedEffect key before PDF work.")

print("Phase 14BE operational lifecycle regression PASS")
