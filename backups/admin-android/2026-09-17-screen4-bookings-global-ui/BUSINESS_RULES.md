# Zhagmag Dresses — Business Rules

## Scope
- Business/brand name: **ઝગમગ ડ્રેસીસ / Zhagmag Dresses**.
- The system manages traditional-dress rental inventory, customers, bookings, pickups, returns, reports and administration.
- Public catalog remains available without public login when enabled by settings.

## Pricing exclusion
Pricing/payment calculations are not part of the current scope. Do not add or display rent amount, deposit, discount, late fee, damage charge, payment total or similar pricing fields unless explicitly requested later.

## Categories and items
- Item Master is category-driven.
- Common base fields stay fixed; category-specific attributes are defined dynamically by Admin.
- Supported custom-field types follow the backend contract (text, number, dropdown, multi-select, yes/no, date).
- Public item details render only public-visible fields.
- Availability is derived from authoritative inventory + booking/pickup/return state, not a manually editable availability number.

## Customers
- Primary mobile identifies/duplicates customers according to backend validation.
- Customer history must be preserved.
- Operational customer views provide quick Call and WhatsApp actions.

## Booking
- Category-first item selection is the current canonical create-booking UX.
- A booking may contain multiple items and quantities.
- Backend availability validation remains authoritative.
- Duplicate/double-submit protection must be preserved.

## Pickup and return
- Partial pickup and partial return remain supported.
- Pickup/return events and corrections must preserve the backend audit trail.
- Overdue status is derived from return due date and outstanding quantity.

## Dashboard
- Today Bookings, Today Pickups, Today Returns and Overdue Returns show customer context.
- Each operational row/card provides Call and WhatsApp quick actions.
- Dashboard reads should use bundled backend data; do not introduce per-card API calls.

## Roles
- OWNER: full access.
- ADMIN: full access subject to backend rules.
- STAFF: dashboard, item view, customers, bookings, pickup, returns and reports as permitted by backend.
- UI hiding is not authorization. Worker permission checks are authoritative.

## Source of truth
Cloudflare D1 and the Worker API remain authoritative for business validation, permissions, inventory state, booking state and audit logging.
