# User Management

Status: **Implemented / staging validated / finalized / locked**

## Admin Android Screen 9

Screen 9 is **Users** and is available to **OWNER only**.

The product uses exactly two user roles:
- **OWNER** — full access. Owner access is not reduced by Staff permission checkboxes.
- **STAFF** — operational access is controlled per user through Staff Access checkboxes.

The legacy `ADMIN` role is retired. Existing staging `ADMIN` rows are migrated to `STAFF` with all operational module permissions enabled so the migration does not grant Owner privilege.

## Identity
User Management is mobile-number based:
- Name
- Mobile Number
- Role
- Password on Add
- Active status on Edit
- Staff Access checkboxes when Role = STAFF

Email is not shown, searched, edited or required by User Management. Existing legacy email values may remain stored for compatibility but are not part of the User Management UI.

Mobile Number is mandatory, normalized to the existing India-compatible 10-digit form, and unique across users.

## Staff Access checkboxes
Available Staff module permissions:
- Dashboard
- Items / Stock
- Customers
- Bookings
- Pickup
- Returns
- Reports

New Staff users default to all operational module permissions enabled. Owner can use **Select All** or **Clear All** and can toggle each module individually.

Users and Settings are always Owner-only and are never Staff permissions.

Current Android navigation hides Dashboard, Customers and Bookings when the Staff user does not have that module permission. More uses the approved compact two-column card-grid style and shows Category & Items only when Items access is enabled. Screen 6 Pickup and Screen 7 Returns remain skipped for the current Android rebuild. Screen 8 Reports is implemented and is surfaced only when Reports access is enabled.

## Actions
- Add user
- Edit user
- Activate / deactivate
- Reset password
- Archive / restore
- Search by name or mobile, with a short client debounce before the server request
- Filter by role: Owner / Staff
- Filter by status: Active / Inactive / Archived / All
- Incremental list loading in batches of 10

## Safety
- No hard delete: user records are archived so bookings, pickups, returns and audit history remain traceable.
- At least one active, non-archived Owner must remain.
- Self role change, self deactivation and self archive are blocked.
- Password reset, role change, deactivation and archive revoke active sessions when applicable.
- Passwords use the existing PBKDF2 hashing implementation and are never returned by APIs.
- Worker/D1 permission checks are authoritative; hiding Android navigation is not authorization.
- Owner-only APIs include Users, Settings, WhatsApp Templates and Audit Log.
- Staff module access is enforced by the cumulative staging Worker before module routes are delegated.
- Session lookup is request-cached inside the Worker wrapper chain so permission guarding does not duplicate D1 session lookups.

## API efficiency
- Users list is server filtered and paged at 10 rows.
- Android appends the next 10 only when the current list reaches the load-more boundary.
- Search/filter changes reset to the first 10.
- No polling is used.
- Mutations refresh only the Users list.

## Locked navigation capability rule
- Pickup/Returns permissions remain valid capabilities even though Android Screens 6–7 are currently skipped/hidden.
- Reports permission now controls visibility/access to implemented Android Screen 8 Reports.
- Permission presence does not make skipped Android Screens 6–7 visible.
- Users and Settings remain OWNER-only.
