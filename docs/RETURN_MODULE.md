# Return Module

Phase 8 adds quantity-safe return processing.

## Flow
Return state is quantity-derived. `Full Returned` requires Pickup Pending = 0 and Return Pending = 0; returning all currently picked quantity is only partial/order-open when unpicked quantity remains.

`Overdue` is not stored as a booking status. It is calculated when the expected return date is before today and pending returned quantity is greater than zero.

## Safety
- Return quantity cannot exceed pending given quantity.
- Request keys make retries idempotent.
- Existing booking-item triggers prevent returned quantity exceeding given quantity.
- Return corrections create separate correction events and audit records rather than overwriting history.
- A return event permanently locks historical pickup correction, but normal remaining pickup may continue when Pickup Pending > 0 and the operator chose Keep Order Open.

## Current permission rule
- STAFF with Returns permission may perform normal operational return actions allowed by the backend contract.
- Historical return correction is OWNER-only and must preserve an explicit audit/correction event.
- If Pickup Pending > 0 at Return confirmation, the operator must choose Keep Order Open or Close Remaining Items; Close Remaining releases unpicked quantity and records an item-level audit event.
