# Zhagmag Dresses — Global Admin Android UI Rules

These rules apply to every applicable Admin Android screen. Screen-specific documents may add behavior but must not silently contradict these shared rules.

## Reusable cards
- Reuse the shared `AppCard` visual family for equivalent record cards.
- Keep border, radius, surface treatment, compact spacing, information hierarchy, semantic badges and action placement consistent.
- Prefer: primary identity/status → essential metadata → item/date information → bottom action row.
- Do not create one-off card styling when the shared card can represent the content.
- For compact summary/list booking cards, show at most the first **2** item rows and then a compact `+ N more` indicator when more items exist.
- `+ N more` is tappable: tapping it expands the same card in place to show the available complete item list; it does not navigate away from the card.
- Every expanded compact list/card exposes the same reusable **Show less** action in the expand-control area. Show less restores the original compact limit with no API call.
- Full-detail and confirmation/review surfaces are exceptions: they must show the complete item list and must not collapse it behind `+ N more`.
- **Shared Order Preview** is one read-only reusable bottom-sheet/card surface. Select Order and Bills List must reuse it instead of creating separate preview UIs. It shows the shared Order Card with full item rows and no mutation actions.
- Booking list and Dashboard operational queues use the same reusable booking-summary card structure; context-specific bottom actions may differ while identity/date/item/lifecycle ordering stays consistent.

## Labeled section cards
- Where a separate section heading would waste vertical space, use a reusable labeled/notched section card.
- The section title sits in the card's top-left border notch with the same surface behind the text so the border does not run through the label.
- Examples include Customer, Details and Items when this pattern improves compactness.
- Do not force this treatment onto page titles or places where a normal standalone heading is clearer.

## Global Item display row

- Applicable Item rows/cards reuse one shared Item display component rather than rebuilding image/name/code/category/quantity per screen.
- Left: one canonical rounded Item thumbnail with the same placeholder/error fallback.
- Middle line 1: **Item Name**.
- Middle line 2: **Item Code · Category**.
- Middle line 3: optional contextual operational status. When quantity lifecycle data is available, keep the inline order/separator pattern such as **Booked 2 · Picked 0 · Returned 0**.
- Right: show quantity consistently as **× N** when that screen has a meaningful quantity.
- Keep thumbnail size, typography, spacing, divider treatment and narrow-screen behavior consistent. Hide line 3 when the screen has no useful contextual status rather than filling it with decorative text.
- Reuse the same row for Booking List/Dashboard, Booking Details/Pickup/Return, New Booking selected/review rows, Item Management and other applicable Item surfaces.

## Card images
- Item/product images shown inside cards use the same approved rounded-corner treatment.
- Keep image size/aspect ratio consistent by context and use crop/fill behavior without distortion.
- Placeholder/fallback images use the same rounded container; do not mix sharp square thumbnails with rounded card imagery.
- Every Item thumbnail with at least one real image is tappable and opens the one shared view-only Item Image Gallery.
- The gallery starts on the tapped image and is a **true edge-to-edge full-screen viewer** on a solid black background; the underlying Admin screen must not remain visually visible through the viewer.
- Images use **Fit / Contain** without crop or distortion. The viewer supports horizontal swipe across all already-bundled Item images, shows a compact `1 / N` counter, and closes through a compact top-right `X` or Android Back.
- Pinch-to-zoom, double-tap zoom/reset and pan are supported. At 1×, a one-finger horizontal drag belongs to the pager; transform handling starts only for multi-touch zoom or an already-zoomed image. While zoomed, pan owns the gesture; returning to 1× restores normal page swipe.
- Tapping the image toggles the compact top controls. Loading uses an in-view progress indicator and a failed image shows a readable fallback instead of a blank/broken surface.
- A single real image still opens enlarged; no unnecessary page arrows/dots are shown for a single image. A missing/placeholder-only image is not clickable.
- Gallery presentation is local-only: opening/swiping/zooming/panning/closing it must not create API calls, polling or mutations.

## Information rows: icon + value
- This is an **information/data-display rule, not a button rule**.
- Where a related Material Rounded icon makes a field self-explanatory, show the icon directly with the value instead of repeating a tall label/value stack.
- Examples: Person icon + customer name; Phone/Call icon + mobile number; Location icon + address; Calendar icon + date; Notes icon + notes.
- Use the same icon for the same information meaning throughout the app.
- Do not add decorative icons that do not clarify the value.
- Buttons are not globally required to use icon + label. Use the appropriate existing screen/action treatment; icon-only utility actions remain accessible.
- There is no global requirement to rename busy buttons to `Action...`. Duplicate mutations must still be prevented while a request is active.

