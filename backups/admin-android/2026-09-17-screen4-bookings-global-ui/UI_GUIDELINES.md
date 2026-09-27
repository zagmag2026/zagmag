# Zhagmag Dresses — Admin Android UI Guidelines

This file is the Android Admin app-specific UI authority for `apps/admin-android/`.

When an Android Admin UI rule here conflicts with the generic `UI_DESIGN_SYSTEM.md`, this file takes precedence. Shared color, typography, shape and component tokens still come from `UI_DESIGN_SYSTEM.md` unless this file explicitly overrides their use.

## Core principles

- Admin Android UI is **English only**.
- Keep screens vertically compact wherever practical.
- Do not show explanatory, instructional or helper labels/cards that are not required to complete an action.
- Preserve readability, accessibility and practical touch targets while reducing wasted vertical space.
- Reuse shared components and semantic tokens; do not introduce one-off styling per screen.
- Keep business state server-authoritative. UI must not invent or manually override booking/inventory state.
- When creating a new screen, reuse the established visual style and shared components from existing screens. Do not introduce a new independent visual style, spacing system, card treatment, button style or navigation pattern unless explicitly approved.

## Text policy

Allowed UI text:
- page and section titles,
- field labels,
- button/action names,
- navigation labels,
- status badges,
- validation messages,
- error messages,
- concise success notices when an action needs acknowledgement,
- essential record metadata.

Do not add:
- general helper paragraphs,
- tutorial/explanation cards,
- labels such as `Auto search`, `How this works`, `Server-side paging`, `Planned next batch`, or similar internal/explanatory copy,
- duplicated descriptions that repeat the action/button meaning,
- Gujarati strings or language selectors in the Admin Android app unless explicitly re-approved later.

A form should normally read as **label/input → validation/error if needed → action**.

The UI must remain understandable after unnecessary explanatory text is removed. Users should be able to understand what to do from the fields, actions, icons, status and layout itself. Do not compensate for a weak or confusing UI by adding helper paragraphs or info cards.

## Compact-height policy

Use the smallest appropriate shared spacing token from `AppSpacing` while keeping the UI usable.

Prefer:
- compact page headers,
- compact cards,
- compact search/input fields,
- compact list rows,
- compact badges/chips,
- compact button groups,
- compact bottom navigation,
- small but consistent section gaps.

Avoid:
- oversized hero headers,
- large empty top/bottom padding,
- repeated card padding inside already padded containers,
- tall cards containing only one or two short values,
- large descriptive blocks between functional controls.

Touch interaction must remain comfortable. Do not shrink interactive hit areas so far that normal phone use becomes difficult. Where a visual control is compact, preserve an adequate tappable area around it.

Fit more useful content on a screen where practical, but do not achieve compactness by shrinking text, reducing readability or making touch areas impractical. Reduce height primarily through spacing, padding, layout hierarchy and removal of unnecessary elements.

## Page header

- Respect system bars; edge-to-edge screens must use the required status-bar inset handling.
- Keep title and optional functional context compact.
- Do not place long explanatory subtitles under every title.
- Narrow screens must not force title/action collisions.
- Back/close actions should remain easy to reach without increasing header height unnecessarily.
- Detail and form screens should use a consistent structure: **Back + Title + optional status/action**.
- List/dashboard screens should keep title alignment and spacing consistent with the rest of the app.
- Where manual refresh is useful, show a standard Google Material Rounded `Refresh` icon on the right side of the header rather than a text button.
- Header refresh reloads only the current screen/module data.
- While refresh is active, prevent duplicate refresh requests and show a disabled/busy state as appropriate.
- Do not add polling because a refresh icon exists.
- Do not show a refresh icon on forms/edit screens where it has no useful purpose.
- Icon-only refresh must expose an accessibility content description such as `Refresh`.

## Cards and list rows

