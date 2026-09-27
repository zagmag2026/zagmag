from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"

def read(path):
    return Path(path).read_text(encoding="utf-8")

errors=[]

def need(body, token, label):
    if token not in body:
        errors.append(f"{label}: missing {token}")

auth=read(ROOT/"worker/src/auth.ts")
core=read(ROOT/"worker/src/index.ts")
media=read(ROOT/"worker/src/phase14e.js")
wa=read(ROOT/"worker/src/phase14bg-settings-whatsapp.js")
admin=read(ROOT/"apps/admin-web/src/main.tsx")
admin_css=read(ROOT/"apps/admin-web/src/styles.css")
public=read(ROOT/"apps/public-web/src/main.tsx")
public_h=read(ROOT/"apps/public-web/src/phase14h.css")
public_i=read(ROOT/"apps/public-web/src/phase14i.css")
migration=read(ROOT/"database/migrations/0022_cloudinary_cleanup_queue.sql")
admin_vm=read(ANDROID/"AdminViewModel.kt")
entry=read(ANDROID/"AdminEntryScreen.kt")
booking=read(ANDROID/"BookingLifecycleViewModel.kt")
booking_list=read(ANDROID/"BookingListViewModel4.kt")
customers=read(ANDROID/"CustomerScreen3ViewModel.kt")
customer_ui=read(ANDROID/"CustomerSharedUi.kt")
dashboard=read(ANDROID/"DashboardScreenV2.kt")
items=read(ANDROID/"Screen5ItemManagement.kt")
item_vm=read(ANDROID/"Screen5ItemManagementViewModel.kt")
reports=read(ANDROID/"Screen8ReportsViewModel.kt")
users=read(ANDROID/"Screen9UsersViewModel.kt")
users_ui=read(ANDROID/"Screen9Users.kt")
settings=read(ANDROID/"Screen10SettingsViewModel.kt")
settings_ui=read(ANDROID/"Screen10Settings.kt")
settings_models=read(ANDROID/"Screen10SettingsModels.kt")
gallery=read(ANDROID/"ui/components/ItemImageGallery.kt")
components=read(ANDROID/"ui/components/Components.kt")

# Group 13 — restoration
for body,token,label in [
    (booking,"SavedStateHandle","Booking draft state"),
    (booking,'"bookingDraftLines"',"Booking lines restoration"),
    (booking,'"bookingCustomerId"',"Booking customer restoration"),
    (booking,'"bookingPickupDate"',"Booking dates restoration"),
    (booking_list,'"bookingListPage"',"Booking list page restoration"),
    (customers,'"customerPage"',"Customer page restoration"),
    (reports,'"reportsConfig"',"Reports filter restoration"),
    (users,'"usersPage"',"Users page restoration"),
    (settings,'"auditPage"',"Audit page restoration"),
    (settings_ui,"Screen10SettingsSaver","Settings draft saver"),
    (customer_ui,"rememberSaveable(initialName)","Customer form saver"),
    (items,"Screen5PhotoListSaver","Item media draft saver"),
    (users_ui,"Screen9PermissionSetSaver","User permission saver"),
    (dashboard,"expandedKeysState","Dashboard expansion saver"),
]:
    need(body,token,label)

# Group 14 — network/offline/races
for body,token,label in [
    (admin_vm,"SessionCheckFailed","Android session retry"),
    (entry,"Connection unavailable","Android connection state"),
    (admin,"class ApiError","Admin Web network classification"),
    (admin,"Connection unavailable","Admin Web session Retry"),
    (admin,"bookingListSerial","Admin Web booking stale-response guard"),
    (admin,"pickupLoadSerial","Admin Web pickup stale-response guard"),
    (admin,"returnLoadSerial","Admin Web return stale-response guard"),
    (public,"async function publicJson","Public timeout wrapper"),
    (public,"catalogSerial","Public catalog stale-response guard"),
    (public,"availabilitySerial","Public availability stale-response guard"),
    (public,">Retry</button>","Public Retry action"),
    (admin,"Pickup saved. Latest details could not be refreshed","Pickup post-save truthfulness"),
    (admin,"Return saved. Latest details could not be refreshed","Return post-save truthfulness"),
]:
    need(body,token,label)

# Group 15 — media/upload lifecycle
for body,token,label in [
    (settings_models,"logoPublicId","Managed logo identity"),
    (settings_ui,"discardPendingLogo","Android pending-logo cleanup"),
    (media,"CLOUDINARY_BRANDING_FOLDER","Branding upload folder"),
    (media,"drainCleanupQueue","Durable cleanup retry"),
    (migration,"cloudinary_cleanup_queue","Cleanup queue migration"),
    (admin,'uploadManagedImage(file:File,purpose:"item"|"logo")',"Admin Web managed uploads"),
    (admin,"relatedItemIds","Admin Web Related Items"),
    (gallery,"error = painterResource","Android full-image fallback"),
]:
    need(body,token,label)
if "Photo URLs" in admin:
    errors.append("Admin Web must not expose raw Item Photo URLs.")
if 'u.protocol === "http:"' in core:
    errors.append("New Item media must not allow http:// URLs.")

# Group 16 — accessibility
for body,token,label in [
    (admin,"useAdminWebA11y","Admin Web dialog accessibility"),
    (admin,'event.key==="Escape"',"Admin Web Escape handling"),
    (admin,'aria-live',"Admin Web async announcements"),
    (public,'event.key==="Escape"',"Public modal Escape handling"),
    (public_h,"focus-visible","Public visible keyboard focus"),
    (public_i,"min-width:44px","Public carousel target size"),
    (components,"minHeight = 48.dp","Android accessible target size"),
]:
    need(body,token,label)

# Group 17 — cross-surface parity
need(admin,'type Role = "OWNER" | "STAFF";',"Web active-role parity")
if '"ADMIN"' in admin or "'ADMIN'" in admin:
    errors.append("Admin Web must not expose legacy ADMIN role.")
for body,token,label in [
    (admin,"staffPermissions","Web Staff permissions"),
    (admin,"messageGu","Web bilingual WhatsApp template"),
    (admin,"linkedAction","Web linked WhatsApp action"),
    (admin,"workingHours","Web Working Hours"),
    (admin,"whatsappTemplateLanguage","Web WhatsApp language"),
    (admin,"/api/admin/report-generator","Web Global Reports"),
    (admin,"/api/admin/related-items/","Web Related Items API"),
    (customers,"alternateMobile.orEmpty()","Android legacy alternate-mobile preservation"),
]:
    need(body,token,label)

# Group 18 — auth/session/permission boundaries
for body,token,label in [
    (wa,'hasStaffPermission(user, "BOOKINGS")',"WhatsApp booking permission"),
    (wa,'hasStaffPermission(user, "ITEMS")',"WhatsApp item permission"),
    (wa,'hasStaffPermission(user, "CUSTOMERS")',"WhatsApp customer permission"),
    (core,"LOGIN_ACCOUNT_MAX_FAILURES","Account-wide login throttle"),
    (auth,"const PASSWORD_ITERATIONS = 210_000;","Current PBKDF2 work factor"),
    (auth,"passwordNeedsRehash","Legacy password rehash"),
    (admin,"setLogoutError","Logout failure remains authenticated"),
]:
    need(body,token,label)

if "passwordNeedsRehash(row.password_hash)" in core:
    errors.append("Login must not synchronously rehash a verified legacy password.")

if errors:
    print("Groups 13-18 hardening regression FAILED")
    for error in errors:
        print(" -",error)
    raise SystemExit(1)

print("Groups 13-18 hardening regression PASS")