## Booking summary identity order
- Compact booking summary/list cards show **Customer Name → Mobile Number → Booking Number** in that order.
- Booking number is secondary metadata rather than the largest heading.
- The card top-right area shows **Payment Status only**: Pending Payment, Part Payment or Full Payment. Reserved shows no payment badge. Lifecycle status is represented by the Current tile and must not be duplicated in the top-right area.

## Summary status badges
- Current/Next lifecycle summary information uses compact semantic badges with **icon + label + value on one line** and one reusable badge component.
- Examples: `Current · Reserved` and `Next · Confirmation Pending`.
- Current and Next use distinct semantic soft colors and consistent icons. They render side-by-side when width allows and stack responsively on narrow phones without changing meaning.
- Do not wrap the label/value into stacked two-line blocks. If two badges cannot fit side-by-side on a narrow phone, stack the badges vertically while keeping each badge itself single-line.
- In compact booking cards, Pickup/Return date badges appear above the item/lifecycle summary area and Current/Next badges appear below the item list.

- Compact Booking cards use one fixed top metadata row: **Pickup | Current | Return**. Each card uses the same four-line hierarchy: **icon + Label → icon + Status → icon + Date → icon + Day**. Pickup/Return status is `Pending | Done`; Current status is the lifecycle label. Time is not rendered in these cards.
- All three badges share the same compact visual family and divide available width equally. Their four lines are left-aligned with compact vertical spacing and consistent icons.
- Pickup keeps the shared blue/info family; Return keeps the shared amber/warning family. Current is lifecycle-sensitive and must use a color family not already used by Pickup or Return: Reserved = purple/category, Booked = brand/magenta, Part Picked Up = teal, Full Picked Up = violet, Part Return = neutral, Full Returned = green/success, Cancelled = red/error.
- The former lower **Current** badge is removed. A single full-width **Next** semantic badge remains below the item list when a next lifecycle action exists.
- Keep badge metadata readable without horizontal scrolling.

## Booking Pickup / Return date badges
- In card/summary views, Pickup and Return use compact rounded semantic badges.
- Each badge shows **calendar icon + label + `DD-MM-YYYY · FullDayName` on one line**.
- Pickup and Return use distinct soft semantic colors and the same reusable compact date/status badge pattern. They render side-by-side when width allows and stack on narrow phones.
- Never split the date/day inside one badge into two lines. On narrow phones, stack Pickup and Return badges vertically instead of wrapping their text.

## Compact search
- Reusable search fields use the compact visual treatment (about 52dp high) with icon/text vertically centered.
- Non-empty reusable search fields expose the built-in clear action; active remote/debounced search exposes a visible searching indicator.
- Search-driven screens use deterministic `Idle | Searching | Results | NoResult | Error` presentation where asynchronous search can otherwise leave an unsettled blank state.
- Avoid unnecessary vertical padding.
- The Booking List placeholder is exactly `Search`.

## Text inputs
- Every normal editable textbox/input field on new or actively edited screens uses a relevant Google Material Rounded leading icon.
- Every textbox has a generic task-oriented placeholder such as `Enter category name`, `Enter code prefix`, `Enter item name`, `Enter quantity` or `Enter display order`.
- Placeholders must never contain actual, sample or business-specific example data such as a real category name, code, quantity or order number.
- Keep the field label as the semantic field name; avoid redundant helper copy when label + generic placeholder already make the action clear.
- Search fields keep the Search icon and search-specific placeholder.
- Decorative leading icons use null accessibility descriptions when the field label already supplies the semantic meaning.
- NUMBER, DATE, DROPDOWN, MULTI_SELECT and YES_NO values use their proper controls; do not force them through a generic text box.
- Screens already locked by their screen contract are not retroactively restyled only because this global rule exists. Apply it when that screen is explicitly unlocked or actively edited.

## Numeric amount / quantity input
- Shared numeric amount/quantity text fields select the entire current value on first focus so the operator can type a replacement immediately; deleting the old value first is never required.
- This applies to Quantity, Total Quantity, Rent, Rent Rate, Discount Amount, Advance Amount and equivalent numeric amount/quantity fields that use the shared text-field family. Existing custom quantity steppers keep the same select-all-on-focus behavior.
- A cleared optional numeric amount may remain visually empty while editing and use its field placeholder; ViewModel/Worker normalization still decides the authoritative saved default.
- Booking `Advance Amount` defaults to actual value `0`. If the operator clears it, the placeholder is exactly **Advance Amount**. Do not show a helper/supporting label such as `Cash advance · default ₹0` below the field.

