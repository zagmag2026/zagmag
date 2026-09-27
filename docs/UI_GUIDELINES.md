# Zhagmag Dresses — Admin Android UI Guidelines

This file is the Android Admin app-specific UI authority for `apps/admin-android/`. When it conflicts with the generic `UI_DESIGN_SYSTEM.md`, this file takes precedence. `GLOBAL_UI_RULES.md` contains the latest cross-screen approved rules and takes precedence for an explicitly approved global pattern.

## Core principles
- Admin Android UI is **English only**.
- Keep screens vertically compact wherever practical without sacrificing readability or touch targets.
- Do not add explanatory/helper cards that are not required to complete an action.
- Reuse shared components and semantic tokens; do not create one-off styling per screen.
- Keep business state server-authoritative. UI must not invent booking/inventory state.
- Reuse the established visual style, spacing, card treatment and navigation pattern.

## Text policy
Allowed UI text includes page/section titles, field labels, actions, navigation labels, status badges, concise validation/errors/success acknowledgement and essential record metadata.

Do not add tutorial copy, implementation details, `Auto search`, `How this works`, paging explanations, internal roadmap text, or Gujarati strings in Admin Android unless explicitly approved later.
Raw server/network/DNS/exception/stack/endpoint text is never user-facing; map failures to the concise global error messages defined in `GLOBAL_UI_RULES.md`.

## Compact-height policy
Prefer compact headers, cards, search/input fields, rows, badges, action groups and section gaps. Avoid oversized hero headers, repeated nested padding and tall cards containing little information. Compact visuals must retain adequate tappable areas and readable text.

## Page header
- Respect system bars and insets.
- Keep titles and functional context compact.
- List/dashboard manual refresh uses the standard Material Rounded Refresh icon when useful.
- Header refresh reloads only current screen/module data and must not create polling.
- Do not add refresh to forms where it has no useful purpose.
- Icon-only utilities require accessibility content descriptions.

## Cards, list rows and information values
- Reuse `AppCard` or the current shared card treatment.
- Prefer one compact hierarchy rather than nested cards.
- Key identity/status first, then essential metadata, item/date information and actions.
- Equivalent record cards keep action placement consistent.
- Operational customer rows preserve Call/WhatsApp quick actions where required.
- For ordinary record information, follow the global **related icon + value** pattern when it improves scanability: Person icon + name, Phone/Call icon + mobile number, Location icon + address, Calendar icon + date, Notes icon + notes.
- The icon + value rule describes **information display**, not a mandatory button layout.
- Customer cards place `Total Booking` and `Active Booking` badges in one compact horizontal row directly below Address where width permits.
- Booking summary cards use the identity order **Customer Name → Mobile Number → Booking Number**, with status as a compact badge.
- Compact summary/list cards show first two item rows plus `+ N more`; confirmation/review/detail surfaces show all items.
- Item/product thumbnails in cards use the same rounded-corner crop treatment, including placeholders.
- Current/Next summary state and Pickup/Return date summaries use the global compact single-line icon badge patterns.
- A labeled/notched section card may replace a separate section heading where it reduces unnecessary height without harming clarity.
- Do not add decorative icons with no semantic value.

## Search
Default search for normally sized already-loaded datasets is local/instant and must not create an API request on every keystroke. Clearing restores the loaded list. A separately approved large server-paginated module may use server-side search. Search/filter/sort changes on incremental lists reset to the first approved batch.

## Incremental lists
- Normal scrolling lists do not show Previous/Page/Next controls.
- Initial load is at most 10 rows where the global +10 rule applies.
- Reaching the bottom appends the next batch of up to 10.
- Prevent duplicate load-more requests/rows and stop after exhaustion.
- Keep already visible rows while appending.

## Pull-to-refresh
- Applicable list/data screens support swipe-down refresh.
- Refresh only current module data; no polling.
- Keep valid content visible during refresh where practical.
- Prevent duplicate refresh requests.
- A failed refresh should retain the previous valid data instead of unnecessarily blanking the screen.

## Forms and IME
- Use shared `AppTextField`/form components.
- Keep fields compact and readable.
- Opening the keyboard must not hide the focused field or the required form action area.
- Activity-level resize behavior plus scrollable/IME-padded form containers should be used where needed.
- Approved bottom-sheet forms open fully enough to expose their content and keep the field body scrollable.
- Keep the commit/cancel footer fixed above keyboard and system-navigation insets; `Cancel | Save` must not be clipped behind the device navigation bar.
- Standard sequential fields use IME **Next**; the final field uses **Done** where appropriate. Done preserves the value, clears focus, reliably hides the keyboard, and must not close the form/sheet.
- `AppTextField` centrally resolves unspecified single-line IME actions to **Done** and wraps Done with the shared reliable dismiss flow before invoking any custom Done action. Explicit **Next** remains explicit.
- `CompactSearchField` also uses **Done** with the same shared dismiss flow.
- Reuse the shared reliable keyboard-dismiss helper (Compose focus clear + keyboard hide + platform IME fallback, including a post-focus hide retry where needed) instead of screen-local dismiss logic.
- If a multiline configuration causes unreliable Done behavior on common Android keyboards, use a reliable final-field input configuration instead of leaving the keyboard stuck open.
- Preserve entered values after recoverable request failures.
- Validation is concise and shown only when needed.
- Prevent accidental double-submit on async mutations.
- Required fields follow natural business order; optional fields later.
- Editing reuses the existing create/detail form where practical with values prefilled.

