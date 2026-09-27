# Booking Module

## Lifecycle
One booking uses a single operator flow with these user-facing states:
- `Reserved` — inventory is reserved but the booking is not yet confirmed.
- `Booked` — confirmed and waiting for pickup.
- `Part Pickup` — only part of the booked quantity has been given.
- `Full Pickup` — all booked quantity has been given.
- `Part Return` — only part of the given quantity has been returned.
- `Full Return` — everything given has been returned and the booking is complete.

`Cancelled` is a separate terminal state before pickup.

The database keeps the proven operational status values (`BOOKED`, `PARTIALLY_GIVEN`, `GIVEN`, `PARTIALLY_RETURNED`, `RETURNED`, `CANCELLED`) and stores reservation confirmation separately as `confirmation_state = RESERVED|BOOKED`. This preserves existing inventory and web compatibility while allowing the Android app to show the required lifecycle labels.

## Single booking form
Customer → Pickup Date → Return Date → Category → Item → Quantity.

New booking actions:
- `Reserve` saves the booking with `confirmation_state=RESERVED`.
- `Confirm Booking` saves a confirmed booking.
- `Direct Pickup` saves a confirmed booking and immediately records pickup without re-entering customer/items.

Existing booking actions are status driven. Status is never manually selected.
- Reserved: Edit, Confirm Booking, Cancel.
- Booked: Edit, Pickup, Cancel.
- Part Pickup: remaining Pickup and Return.
- Full Pickup: Return.
- Part Return: remaining Return.
- Full Return: read-only complete state.

## Core rules
- Same item cannot exceed total quantity across overlapping active bookings.
- Reserved bookings reserve inventory exactly like confirmed bookings.
- Date overlap is inclusive of Pickup and Return dates.
- Cancelled and fully Returned bookings do not reserve future quantity.
- Editing re-checks availability while excluding the booking being edited.
- Once pickup has started, booking structure is locked; corrections belong to Pickup/Return workflow.
- Booking number is generated server-side.
- Create, reserve, confirm, update, cancel, pickup and return actions are audited.

## Direct pickup safety
Direct Pickup remains one operator action but two server records:
- create the confirmed booking with its own idempotency request key;
- record pickup with a different idempotency request key;
- if booking succeeds but pickup fails, retain the booking and allow pickup retry; never create a duplicate booking.

## API efficiency
Customer/booking auto-search uses a two-character minimum and only the latest pending query. Item options are bundled in booking bootstrap. Save/update is server validated; no per-item polling is used.