## Form validation / required fields
- Required fields use a visible `*` marker in the field label or equivalent selector label.
- Save/Create remains disabled while authoritative required values are invalid, but the UI also explains the problem beside the affected field instead of relying on a disabled button alone.
- Field-local validation appears after the operator interacts with an invalid text/number field; partially entered values show the specific rule, such as a 10-digit mobile or minimum 8-character password.
- Blank required values never silently coerce to a valid persisted value. If `0` is valid, the operator may enter `0` explicitly; blank remains invalid.
- Type-specific controls keep type-specific validation: numeric fields parse as numbers, dropdown/multi-select values must belong to configured options, dates remain valid ISO values behind the shared picker, and dynamic required custom fields retain their existing typed checks.
- ViewModel/Worker validation remains authoritative as a second layer. UI validation prevents avoidable requests but must not replace server-side business-rule validation.
- Server validation for an open editor should remain visible inside that editor when practical; do not show the same error simultaneously as both editor-inline feedback and a duplicate global popup.
## Shared date picker
- All active Admin Android DATE inputs reuse `AppDatePickerField`; screen-local `DatePickerDialog` implementations are not allowed for equivalent date fields.
- Stored/API date values remain ISO `yyyy-MM-dd`; visible field text uses `DD-MM-YYYY`, with optional full weekday where the screen benefits from it.
- The shared field supports optional minimum/maximum dates, clear behavior and the same 18dp calendar icon treatment.
- From/To ranges constrain each other when both sides are known: From cannot be after To and To cannot be before From. ViewModel/server validation remains authoritative as a second safety layer.
- Business-specific date rules remain intact. Booking Pickup cannot be before the business day and Return remains strictly after Pickup.
- Two-date layouts use the shared responsive pair so they stay side-by-side when practical and stack instead of clipping on narrow phones.
## Dropdown / option controls
- Generic form dropdowns reuse the shared selector family; Settings and dynamic Item forms align to the same visual/width behavior instead of one-off raw dropdown implementations.
- Filter sheets show **No options found.** when the current local filter query leaves zero options; pending/query presentation state is saveable where practical.
- Closed dropdown state shows a generic prompt before selection, e.g. `Select Category`, `Select Field Type`, `Select Option` or `Select Source Item`.
- The opened menu starts with the same prompt as a non-selectable placeholder row, followed by the real options.
- Required dropdowns cannot save while the prompt/empty value is still selected.
- Use a relevant Material Rounded leading icon and a clear dropdown arrow.
- The selected option is visibly identified in the menu with a check/highlight, not by color alone.
- Dropdown menus align to the parent field width where practical and must not appear as a narrow detached popup.
- Long option text remains readable with safe truncation/wrapping, and long option lists scroll inside the available screen bounds.
- Disabled/inactive options are visually muted and cannot be selected unless an edit flow explicitly preserves the record's already-saved legacy value.
- MULTI_SELECT uses a multi-option selector with clear selected checks and preserves earlier selections while more options are chosen.
- YES_NO uses a Yes/No selector, DATE uses a date picker, and NUMBER uses numeric input.
- Screens already locked by their screen contract are not retroactively restyled only because this global rule exists. Apply it when that screen is explicitly unlocked or actively edited.


## Incremental lists
- Do not show `Previous`, `Page X of Y`, or `Next` controls on normal scrolling lists.
- Initial batch: maximum 10 rows.
- When the user reaches the bottom and more rows exist, append the next batch of up to 10.
- Never replace already visible rows during load-more.
- Prevent duplicate load-more requests and duplicate rows.
- Stop requesting when the final page is reached.
- Search/filter/sort changes reset the list to the first batch.

## Global filter pattern

- List/workspace filters use the shared reusable bottom-sheet/modal filter pattern instead of raw overlapping dropdown menus.
- The closed filter control always shows the current selection. A non-default applied filter also shows a compact applied indicator/count where useful.
- Filter sheets use consistent **Cancel | Clear/Reset | Apply** actions. Clear/Reset restores that filter's existing screen default; Apply is the only action that commits the pending selection.
- Large option sets include in-sheet search. Search is local to the already-loaded option list unless the screen's existing contract explicitly requires server search.
- Reusing the global filter pattern must not change the screen-specific filter options, defaults, query semantics, pagination reset behavior or backend calls.
- Sort controls and ordinary form selectors are not list filters and keep their existing controls unless separately changed.

## Item selection and category filter
- New Booking item selection is search-first and uncluttered.
- Do not use an always-visible horizontally scrolling category-chip strip as the primary category filter.
- Use a compact Category filter control that opens a reusable bottom sheet with `All Categories` plus available categories and `Cancel | Apply`.
- Search works inside the active category filter; changing search/filter never discards already selected items.
- Result cards default to a simple `Select` action. A selected result is visibly highlighted and shows a `Selected` state rather than a separate duplicate Add flow.
- Quantity editing/removal belongs in the selected-item review area.
- `Selected Items (N)` is shown **after the search/result cards**, not above them.
- The same item cannot exist as duplicate booking lines; selection updates the existing line.
- Unavailable items cannot be selected and quantity cannot exceed authoritative availability.

