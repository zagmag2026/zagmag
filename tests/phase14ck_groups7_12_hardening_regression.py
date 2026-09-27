#!/usr/bin/env python3
from __future__ import annotations

import sqlite3
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"
MIGRATIONS = ROOT / "database" / "migrations"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


def need(body: str, token: str, label: str) -> None:
    if token not in body:
        errors.append(f"{label} missing: {token}")


def apply_migrations(conn: sqlite3.Connection) -> None:
    for migration in sorted(MIGRATIONS.glob("*.sql")):
        conn.executescript(migration.read_text(encoding="utf-8"))


freshness = read(ANDROID / "AdminDataFreshness.kt")
admin_vm = read(ANDROID / "AdminViewModel.kt")
root = read(ANDROID / "AdminAppScreen4.kt")
lifecycle = read(ANDROID / "BookingLifecycleViewModel.kt")
admin_repo = read(ANDROID / "data" / "AdminRepository.kt")
customer_repo = read(ANDROID / "data" / "CustomerScreen3Repository.kt")
customer_models = read(ANDROID / "data" / "CustomerScreenModels.kt")
screen5_repo = read(ANDROID / "Screen5ItemManagementRepository.kt")
screen5_models = read(ANDROID / "Screen5ItemManagementModels.kt")
screen5_vm = read(ANDROID / "Screen5ItemManagementViewModel.kt")
screen8_repo = read(ANDROID / "Screen8ReportsRepository.kt")
screen8_vm = read(ANDROID / "Screen8ReportsViewModel.kt")
screen9_repo = read(ANDROID / "Screen9UsersRepository.kt")
screen9_vm = read(ANDROID / "Screen9UsersViewModel.kt")
screen10_repo = read(ANDROID / "Screen10SettingsRepository.kt")
screen10_vm = read(ANDROID / "Screen10SettingsViewModel.kt")
screen10_ui = read(ANDROID / "Screen10Settings.kt")
pdf = read(ANDROID / "Screen8PdfExporter.kt")
main_activity = read(ANDROID / "MainActivity.kt")

