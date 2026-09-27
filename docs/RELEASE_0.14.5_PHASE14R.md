# Zhagmag Dresses 0.14.5 — Phase 14R User Experience Hardening

Phase 14R is an Admin Web UX-only hardening batch on top of Phase 14Q.

## User experience changes

- Item, Customer and Booking drawers protect trusted user edits with an unsaved-changes confirmation on Close, backdrop, Escape and browser navigation.
- Mobile Booking uses a four-step flow: Customer → Dates → Items → Review & Save. Desktop retains the dense single-page ERP form.
- Booking item selection adds a visual picker with item thumbnail/placeholder, code, name, category and available/total context when already present in existing loaded data.
- Visual picker reuses the existing booking bootstrap/availability responses by cloning them in-browser; it does not make an extra API/D1/Cloudflare request.
- Visual item names/meta are rendered through DOM text nodes and image URLs are restricted to http/https before preview rendering.
- Required/invalid fields receive inline error text, `aria-invalid`, focus and scroll-to-error guidance.
- Mobile Reports use the same `Filters (N)` bottom-sheet language as operational lists.
- Existing `More` menus become labelled mobile action sheets with a backdrop and safe-area handling.
- Booking detail adds contextual `Edit Booking`, `Go to Pickup`, or `Go to Returns` actions when applicable.
- Existing Phase 14P/14Q drawers, focus trap, date rules, availability rules and business logic remain intact.

## Technical impact

- Version remains `0.14.5`.
- Manifest phase: `14R`.
- Admin Web rebuild/deploy required.
- Public Web source unchanged.
- Worker/API source unchanged.
- D1 schema/migrations unchanged.
- No pricing/payment features added.

## Verification

`tests/phase14r_regression.py` is included in `npm run test:hardening` and checks the UX guards, mobile step flow, visual picker, safe DOM rendering, inline validation, filter/action sheets, contextual actions and zero-extra-request design.