## Customer mobile / Call / WhatsApp
- Admin WhatsApp uses one reusable central template system; screens must not carry separate hard-coded operational messages.
- For an Order/Booking WhatsApp action, the Worker derives the authoritative current status group and returns only active templates applicable to that group: **Reserved | Confirmed / Booked | Part Pickup | Picked Up | Part Return | Returned | Overdue | Cancelled**.
- Customer/general WhatsApp without a Booking uses **Other**, which contains general messages only. Order-status-specific templates must never be assigned to Other.
- Canonical flow: **WhatsApp Button → Loading → Current Status → Filtered Templates → Select Template → Message Preview → Open WhatsApp → Manual Send**.
- A WhatsApp tap must never silently exit. Loading must always resolve to exactly one visible outcome: template selection, direct preview, or a clear visible error.
- If exactly one applicable active template exists, skip template selection and open Message Preview directly. If none exists, show a clear action error and do not open WhatsApp.
- Show an immediate shared loading/preparing state after the WhatsApp button is tapped so the action never appears unresponsive; the same busy state blocks rapid/repeated taps.
- While rendering a status group, one invalid/empty/unresolved active template is skipped without blocking valid siblings. Return an error only when no valid applicable template remains.
- Public Website Item WhatsApp Inquiry uses the central **General Inquiry** template renderer; no public screen-local hard-coded WhatsApp message is allowed.
- Supported Admin Web Dashboard, Customers, Bookings, Pickup, Return and Reports reuse the same status-filtered selector/preview/error flow and must not bypass it with page-local direct `wa.me` links.
- WhatsApp template language is one global Settings value: Gujarati, English or Both. Every template stores both Gujarati and English content; Both renders Gujarati, one blank line, then English.
- Preferred placeholder syntax shown to operators is double-brace, for example `{{customer_name}}`, `{{booking_id}}`, `{{items}}`, `{{pickup_date}}`, `{{return_date}}`, `{{status}}`, `{{business_name}}`. The Worker also accepts the legacy single-brace form for backward compatibility.
- Unknown, action-inapplicable or unresolved placeholders are blocked before customer launch. The previewed message and the message passed to WhatsApp must be identical.
- Customer has one user-facing **Mobile Number** only.
- Alternative Mobile is removed from the current product UI and must not appear in forms, cards, details, chooser dialogs, search copy, or new business rules.
- Do not show a number chooser for Call or WhatsApp.
- Existing legacy database values, if any, are not exposed or used by the Admin Android UI.
- **Call is confirmation-first everywhere:** tapping Call opens the same shared compact confirmation bottom sheet; launch the dialer only after explicit `Call` confirmation. No applicable screen may direct-dial from the initial Call button.
- The shared Call popup uses exactly **Person/User icon → Customer Name**, **Call icon → Mobile Number**, then **Cancel | Call** with Cancel left and Call right.
- **WhatsApp is preview-first after template selection:** the shared preview shows Customer Name, Mobile Number and the full generated message; launch WhatsApp only after explicit `WhatsApp` confirmation.
- Contact confirmation sheets use the same approved visual family as New/Edit Customer: drag handle, title, close `X`, rounded top corners, compact content, and bottom actions.
- Invalid/blank Mobile Number must not launch an external app; use the shared transient error feedback.
- If the Phone or WhatsApp external intent cannot be opened, the action must resolve to shared visible error feedback rather than silently exiting.
- The current runtime contact surface is primary-Mobile-only; alternate-mobile parameters may exist only as dead-source compatibility and must not be wired into active UI.
- Prevent duplicate external launches from rapid/double taps.

## Keyboard / IME safety
- Opening the keyboard must not hide the focused field or the form's required action area.
- The activity uses resize-safe IME behavior and scrollable forms/sheets add IME padding where needed.
- Form bottom sheets open in the expanded state when their content requires it; do not leave the required footer clipped below the viewport.
- Keep the field body scrollable and keep the required bottom action row fixed above IME/system-navigation insets.
- `Cancel | Save` must remain visibly tappable above the device navigation bar with the keyboard open or closed.
- Forms must remain usable on normal and narrow phones with the keyboard open.
- Standard form keyboard navigation uses **Next** between fields and **Done** on the final field where appropriate.
- `Done` must preserve the current field value, clear focus and reliably close the software keyboard without closing the form/sheet or discarding entered values.
- Final-field Done handling reuses the shared reliable keyboard-dismiss flow: clear Compose focus, request keyboard hide, use the platform IME fallback, and repeat the hide after focus propagation when required by the device keyboard.
- Shared `AppTextField` is the enforcement point: single-line fields that do not explicitly request another IME action resolve to **Done**, and Done always runs the reliable dismiss flow before any caller-supplied Done action. Explicit **Next** remains unchanged.
- Shared `CompactSearchField` uses **Done** and the same reliable dismiss flow.
- Do not create screen-specific one-off keyboard-dismiss code when the shared reliable dismiss helper is applicable.
- For fields where multiline IME behavior prevents a reliable Done action, prefer an input configuration that preserves a dependable Done-to-hide-keyboard flow.
- Do not solve IME overlap by shrinking text or touch targets.
- Standard editable bottom sheets reuse shared `AppFormSheetScaffold` for title/close, scrollable body, fixed `Cancel | Save` footer, IME/system-navigation padding and busy-state action locking.
- While an async form mutation is busy, swipe/back/close dismissal must be blocked until the mutation completes.
- Read-only/detail sheets with potentially long dynamic content keep the body scrollable and the required `Close` action fixed above system-navigation insets.

