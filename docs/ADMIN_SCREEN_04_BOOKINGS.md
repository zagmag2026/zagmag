# Admin Screen 4 — Bookings

Status: **Implemented / finalized / locked**

This document is the approved Admin Android Screen 4 contract. Apply `GLOBAL_UI_RULES.md`, `UI_GUIDELINES.md`, `UI_DESIGN_SYSTEM.md`, `BOOKING_MODULE.md`, and backend business rules.

## Booking List
Status filters, in this order:

`All | Reserved | Booked | Part Picked Up | Full Picked Up | Part Return | Full Returned | Overdue | Cancelled`

The tabs are quantity-derived. `Overdue` remains a derived date + Return Pending condition rather than a terminal lifecycle status.

- Search placeholder: `Search`.
- Compact search field and sort control.
- Global +10 incremental scrolling; no Previous/Page/Next controls.
- Swipe-down refresh reloads the current Booking List without adding polling.
- Booking card identity order is **Customer Name → Mobile Number → Booking Number**, with lifecycle status kept as a compact badge.
- Booking summary/list cards show at most the first **2** item rows, then `+ N more` when additional booked item lines exist.
- Booking Details and New Booking confirmation/review remain complete-item views and show all booked/selected items.
- Item thumbnails use the global rounded-corner treatment.
- Current/Next lifecycle summary uses one-line icon badges. Pickup/Return use one-line calendar/date/day badges; stack badges vertically on narrow phones rather than wrapping badge text.
- Booking List quick actions are View, Edit when structurally editable, Bill when financially applicable, Call, WhatsApp.
- Booking List quick actions preserve View → Edit → Bill → Call → WhatsApp order for whichever actions are visible.
- Call uses the same shared confirmation-first bottom sheet before dialer launch: Person/User icon + Customer Name, Call icon + Mobile Number, then Cancel | Call.
- WhatsApp detects the current Booking status group, shows Loading immediately, loads only applicable valid active templates, lets the operator select when multiple are available, then shows the shared full-message preview before WhatsApp launch. A single applicable template opens Preview directly.
- WhatsApp loading must always resolve to selector, preview or visible error; Dashboard, Booking List and Booking Details must never silently swallow template/API failures.
- Booking List includes the shared WhatsApp busy state so rapid/repeated taps cannot start overlapping template requests.
- One invalid/unresolved template in the group is skipped; valid siblings remain selectable. If no valid template remains, show a clear error and do not open WhatsApp.
- There is no alternate-number chooser.
- Apply the global information-row rule where practical: related icon + actual value for customer name/mobile and other compact metadata.

## Source-aware navigation
- Booking editor/detail flows are subflows and return to their source screen when closed.
- Dashboard → New Booking/Booking Details → Back/Cancel/close returns to Dashboard.
- Customers → Customer card `Booking` → New Booking → Back/Cancel/close returns to Customers.
- Bookings → New Booking/Booking Details → Back/Cancel/close returns to Bookings.
- A successful create/lifecycle action that closes the editor follows the same source-return rule unless explicitly overridden later.
- Preserve the source screen's search/tab/sort/scroll state where practical.

## Confirmation consistency
- Booking Cancel uses the shared destructive confirmation dialog with `Back | Cancel Booking`, busy-safe dismissal and explicit booking-number consequence copy.
- Return with pending Pickup keeps its specialized choice dialog because it offers two materially different business outcomes: **Keep Order Open** or destructive **Close Remaining Items**.
## Mutation completion safety
- Booking Create/Edit/Confirm/Cancel/Pickup/Return mutations lock synchronously before their coroutine/request starts so rapid repeated taps cannot create overlapping requests.
- Pickup and Return keep the action busy through the authoritative Booking Detail reload. Success feedback appears only after the refreshed detail is applied and quantity controls are reset/replaced by the correct read-only lifecycle state.
- During that authoritative reload, keep the currently loaded Booking Detail visible; do not replace it with an empty/loading-only state that produces a blank screen.
- If the post-mutation detail refresh fails, keep the existing detail visible, show a refresh error, block further booking mutations on that stale detail, and allow an explicit Retry Refresh/reopen path.
- If the post-mutation detail reload fails, stale Pickup/Return controls must not remain usable as though the mutation had not happened.
- Existing request-key/idempotency behavior remains unchanged.
## New Booking
Four steps:

