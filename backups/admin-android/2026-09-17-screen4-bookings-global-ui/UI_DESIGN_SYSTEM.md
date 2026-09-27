# Zhagmag Dresses — UI Design System

All new web/Android UI must reuse these rules instead of introducing one-off screen styling.

For the **Admin Android app**, also follow `UI_GUIDELINES.md`. When an Android-specific rule in `UI_GUIDELINES.md` conflicts with a generic rule here, `UI_GUIDELINES.md` takes precedence.

## Color tokens
Canonical Android tokens live in `apps/admin-android/.../ui/theme/Color.kt`.
- Primary: brand maroon `#7D1747`.
- Secondary: `#B62E69`.
- Background: `#F8F5F6`.
- Surface: white.
- Success: green semantic pair.
- Warning: amber semantic pair.
- Error: red semantic pair.
- Info: blue semantic pair.

Do not add arbitrary status colors on individual screens. The same meaning uses the same semantic color everywhere.

## Typography
Use shared Material typography roles:
- Page title → `headlineMedium`.
- Section/card title → `titleLarge` or `titleMedium`.
- Body → `bodyLarge`/`bodyMedium`.
- Caption/badge/metadata → `labelMedium`.

Avoid fixed text heights. Web/public surfaces may support Gujarati and English as defined by their own module rules. The Admin Android app is currently English-only per `UI_GUIDELINES.md`.

## Spacing
Use the fixed scale from `AppSpacing`:
- 4 / 8 / 12 / 16 / 24 / 32 dp.
- Avoid arbitrary spacing unless a platform constraint requires it.
- Admin Android should prefer the smallest appropriate token to keep vertical height compact while preserving usability.

## Shapes
Use shared Material shapes (8 / 10 / 14 / 18 / 26 dp tiers). Cards and form fields must not invent screen-specific radii.

## Reusable components
Android shared components live under `ui/components/` and should be extended rather than duplicated:
- `AppPageHeader`
- `AppCard`
- `AppTextField`
- `PrimaryButton`
- `SecondaryButton`
- `StatusBadge`
- `InlineMessage`
- `EmptyState`
- `LoadingState`

When future needs appear, add reusable `AppDialog`, dropdown/date field, search bar and specialized empty/error components here instead of duplicating screen code.

## Buttons
- Primary: main commit/action.
- Secondary/outline: refresh, cancel, paging, non-destructive secondary action.
- Danger: only destructive confirmed actions.
- Every async action has disabled/loading behavior and double-submit protection.
- Touch targets must remain comfortably tappable on phones.

## Cards
- One border/shadow treatment across equivalent cards.
- Consistent padding and title/status layout.
- Operational customer/booking cards keep customer, status, dates/items and quick actions in the same hierarchy.
- Admin Android cards should avoid unnecessary explanatory content and excessive vertical padding.

## Forms
Generic pattern: label/input → validation/error when needed → action.
- Preserve user-entered data on request failure/timeouts.
- Keep focused field and action accessible above IME.
- Do not run network requests on every keystroke unless explicitly approved.
- Admin Android does not show general helper/explanation text under ordinary fields; see `UI_GUIDELINES.md`.

## Dialogs
Use one title/content/action hierarchy. Cancel precedes confirm. Destructive confirmations use danger styling and explicit wording.

## Badges/chips
Semantic status colors only. Do not use the same decorative color for unrelated meanings.

## Icons
Use one Material icon family for Android. Use consistent 18/20/24 dp visual sizes by context.

## Navigation
- Dashboard / Customers / Bookings / More are the Android bottom-level destinations unless a separately approved change updates this structure.
- Future module navigation must preserve a consistent back/title pattern.
- Role-restricted modules must not be presented as usable to roles that cannot access them.

## Responsive/mobile rules
- No horizontal content clipping.
- Lists/cards adapt to screen width.
- Text wraps where necessary.
- Tablet/large-screen adaptations may widen content but should reuse the same components/tokens.
- Admin Android-specific compactness and IME rules are defined in `UI_GUIDELINES.md`.

## UI consistency audit
After every new screen/button/form/card/dialog batch, compare against existing UI for:
1. colors/tokens,
2. typography,
3. spacing/radius,
4. button height/state,
5. icon family/size,
6. badges/status semantics,
7. form/error/loading/empty state,
8. navigation behavior,
9. phone/tablet responsiveness,
10. surface-specific language/text policy.

For Admin Android, also run the complete checklist in `UI_GUIDELINES.md`.

A UI batch is incomplete until the applicable audit passes.