## Source-aware subflow navigation
- Opening a New/Edit/Detail booking subflow records the logical source screen.
- `Cancel`, close, the visible Back action, and Android/system Back return to the screen from which the subflow was opened.
- Examples: Customers → New Booking returns to Customers; Bookings → New Booking returns to Bookings; Dashboard → New Booking/Booking Details returns to Dashboard.
- Successful completion that closes the subflow follows the same source-return rule unless a screen-specific contract explicitly overrides it.
- Preserve source-screen tab/search/sort/scroll state where practical; do not force an unnecessary Dashboard jump.
- Avoid duplicate logical destinations in the navigation stack.

## Mutation double-submit safety
- Protected async mutations must acquire their busy/action lock **synchronously before launching a coroutine/request**. Do not wait for the coroutine body or a later recomposition to set the lock.
- The same mutation entry point must immediately return while its lock is already active. Rapid/double taps must therefore collapse into one request.
- If a mutation requires authoritative refresh before the UI can safely be used again, keep the lock active until the refreshed state is fetched and applied. Do not show success while stale action controls remain active.
- Booking Pickup/Return specifically follow: submit mutation → fetch updated Booking Detail → apply/reset quantities/read-only state → show success → release busy lock.
- Repeatable local controls such as quantity `+ / -`, tabs, filters and Show/Hide are not mutation-debounced.
- Existing backend idempotency/request-key protection remains authoritative and complements, rather than replaces, the UI/ViewModel lock.
## Shared buttons and action states
- Reuse shared `PrimaryButton`, `SecondaryButton`, `DangerButton`, `DangerTextButton`, `SoftActionButton` and `CompactNewActionButton` instead of screen-local equivalents for the same action class.
- Primary = main commit/action; Secondary = non-destructive alternate; Danger = destructive confirmed action only.
- Primary/Secondary/Danger buttons keep a minimum **48dp** interactive height. Header utility/logout controls also use the **48dp** touch-target standard.
- Shared normal action icons are constrained to **18dp** so caller-supplied icons cannot make equivalent buttons visually inconsistent.
- When `loading = true`, the shared button shows an **18dp** progress indicator in the icon slot, keeps the action label stable/single-line, disables duplicate taps and does not require a screen-specific busy label.
- Single-line shared button/action labels use safe ellipsis rather than clipping on narrow phones or larger font scales.
- Generic New/Add actions use shared `CompactNewActionButton`; booking-specific wrappers may delegate to it but must not redefine its styling.
- Destructive confirmation actions use the shared danger treatment; ordinary Cancel/Back/Close remain neutral.
- When several async actions share one busy state, only the action actually in progress shows loading while sibling actions remain disabled.

## Responsive badges and action groups
- `StatusBadge` and `CompactMetaBadge` keep approved single-line text and use ellipsis rather than internal multi-line wrapping.
- When multiple badges cannot fit, move the whole badge to a responsive row/column arrangement instead of wrapping badge text.
- Reuse `ResponsiveCompactPair` for two compact badges/fields and the shared responsive action-grid/triple primitives for crowded action groups.
- `SoftActionButton` keeps its minimum 48dp touch target. Crowded card actions wrap into additional rows before labels become unreadably narrow.
- Responsive pair/triple decisions use the component's actual available content width; the shared side-by-side threshold is tuned so a normal 360dp phone does not stack unnecessarily, while a narrower 320dp layout may stack instead of clipping.
- Customer, Booking/Dashboard quick actions, Booking Details and other applicable card actions preserve semantic tones and action order while adapting column count to available width.
- Destructive actions retain danger treatment when regrouped; responsiveness must not change permissions, callbacks, busy-state locking or backend calls.
## Lifecycle action layout
- Lifecycle/action labels must remain single-line on phones.
- New/Edit Booking uses **Customer → Details → Items → Preview → Payment**. Preview is read-only review. Payment contains `Advance Amount`, then `Reserve | Confirm`, full-width `Directly Pickup`, and `Back | Cancel`.
- If a label would wrap, adjust grouping/padding/icon usage or move the action to its own row instead of splitting the text.
- `Directly Pickup` is the UI label for the existing direct-pickup business flow; backend booking/pickup records and idempotency behavior remain unchanged.