`1 Customer → 2 Details → 3 Items → 4 Confirm`

Completed steps use a completed treatment; only the current step uses the active treatment.

### Step 1 — Customer
- `+ New Customer` is in the New Booking title row on the right.
- Reuse the same Customer form: Name, Mobile Number, Address.
- Alternative Mobile is not part of the form.
- Customer search supports quick create only after the debounced search completes with no matching result.
- A valid searched 10-digit mobile exposes `Create new customer`; opening it pre-fills Mobile Number so the operator does not re-enter the search value.
- A searched name with no result exposes the same quick-create action and pre-fills Name.
- Incomplete/invalid mobile searches do not expose quick create.
- A completed no-result search may still expose Quick Create when the search request itself reports a transient error; show that search error inline instead of silently suppressing the Quick Create action. Server-side duplicate-mobile validation/recovery remains authoritative.
- On successful create, close the sheet, auto-select the new customer, preserve the Booking draft/search context, and stay in Step 1.
- If the mobile becomes a duplicate before save completes, resolve and auto-select the exact existing active customer when available instead of creating a duplicate.
- Next is disabled until a customer is selected.
- The Customer section may use the approved labeled/notched section-card pattern rather than a separate heading above the border.
- The customer sheet must open correctly and remain usable with the keyboard open: expanded when necessary, scrollable field body, fixed inset-safe `Cancel | Save` footer, `Name → Next → Mobile Number → Next → Address → Done` keyboard order.
- `Done` clears focus and closes the software keyboard.
- When New Booking is opened from a Customer card's `Booking` action, that customer is already selected in Step 1 and Next is immediately enabled; the operator must not search/select the same customer again.

### Step 2 — Details
- Default Pickup = Today.
- Default Return = Next Day.
- Return must be at least Pickup + 1 day.
- If Pickup moves forward beyond the current Return, automatically move Return to the minimum valid date.
- Notes are optional and compact.
- The section may use the approved labeled/notched section-card pattern.

### Step 3 — Items
Canonical item-selection rule:
- Search-first layout; do **not** use the permanently visible horizontal category-chip strip as the primary category control.
- A compact `Category` filter action opens a bottom sheet containing `All Categories`, dynamic categories and `Cancel | Apply`.
- Search works within the active category filter.
- Multiple categories/items may be selected in one booking.
- Selected items remain retained when search/category changes.
- Search/result cards appear first. `Selected Items (N)` appears **below the visible search results**.
- Result cards use a simple `Select` action; once selected, they show a clear selected state instead of an additional Add flow.
- Quantity editing and removal are performed in the Selected Items review area.
- `Selected Items (N)` counts distinct item lines; quantity is shown separately.
- The same item cannot be added as a duplicate line; its quantity is updated instead.
- Unavailable items cannot be selected.
- Quantity cannot exceed availability for the selected Pickup–Return range.
- Worker/D1 validation remains authoritative.
- Next is disabled until at least one item is selected.

### Step 4 — Confirm
Main action layout:

`Reserve | Confirm`

`Directly Pickup`

Navigation actions:

`Back | Cancel`

- Reserve and Confirm share the first action row.
- `Directly Pickup` is a full-width second-row action so its label remains single-line on narrow phones.
- `Directly Pickup` is the UI label for the existing Direct Pickup behavior; backend booking/pickup records and idempotent safety remain unchanged.
- Reserve creates a reserved booking.
- Confirm creates a confirmed booking.
- Directly Pickup creates a confirmed booking then records pickup using the existing two-record/idempotent backend safety flow.
- The confirmation/review item card shows **all selected items**; never use `+ N more` here.
- Item thumbnails are rounded.
- Pickup and Return summaries use the global one-line icon/date/day badge pattern.
- New/Edit Booking date fields reuse the shared date picker while preserving Pickup ≥ business day and Return > Pickup; visible values use DD-MM-YYYY with full weekday.