core = read(ROOT / "worker" / "src" / "index.ts")
lifecycle_worker = read(ROOT / "worker" / "src" / "phase14at-booking-lifecycle.js")
operational = read(ROOT / "worker" / "src" / "phase14be-operational-lifecycle.js")
customer_worker = read(ROOT / "worker" / "src" / "phase14av-customers-screen3.js")
screen5_worker = read(ROOT / "worker" / "src" / "phase14ba-screen5-item-management.js")
cloudinary_worker = read(ROOT / "worker" / "src" / "phase14e.js")
reports_worker = read(ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js")
migration = read(MIGRATIONS / "0021_phase14ck_hardening.sql")

# Group 7 — call efficiency / lifecycle triggers.
for token in [
    "dashboardRevision", "fun ensureDashboard()", "seenDashboardFreshnessRevision",
    "fun ensureAuditLoaded()", "!state.auditLoaded", "UPLOAD_SIGNATURE_CACHE_MS",
    "private var cachedUploadSignature", "var auditRevision", "fun markPasswordMutation()",
]:
    need(freshness + admin_vm + screen10_vm + screen5_repo, token, "Group 7 efficiency")
if "dashboardRefresh?.invoke()" in freshness or "registerDashboardRefresh" in admin_vm:
    errors.append("Dashboard mutations must not trigger off-screen refresh callbacks.")
for token in ["seenAuditRevision", "auditStale", "AdminDataFreshness.auditRevision"]:
    need(screen10_vm, token, "Lazy Audit freshness")
if "val list=await bookingListData(env,new URL(request.url));" in core:
    errors.append("Booking bootstrap must not execute the unused booking list query.")
if "repository.resetPassword(id, password)" not in screen9_vm or 'mutate("Password reset."' in screen9_vm:
    errors.append("Password reset must not use the generic User metadata reload mutation path.")

# Group 8 — idempotency / atomic mutations.
for token in [
    "stableMutationKeys", 'requestKey("booking", bookingSignature)', 'clearRequestKey("booking")',
    'requestKey("pickup", pickupSignature)', 'requestKey("return", returnSignature)',
]:
    need(lifecycle, token, "Stable client mutation keys")
for token in [
    "confirmation_state,advance_amount,notes,created_by_user_id,request_key",
    "confirmationState=text(parsed.body,'confirmationState')",
    "await env.DB.batch(statements)",
    'error:"STALE_WRITE"',
]:
    need(core, token, "Atomic booking mutation")
create_wrapper = lifecycle_worker[lifecycle_worker.find("async function createWithConfirmation"):lifecycle_worker.find("async function reservedPickupGuard")]
if "UPDATE bookings SET confirmation_state='RESERVED'" in create_wrapper:
    errors.append("Reserved booking creation must not use a second BOOKED -> RESERVED write.")
pwd_start = core.find("async function resetManagedUserPassword")
pwd_end = core.find("async function archiveManagedUser", pwd_start)
if "env.DB.batch([" not in core[pwd_start:pwd_end]:
    errors.append("Password + session revoke + audit must remain one D1 batch.")
for token in [
    '"CREATE","CATEGORY"', '"UPDATE","CATEGORY"', '"DELETE","CATEGORY"',
    '"CREATE","CATEGORY_FIELD"', '"UPDATE","CATEGORY_FIELD"', '"DELETE","CATEGORY_FIELD"',
]:
    need(core, token, "Category/field audit atomicity")

# Group 9 — retention/orphan/history.
for token in [
    "customer_name_snapshot", "customer_mobile_snapshot", "booking_no_snapshot", "item_name_snapshot",
    '"redacted":true,"customerDeleted":true', "{ customerId: id, redacted: true }",
    "COALESCE(c.name,w.customer_name_snapshot)", "COALESCE(b.booking_no,w.booking_no_snapshot)",
    "trg_items_cloudinary_asset_map_cleanup",
]:
    need(customer_worker + reports_worker + migration, token, "History/privacy preservation")
for token in ["validCategoryIds", "fieldCategoryById", "validStaffIds", 'staffUserId: ""', "customFilters: {}"]:
    need(reports_worker, token, "Preset stale-reference reconciliation")

# Group 10 — D1 efficiency / quantity correctness.
list_block = operational[operational.find("async function listBookings"):operational.find("function dashboardRowsFromJson")]
if list_block.count("WITH q AS (") != 1:
    errors.append("Booking list must aggregate booking_items only once per page.")
if "SELECT COUNT(*) AS count" in list_block:
    errors.append("Booking list must not run a second count query over the quantity rollup.")
need(list_block, "COUNT(*) OVER() AS __total", "Booking list window count")
for token in [
    "WITH page_users AS (", "booking_counts AS (", "pickup_counts AS (", "return_counts AS (",
    "ix_bookings_created_by_booking_date", "ix_pickup_events_handler_at", "ix_return_events_receiver_at",
]:
    need(core + migration, token, "Bounded User activity counts/indexes")
for token in [
    "bi.booked_qty-bi.closed_qty-bi.given_qty", "pe.pickup_at>=?", "re.return_at>=?",
    "COUNT(*) OVER() AS __users",
]:
    need(screen5_worker + reports_worker, token, "Inventory/report query hardening")
if "datetime(pe.pickup_at)>=?" in reports_worker or "datetime(re.return_at)>=?" in reports_worker:
    errors.append("Staff Activity must use index-friendly direct UTC timestamp comparisons.")
for token in [
    "function withWindowTotal(rowsSql)", "env.DB.prepare(withWindowTotal(rowsSql))",
]:
    need(reports_worker, token, "Paged report count/query consolidation")
for token in ["WITH field_counts AS (", "WITH value_counts AS (", "WITH stock AS ("]:
    need(screen5_worker, token, "Screen 5 bootstrap pre-aggregation")

# Group 11 — security/privacy.
for token in [
    'const ALLOWED_IMAGE_FORMATS = new Set(["jpg", "jpeg", "png", "webp"])',
    "publicIdFromCloudinaryUrl", "cloudinaryResource", "validateUploadedAssets",
    "bytes > MAX_IMAGE_BYTES", 'allowedFormats = "jpg,jpeg,png,webp"',
    "MAX_MUTATION_BODY_BYTES = 256 * 1024", "mutationBodyTooLarge(request)",
    'String(env.APP_ENV || "").toLowerCase() !== "production"',
]:
    need(cloudinary_worker + reports_worker + core, token, "Security hardening")
for token in ["fun purgeCache(context: Context)", "Screen8PdfExporter.purgeCache(appContext)"]:
    need(pdf + admin_vm, token, "PDF cache privacy cleanup")
if any(ext in screen5_repo for ext in ['".gif"', '".heic"', '".heif"', '".avif"']):
    errors.append("Item uploader must remain JPG/PNG/WebP only.")
for token in [
    '"/api/admin/cloudinary/discard"', "destroyUnmappedAssets", "discardItemUploads",
    "onDiscardUploads", "validatedAssets",
]:
    need(cloudinary_worker + screen5_repo + screen5_vm + read(ANDROID / "Screen5ItemManagement.kt"), token, "Pending upload cleanup")
for token in ["cloudinaryAssets", "INSERT INTO cloudinary_item_assets", "DELETE FROM cloudinary_item_assets"]:
    need(core, token, "Atomic Item asset mapping")
if "syncAssetMap(" in cloudinary_worker:
    errors.append("Cloudinary asset map must be written inside the Item D1 transaction, not a post-save wrapper phase.")

# Group 12 — optimistic concurrency / write-time invariants.
for token in [
    "expectedUpdatedAt", "STALE_WRITE", "trg_users_last_owner_guard_update",
    "trg_items_total_quantity_guard_update",
]:
    need(core + migration + customer_worker + screen5_worker + reports_worker, token, "Concurrent write protection")
for token in [
    "expectedUpdatedAt", "updatedAt", "replaceRelatedItems", "updateTemplate",
]:
    need(customer_repo + customer_models + screen5_repo + screen5_models + screen5_vm + screen8_repo + screen8_vm + screen9_repo + screen9_vm + screen10_repo + screen10_vm, token, "Android optimistic concurrency")
for token in ["expectedUpdatedAt", "nextUpdatedAt", "STALE_WRITE"]:
    need(screen5_worker, token, "Related Items CAS")
for token in ["expectedUpdatedAt", "updated_at=?", "This WhatsApp template changed on another device"]:
    need(core, token, "WhatsApp template CAS")

# Settings save no longer does mutation -> bootstrap -> public branding network chain.
for token in [
    "saveSettingsMutation(", "repository.saveSettings(settings, state.updatedAt)",
    "applySettingsBranding", "onBrandingChanged(settingsDraft.shopName, settingsDraft.logoUrl)",
]:
    need(screen10_vm + admin_vm + screen10_ui + root, token, "Settings local post-save state")
save_mutation_start = screen10_vm.find("private fun saveSettingsMutation")
save_mutation_end = screen10_vm.find("private fun mutate(", save_mutation_start)
if "loadBootstrap()" in screen10_vm[save_mutation_start:save_mutation_end]:
    errors.append("Normal Settings save must not reload the full Settings bootstrap.")

# Actual SQLite migration/invariant behavior.
try:
    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    apply_migrations(conn)

    # Migration structure.
    columns = {row[1] for row in conn.execute("PRAGMA table_info(whatsapp_activity_logs)")}
    for col in ["customer_name_snapshot", "customer_mobile_snapshot", "booking_no_snapshot", "item_name_snapshot"]:
        if col not in columns:
            errors.append(f"Phase 14CK WhatsApp snapshot column missing after migrations: {col}")

    index_names = {row[1] for row in conn.execute("PRAGMA index_list('bookings')")}
    if "ix_bookings_created_by_booking_date" not in index_names:
        errors.append("Bookings staff-history index missing after migrations.")

    # Last-owner invariant at write time.
    conn.execute("INSERT INTO users(id,name,email,role,is_active) VALUES('owner-a','A','a@example.com','OWNER',1)")
    conn.execute("INSERT INTO users(id,name,email,role,is_active) VALUES('owner-b','B','b@example.com','OWNER',1)")
    conn.execute("UPDATE users SET role='STAFF' WHERE id='owner-a'")
    try:
        conn.execute("UPDATE users SET role='STAFF' WHERE id='owner-b'")
        errors.append("D1 last-owner trigger allowed zero active Owners.")
    except sqlite3.IntegrityError as exc:
        if "LAST_OWNER" not in str(exc):
            errors.append(f"Unexpected last-owner trigger error: {exc}")
    conn.rollback()

    # Item quantity cannot be reduced below a committed active reservation.
    conn.execute("INSERT INTO users(id,name,email,role,is_active) VALUES('u1','Owner','u1@example.com','OWNER',1)")
    conn.execute("INSERT INTO customers(id,name,mobile,is_active) VALUES('c1','Customer','9999999999',1)")
    conn.execute("INSERT INTO items(id,item_code,item_name,category_id,total_quantity,is_active,public_visible) VALUES('i1','CK-1','CK Item','cat_choli',10,1,1)")
    conn.execute("INSERT INTO bookings(id,booking_no,customer_id,booking_date,pickup_date,return_date,status,created_by_user_id) VALUES('b1','CK-B1','c1','2026-09-19','2026-09-20','2026-09-22','BOOKED','u1')")
    conn.execute("INSERT INTO booking_items(id,booking_id,item_id,booked_qty,given_qty,returned_qty) VALUES('bi1','b1','i1',6,0,0)")
    try:
        conn.execute("UPDATE items SET total_quantity=5 WHERE id='i1'")
        errors.append("D1 Item quantity trigger allowed total below committed reservation.")
    except sqlite3.IntegrityError as exc:
        if "ITEM_QUANTITY_IN_USE" not in str(exc):
            errors.append(f"Unexpected Item quantity trigger error: {exc}")
    conn.close()
except Exception as exc:
    errors.append(f"Phase 14CK migration/invariant test failed: {exc}")

# Syntax checks for modified JavaScript wrappers.
for js in [
    ROOT / "worker" / "src" / "phase14at-booking-lifecycle.js",
    ROOT / "worker" / "src" / "phase14av-customers-screen3.js",
    ROOT / "worker" / "src" / "phase14ba-screen5-item-management.js",
    ROOT / "worker" / "src" / "phase14be-operational-lifecycle.js",
    ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js",
    ROOT / "worker" / "src" / "phase14e.js",
]:
    syntax = subprocess.run(["node", "--check", str(js)], capture_output=True, text=True)
    if syntax.returncode != 0:
        errors.append(f"JavaScript syntax failed for {js.name}: {(syntax.stderr or syntax.stdout).strip()}")

if errors:
    print("Phase 14CK Groups 7-12 hardening regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14CK Groups 7-12 hardening regression PASS")