## Pickup / Return quantity editing
- Pickup and Return lifecycle rows use `− [editable quantity] +`.
- The middle quantity is manually editable with a numeric keyboard.
- Focusing the quantity selects the entire current value, so typing replaces the current value directly rather than appending to it.
- Minimum is `0`; maximum is the authoritative remaining Pickup/Return quantity for that item.
- Invalid and over-limit values are not committed.
- IME Done commits a valid value and uses the shared reliable keyboard-dismiss behavior.
- Existing `All`, plus and minus actions remain available.

## Confirmation / destructive action safety
- Reuse shared `AppConfirmDialog` for ordinary confirmations and `AppDestructiveConfirmDialog` for destructive confirmations instead of creating screen-local variants for the same pattern.
- Destructive dialogs use danger treatment, keep Cancel/Back neutral, block outside/back dismissal while the protected mutation is busy, and disable both confirm and dismiss actions while busy.
- Reversible actions must use reversible wording. Customer Archive is labelled **Archive**, not Delete; permanent removal is separately labelled **Delete permanently**.
- High-impact irreversible deletion may require an exact typed phrase. Customer permanent deletion requires **DELETE CUSTOMER** before `Delete permanently` is enabled.
- Customer permanent-delete copy states that the Customer master and eligible operational Booking/lifecycle history are permanently removed, while Billing financial history/snapshots are preserved. Customer list surfaces must not show the hidden FK tombstone.
- Do not add stacked confirmations when the current editor/action itself already establishes clear intent; Reset Password remains a dedicated form flow rather than adding a redundant confirmation.
- Specialized multi-choice confirmations may remain custom when they present materially different safe/destructive alternatives, such as Return `Keep Order Open | Close Remaining Items` and custom-field `Deactivate | Remove Data & Delete Field`.
## Messages
- Operational feedback uses one shared reusable transient surface for **Success | Error | Warning | Info** across existing and future applicable screens.
- Success = green + check icon; Error = red + error icon; Warning = amber + warning icon; Info = blue + info icon.
- All tones use the same fixed top-center placement below the Main Header through the shared `AppFeedbackHost` / `FeedbackMessage` surface; popup position must not depend on list scroll position or where a mutation control lives.
- **Transient popup feedback is reserved for an explicit user action/mutation and its result**: Save/Add/Edit/Delete, Archive/Restore, Booking Confirm/Cancel, Confirm Pickup/Return, Settings/Template mutations, explicit export/share actions, and failures produced by those actions.
- **Passive UI activity must never create or replay a transient popup**: page/screen open, initial or background read, tab switch, navigation change, expand/collapse, filter sheet open, filter/search/sort selection, local configuration/preset apply, or other presentation-only state changes.
- Passive state/information and passive read/load failures use the normal-layout reusable **InlineStatusMessage** (or an existing normal content/empty/loading state), not the popup feedback surface.
- Changing tabs/navigation/search/filter clears stale transient action feedback so an earlier mutation result cannot replay on the newly selected passive UI state.
- Booking Details Pickup state (Pickup completed. / No pickup has been recorded.) and Return state (Return completed. / No items are ready to return.) are inline status content only.
- Duplicate presentation of the same action/message must be suppressed; one mutation must not create both a form popup and a second page popup.
- Form-field validation stays inline with the form. When an editor sheet is open, its validation/API error is shown inside that sheet instead of simultaneously replaying the same error as the page popup.
- Success/error copy names the completed/failed action where practical, for example **Customer added successfully.**, **Customer updated successfully.**, **Preset saved.** or **User created.**.
- Keep copy concise and user-facing; never expose Worker/D1/endpoint/stack details.
- Raw server, network, DNS/host, exception, stack-trace, endpoint or infrastructure messages must never be rendered directly in visible UI.
- Network/DNS/host failure → **Unable to connect. Check your internet connection and try again.**
- Timeout → **Request timed out. Please try again.**
- Server/5xx → **Service is temporarily unavailable. Please try again.**
- Expired/unauthorized session → **Your session has expired. Please sign in again.**
- Permission failure → **You do not have permission to perform this action.**
- Approved validation/business-rule 4xx messages may remain visible when they are already user-facing and contain no technical implementation detail.
- Unknown technical failure → **Something went wrong. Please try again.**
- Technical details may remain available to diagnostics/logging but must not be copied into user-visible feedback.
- Optional **Retry / Undo** is shown only when the caller has a real safe action for it.
- The feedback is an overlay and does not permanently reserve page layout space. Initial loading and empty states remain normal screen content.

