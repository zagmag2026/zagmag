# Release 0.13.0 — Phase 13 Production Hardening

## Added / hardened
- D1 booking-capacity write guard
- Booking create idempotency
- DB-derived booking status after quantity mutations
- Login attempt throttling
- Origin / JSON / request-size guards
- API security headers
- Generic internal-error response shielding
- Bounded sessions per user
- Unknown-cookie no-op-write removal
- Admin 30-second request timeout and auth-expiry recovery
- Reduced booking-availability debounce traffic
- Automated hardening regression test

## Migration
- `0008_production_hardening.sql`

## Scope preserved
- Pricing/payment/deposit remains OFF
- Laundry/repair/maintenance remains OFF
- Public catalog remains login-free
- Choli remains the initial category with Free Size default
