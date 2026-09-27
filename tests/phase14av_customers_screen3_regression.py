#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def main():
    worker = read("worker/src/phase14av-customers-screen3.js")
    screen4_worker = read("worker/src/phase14aw-screen4-bookings.js")
    migration = read("database/migrations/0012_customer_screen3_whatsapp_bilingual.sql")
    legacy_screen = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/CustomerScreenV3.kt")
    screen = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/CustomerScreenV4.kt")
    shared = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/CustomerSharedUi.kt")
    vm = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/CustomerScreen3ViewModel.kt")
    repo = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/data/CustomerScreen3Repository.kt")
    models = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/data/CustomerScreenModels.kt")
    root = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/AdminAppScreen3.kt")
    activity = read("apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/MainActivity.kt")
    local = read("worker/wrangler.toml")
    staging = read("worker/wrangler.staging.toml.template")
    production = read("worker/wrangler.production.toml.template")

    assert 'import core from "./phase14au-dashboard-screen2.js";' in worker
    assert 'import core from "./phase14av-customers-screen3.js";' in screen4_worker
    assert 'url.pathname === "/api/admin/customers"' in worker
    assert 'Math.min(10' in worker and 'alternate_mobile' in worker
    assert 'NAME_ASC' in worker and 'NAME_DESC' in worker and 'NEWEST' in worker and 'OLDEST' in worker
    assert 'ACTIVE_BOOKINGS' in worker and 'DELETE_PERMANENT' in worker
    assert 'pickup_event_items' in worker and 'return_event_items' in worker and 'booking_items' in worker
    assert 'booking_close_event_items' in worker and 'booking_close_events' in worker
    bill_detach = worker.find("booking_id=NULL")
    close_items_delete = worker.find("DELETE FROM booking_close_event_items")
    close_events_delete = worker.find("DELETE FROM booking_close_events")
    booking_items_delete = worker.find("DELETE FROM booking_items")
    bookings_delete = worker.find("DELETE FROM bookings")
    tombstone_update = worker.find("permanently_deleted_at=?")
    permanent_audit = worker.find('auditStatement(env, user.id, "DELETE_PERMANENT"')
    assert min(bill_detach, close_items_delete, close_events_delete, booking_items_delete, bookings_delete, tombstone_update, permanent_audit) >= 0
    assert bill_detach < close_items_delete < close_events_delete < booking_items_delete < bookings_delete < tombstone_update < permanent_audit
    assert "DELETE FROM customers WHERE id=?" not in worker
    assert 'auditStatement(env, user.id, "DELETE_PERMANENT"' in worker
    assert 'bookingHistoryPreserved: true' in worker
    assert 'financialHistoryPreserved: true' in worker and 'billsPreserved: billCount' in worker
    assert 'c.permanently_deleted_at IS NULL' in worker
    assert 'bookingsArchivedWithCustomer' not in worker and 'bookingsRestoredWithCustomer' not in worker
    assert 'c.archived_at IS NULL' in worker
    assert 'message_gu' in worker and 'message_en' in worker and 'missed_pickup_reminder' in worker
    assert 'ALTER TABLE whatsapp_templates ADD COLUMN message_gu TEXT' in migration
    assert 'ALTER TABLE whatsapp_templates ADD COLUMN message_en TEXT' in migration

    # CustomerScreenV3 is compatibility-only. Canonical Customer UI is V4 so tests must follow
    # the actual rendered implementation rather than requiring retired V3-owned widgets.
    assert 'CustomerScreenV4(' in legacy_screen
    assert 'onLoadMore = { if (state.canLoadMore) onPage(state.page + 1) }' in legacy_screen
    assert 'PullToRefreshBox' in screen
    assert 'Total Booking · ${customer.totalBookings}' in screen
    assert 'Active Booking · ${customer.activeBookings}' in screen
    assert 'label = "Mobile Number *"' in shared and 'label = "Address"' in shared
    assert 'Alternative Mobile Number' not in screen and 'Alternative Mobile Number' not in shared
    assert 'label = "Booking"' in screen and 'label = "Archive"' in screen
    action_start = screen.find("private fun CustomerCard4(")
    assert action_start >= 0
    action_block = screen[action_start:]
    ordered = [action_block.find(f'label = "{label}"') for label in ["Edit", "Archive", "Booking", "Call", "WhatsApp"]]
    assert min(ordered) >= 0 and ordered == sorted(ordered)
    assert "ResponsiveSoftActionGrid(" not in action_block
    assert 'Delete permanently' in screen and 'Restore' in screen and '"Archive"' in screen
    assert 'var confirmRestore by remember' in screen
    assert 'title = "Restore customer?"' in screen
    assert 'title = "Archive customer?"' in screen
    assert 'requiredPhrase = "DELETE CUSTOMER"' in screen
    assert 'Billing financial history is preserved.' in screen and 'This cannot be undone.' in screen
    assert 'Existing booking history will become visible again.' in screen
    assert 'title = { Text("Delete customer?") }' not in screen
    assert 'Confirm Call' in shared and 'WhatsApp Preview' in shared and 'ModalBottomSheet' in shared
    assert 'label = "Name *"' in shared and '"Name is required."' in shared
    assert '"Mobile number is required."' in shared and '"Enter a valid 10-digit mobile number."' in shared
    assert 'No customers found.' in screen and 'No archived customers.' in screen
    assert 'state.canLoadMore' in screen and 'onLoadMore()' in screen

    assert 'delay(300)' in vm and 'sort = "NAME_ASC"' in vm
    assert 'state = state.copy(actionBusy = true, error = null, notice = null)\n        viewModelScope.launch {' in vm
    assert 'fun loadMore()' in vm and 'append = true' in vm
    assert 'suspend fun customers' in repo and '"pageSize" to "10"' in repo
    assert 'CustomerSummary' in models and 'active_booking_options' in models
    assert 'role = user.role' in root and 'bookingViewModel.setSelectedCustomer(customer.id)' in root
    assert 'customerViewModel.ensureLoaded()' in root and 'PullToRefreshBox' in root and 'DashboardScreenV2' in root
    assert 'ZhagmagAdminRootScreen4(' in activity
    assert 'bookingListViewModel = bookingListViewModel' in activity and 'customerViewModel = customerViewModel' in activity

    assert 'main = "src/phase14aw-screen4-bookings.js"' in local
    assert 'main = "src/phase14aw-screen4-bookings.js"' in staging
    assert 'main = "src/phase14at-booking-lifecycle.js"' in production
    print("Screen 3 Customers canonical V4 UI preserved through Screen 4 staging wrapper: PASS")


if __name__ == "__main__":
    main()