- Reuse `AppCard` or the current shared card treatment.
- Prefer one compact information hierarchy rather than nested cards.
- Keep key information visible first: name/number, status, dates or operational counts, then actions.
- Avoid duplicate labels when the value is already self-evident from the section.
- Use icon-only actions only when the icon meaning is standard and accessible; otherwise use concise text.
- Operational customer rows should preserve Call and WhatsApp quick actions where required by module rules.
- Card/list action placement must be consistent across equivalent record types. Do not randomly place the same type of actions on the right in one screen and at the bottom in another.
- Alternate action placement is allowed only when content or responsive layout genuinely requires it.

## Search

Default search behavior for normally sized loaded lists is local and instant:
- Load the required list/data once for the screen.
- Search typing filters the already-loaded/cached data locally.
- Search typing must not trigger Cloudflare/API requests.
- Matching results display immediately while the user types; no separate Search/Submit button is required.
- Clearing the query immediately restores the full loaded list.
- Do not show an `Auto search` helper label.
- Search-state transitions must not present stale results as if they matched the current query.
- Manual Refresh or an explicit reload may fetch fresh data and then replace/update the local searchable dataset.
- Do not add polling or timer-based refresh loops.

Exception: for a genuinely large server-paginated dataset, do not load the entire database solely to enable local search. Use the separately approved server-side search/pagination pattern for that module.

## Forms

- Use shared `AppTextField`/form components.
- Keep fields compact but readable.
- Keep the focused field and primary action accessible above the IME.
- Preserve user-entered values after recoverable request failures.
- Validation appears only when needed and should be concise.
- Do not use persistent helper text merely to explain ordinary fields.
- Date, dropdown and quantity controls should use reusable components when available rather than screen-specific patterns.
- Prevent accidental double-submit on async actions.
- Arrange fields in the natural business-flow order so the operator does not need to move unnecessarily up and down the form.
- Put required fields in the logical sequence first and optional fields later.

Booking example:

`Customer → Pickup Date → Return Date → Category → Item → Qty → Notes`

### Edit behavior

Editing must happen in the same existing form/screen used for create/detail where practical. Do not create a separate duplicate Edit screen or a second independent form pattern.

When editing an existing record:
- open the same form in edit mode,
- prefill the existing values,
- keep the same field order, layout and shared components,
- preserve the same validation and action conventions.

This applies to modules such as Customer, Item, Booking, Category and other equivalent forms.

## Buttons and actions

- Primary button = main commit action for the current state.
- Secondary/outline button = non-destructive alternate action.
- Danger treatment = destructive confirmed action only.
- Disable or show busy state while an async mutation is in progress.
- Keep action labels short and functional.
- Prefer a small number of context-relevant actions over showing every possible action at once.
- Use the same label and the same icon for the same action throughout the app. Do not randomly use labels such as `Save`, `Submit` and `Done` for the same behavior.
- Standard action icons may be icon-only when their meaning is clear, but every icon-only action must provide a tooltip and accessibility description.
- Normal buttons should use **icon + action name** where the action is presented as a button.
- Critical or ambiguous actions should use clear text rather than relying on icon meaning alone.
- Use **Google Material Rounded** icons consistently throughout the Admin Android app.

## Badges and status

Use `StatusBadge` and the existing semantic mapping. The same status meaning must use the same badge treatment everywhere.

Booking lifecycle labels are:

`Reserved → Booked → Part Pickup → Full Pickup → Part Return → Full Return`

`Cancelled` is a separate terminal state.

Status is derived from server state and quantities; users do not manually choose lifecycle status from a dropdown.

Status badges should remain compact and single-line where practical. Prefer showing the badge on the same title/card row when space allows rather than creating a separate tall status row or card.

## Single booking lifecycle screen

A booking should be managed from one compact booking form/detail flow rather than forcing the operator to re-enter the same customer/items across separate disconnected forms.

New booking fields should remain compact and functional:

`Customer → Pickup Date → Return Date → Category → Item → Qty → Notes (optional)`