## Related Item suggestions in New/Edit Booking

- Booking bootstrap includes the existing directional Item relation mapping in the same bundled request; rendering suggestions adds no per-Item API call.
- After a Main Item is selected, its configured Related Items appear immediately below that Main Item with a nested/associated visual treatment.
- Related Items are suggestions only and are never auto-added.
- A Related Item enters the booking only after the operator explicitly taps **Add** and the existing availability, duplicate-line and quantity validation passes.
- When added through the suggestion, preserve the visual association with its source Main Item through selection/review where practical.
- Removing an Item from the booking does not alter Item Master relation configuration. Relation direction remains one-way.

## Booking Details
Header contains only:

`Booking Details`

Do not place booking number or status badge in the page header. They may appear compactly inside the Details card.

Tabs:

`Details | Pickup | Return | History`

There is no separate Items tab; booked items are shown in Details.

### Details
- Reuse compact global card styling.
- Show customer, booking number/status, pickup/return dates and **all** item lines.
- Booking Details card actions are exactly **Edit | Bill | Call | WhatsApp** when all four apply. The four actions stay in one equal-width row on supported narrow-phone layouts; absent actions preserve the relative order of the remaining actions.
- Do not show a standalone **Thank You** button in Booking Details; Returned-state Thank You remains an applicable WhatsApp template inside the Returned template group.
- Full detail is not a summary card, so it does not collapse items behind `+ N more`.
- Item thumbnails use rounded corners.
- Customer information follows the global icon + value pattern where practical.
- Call is confirmation-first through the same shared popup used everywhere: Person/User icon + Customer Name, Call icon + Mobile Number, Cancel | Call.
- WhatsApp uses current-status template filtering before the full-message preview through the shared contact/template flow.
- Reserved/Booked before pickup may expose valid edit/cancel actions.
- Reserved Details show `Cancel Booking | Confirm` plus a full-width `Directly Pickup` below. Directly Pickup confirms the same existing Reserved booking, then opens Pickup quantity selection; it never creates a second booking.
- Lifecycle actions are quantity-driven: Pickup is available while Pickup Pending > 0; Return is available while Return Pending > 0. Both may be valid on the same booking.
- Full Returned has no lifecycle primary action.
- Android system Back returns to the source screen according to the source-aware navigation rule.

### Pickup
- Opening or switching to the Pickup tab is passive and must not create a transient popup.
- When pickup controls are unavailable, `Pickup completed.` or `No pickup has been recorded.` is shown only as the compact inline status/info treatment inside the Pickup tab.
- Supports partial pickup and later additional pickup even if an earlier picked quantity has already been returned, provided the order was kept open.
- `All` fills every currently eligible remaining pickup quantity.
- Confirm Pickup requires at least one positive quantity.
- Pickup Pending = active booked quantity − picked quantity.
- After Pickup Pending reaches zero, pickup controls become read-only and the actual picked-item summary remains visible.

### Return
- Opening or switching to the Return tab is passive and must not create a transient popup.
- When return controls are unavailable, `Return completed.` or `No items are ready to return.` is shown only as the compact inline status/info treatment inside the Return tab.
- Supports partial return.
- `All` fills every currently returnable remaining quantity.
- Confirm Return requires at least one positive quantity.
- Return Pending = picked quantity − returned quantity.
- If Pickup Pending > 0 when Return is confirmed, ask exactly whether to **Keep Order Open** or **Close Remaining Items**.
- Keep Order Open preserves unpicked quantity for later pickup.
- Close Remaining Items releases only the unpicked quantity, preserves original booked quantity/history, and records an auditable Remaining Items Closed event.
- Returning every item picked so far is not Full Returned while Pickup Pending remains greater than zero.
- Full Returned requires Pickup Pending = 0 and Return Pending = 0.

