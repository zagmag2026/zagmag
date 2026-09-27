# Do Not Change — Locked/Protected Behavior

This file summarizes current frozen behavior. It does not replace the specific Screen docs.

## Locked Admin Android screens
Do not change without an explicit user unlock/specific change request:
- Screen 1 Login
- Screen 2 Dashboard
- Screen 3 Customers
- Screen 4 Bookings
- Screen 5 Category & Items
- Screen 9 Users
- Screen 10 Settings

Screens 6 Pickup and 7 Returns are intentionally skipped/hidden. Screen 8 Reports is implemented/staging validated but remains unlocked pending explicit user lock.

## Protected product rules
- Brand remains **ઝગમગ ડ્રેસીસ / Zhagmag Dresses**.
- Version remains **0.14.5 / versionCode 1** until explicitly changed.
- Pricing/payment calculations remain out of scope.
- Customer UI uses one mandatory Mobile Number; Alternative Mobile is not reintroduced.
- Current active roles are exactly OWNER and STAFF.
- Legacy ADMIN is migration compatibility only.
- Users and Settings are OWNER-only.
- Settings business logo uses the approved upload/preview/change/remove flow and saved branding reuse; do not revert to URL-only UI.
- Category/Item master writes are OWNER-only.
- Pickup/Return historical corrections are OWNER-only.
- D1/Worker remains authoritative.
- Production remains untouched without explicit authorization.

## Protected UI/business contracts
- Dashboard operational sections: Today Pickups → Today Returns → Missed Pickups → Overdue Returns; pending-work semantics and Reserved exclusion are locked unless explicitly changed.
- No separate Dashboard page-level Refresh.
- Shared Dashboard/Booking actions: View | Edit | Call | WhatsApp.
- Admin Android compact booking cards: first 2 items → +N more → Expand → Collapse/Show less.
- Booking Details/review always show all items.
- New Booking item picker is search-first with Category bottom sheet; no permanent horizontal category chips.
- Visible direct-pickup label: **Directly Pickup**.
- Operational WhatsApp uses central linked templates and shared preview. Language is one global Gujarati/English/Both setting; every template stores both languages and unresolved placeholders must never reach customers.
- Public inquiry uses General Inquiry template.
- Public availability new/unset default: Exact Quantity; preserve existing explicit saved mode. Status Only hides exact quantity and uses 0 Not Available, 1–2 Few Left, 3–5 Limited, 6+ Available.

## Historical files
Do not rewrite `_backups/`, `backups/` or `docs/RELEASE_*.md` merely to match current rules.
