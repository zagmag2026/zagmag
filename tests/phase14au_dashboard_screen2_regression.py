#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"
ACTIVE = ROOT / "worker/src/phase14be-operational-lifecycle.js"
BILLING = ROOT / "worker/src/phase14br-billing-v1.js"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require(body: str, token: str, label: str) -> None:
    assert token in body, f"{label} missing: {token}"


def main() -> None:
    active = read(ACTIVE)
    billing_worker = read(BILLING)
    reports_worker = read(ROOT / "worker/src/phase14bq-global-rental-reports.js")
    payment_classifier = read(ROOT / "worker/src/payment-classifier.js")
    dashboard = read(ANDROID / "DashboardScreenV2.kt")
    models = read(ANDROID / "data/Models.kt")
    root = read(ANDROID / "AdminAppScreen4.kt")
    booking_vm = read(ANDROID / "BookingListViewModel4.kt")
    reports_vm = read(ANDROID / "Screen8ReportsViewModel.kt")
    reports_repo = read(ANDROID / "Screen8ReportsRepository.kt")
    item_screen = read(ANDROID / "Screen5ItemManagement.kt")
    billing = read(ANDROID / "Screen11Billing.kt")
    billing_vm = read(ANDROID / "Screen11BillingViewModel.kt")
    freshness = read(ANDROID / "AdminDataFreshness.kt")
    wrangler = read(ROOT / "worker/wrangler.toml")
    staging = read(ROOT / "worker/wrangler.staging.toml.template")
    production = read(ROOT / "worker/wrangler.production.toml.template")

    # Runtime route: phase14br -> phase14bq -> phase14bg -> phase14be.
    require(billing_worker, 'import core from "./phase14bq-global-rental-reports.js";', "Active Billing wrapper chain")
    require(reports_worker, 'import core from "./phase14bg-settings-whatsapp.js";', "Active Reports wrapper chain")
    require(active, 'import core from "./phase14bd-screen8-reports.js";', "Active operational wrapper chain")
    require(active, 'url.pathname === "/api/admin/dashboard"', "Active Dashboard route")
    require(active, 'url.pathname === "/api/admin/bookings"', "Active Booking route")
    require(active, 'requirePermission(request, env, "DASHBOARD")', "Dashboard permission")
    require(active, 'requirePermission(request, env, "BOOKINGS")', "Bookings permission")
    require(active, 'import { bookingPaymentStatusSql } from "./payment-classifier.js";', "Active shared payment classifier")

    # One bundled Dashboard query owns the KPI payload.
    for token in [
        "One bundled query owns all 34 fixed KPIs",
        "WITH booking_rollup AS (",
        "item_flow AS (",
        "customer_rollup AS (",
        "bill_rollup AS (",
        "category_inventory_json",
        "kpis: visibleKpis",
        "categoryInventory:",
        "sectionCounts:",
    ]:
        require(active, token, "Dashboard bundled API")

    # Exact fixed KPI groups = 4 + 8 + 8 + 8 + 6 = 34.
    fixed_kpis = [
        "Today Pickups", "Today Returns", "Missed Pickups", "Overdue Returns",
        "Reserved", "Booked", "Part Picked Up", "Full Picked Up",
        "Part Return", "Full Returned", "Cancelled", "Active Rental Orders",
        "Pending Payment", "Part Payment", "Full Payment", "Draft Bills",
        "Final Bills", "Bills Today", "Pending Balance", "Total Received",
        "Available Now", "Pickup Pending Qty", "Currently Out Qty", "Total Quantity",
        "Total Items", "Categories", "Low Stock", "Unavailable",
        "Total Customers", "New Customers", "Returning Customers",
        "Frequent Customers", "Active Rental Customers", "Customer Exceptions",
    ]
    assert len(fixed_kpis) == 34
    for label in fixed_kpis:
        require(dashboard, f'"{label}"', "34-KPI Dashboard UI")

    for group in ["Today Overview", "Booking Status", "Payment & Billing", "Inventory", "Customers"]:
        require(dashboard, f'title = "{group}"', "Dashboard section")
    require(dashboard, '"Category-wise Inventory"', "Dynamic category section")
    for forbidden in ["Subcategory-wise", "Owner Activity", "Owner / Activity"]:
        assert forbidden not in dashboard

    # Responsive reusable cards.
    require(dashboard, "val columns = if (maxWidth >= 720.dp) 4 else 2", "Dashboard 2/4-column layout")
    require(dashboard, "private fun DashboardKpiCard(", "Shared KPI card")
    require(dashboard, "private fun DashboardCategoryCard(", "Dynamic category card")

    # Inventory authority: all active commitments, including Reserved/future, reduce Available Now.
    item_flow_start = active.index("item_flow AS (")
    item_rollup_start = active.index("item_rollup AS (", item_flow_start)
    item_flow = active[item_flow_start:item_rollup_start]
    require(item_flow, "b.status<>'CANCELLED'", "Pickup Pending active booking guard")
    require(item_flow, "(bi.booked_qty-COALESCE(bi.closed_qty,0))>bi.given_qty", "Pickup Pending quantity rule")
    assert "confirmation_state" not in item_flow, "Reserved commitments must remain in Pickup Pending inventory."
    assert "pickup_date" not in item_flow, "Future commitments must remain in Pickup Pending inventory."
    require(active, "MAX(0,total_quantity-pickup_pending_qty-currently_out_qty) AS available_now", "Available Now formula")
    require(reports_worker, "dashboard_pickup_pending_qty", "Inventory drilldown parity")
    report_pickup = reports_worker[reports_worker.index("dashboard_pickup_pending_qty") - 500:reports_worker.index("dashboard_pickup_pending_qty") + 700]
    assert "confirmation_state" not in report_pickup
    assert "pickup_date='" not in report_pickup

    # Mutually exclusive lifecycle precedence.
    require(active, "AND NOT (pending_return_qty>0 AND returned_qty>0)", "Dashboard Part Pickup exclusion")
    require(active, 'view === "PART_RETURN"', "Part Return Booking filter")
    require(active, 'view === "FULL_RETURN"', "Full Return Booking filter")
    require(active, 'view === "TODAY_PICKUP"', "Today Pickup Booking filter")
    require(active, 'view === "TODAY_RETURN"', "Today Return Booking filter")
    require(active, 'view === "MISSED_PICKUP"', "Missed Pickup Booking filter")
    require(active, 'view === "ACTIVE_RENTAL"', "Active Rental Booking filter")
    require(reports_worker, "WHEN COALESCE(q.pending_return_qty,0)>0 AND COALESCE(q.returned_qty,0)>0 THEN 'PART_RETURN'", "Reports lifecycle precedence")

    # Payment classification is shared by active Dashboard, active Booking list and active Billing enrichment.
    require(active, 'const paymentStatusSql = bookingPaymentStatusSql("b");', "Active payment classifier")
    for token in ['view === "PAYMENT_PENDING"', 'view === "PAYMENT_PART"', 'view === "PAYMENT_FULL"']:
        require(active, token, "Booking payment filter")
    require(billing_worker, 'import { bookingPaymentStatusSql } from "./payment-classifier.js";', "Billing payment classifier")
    require(billing_worker, 'const paymentCase = bookingPaymentStatusSql("b")', "Billing enrichment classifier")
    require(payment_classifier, "COALESCE(pb.net_amount,0) > 0", "Positive Bill Amount Full Payment guard")
    assert "pb.payment_status" not in payment_classifier

    # OWNER-only monetary KPI values are not usable for Staff.
    for token in [
        'billsToday: user.role === "OWNER" ? kpis.paymentBilling.billsToday : 0',
        'pendingBalance: user.role === "OWNER" ? kpis.paymentBilling.pendingBalance : 0',
        'totalReceived: user.role === "OWNER" ? kpis.paymentBilling.totalReceived : 0',
    ]:
        require(active, token, "Dashboard monetary permission boundary")

    # Retired Dashboard queues remain empty and the active Dashboard SQL has no queue JSON payload.
    for removed in ["today_pickups_json", "today_returns_json", "missed_pickups_json", "overdue_returns_json"]:
        assert removed not in active
    for empty_queue in [
        "missedPickups: []", "todayBookings: []", "todayPickups: []",
        "todayReturns: []", "overdueReturns: []",
    ]:
        require(active, empty_queue, "Retired Dashboard detail queue")

    # Exact drilldown reuse and Back state restoration.
    for token in [
        "onKpiClick = onDashboardKpiClick",
        "openDashboardBookingFilter(BookingListFilter4.TODAY_PICKUP)",
        "openDashboardBookingFilter(BookingListFilter4.PAYMENT_FULL)",
        'openDashboardMore(MoreDestination4.BILLING',
        'openDashboardReport("PENDING_BALANCE"',
        'openDashboardReport("AVAILABILITY"',
        'destination = Screen4Destination.CUSTOMERS',
        "DashboardKpiAction.CATEGORY_INVENTORY",
        "returnToDashboardFromDrilldown",
        "dashboardScreenState",
    ]:
        require(root, token, "Dashboard exact drilldown navigation")

    require(item_screen, 'initialCategoryId: String = ""', "Category exact-filter deep link")
    require(item_screen, "vm.openItemCategory(initialCategoryId)", "Category exact paged filter applied")
    require(billing, 'initialPaymentFilter: String = ""', "Billing payment deep link")
    require(billing, 'initialStatusFilter: String = ""', "Billing status deep link")
    require(billing_vm, "fun openFilteredList(", "Billing filtered list")
    require(reports_vm, "fun openFromDashboard(", "Reports Dashboard deep link")
    require(reports_repo, '"dashboardFilter" to config.dashboardFilter.takeIf { it.isNotBlank() }', "Reports exact hidden filter")
    assert reports_repo.count('config.copy(dashboardFilter = "").toJson()') == 2

    start = booking_vm.index("fun clearDashboardFilter()")
    end = booking_vm.index("fun setSort(", start)
    clear_block = booking_vm[start:end]
    require(clear_block, "filter = BookingListFilter4.ALL", "Temporary Booking filter reset")
    require(clear_block, "loaded = false", "Deferred normal Booking reload")
    assert "requestLoad(" not in clear_block
    assert "setFilter(" not in clear_block

    # Billing mutations make Dashboard stale, without Dashboard polling.
    require(freshness, "fun markBillingMutation()", "Billing freshness")
    billing_freshness = freshness[freshness.index("fun markBillingMutation()"):freshness.index("fun markReportsMetadataMutation()")]
    require(billing_freshness, "dashboardRevision += 1", "Dashboard billing freshness")
    assert "while (true)" not in dashboard
    assert "delay(" not in dashboard

    for token in [
        "data class DashboardTodayKpis(", "data class DashboardBookingKpis(",
        "data class DashboardPaymentBillingKpis(", "data class DashboardInventoryKpis(",
        "data class DashboardCustomerKpis(", "data class DashboardCategoryInventory(",
        "data class DashboardAccess(",
    ]:
        require(models, token, "Dashboard KPI models")

    # Staging/local entrypoint is Billing V1; production template remains independently unchanged.
    assert 'main = "src/phase14br-billing-v1.js"' in wrangler
    assert 'main = "src/phase14br-billing-v1.js"' in staging
    assert 'main = "src/phase14at-booking-lifecycle.js"' in production

    for rel in [
        "worker/src/payment-classifier.js",
        "worker/src/phase14be-operational-lifecycle.js",
        "worker/src/phase14bq-global-rental-reports.js",
        "worker/src/phase14br-billing-v1.js",
    ]:
        proc = subprocess.run(["node", "--check", str(ROOT / rel)], cwd=ROOT, text=True, capture_output=True)
        if proc.returncode != 0:
            raise AssertionError(f"{rel} JS syntax failed: {(proc.stderr or proc.stdout).strip()}")

    print("Phase 14AU/Issue 6 active 34-KPI Dashboard contract: PASS")


if __name__ == "__main__":
    main()