## Loading / empty / error / retry states
- Shared empty states distinguish normal-empty, search-no-result and filtered-no-result contexts and may expose one contextual action such as Clear Filters or Create New Customer.
- Initial/loading/search-loading states include a readable loading indication instead of a spinner-only unexplained state.
- Initial load: Loading → Content / Empty / Error + Retry. Do not leave a signed-in module with an unexplained blank content area while its first read is running or has failed.
- Load failure is not an empty result. `No ... found` is shown only after a successful load establishes that the authoritative result is empty.
- Reuse the shared `LoadFailureState` for first-load failures when a safe explicit retry exists; keep the normal Back/header path available so the operator is never trapped in a failed load.
- Refresh keeps already loaded content visible where practical. A manual/background refresh may show a refresh indicator and inline error, but must not replace valid content with an empty/loading-only state.
- Load-more appends to existing content. A load-more failure keeps already loaded rows visible, shows a passive inline error, and must stop automatic retry loops until an explicit refresh/filter/search action starts a new request.
- Initial totals/count labels are hidden until the corresponding dataset has loaded successfully; failure must not be presented as `0 records`.
- Booking Detail first open/edit shows its shared header plus loading state; on failure it shows Error + Retry. Retry must target the same booking request, not an unrelated bootstrap call.
- Passive load/read errors use normal-layout error/retry or inline status surfaces, never transient mutation success/error popups.
## Pull-to-refresh
- Applicable list/data screens support swipe-down pull-to-refresh using the shared pattern.
- Pull-to-refresh refreshes only the current screen/module data.
- Keep already-valid content visible while refresh is running where practical.
- Prevent duplicate refresh requests.
- A refresh failure must not unnecessarily destroy the previously valid visible dataset.
- Manual refresh does not add polling or periodic background requests.

## Date and time display
- Applicable signed-in module pages reuse the same compact **Date | Day | Time** row immediately below the fixed Main Header and before the page title/actions/content.
- Reuse the existing shared `MainScreenDateRow`; do not build one-off variants. Users, Settings and Reports follow the same component/visual sizing when opened from More.
- The shared row is intentionally compact: approximately **32dp** visual height, **12sp / 16sp** SemiBold text, **14dp** icons inside **24dp** soft-color icon boxes, **4dp** icon-to-text gap, **4dp** vertical row padding and **24dp** dividers.
- Date, Day and Time remain three equal-width centered cells.
- Date: `DD-MM-YYYY`.
- Day name: full English day name.
- Time: 12-hour clock with `am`/`pm`.
- The compact visual treatment must not add network/API polling. The Time cell uses a local minute-aligned clock update in the business timezone so it does not stay stale when the screen is otherwise idle.
- When a real timestamp exists and time is useful: `DD-MM-YYYY · FullDayName · h:mm am/pm`.
- When only a date exists: show date + full day name; never invent a time.
- Use actual audit/event timestamps for history instead of scheduled dates when recording what happened.

## Signed-in top spacing rhythm
- Applicable signed-in module pages use one compact vertical rhythm below the fixed Main Header: **8dp → Date | Day | Time → 8dp → Screen Title/Action → 8dp → first Tabs/Stepper/Search/Card content**.
- Use **8dp** top content padding and **8dp** normal item spacing for this top stack; do not insert empty feedback rows or placeholder items that create extra gaps.
- The More hub also shows the shared Date | Day | Time row before its **More** title so moving between primary and More screens does not change the top structure.
- Main Header refresh/logout action containers should remain visually height-aligned; compact visuals must not change the existing navigation semantics.

## Global compact tabs
- All applicable top-level tabs use the shared compact tab family and one visual system: **44dp** visual height, **3dp** selected indicator, primary selected color, muted unselected color and centered labels.
- Tab labels use **16sp / 20sp** typography; selected = **SemiBold**, unselected = **Medium**.
- Use `AppCompactFixedTabs` when all tabs fit without horizontal scrolling. The row fills the available width and divides it **equally** between tabs; each label is centered in its equal-width cell.
- Use `AppCompactScrollableTabs` when the tab set genuinely needs horizontal scrolling. The validated Run #79 behavior is authoritative: **0dp edge padding, 96dp minimum tab width**, 44dp height, shared divider/indicator, selected SemiBold and unselected Normal. Tab geometry/spacing/indicator behavior must not change; the shared tab label font is **16sp**.
- Fixed/non-scroll tabs fill the available width equally. Scrollable tabs preserve the Run #79 minimum-width behavior and may grow for longer labels.
- Customers and Category & Items use equal full-width tabs. Booking lifecycle, Reports, Settings main tabs and WhatsApp Centre use scrollable tabs. Booking Details uses scrollable tabs for **Details | Pickup | Return | Bill | History** with the validated **96dp minimum tab width**.
- Changing a tab remains a presentation/navigation action and must not itself create a transient success popup.


