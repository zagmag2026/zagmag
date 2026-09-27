# Zhagmag Dresses 0.14.5 — Phase 14P

Phase 14P is an Admin Web structural ERP UI release. Version remains `0.14.5`.

## Admin UI changes

- Item Master is list-first; Add/Edit opens in a responsive right-side drawer.
- Items, Customers and Bookings receive desktop ERP column headers and aligned row layouts.
- Desktop list filters remain available as sticky toolbars.
- Mobile filters collapse behind a `Filters (N)` control and open as a bottom sheet.
- Secondary/destructive row actions move behind an Icon + Name `More` menu while primary operational actions stay visible.
- Dashboard KPI priority is reordered visually so Available, Booked Pending, Given Out and Overdue are primary; master totals become secondary metrics.
- Existing mobile cards, keyboard focus behavior, safe-area handling and strict rental-date rules remain intact.

## Scope / deployment impact

- Admin Web changed.
- Public Web unchanged.
- Worker/API logic unchanged.
- D1 schema/data unchanged; no migration is required by this release.
- Pricing/payment remains out of scope.

## Regression

`tests/phase14p_regression.py` verifies structural enhancer activation, drawer/list behavior markers, desktop headers, mobile filter-sheet safeguards, overflow action accessibility, KPI priority rules and current manifest identity.