Context actions:
- New: `Reserve`, `Confirm Booking`, `Direct Pickup` where valid.
- Reserved: confirm/edit/cancel actions as allowed.
- Booked: pickup actions.
- Part Pickup: remaining pickup and valid return actions.
- Full Pickup: return actions.
- Part Return: remaining return actions.
- Full Return: completed/read-only state.

Direct Pickup remains one operator flow while preserving separate booking and pickup server records/audit history.

Do not add an explanatory `Booking + pickup at the same time` info card; the action labels themselves must communicate the workflow.

After a successful lifecycle action, the same booking screen must immediately reflect the updated status, quantities and currently valid actions. The operator should not need to navigate back and reopen the booking merely to see the new state.

## Booking item quantities

For lifecycle/detail views, keep item quantity information compact and consistent. When useful, show operational quantities such as:
- Booked,
- Picked,
- Returned,
- Remaining.

Do not display redundant quantity labels when the section context already makes the meaning clear.

For Pickup/Return quantity actions:
- the maximum currently valid quantity may be prefilled by default,
- the operator may reduce it to a smaller valid quantity,
- a quantity greater than the currently allowed/remaining amount must not be accepted,
- validation should be immediate and concise.

## Navigation

- Preserve current bottom-level navigation structure unless a separately approved module change requires otherwise.
- Back navigation must be predictable.
- Do not expose role-restricted destinations as usable actions to roles without permission.
- Use cache-first navigation: when the user returns to a screen and already-valid data is available, show it immediately rather than forcing a fresh request.
- Fetch fresh data only when it is genuinely required by staleness, explicit refresh, mutation consistency or another approved condition.
- Keep bottom navigation for stable main modules only.
- Do not add bottom-nav destinations for detail, edit, form, pickup, return or other sub-screens; open those from their parent module.
- Avoid accidental duplicate screen-stack entries when navigating repeatedly to the same logical destination.

## Dialogs

- Keep dialogs compact.
- Title, essential content, actions only.
- Do not add explanatory paragraphs unless they are required to prevent a destructive or irreversible mistake.
- Destructive actions require explicit confirmation and danger styling.
- Avoid stacking multiple confirmations for normal non-destructive actions.
- If an action can be handled safely inline on the current screen, do not open an unnecessary dialog.
- Use dialogs primarily for confirmation, small selections or focused short input.
- Do not place full forms inside dialogs; use the normal reusable screen/form pattern instead.

## Loading, empty and error states

Reuse shared components:
- `LoadingState`,
- `EmptyState`,
- `InlineMessage`.

Messages must be short and actionable. Do not expose implementation details such as Worker/D1/internal endpoint names to normal users.

- Use a full-screen loading state only for an initial critical load when there is no valid content to show.
- For small refresh/action operations, do not unnecessarily block or hide the entire screen.
- If valid data is already visible, keep it visible during refresh where practical.
- If refresh/background reload fails, retain the previous valid data instead of clearing the screen.
- Show a small concise error and a `Retry` action where retry is useful.

## Responsive behavior

- Phone-first, tablet-compatible.
- No horizontal clipping.
- Text may wrap when necessary but avoid creating tall cards through unnecessary copy.
- Header/action layouts must remain collision-free on narrow screens.
- Forms must remain IME-safe.
- Avoid unnecessary fixed widths that fail on narrow or large screens.
- Do not create an independent tablet UI design. Use the same screen structure and adapt layout responsively.
- Larger screens may use wider content, multiple columns or available space more efficiently while preserving the same navigation, action placement, component style and hierarchy.

## Icon rules

- Use **Google Material Rounded** icons consistently throughout the Admin Android app.
- Keep icon sizes consistent by context, typically using the shared 18/20/24 dp conventions where appropriate.
- Do not mix unrelated icon families, custom random icon styles or emoji-style action icons.
- Use the same icon for the same action throughout the app.
- Action icon = icon-only + tooltip + accessibility description when icon-only presentation is appropriate.
- Button = icon + name for normal button presentation.
- Icons must not be used purely as decoration when they add visual noise or height.
- If no clear Material Rounded icon exists for an action, do not invent or choose an ambiguous icon. Use a clear icon + text button or a clear text action instead.

