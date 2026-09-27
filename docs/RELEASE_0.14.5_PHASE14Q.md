# Zhagmag Dresses 0.14.5 — Phase 14Q

## Scope
Admin Web structural UI only. Public Web, Worker/API, D1 schema and business rules are unchanged.

## UI changes
- Customers are list-first; Add/Edit opens a responsive right-side drawer.
- Bookings are list-first; New/Edit opens a wider multi-item drawer.
- Dashboard New Booking continues directly into the booking editor drawer.
- Drawer close supports backdrop, Close button, Escape and keyboard focus trapping.
- Primary save actions remain sticky at the bottom of long customer/booking forms.
- Expanded Category + Custom Fields receives a desktop split-view treatment.
- History/detail drawers receive a sticky contact bar with WhatsApp, Call and Close when a customer phone is available.
- Existing Phase 14P desktop row headers, mobile filter sheets and More menus remain active.

## Safety
- Existing React API calls and form save handlers are preserved.
- Strict Pickup < Return enforcement from Phase 14O is unchanged.
- Pricing/payment scope remains disabled.
- No D1 migration.
- No Worker/API change.

## Deployment
Admin Web rebuild/deploy required. The standard same-origin staging workflow may rebuild all static assets and redeploy the Worker shell, but Phase 14Q itself changes only Admin Web source, manifest/tests and documentation.
