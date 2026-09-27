# Booking Module — Business Contract

This file defines booking lifecycle/backend behavior. Detailed Admin Android UI/layout/action rules live in `ADMIN_SCREEN_04_BOOKINGS.md`, `ADMIN_ANDROID_SCREEN4_HARDENING.md` and `GLOBAL_UI_RULES.md`.

## Lifecycle
Canonical visible lifecycle labels:
`Reserved → Booked → Part Picked Up / Full Picked Up → Part Return → Full Returned`

Order/Booking number allocation is daily and atomic: `BK-YYYYMMDD-001`, `002`, ...; each new business date starts at `001`. Idempotent request-key retries reuse the already-created Order and do not consume/create a second number.

`Cancelled` is terminal before pickup. `Overdue` is a derived list/filter condition when return due date has passed and quantity remains outstanding; it does not replace the underlying lifecycle state.

## Quantity/state authority
- D1/Worker quantities and derived lifecycle state are authoritative.
- Original Booked Qty is preserved for history. Active booked quantity may be reduced only by the explicit Close Remaining Items flow.
- Pickup Pending = active booked quantity − picked quantity.
- Return Pending = picked quantity − returned quantity.
- Partial pickup and partial return are supported, including later pickup after earlier returned quantity when the order was kept open.
- Returning everything picked so far does not mean Full Returned while Pickup Pending > 0.
- Full Returned requires Pickup Pending = 0 and Return Pending = 0.
- Booking structure becomes locked after pickup starts except for the approved quantity movements/remaining-item closure; later historical correction belongs to Pickup/Return correction flow.
- Any valid correction must remain auditable.
- Historical Pickup/Return correction is OWNER-only.

## Availability
- Date overlap is inclusive of Pickup and Return dates.
- Requested quantity cannot exceed authoritative availability for the selected date range.
- Final server-side capacity guards remain authoritative even if UI performed an earlier availability read.

## Direct pickup
Direct pickup is one operator flow but preserves separate server records:
1. save a confirmed booking with its idempotency request key;
2. record pickup with a different idempotency request key.

If booking succeeds and pickup fails, keep the booking and allow pickup retry. Never create a duplicate booking.

Admin Android visible label is exactly **Directly Pickup**. Reserved Booking Details may use Directly Pickup on the same booking: confirm first, then open quantity-selectable pickup.

## Audit/history
Create, reserve, confirm, update, cancel, pickup, return, remaining-item closure and correction events are audited as applicable. Pickup/Return/closure events include item + quantity detail and use actual event timestamps, not scheduled dates.

## Admin Android list grouping
Current filters:
`All | Reserved | Booked | Part Picked Up | Full Picked Up | Part Return | Full Returned | Overdue | Cancelled`

If a Return is recorded while Pickup Pending > 0, the operator must choose **Keep Order Open** or **Close Remaining Items**. Close Remaining releases only unpicked quantity and keeps an audit trail.

Compact Android Booking List cards use first 2 item rows + tappable `+ N more`, with same-card Expand and Collapse/Show less. Booking Details and New Booking confirmation/review show all items.

## Related Item suggestions
Configured directional Related Items are UI suggestions in New/Edit Booking only. The same booking bootstrap supplies the relation links with Item options. Selecting a Main Item may display related suggestions underneath it, but the booking line is created only after explicit operator **Add**. No relation is persisted into the Booking business model, no reverse relation is inferred, and availability/duplicate/capacity validation remains unchanged.

## Item picker
The Android item picker is search-first. A compact Category control opens a bottom sheet containing `All Categories`, dynamic categories and `Cancel | Apply`. Permanent horizontal category chips are not used. Selected items persist across search/filter changes.

## Detailed Android authority
Do not duplicate layout/action wording here. Follow:
- `ADMIN_SCREEN_04_BOOKINGS.md`
- `ADMIN_ANDROID_SCREEN4_HARDENING.md`
- `GLOBAL_UI_RULES.md`
- `UI_GUIDELINES.md`

## Billing V1 Booking Advance
- New/Edit Booking uses **Customer → Details → Items → Preview → Payment**. Preview shows the complete Customer/Date/Items review. **Advance Amount** is shown only on Payment.
- Initial actual value is **0** and the field is numeric.
- On focus, the current numeric value is selected so typing replaces it immediately without Delete/Backspace first.
- If `0` is cleared, the field may remain empty while editing and shows placeholder **Advance Amount**. Do not show a `Cash advance · default ₹0` helper line.
- Blank Advance is treated as ₹0 when Reserve, Confirm, Directly Pickup or Save is executed.
- Rent is not shown anywhere in the Booking flow.
- The same Advance Amount is persisted for **Reserve**, **Confirm** and **Directly Pickup**.
- Edit Booking rehydrates and may update Advance while structural Booking edit remains allowed.

### Advance settlement when cancelling before pickup
If saved Advance is greater than 0, cancellation cannot continue until the operator chooses:
- **Full Refund**
- **Partial Refund** with Refund Amount > 0 and < original Advance
- **No Refund**

The original Advance remains historical and is never zeroed/overwritten. Refund Amount and settlement status are stored separately; Retained Amount is derived as `Advance - Refund`. Settlement user/time remains auditable.

Booking cancellation synchronizes linked Billing:
- linked Draft Bill → **Cancelled** historical Bill;
- linked Final Bill → **Cancelled** historical Bill;
- already Cancelled Bill stays historical;
- no Bill + Advance settlement remains accessible as a read-only **Advance Settlement** financial record from the Booking Bill action.

### Booking → Bill handoff
- Newly created Bills are always linked to an existing Order/Booking; standalone/direct Bill creation is removed.
- Booking Details/Summary shows **Bill** for all non-cancelled Orders.
- Cancelled Order shows Bill only when a linked Bill exists or Advance settlement/history exists; otherwise Bill is hidden.
- Bill click opens the existing linked Bill or a Booking-seeded linked Draft editor **directly**, without first rendering the Bills List.
- Full Return remains authoritative only when Pickup Pending = 0 and Return Pending = 0.
- When a Return mutation makes the Order **Full Returned**, Worker idempotently ensures the one linked Draft Bill and Android opens the related Billing page.
- Existing Draft is reused. Existing Final/Cancelled Bill is reused and no duplicate is created.
- If Draft preparation/opening fails, Return stays completed and a visible Billing error is shown; never roll back the successful Return.
- A Booking may have at most one linked Bill.
- Booking summary/list action order is **View | Edit | Bill | Call | WhatsApp** where valid.
- Compact Booking cards use one top row **Pickup | Current | Return**. All three cards use **Label → Status → Date → Day**. Pickup/Return show `Pending | Done`; Current shows the lifecycle state. Time is removed. The lower Current badge is removed; the Next badge remains below the item list.

### Bills → Add Bill
- **Add Bill** opens **Select Order**, never a standalone Customer/Item Bill form.
- Select Order shows only non-cancelled Orders that do not already have a Bill.
- Search supports Order/Booking number, Customer name and Mobile.
- **Create Draft** creates the linked Draft server-side and opens it.

