# Staging Smoke-Test Checklist

Use this checklist against the current active contract, not historical release behavior.

## Deployment/data
- Staging Admin/Public surfaces load over HTTPS.
- Staging uses staging-only configuration/D1/secrets.
- Apply all migrations in `database/migrations/` in order through the latest migration required by current `main`.
- Current latest migration: `0016_report_presets.sql`.
- Production is untouched.

## Authentication/roles
- OWNER login/session works.
- STAFF login/session works.
- Active roles are exactly OWNER and STAFF.
- Legacy ADMIN is not selectable/exposed as a current role.
- STAFF only sees permitted modules.
- Worker rejects unauthorized direct/API access.
- Users and Settings remain OWNER-only.

## Dashboard
- Operational sections are exactly: Missed Pickups, Today Pickups, Today Returns, Overdue Returns.
- Today Bookings is absent from Dashboard.
- Shared Main Header refresh works; no duplicate page-level Dashboard Refresh appears.
- Dashboard cards use shared View/Edit/Call/WhatsApp behavior.
- No separate Pending Pickup badge appears on Dashboard cards.
- Android compact item cards expand from first 2 rows via +N more and can Collapse/Show less without an API call.

## Customers
- Single Mobile Number workflow works.
- Alternative Mobile is not shown/used.
- Call uses confirmation-first single-number flow.
- WhatsApp uses central template resolution and shared preview.

## Bookings
- Filters: All, Reserved, Booked, Picked Up, Returned, Overdue, Cancelled.
- New Booking item picker is search-first with Category bottom sheet.
- Visible direct-pickup label is Directly Pickup.
- Compact list cards support Expand and Collapse.
- Details/review show all items.
- Reserve/confirm/direct pickup are idempotency-safe.

## Pickup/Return
- Partial and full pickup/return preserve authoritative quantities/status.
- Normal STAFF operations require the corresponding permission.
- Historical Pickup/Return correction is OWNER-only.
- Audit history uses actual event timestamps.

## Item management
- Category-driven custom fields work.
- Signed Cloudinary item-image flow works.
- Category/Item writes are OWNER-only.
- STAFF with Items / Stock permission is read/use only.

## Screen 9 Users
- OWNER/STAFF user CRUD rules work.
- Mobile is mandatory/unique as defined by current contract.
- Staff Access permissions are enforced.
- Android Screens 6–8 remain skipped/hidden even when corresponding permissions exist.

## Screen 10 Settings
- Basic, Public Website, WhatsApp Templates and Audit Log tabs work for OWNER.
- Availability mode supports Status Only / Exact Quantity.
- New/unset default is Exact Quantity without overriding an existing explicit saved mode.
- Central linked WhatsApp templates support Gujarati / English / All.
- Preview text equals text passed to WhatsApp.
- Public Website inquiry resolves through General Inquiry.
- Audit Log remains read-only/no polling.

## Public catalog
- Public catalog returns only approved public fields.
- Exact Quantity/Status Only behavior follows Settings.
- Saved contact number → environment fallback → hide/disable when unavailable.
- Public WhatsApp inquiry uses the central General Inquiry template.

## Reliability
- No duplicate action occurs on double tap/retry.
- No polling is introduced.
- Cumulative hardening/static regression passes before declaring staging valid.

## Screen 8 Reports
- OWNER and REPORTS-permission STAFF can open Reports.
- Flexible Report Generator supports dynamic filters/grouping/sort.
- Initial result batch is 10 and bottom scroll appends +10 without polling.
- Saved presets obey My/Shared ownership rules.
- View/Download/Share PDF use one explicit complete export request for unchanged filters.
- PDF is generated locally on Android as Summary + Detailed Table with automatic orientation.