## Back / system navigation
- Android system/gesture Back and a visible Back control must resolve to the same logical parent/subflow state.
- More sub-pages **Category & Items**, **Reports**, **Users** and **Settings** use the shared compact `AppBackHeader` and return to the More hub.
- A nested editor must consume Back before its parent: for example, Related Items edit returns to the Related Groups list before leaving Category & Items.
- While a mutation/upload/export or other protected async action is busy, Back is consumed/blocked instead of falling through to Activity/app exit or leaving a half-completed action.
- Booking editor/detail Back remains source-aware. When the detail read itself is loading or has failed before content appears, Back still returns safely to the source screen.
- Leaving Settings with unsaved Basic/Public settings draft requires an explicit **Discard unsaved changes?** confirmation. Choosing Keep editing stays on Settings.

## General
- Admin Android remains English-only unless explicitly changed.
- Preserve comfortable touch targets even when visuals are compact.
- Use semantic colors/tokens and Google Material Rounded icons.
- Keep network/business state server-authoritative and avoid polling.

## Compact booking-card item expansion
- On Admin Android Dashboard and Booking List compact booking cards, show at most the first 2 item rows by default.
- If more items exist, show tappable `+ N more`.
- Expanded state shows all item rows in the same card and exposes `Collapse` / `Show less`.
- Collapse returns to the first 2 rows plus `+ N more`.
- Expand/collapse uses already-loaded state and must not trigger an API call.
- Booking Details and New Booking confirmation/review show all items and do not use this compact collapse pattern.
- This compact expansion rule is Admin Android-specific and does not silently redesign Admin Web.

## Screen title and Global New/Add action

- Applicable signed-in Admin Android page titles reuse the shared `AppScreenTitle` hierarchy: **22sp / 28sp / Bold**, single-line with ellipsis only when width is constrained.
- Dashboard, Customers, Bookings, More and New/Edit Booking use `AppScreenTitle`; Booking Details and More sub-pages reuse the same title through `AppBackHeader`.
- Compact tab labels follow the shared Run #79 tab components. Scrollable tabs use the validated 96dp minimum width and 0dp edge padding; screens must not reintroduce one-off content-width tab rows.
- Back-header arrow glyph is **20dp**; existing accessible Back interaction behavior remains unchanged.
- Reusable top primary New/Add actions use one compact content-fit `CompactNewActionButton`.
- New/Add action text is intentionally subordinate to the page title: **14sp / 20sp / SemiBold**, with a **16dp** Add icon, **10dp** horizontal / **6dp** vertical internal padding and **4dp** icon-to-label gap.
- Icon + label are visually centered with equal left/right padding; do not reserve excessive trailing width.
- In a title/action header row, the title stays left with flexible width and the primary New/Add action aligns to the far right.
- Reuse this component for New Booking, New Customer, New Category, New Item, New Group, Add User, Add Template and equivalent actions; do not create one-off variants for the same purpose.
- Preserve responsive behavior, semantics and comfortable interaction targets even while the visual treatment is compact.

## Dashboard locked shared actions
- Dashboard has no separate page-level Refresh action; use the shared Main Header refresh.
- Dashboard operational sections are Missed Pickups → Today Pickups → Today Returns → Overdue Returns.
- Dashboard/Booking shared quick actions use icon + visible label: View | Edit | Call | WhatsApp where valid.
- Dashboard must not show a separate Pending Pickup badge.


## Typography semantic hierarchy
- Page title: **22sp**.
- Section heading: approximately **18–20sp**.
- Card title: **16sp**.
- Body: **14–16sp**.
- Metadata: **12–13sp**.
- Action labels: **12–14sp**.
- **11sp** is reserved for genuinely tiny metadata.
- Shared `titleSmall`, `bodySmall`, `labelMedium` and `labelSmall` roles are explicitly defined so Material defaults do not silently change hierarchy.


## Step 3–7 interaction hardening
- Editable forms use one reusable dirty-state Back/dismiss rule: untouched forms close immediately; changed forms show **Discard unsaved changes?** with **Keep Editing | Discard**; protected busy mutations cannot be dismissed.
- This dirty-state rule applies to Booking New/Edit, Customer Add/Edit, Category/Custom Field/Item editors, Related Group editing, User Add/Edit, typed Reset Password, WhatsApp Template Add/Edit and Bill Edit. Read-only views/previews do not add redundant confirmation.
- Initial-load failure and successful-empty state are distinct. Never render an API failure as a successful empty list.
- Infinite-scroll failure preserves already loaded rows and shows an inline **Retry loading more** action for the failed next page; it must not erase page 1 or auto-loop requests.
- Item thumbnails use the shared full-screen gallery wherever item images are shown. Gallery data may be fetched/returned as URL metadata; binary image data is never bundled into screen bootstrap payloads.
- Multiline inputs keep newline behavior. Screens provide an explicit keyboard-dismiss path, and primary Save/Finalize actions dismiss the keyboard before mutation.
- Horizontal chip/action rows must bring the current selected value into view when an editor opens, without introducing swipe-to-change-tab behavior.
