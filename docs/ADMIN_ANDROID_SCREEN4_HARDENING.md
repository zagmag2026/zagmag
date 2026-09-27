# Admin Android — Screen 4 Hardening

This file is the latest specific authority for the current Admin Android Customer + Booking reliability/compact-UI hardening. It supplements `GLOBAL_UI_RULES.md`, `UI_GUIDELINES.md` and `ADMIN_SCREEN_04_BOOKINGS.md`.

## Operational feedback
- Mutation success/error feedback is transient and one-time. A Save/Delete/Archive/Restore/Permanent Delete result must not replay merely because the user leaves and revisits the screen.
- Operational success/error/warning/info feedback keeps the existing floating popup/snackbar visual style but uses the global full-width placement immediately below the Main Header. It remains an overlay and may cover the scrolling Date / Day / Time row.
- Do not use polling to clear or refresh data.

## Reliable keyboard Done
- Final editable fields use `ImeAction.Done`.
- Done/✓ must clear Compose focus, request Compose keyboard hide, and use Android `InputMethodManager.hideSoftInputFromWindow(...)` as the reliability fallback.
- The same reliable keyboard-dismiss helper is reused by manual quantity input.

## Editable date fields
- Editable Pickup and Return date fields are always vertical, never side-by-side.
- Each field renders in this order: label → calendar icon + `DD-MM-YYYY` → full English weekday.
- Pickup is above Return on phone and tablet.
- The whole date field remains tappable and Return stays at least one day after Pickup.
- This does not change read-only summary badge rules.

## New/Edit Booking — Customer
- A selected customer has a visible selected check plus a separate `Clear selected customer` action.
- Clear removes only the selected customer, keeps the booking subflow open, resets customer search, and disables Next until another customer is selected.

## New/Edit Booking — Items
- Search remains first; Category opens the existing bottom-sheet filter.
- Item result card is compact: image left, item identity in the middle, `Select` / `Selected` action on the right. There is no separate bottom action row.
- Existing availability text remains only on the item-selection result where it already belongs; do not add availability to unrelated name-only rows.
- Selected Items remain below visible search results.
- Selected quantity uses `− [editable quantity] +`. Focusing the value selects the existing number so typing replaces it without manual deletion.
- Quantity input is numeric, minimum 1, maximum authoritative available quantity; invalid, zero and over-available values are not committed. Done/✓ commits and hides the keyboard.

## Item image radius
- Every current Admin Android booking/item image uses the shared `RoundedItemImage` treatment with an exact `10.dp` corner radius.

## Booking summary cards
- Booking List and Dashboard operational queues reuse the same `BookingSummaryCard` structure.
- Identity order remains Customer Name → Mobile Number → Booking Number with lifecycle status at right.
- Pickup and Return date badges appear above the item/lifecycle summary area.
- Compact state initially shows at most two item rows plus `+ N more`; tapping `+ N more` expands the same card in place to show the available complete item list.
- Current and Next lifecycle badges appear below the item list.
- Booking List and Dashboard reuse the same booking-card action component for `View | Edit | Bill | Call | WhatsApp` where applicable; Edit is shown only when structurally editable and Bill follows the linked-Billing visibility rule.
- Call uses the shared Call confirmation popup and WhatsApp uses the shared WhatsApp preview/confirmation popup; Dashboard must not introduce separate contact popups.

## Related Item suggestion UI
- New/Edit Booking consumes the existing directional Related Items mapping from the bundled booking bootstrap.
- A selected Main Item renders configured suggestions directly underneath it using the global Item row with nested indentation.
- Suggestions never auto-add. The operator must tap **Add**; existing authoritative availability and duplicate-line rules remain unchanged.
- A Related Item explicitly added from the nested suggestion stays visually associated with its Main Item where the current booking surface can preserve that relationship.
- No extra request is issued when suggestions are expanded/rendered.

## Booking Details
- Identity order is Customer Name → Mobile Number → Booking Number.
- Current lifecycle status remains on the right side of the identity row.
- Details shows every item and keeps `Item Code · Category` directly below Item Name.
- Booking Details provides a visible Back action and Android system/gesture Back; both close the detail subflow through the same source-aware return path.
- For a Reserved booking, `Cancel Booking` and `Confirm` are equal-width single-line actions in one row, with full-width `Directly Pickup` below. Directly Pickup confirms that same booking and opens Pickup quantity selection.
- Details tabs remain exactly `Details | Pickup | Return | History`.
- Booking Details action order is **Edit | Bill | Call | WhatsApp** when all four apply; the four actions remain in one row on supported narrow-phone widths.
- Active Orders expose Bill. Cancelled Orders expose Bill only when a linked Bill or Advance settlement exists.
- Full Return performs a post-return Billing handoff that reuses/ensures the one linked Bill; a Billing error never reverses the saved Return.
- Pre-pickup cancellation with Advance > 0 requires Full Refund, valid Partial Refund or No Refund and preserves original Advance separately from Refund.

## Pickup / Return quantity
- Pickup and Return eligibility is quantity-derived. Pickup may continue after an earlier return when unpicked quantity remains and the operator kept the order open.
- Return after a partial pickup with unpicked quantity requires the Keep Order Open / Close Remaining Items decision.
- Pickup and Return quantity rows use `− [editable quantity] +`.
- The editable middle value selects its complete current number on focus, so typing replaces instead of appending.
- Input is numeric with minimum 0 and maximum the authoritative remaining quantity for that item.
- Invalid or over-limit input is not committed; Done commits a valid value and closes the keyboard through the reliable keyboard-dismiss helper.
- Existing All, plus and minus actions remain.

## History
- History uses persisted event timestamps and event-time payloads; never infer old events from the booking's final current status.
- Reserved creation shows `Reserved` and suppresses the redundant underlying initial `CREATE`/Booked entry for the same reserved flow.
- `CONFIRM_BOOKING` shows `Confirmed`.
- Pickup event status `PARTIALLY_GIVEN` → `Part Picked Up`; `GIVEN` → `Full Picked Up`.
- Return event status `PARTIALLY_RETURNED` → `Part Return`; `RETURNED` → `Full Returned`.
- Update → `Booking Updated`; Cancel → `Cancelled`; corrections remain explicit correction events.

## Cancelled filter
- Worker/D1 remains authoritative and `view=CANCELLED` must return cancelled bookings only.
- Android also defensively refuses to render a non-cancelled row in the Cancelled tab.

## Mutation data freshness
- Successful mutations mark only affected data domains stale; failed mutations do not.
- Booking mutations invalidate Booking list, Customer booking-derived counts and Dashboard.
- Customer mutations invalidate Customers, Booking rows/customer identity, Booking customer bootstrap and Dashboard.
- Dashboard gets one targeted refresh through the shared coordinator; list/customer screens refresh once on next entry when their revision is stale.
- Fresh/not-dirty screen entry makes zero extra request. No polling and no refresh-on-every-navigation.
- Existing valid data is retained while a refresh is in progress or if refresh fails where the owning state supports it.

## Release safety
- Android identity remains `0.14.5 / versionCode 1` for this hardening batch.
- Validation target is Admin Android staging APK only.
- Production Worker, production API configuration, production APK/AAB and production deployment remain untouched unless explicitly requested.
