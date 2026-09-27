# Phase 13 — Production Hardening

## Concurrency and inventory integrity
Availability is still checked before save for a useful UI error, but that read is not trusted as the final protection. Migration `0008_production_hardening.sql` adds D1/SQLite triggers that re-check total capacity when a `booking_items` row is written. This closes the classic two-request race where both requests see the same available stock and then both attempt to reserve it.

Overdue, not-yet-returned quantity is also treated as reserved for later bookings.

## Booking idempotency
New bookings carry a client-generated `requestKey`. The value is unique in `bookings.request_key`. If the browser times out after the server has already saved the booking, retrying with the same key returns the previously saved booking instead of creating another one.

## Pickup / Return status consistency
Quantity-integrity triggers from earlier phases remain active. Phase 13 adds a status-sync trigger: after `given_qty` or `returned_qty` changes, D1 derives the booking status from authoritative line quantities. Pickup/Return code no longer relies on a potentially stale pre-write status snapshot.

## Authentication hardening
- PBKDF2 password hashing remains enabled.
- Failed logins are throttled per normalized identifier + hashed source IP.
- Eight failures inside fifteen minutes cause a temporary fifteen-minute block.
- Successful login clears that throttle key.
- Initial Owner setup first checks whether any user already exists, so an initialized installation does not keep validating the setup token.
- At most ten active session rows are retained per user.
- Unknown/forged session cookies do not cause a no-op D1 delete on every request.

## Request protection
For mutating API routes:
- disallowed browser Origins are rejected;
- JSON content type is required;
- declared bodies over 256 KiB are rejected;
- API responses include `X-Content-Type-Options`, strict Referrer Policy and a restrictive Permissions Policy.

`/api/public/availability` is a read-only POST and is exempt from the mutation-origin restriction, though normal CORS still controls browser access across origins.

## Failure recovery
Admin API calls have a 30-second client timeout. A timeout reports that entered data is still on screen. Booking create keeps the same idempotency key after a failed/timed-out attempt and generates a new one only after reset/success.

Expired sessions emit a client auth-expired event so the Admin UI returns to Login instead of leaving protected screens in a confusing stale state.

## API / D1 efficiency
- Dashboard remains one bundled dashboard endpoint; no polling.
- Public catalog remains bundled; no per-card fetch.
- Public availability remains a single bulk request.
- Reports load only the selected report.
- Booking availability debounce is 500 ms.
- Unknown session cookies no longer produce an extra write.

## Regression test
Run:

```bash
python3 tests/production_hardening.py
```

It validates all migrations, booking capacity guards, booking request-key uniqueness, status self-healing, overdue pending stock reservation, login-throttle schema and foreign keys.
