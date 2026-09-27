# Release 0.7.0 — Phase 7 Pickup Module

## Added
- Pickup queue with Today / Upcoming / Partially Given / Completed / All views
- Full and partial pickup
- Given To = booking customer
- Given By = authenticated Admin/Staff
- Server-side pickup timestamp
- BOOKED → PARTIALLY_GIVEN → GIVEN transitions
- Pickup event history
- WhatsApp and Call quick actions
- Backend idempotency key protection with retry-safe client key reuse
- D1 quantity-integrity triggers
- Owner/Admin Correct Pickup UI and API
- Correction creates a separate correction event and audit history
- Correction is locked after Return activity starts

## Preserved
- Generic rental category/item architecture
- Choli as current category
- Size = Free Size seed
- No pricing/payment/deposit module
- No laundry/repair/maintenance workflow
- Existing authentication, category, item, customer and booking modules

## Validation
- TypeScript/TSX syntax validation: PASS
- D1 migrations 0001, 0002, 0003: PASS
- Choli / Free Size seed: PASS
- Pickup request idempotency unique constraint: PASS
- Partial → full pickup quantity smoke test: PASS
- Given quantity guard: PASS
- Returned <= Given future-integrity guard: PASS
- Foreign key check: PASS
