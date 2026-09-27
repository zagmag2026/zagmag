# Pickup Module — Phase 7

- Full and partial pickup
- Given To is always the booking customer
- Given By is the authenticated OWNER/STAFF user
- Server timestamp records pickup time
- Quantity-derived pickup state: Booked → Part Picked Up → Full Picked Up when Pickup Pending reaches zero
- Backend request idempotency prevents duplicate saves
- D1 triggers prevent given quantity from exceeding booked quantity
- OWNER pickup correction creates a new correction event and audit trail; it never silently overwrites history
- Historical pickup correction remains locked after return activity starts; normal remaining Pickup may continue after an earlier return only when the order was explicitly kept open
- Today / Upcoming / Partially Given / Completed / All views
- WhatsApp and Call quick actions

## Current permission rule
- STAFF with Pickup permission may perform normal operational pickup actions allowed by the backend contract.
- Historical pickup correction is OWNER-only and always creates/retains an audit event; it never silently rewrites history.