## Customer contact
- Customer has one user-facing Mobile Number.
- Alternative Mobile is removed from the current product UI.
- No Call/WhatsApp number chooser is shown.
- Existing legacy alternative values are not surfaced or used by Admin Android.
- Call must show the shared confirmation bottom sheet before opening the dialer.
- WhatsApp must show the shared full-message preview bottom sheet before opening WhatsApp.
- Contact sheets reuse the approved New/Edit Customer bottom-sheet family and keep actions inset-safe.
- Invalid mobile data never launches an external intent; use transient error feedback.
- Double taps must not launch the same external action twice.

## Buttons and actions
- Primary treatment = the main commit action for the current state.
- Secondary/outline = non-destructive alternate action.
- Danger = destructive confirmed action only.
- Async mutations must prevent duplicate submissions.
- Labels stay short, functional and consistent for the same behavior.
- Lifecycle/action labels must remain single-line; regroup actions instead of allowing awkward word wrapping.
- New Booking confirmation uses `Reserve | Confirm`, then a full-width `Directly Pickup`, then `Back | Cancel`.
- Google Material Rounded icons remain the app icon family.
- Icon-only utility actions are allowed when standard/clear and must have accessibility semantics.
- **There is no global rule requiring every normal button to be icon + label.** Use the appropriate existing component/screen treatment.
- **There is no global rule requiring busy labels such as `Save...`, `Confirm...` or `Action...`.** Busy-state presentation may remain component-specific while duplicate taps are blocked.
- Critical/ambiguous actions must remain clear in text even if an icon is also used.

## Badges and status
Use `StatusBadge` and semantic mapping consistently. Booking lifecycle wording is:

`Reserved → Booked → Part Pickup → Full Pickup → Part Return → Full Return`

`Cancelled` is a terminal state before pickup. Status is derived from server state/quantities, not manually selected by the operator.

## Booking lifecycle
Manage a booking from one compact lifecycle flow. New booking follows the approved four-step Screen 4 contract. `Directly Pickup` remains one operator flow while preserving separate booking and pickup records/audit history. After a successful lifecycle mutation, the same booking UI should reflect updated server-authoritative status/quantities/actions without forcing unnecessary reopen/navigation.

Pickup/Return quantity controls must never accept more than the current valid remaining quantity.

## Item selection
- New Booking Step 3 is search-first; do not use a permanently visible horizontally scrolling category-chip strip as the primary filter.
- A compact Category control opens a bottom sheet with `All Categories`, dynamic categories and `Cancel | Apply`.
- Selected items survive search/category changes.
- Result cards use `Select` / `Selected` state; unavailable items cannot be selected.
- Quantity and remove controls live in the selected-item review area.
- `Selected Items (N)` appears after the visible item results.

## Navigation
- Preserve the stable bottom-level navigation unless separately approved.
- Detail/edit/form/pickup/return screens are subflows, not bottom-nav destinations.
- Back behavior is predictable and role restrictions are respected.
- Navigation is cache-first when already-valid data exists.
- Avoid duplicate logical destinations in the stack.
- Customer-card `Booking` opens New Booking with that customer already selected in Step 1 and Next enabled; do not force a second manual customer selection.
- A subflow returns to the screen that opened it: Customers → booking flow → Customers, Bookings → booking flow → Bookings, Dashboard → booking flow → Dashboard.
- Cancel, close, system Back and successful flow completion follow the same source-return rule unless a screen-specific contract explicitly overrides it.
- Preserve source-screen tab/search/sort/scroll state where practical instead of forcing a Dashboard reset.

## Dialogs and sheets
- Keep confirmation dialogs compact: title, essential content, actions.
- Destructive actions require explicit confirmation/danger treatment.
- Avoid unnecessary stacked confirmations.
- Reusable forms use shared `AppFormSheetScaffold` when the standard title/body/`Cancel | Save` editor pattern applies; such sheets must be scrollable, expanded when necessary and IME/system-inset safe.
- During an async mutation, form-sheet swipe/back/close dismissal stays blocked until the busy state clears.
- Long read-only/detail sheets keep their content body scrollable and their required `Close` action fixed instead of allowing it to fall below a short viewport.
- Call confirmation and WhatsApp message preview use the same shared bottom-sheet family as New/Edit Customer rather than one-off dialogs.

