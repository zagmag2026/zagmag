# Zhagmag Dresses 0.14.5 — Phase 14W Compact Dashboard Cards

Phase 14W continues from verified Phase 14V and keeps version `0.14.5`.

## Admin Dashboard UI
- Today Bookings, Today Pickups, Today Returns and Overdue Returns use a denser ERP-style operational card layout.
- Customer name/mobile and status stay in one compact header row on mobile instead of stacking the status below the customer block.
- Every booked item remains individually visible. Each item keeps its own thumbnail, item code/name, category and quantity/status detail.
- Item rows use smaller thumbnails, tighter spacing, flatter borders and reduced vertical height for faster scanning.
- Booking/customer metadata chips are smaller and aligned consistently.
- WhatsApp, Call and Open Booking actions use one compact action row; on mobile they share equal-width columns.
- Overdue semantic emphasis is preserved.

## Technical impact
- Admin Web CSS/entry chain changed only.
- No Public Web behavior change.
- No Worker/API business logic change.
- D1 schema/migrations unchanged.
- No new API/Cloudflare calls.
- Pricing/payment features remain OFF.
- Production is NOT deployed by this phase.