## Accessibility

Compact does not mean cramped.
- Maintain readable contrast.
- Keep text legible.
- Preserve adequate touch targets.
- Provide accessible semantics/content descriptions for icon-only interactive controls.
- Tooltip text does not replace the accessibility label/content description.
- Do not rely on color alone to communicate booking status or errors.
- Disabled state must be visually clear.
- Do not hide important actions behind tiny or ambiguous icons.
- Respect system font scaling. If the user increases font size, important text must not be cut off or overlap.
- Allow rows/cards to grow or text to wrap when necessary rather than truncating important information.

## API/refresh discipline

UI design must support the project’s limited-call policy:
- no automatic polling,
- no hidden periodic refresh,
- no duplicate request on recomposition,
- avoid one-request-per-item patterns when a bundled endpoint exists,
- manual refresh only where useful,
- default list search uses local filtering of loaded/cached data with zero API calls while typing,
- navigation is cache-first when already-valid data exists,
- refresh should fetch only the data needed by the current screen/module,
- duplicate taps must not create duplicate mutations or requests.

After successful create/edit/delete/pickup/return operations, update the local screen/list state directly from the successful response where practical. Do not require a full list reload after every mutation when the returned result is sufficient to keep the visible state correct.

## Reusable component rule

Before adding new screen-specific UI code, check whether the need belongs in a shared component. Prefer extending shared components such as:
- `AppPageHeader`,
- `AppCard`,
- `AppTextField`,
- `PrimaryButton`,
- `SecondaryButton`,
- `StatusBadge`,
- `InlineMessage`,
- `EmptyState`,
- `LoadingState`.

New shared patterns should be added once and reused rather than copied into multiple screens.

Do not copy-paste a shared component into a screen and create a slightly different local version. If a legitimate variation is required, add an appropriate parameter/variant to the shared component and reuse it.

## UI completion audit

Before considering an Android UI batch complete, verify:
1. English-only visible UI.
2. No unnecessary explanatory/helper labels/cards.
3. The screen is understandable without explanation text compensating for weak layout/action design.
4. Compact vertical height without sacrificing readable text or practical touch targets.
5. New screens reuse the established visual style and shared components.
6. Consistent colors, typography, shapes and spacing.
7. Same action uses the same label and Google Material Rounded icon.
8. Icon-only actions have tooltip and accessibility description; buttons use icon + name where appropriate.
9. Card/list action placement is consistent across equivalent records.
10. Default loaded-list search returns immediate local results without API calls while typing.
11. Forms follow natural business order and optional fields come later.
12. Edit reuses the same existing form/screen with prefilled data.
13. Correct compact semantic status badges.
14. IME-safe forms.
15. No horizontal clipping on narrow phones.
16. Phone and tablet use the same adaptive screen structure rather than unrelated designs.
17. Loading/empty/error states are present where needed.
18. Refresh/background failure retains previously valid data and offers concise retry behavior where useful.
19. Async actions are protected from double-submit.
20. No polling, hidden periodic refresh or duplicate recomposition calls.
21. Role restrictions are respected.
22. System bars/insets are handled correctly.
23. Booking lifecycle actions match server-authoritative state.
24. Lifecycle actions update the same booking screen immediately after success.
25. Pickup/Return quantity validation never accepts more than the current valid remaining quantity.
26. Bottom navigation contains stable main modules only, not detail/edit/sub-flow screens.
27. Navigation uses already-valid cached data before fetching unnecessarily.
28. Successful mutations update local state from the response where practical instead of forcing a full reload.
29. Shared components are extended with variants rather than copied into one-off versions.
30. System font scaling does not cut off or overlap important content.
31. At least one narrow-phone view and one normal-phone view are audited for spacing, wrapping, button overflow, header/action collision and card/list action placement.

A UI batch is not complete until these checks pass.