## Loading, empty and operational messages
Reuse shared loading/empty/message components.
- Full loading is for initial critical load when no valid content exists.
- Small refresh/action operations should not unnecessarily hide the entire screen.
- Operational success/error/warning/info acknowledgement from an **explicit action/mutation** uses the shared transient popup/snackbar-style message surface.
- Passive page load, screen/tab/navigation changes, expand/collapse, filter opening/selection, search/sort changes and other presentation-only state changes do **not** create a popup.
- Passive state/information and passive read/load failures use the shared normal-layout **InlineStatusMessage** or an existing loading/empty/content treatment.
- Clear stale transient action feedback when moving into another tab/navigation/filter/search state so old success/error acknowledgements cannot replay.
- Messages are concise and do not expose Worker/D1/endpoint/internal details.
- Refresh failure retains prior valid content where practical and is inline unless the failure belongs to an explicit mutation/action.

## Date fields
- Reuse `AppDatePickerField` for active Admin Android date inputs instead of maintaining screen-local calendar dialogs.
- Keep ISO `yyyy-MM-dd` internally/API-side and show readable `DD-MM-YYYY` in the field; Booking may also show the full weekday.
- Apply min/max range constraints in the picker where the screen already has those rules, and keep ViewModel validation as a second guard.
- From/To pairs use the shared responsive pair and must not accept a reversed range.
## Responsive behavior
- Action groups with several compact actions use the shared responsive grid/triple layout; keep 44dp touch targets and move actions to another row before text becomes too narrow.
- Single-line badges remain single-line with ellipsis; if multiple badges do not fit, stack/regroup the badges rather than wrapping their text.
- Phone-first, tablet-compatible, same adaptive structure.
- No horizontal clipping.
- Important text may wrap where content genuinely requires it, but approved single-line badges/action labels must adapt layout instead of wrapping.
- Headers/actions must remain collision-free.
- Forms remain IME-safe.
- Avoid unnecessary fixed widths.

## Icon rules
- Use Google Material Rounded consistently.
- Same information/action meaning uses the same icon.
- Icon sizes remain consistent by context.
- Do not mix unrelated families or decorative emoji-style icons.
- Icon-only interactive controls require accessibility descriptions/tooltips where appropriate.
- Information rows use icon + actual value where the global rule applies.
- Buttons are **not** globally forced into icon + name layout.

## Accessibility
Maintain contrast, legible text, practical touch targets, content descriptions for icon-only controls, clear disabled state and support for system font scaling. Do not rely on color alone for status/error meaning.

## API/refresh discipline
- No automatic polling or hidden periodic refresh.
- No duplicate request on recomposition.
- Avoid one-request-per-item when a bundled endpoint exists.
- Manual/pull refresh only where useful.
- Search typing should not cause unnecessary network calls.
- Navigation is cache-first.
- Duplicate taps must not create duplicate mutations or duplicate external launches.
- After successful mutations, update local visible state from the response where practical instead of forcing unnecessary full reloads.

## Reusable component rule
Before adding screen-specific UI, check whether it belongs in shared components such as `AppPageHeader`, `AppCard`, `LabeledSectionCard`, rounded item-image treatment, `InfoValueRow`, `AppTextField`, `CompactSearchField`, button components, `StatusBadge`, the shared transient message surface, customer/contact confirmation sheets, `EmptyState` and `LoadingState`. Extend a shared component rather than copy-pasting a slightly different local version.

## UI completion audit
Before an Android UI batch is complete, verify:
1. English-only visible UI and no unnecessary helper copy.
2. Compact, readable and touch-safe layout using shared tokens/components.
3. Related record information uses the approved icon + value pattern where appropriate.
4. Summary cards use approved identity order, rounded images, +N rule, single-line summary/date badges and complete-detail exceptions.
5. Buttons are clear without applying an invented global icon+label/busy-text mandate; lifecycle labels do not wrap.
6. Search/incremental-list behavior does not cause unnecessary API calls.
7. Applicable screens support working swipe-down refresh and retain valid data during refresh where practical.
8. Forms open correctly with the keyboard, keep focused field/footer visible, and use reliable Next/Done behavior.
9. Operational success/error feedback is transient popup-style rather than permanent inline cards.
10. Customer UI exposes only one Mobile Number and no alternative-number chooser.
11. Call is confirmation-first and WhatsApp is full-message-preview-first on applicable contact actions.
12. Customer-card Booking enters New Booking with the selected customer already applied and closes back to Customers.
13. Semantic statuses/badges and server-authoritative lifecycle behavior remain correct.
14. No horizontal clipping; narrow-phone and normal-phone layouts are audited.
15. Async mutations/external launches are duplicate-submit safe; no polling/duplicate recomposition calls.
16. Role restrictions, system bars/insets, font scaling and accessibility semantics are preserved.
17. Shared components are extended/reused rather than copied into one-off variants.

A UI batch is not complete until the relevant checks pass.

## Compact booking-card expansion
- Dashboard and Booking List compact cards show first 2 item rows, then tappable `+ N more` when needed.
- Expanded cards must provide `Collapse` / `Show less` and return to first 2 rows when collapsed.
- This is local Compose state only; no API call is allowed for expand/collapse.
- Booking Details and New Booking review show all items.

## Locked-screen guardrail
- Screens 1–5 and 9–10 are finalized/locked at their currently approved contracts. Screen 8 Reports is implemented and validation pending. Screens 6–7 remain intentionally skipped.
- Do not change a locked screen unless the user explicitly unlocks or requests a specific change.
