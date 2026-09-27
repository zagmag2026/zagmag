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
- Alternative Mobile is retired from the current product UI/workflow. Call and WhatsApp use the single mandatory Mobile Number with no number chooser; legacy alternative-mobile values are not exposed or used.

## Booking
- The canonical Admin Android item picker is search-first. A compact Category control opens a bottom sheet with `All Categories` plus dynamic categories and `Cancel | Apply`; permanent horizontal category chips are not used. Selected items persist across search/filter changes.
- A booking may contain multiple items and quantities from multiple categories.
- Search/category changes do not remove already selected booking items.
- The same item cannot appear as duplicate booking lines; quantity is updated on the selected line.
- Backend availability validation remains authoritative.
- Duplicate/double-submit protection must be preserved.
- Reserved bookings reserve inventory like confirmed bookings.
- Date overlap remains inclusive of Pickup and Return dates.

## Pickup and return
- Partial pickup and partial return remain supported.
- Pickup Pending = active booked quantity − picked quantity; Return Pending = picked quantity − returned quantity.
- A customer may return picked quantity while unpicked quantity remains. The order stays pickup-open unless the operator explicitly closes the remaining unpicked items.
- Return with Pickup Pending > 0 requires an explicit Keep Order Open / Close Remaining Items decision.
- Closing remaining items releases only unpicked quantity while preserving original booked quantity and an item-level audit event.
- Full Returned requires both Pickup Pending = 0 and Return Pending = 0.
- Pickup/return/closure events and corrections must preserve item-level backend audit history.
- Overdue is derived from return due date and Return Pending > 0.

## Dashboard
- Dashboard operational sections are fixed as `Today Pickups → Today Returns → Missed Pickups → Overdue Returns`; Today Bookings is intentionally not a Dashboard section. These are pending-work queues, and Reserved bookings do not enter Today/Missed Pickup.
- Each operational row/card provides Call and WhatsApp quick actions.
- Dashboard reads should use bundled backend data; do not introduce per-card API calls.

## WhatsApp templates
- Admin operational WhatsApp messages use the central linked-template system rather than screen-local hard-coded message text.
- WhatsApp Template Language is one global Settings value: Gujarati, English or Both. Every template stores both Gujarati and English content.
- All renders Gujarati followed by one blank line and English.
- Related buttons resolve the linked action, fill only its approved placeholders from fixed sources, render the global language choice, show the shared preview, then open WhatsApp only after explicit confirmation. Unknown or unresolved placeholders are blocked.
- Completion contexts include Pickup Done, Part Pickup Done, Return Done, Part Return Done and Thank You.
- Only one active template may be linked to a given action at a time.

## Roles
- OWNER: full access, including Users and Settings.
- STAFF: operational module access is assigned per user through Staff Access checkboxes.
- Staff permission modules are Dashboard, Items, Customers, Bookings, Pickup, Returns and Reports.
- Users and Settings are always Owner-only.
- The legacy ADMIN role is retired; it must not be offered by current UI or APIs.
- UI hiding is not authorization. Worker permission checks are authoritative.

## Source of truth
Cloudflare D1 and the Worker API remain authoritative for business validation, permissions, inventory state, booking state and audit logging.

## Public availability
- Public Website supports Status Only or Exact Quantity availability display. Status Only never exposes the exact quantity and uses centralized states: 0 Not Available, 1–2 Few Left, 3–5 Limited, 6+ Available. Exact Quantity exposes the actual available quantity.
- New/unset configuration defaults to Exact Quantity; preserve any existing explicitly saved mode.

## Master-data/correction permissions
- Category/Item master writes are OWNER-only; permitted STAFF access is read/use only.
- Pickup/Return historical correction actions are OWNER-only; permitted STAFF may perform normal operational pickup/return.
