# Admin Android — Screen 9: Users

Status: **Implemented / staging validated / finalized / locked**

## Scope
- Screen 6 Pickup and Screen 7 Returns are intentionally skipped in the current Android rebuild. Screen 8 Reports is implemented and permission-aware.
- Screen 9 is **Users**.
- Screen 9 is visible to **OWNER only**.
- Roles are exactly **OWNER** and **STAFF**.
- Email is removed from User Management UI and search. Mobile Number is the required user identity field.

## More navigation
- Bottom navigation remains Dashboard | Customers | Bookings | More, filtered by Staff module access.
- More opens a compact **two-column pastel card-grid** hub rather than opening Category & Items directly.
- While any More sub-page is open, tapping bottom **More** again reopens the More hub.
- Cards use a relevant Material Rounded icon, title, concise subtitle and chevron in the same visual family as the approved More reference.
- Category & Items opens existing Screen 5.
- Users opens Screen 9 for Owner.
- Settings is implemented as Screen 10 in the next finalized batch and remains Owner-only.
- Skipped Screens 6–8 are not shown in More.

## User list
- Immediately below the Main Header, show the reusable **Date | Day | Time** row before the Users title/action row.
- The page header uses the shared compact Back header with **Users** on the left and the shared compact **Add User** action on the right.
- Do not show the old Access Control eyebrow or Owner/Staff subtitle. Visible Back and Android system/gesture Back return to the More hub.
- While a User mutation/password reset is busy, Back/dismiss is blocked so the sheet cannot disappear while the request is in flight.
- Server-side search: name or mobile.
- Search input is debounced before the API request so typing does not create one request per character.
- Role filters: All Roles / Owner / Staff.
- Status filters: Active / Inactive / Archived / All.
- Initial load is 10 users; load-more appends up to 10 more.
- Cards show name, mobile, role, status, activity counts and last login.
- Actions: Edit, Reset Password, Archive / Restore.
- Current Owner cannot archive their own account.

## Add / Edit User
- Name.
- Mobile Number, exactly 10 digits.
- Role: Owner or Staff.
- Password on Add only; minimum 8 characters.
- Active Account on Edit.
- Owner self role/deactivation controls are locked.
- Staff role shows Staff Access checkboxes.
- Owner role always has full access and does not use checkbox permissions.

## User form validation
- Add/Edit User shows required markers for Name and Mobile Number; Add also marks Password required.
- Name must be nonblank, Mobile Number exactly 10 digits, and Add/Reset Password at least 8 characters.
- After field interaction, blank required values show a specific required message and partial invalid values show the existing format/length rule.
- Reset Password uses the same required/minimum-length feedback while repository/Worker validation remains authoritative.
## Staff Access
Checkbox modules:
- Dashboard
- Items / Stock
- Customers
- Bookings
- Pickup
- Returns
- Reports

New Staff defaults to all operational module permissions enabled. Select All and Clear All are available.

## Backend rules
- D1 migration 0014 adds `staff_permissions_json`.
- Legacy ADMIN rows are converted to STAFF with all operational permissions.
- User Management APIs are Owner-only.
- Settings / WhatsApp Templates / Audit Log APIs are Owner-only.
- A cumulative Worker permission guard enforces Staff module access before delegating to existing module routes.
- Worker session lookup is cached per Request so wrapper permission checks do not duplicate the session D1 read.
- Production remains pinned to its existing Worker entry; only staging/local cumulative entries move to Screen 9 wrapper.

## Safety
- No hard delete; archive preserves history.
- Last active Owner protection remains enforced.
- Self role change, self deactivation and self archive remain blocked.
- Role change/deactivation/archive/password reset revoke sessions when applicable.
- No polling.
- Android version remains 0.14.5 / versionCode 1.


## Verified validation
- Admin Android staging APK: Build Admin Android **Run #43** — SUCCESS.
- Android cumulative foundation audit: PASS.
- Artifact: `zhagmag-admin-android-43`.
- Staging deployment: Deploy Staging **Run #151** — SUCCESS.
- Full hardening regression: PASS, including Screen 9 Users regression.
- D1 migration `0014_staff_permissions.sql`: applied successfully on staging.
- Active staging Worker version ID: `d41a5792-aafc-4932-a93a-7bd5da56b0ed`.
- Staging smoke test: PASS.
- Production remains unchanged.

## Locked role/navigation rule
- Current active roles are exactly OWNER and STAFF.
- Legacy ADMIN is migration compatibility only and is never selectable/exposed as a current role.
- STAFF sees only modules granted by Staff Access permissions; Worker-side permission enforcement remains authoritative.
- Pickup and Returns permissions remain valid capabilities while Android Screens 6–7 are intentionally skipped/hidden. Reports permission controls implemented Screen 8 visibility/access.

## Confirmation consistency
- User Archive uses the shared destructive confirmation dialog. During the archive mutation, outside/back dismissal and Cancel are locked consistently with the confirm action.
- Restore remains a direct reversible action; no redundant extra confirmation is required.
- Reset Password remains its dedicated password-entry form; do not add a second confirmation on top of that explicit form action.
## Loading / empty / retry behavior
- Initial Users failure shows Error + Retry and does not show `No users found.` or a misleading `0 users` count.
- User totals and the empty state are rendered only after a successful load.
- Refresh/load-more failure preserves already loaded rows. Append failure stops the automatic loop and shows **Retry loading more**, which retries only the failed next page.


## Unsaved editor protection
- Add/Edit User tracks Name, Mobile, Role, Active state, Password and Staff Access permissions against the opening values.
- Back, sheet gesture, Close or Cancel closes directly when unchanged; changed forms require **Discard unsaved changes?**.
- Reset Password requires discard confirmation only after a password has been typed.
