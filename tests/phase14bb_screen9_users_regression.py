from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / 'apps' / 'admin-android' / 'app' / 'src' / 'main' / 'java' / 'com' / 'nimsdeveloper' / 'zhagmagdresses' / 'admin'
WORKER = ROOT / 'worker' / 'src' / 'phase14bb-screen9-users.js'
AUTH = ROOT / 'worker' / 'src' / 'auth.ts'
CORE = ROOT / 'worker' / 'src' / 'index.ts'
MIGRATION = ROOT / 'database' / 'migrations' / '0014_staff_permissions.sql'
DOC = ROOT / 'docs' / 'ADMIN_SCREEN_09_USERS.md'
WRANGLER = ROOT / 'worker' / 'wrangler.toml'
STAGING = ROOT / 'worker' / 'wrangler.staging.toml.template'

errors = []

def text(path: Path) -> str:
    if not path.exists():
        errors.append(f'Missing: {path.relative_to(ROOT)}')
        return ''
    return path.read_text(encoding='utf-8')

models = text(ANDROID / 'data' / 'Models.kt')
ui = text(ANDROID / 'Screen9Users.kt')
repo = text(ANDROID / 'Screen9UsersRepository.kt')
vm = text(ANDROID / 'Screen9UsersViewModel.kt')
root = text(ANDROID / 'AdminAppScreen4.kt')
auth = text(AUTH)
core = text(CORE)
worker = text(WORKER)
migration = text(MIGRATION)
doc = text(DOC)
wrangler = text(WRANGLER)
staging = text(STAGING)

checks = [
    (models, 'enum class UserRole { OWNER, STAFF }', 'two Android roles'),
    (models, 'object StaffAccess', 'Staff access constants'),
    (models, 'staffPermissions: Set<String>', 'session Staff permissions'),
    (models, 'fun SessionUser.hasAccess(permission: String)', 'permission helper'),
    (ui, 'MainScreenDateRow(businessDate)', 'Users Date/Day/Time row'),
    (ui, 'title = "Users"', 'Users Back header title'),
    (ui, 'label = "Add User"', 'Add User action'),
    (ui, 'placeholder = "Search name or mobile"', 'mobile-only search'),
    (ui, 'label = "Mobile Number *"', 'required mobile field'),
    (ui, 'label = "Password *"', 'required password field'),
    (ui, '"Name is required."', 'name required validation'),
    (ui, '"Mobile number is required."', 'mobile required validation'),
    (ui, '"Password is required."', 'password required validation'),
    (ui, '"Staff Access"', 'Staff Access section'),
    (ui, '"Select All"', 'Select All permissions'),
    (ui, '"Clear All"', 'Clear All permissions'),
    (ui, 'StaffAccess.DASHBOARD', 'Dashboard permission'),
    (ui, 'StaffAccess.ITEMS', 'Items permission'),
    (ui, 'StaffAccess.CUSTOMERS', 'Customers permission'),
    (ui, 'StaffAccess.BOOKINGS', 'Bookings permission'),
    (ui, 'StaffAccess.PICKUPS', 'Pickup permission'),
    (ui, 'StaffAccess.RETURNS', 'Returns permission'),
    (ui, 'StaffAccess.REPORTS', 'Reports permission'),
    (repo, '"pageSize" to pageSize.coerceIn(1, 10).toString()', '10-row paging'),
    (repo, '.put("staffPermissions"', 'Staff permissions mutation payload'),
    (vm, 'val canLoadMore', 'incremental paging flag'),
    (vm, 'delay(350)', 'debounced user search'),
    (vm, 'pendingReset', 'latest search/filter refresh preservation'),
    (vm, 'distinctBy { it.id }', 'deduplicated append'),
    (root, 'MoreDestination4.USERS', 'Users More destination'),
    (root, 'title = "Category & Items"', 'More Category card'),
    (root, 'title = "Reports"', 'More Reports card'),
    (root, 'title = "Bills"', 'More Bills card'),
    (root, 'title = "WhatsApp Centre"', 'More WhatsApp card'),
    (root, 'title = "Users"', 'More Users card'),
    (root, 'title = "Settings"', 'More Settings card'),
    (root, 'modifier = Modifier.weight(1f)', 'two-column More card layout'),
    (root, 'user.hasAccess(StaffAccess.ITEMS)', 'Items card permission'),
    (root, 'user.role == UserRole.OWNER', 'Users card Owner-only'),
    (root, 'canDashboard = user.hasAccess(StaffAccess.DASHBOARD)', 'Dashboard nav permission'),
    (root, 'canCustomers = user.hasAccess(StaffAccess.CUSTOMERS)', 'Customers nav permission'),
    (root, 'canBookings = user.hasAccess(StaffAccess.BOOKINGS)', 'Bookings nav permission'),
    (auth, 'export type UserRole = "OWNER" | "STAFF";', 'two Worker roles'),
    (auth, 'STAFF_PERMISSION_KEYS', 'Worker permission keys'),
    (auth, 'staff_permissions_json', 'session permission read'),
    (auth, 'sessionUserCache', 'request-scoped session lookup cache'),
    (core, 'const ADMIN_ROLES: readonly UserRole[] = ["OWNER"]', 'Owner-only management APIs'),
    (core, 'permissionOptions:STAFF_PERMISSION_KEYS', 'permission options response'),
    (core, 'Mobile number is already used by another user.', 'mobile duplicate mapping'),
    (worker, 'import core from "./phase14ba-screen5-item-management.js"', 'cumulative Screen 9 wrapper'),
    (worker, 'pathname.startsWith("/api/admin/users")', 'Owner-only Users guard'),
    (worker, 'pathname.startsWith("/api/admin/settings")', 'Owner-only Settings guard'),
    (worker, 'hasStaffPermission(user, permission)', 'Staff module guard'),
    (migration, 'staff_permissions_json', 'Staff permissions schema'),
    (migration, "WHERE role='ADMIN'", 'legacy Admin migration'),
    (migration, "SET role='STAFF'", 'legacy Admin becomes Staff'),
    (doc, 'Roles are exactly **OWNER** and **STAFF**.', 'documented two-role rule'),
    (doc, 'Email is removed from User Management UI', 'documented email removal'),
    (wrangler, 'main = "src/phase14bd-screen8-reports.js"', 'local cumulative Screen 8 Worker entry'),
    (staging, 'main = "src/phase14bd-screen8-reports.js"', 'staging cumulative Screen 8 Worker entry'),
]

for body, token, label in checks:
    if token not in body:
        errors.append(f'{label} missing: {token}')

for forbidden, label in [
    ('UserRole.ADMIN', 'Android ADMIN role'),
    ('All Roles</option><option>OWNER</option><option>ADMIN</option>', 'legacy web Admin selector marker in Screen 9'),
    ('Previous', 'Previous paging control'),
    ('Page X', 'Page X paging control'),
]:
    if forbidden in ui:
        errors.append(f'Screen 9 must not contain {label}.')

if '"ADMIN"' in models:
    errors.append('Android session model must not retain the ADMIN role.')

if errors:
    print('Screen 9 Users regression FAILED')
    for error in errors:
        print(' -', error)
    raise SystemExit(1)

print('Screen 9 Users regression PASS')
