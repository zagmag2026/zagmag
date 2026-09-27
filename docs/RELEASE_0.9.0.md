# Release 0.9.0 — Phase 9 Live Dashboard

## Added
- Live dashboard summary cards
- Today Bookings
- Today Pickups
- Today Returns
- Overdue Returns
- Customer Name + Mobile on dashboard activity
- WhatsApp quick action
- Call quick action
- Open exact Booking from dashboard
- Manual Refresh button
- One bundled Dashboard API request
- One bundled D1 statement per dashboard refresh
- No auto-polling

## Preserved
- Generic category-driven rental architecture
- Choli current category
- Size = Free Size seed
- Pricing/payment/deposit disabled
- Laundry/repair/maintenance disabled
- Phase 2–8 authentication, CRUD, booking, pickup and return rules

## Database
No new D1 migration is required.

## Validation
- Clean migrations 0001–0004: PASS
- Foreign key check: PASS
- Dashboard one-statement SQL sample-data smoke test: PASS
- Summary quantity calculation: PASS
- Today Bookings query: PASS
- Today Pickups query: PASS
- Today Returns query: PASS
- Overdue Returns query: PASS
- Admin temporary strict TypeScript typecheck: PASS
- Public temporary strict TypeScript typecheck: PASS
- Worker temporary strict TypeScript typecheck: PASS