### History
- Read-only actual event/audit timeline.
- Use actual event timestamps, not scheduled pickup/return dates, when describing what happened.
- Every Pickup, Return and Remaining Items Closed event shows the actual item name + quantity affected by that event.
- Multiple partial pickup/return events appear separately.
- Do not synthesize a duplicate completion event when Full Returned already represents completion unless the backend contains a real separate completion event.

## Messages
- Success/error/warning/info popup acknowledgement follows the global rule and is reserved for actual user actions/mutations and their failures.
- Booking Details tab clicks, screen open, navigation, expand/collapse and filter/search/sort presentation changes must not generate or replay popup feedback.
- Passive lifecycle state belongs inside the relevant tab as inline status content.
- Confirm Pickup, Confirm Return, Booking Confirm/Cancel and other real mutations may show the shared transient result popup.

## Status wording
Lifecycle labels are:

`Reserved → Booked → Part Picked Up / Full Picked Up → Part Return → Full Returned`

`Cancelled` is terminal before pickup. Operational movement can be non-linear after Part Picked Up: returned picked items may coexist with later Pickup Pending when the order is kept open. Do not use wording such as `Reserved Done`.

## Locked compact list-card expansion
- Booking List compact cards show at most the first 2 item rows by default.
- If more items exist, show tappable `+ N more`.
- Expanded state shows all item rows in the same card and provides `Collapse` / `Show less`.
- Collapse returns to the first 2 rows plus `+ N more`.
- Expand/collapse uses already-loaded data and causes no API call.
- Booking Details and New Booking confirmation/review always show all items and do not use this compact collapse rule.

## Loading / empty / retry behavior
- Booking List initial failure shows Error + Retry and is not marked as a successful empty load; `No bookings found.` is shown only after a successful empty response.
- Booking Detail initial View/Edit keeps the Date row and Back header visible, shows Loading while the booking is fetched, and shows Error + Retry on failure.
- Booking Detail retry targets the same booking id that originally failed.
- Manual/detail refresh preserves already loaded Booking Detail while refreshing. Booking List next-page failure preserves already loaded rows, stops the auto-load loop and shows a bottom **Retry loading more** action that retries only the failed next page.

## Explicit Billing V1 extension
This locked Screen 4 contract is explicitly extended by the approved booking-linked Billing work:
- Confirm Booking shows **Advance Amount** immediately below Items with actual default value `0`.
- Focusing Advance selects the whole numeric value. If cleared, placeholder is exactly **Advance Amount**, no helper text is shown underneath, and blank normalizes to ₹0 on action/save.
- Reserve, Confirm and Directly Pickup all save Advance Amount. Booking UI does not display Item Rent.
- New Bills are **Order-linked only**; standalone/direct Bill creation is retired.
- Active Orders expose **Bill**. Cancelled Orders expose Bill only when an existing linked Bill or Advance settlement/history is present.
- If Advance > 0, pre-pickup cancellation requires **Full Refund | Partial Refund | No Refund**. Partial Refund must be > ₹0 and < Advance. Original Advance remains historical and Refund/Retained settlement is audited.
- Cancelling an Order moves any linked Draft/Final Bill to **Cancelled** history.
- A Return that makes the Order **Full Returned** idempotently ensures/reuses the one linked Bill and hands Android to Billing. Billing handoff failure never rolls back the successful Return.
- Booking Details action order is **Edit | Bill | Call | WhatsApp** when all four apply.
All other locked Booking behavior remains unchanged.


## New/Edit Booking dirty-exit protection
- New/Edit Booking captures an editor baseline when opened.
- Back/Cancel from the first wizard step closes immediately only when unchanged.
- If Customer, dates, notes, Advance or selected Item quantities differ from the baseline, exit requires **Discard unsaved changes?** confirmation.
- Wizard Back between steps remains normal navigation and does not show a discard dialog.
