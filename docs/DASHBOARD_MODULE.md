# Dashboard Module — Business/Data Contract

This file defines the current Dashboard data/business behavior. Admin Android layout/navigation details live in `ADMIN_SCREEN_02_DASHBOARD.md`, `GLOBAL_UI_RULES.md` and `UI_GUIDELINES.md`.

## Endpoint
`GET /api/admin/dashboard`

Authenticated OWNER and STAFF with Dashboard permission may read Dashboard data. Worker authorization is authoritative. Domain data is permission-aware; inaccessible domain KPI data is not exposed as usable values. Billing monetary Dashboard values are OWNER-only.

## Loading/freshness
- one bundled Dashboard request and one bundled Worker/D1 summary query;
- no polling or per-card API calls;
- initial entry may load once;
- shared Main Header refresh is the only manual Dashboard refresh;
- booking, pickup, return, cancellation, customer, inventory and Billing mutations mark relevant Dashboard data stale;
- returning to Dashboard reloads only when the freshness revision changed;
- valid old data remains visible during refresh when practical;
- duplicate concurrent refresh is prevented.

## Fixed KPI structure
Dashboard has exactly **34 fixed KPIs**, grouped as:

### Today Overview — 4
- Today Pickups: confirmed/not Reserved, Pickup Date = today, Pickup Pending > 0.
- Today Returns: Return Date = today, Return Pending > 0.
- Missed Pickups: confirmed/not Reserved, Pickup Date < today, Pickup Pending > 0.
- Overdue Returns: Return Date < today, Return Pending > 0.

### Booking Status — 8
The lifecycle states are quantity-derived and mutually exclusive, using the authoritative precedence where Part Return wins over Part Pickup when both return activity and outstanding return quantity exist.
- Reserved
- Booked
- Part Picked Up
- Full Picked Up
- Part Return
- Full Returned
- Cancelled
- Active Rental Orders — Return Pending > 0; this is an operational aggregate and may overlap a lifecycle state.

### Payment & Billing — 8
Booking payment classification reuses the shared authoritative classifier and ignores stale stored Bill payment-status labels.
- Pending Payment
- Part Payment
- Full Payment
- Draft Bills
- Final Bills
- Bills Today
- Pending Balance
- Total Received

Full Payment requires an active linked Bill with positive Bill Amount and Total Received >= Bill Amount, or the existing no-Bill Booking fallback rules defined in `PROJECT_RULES.md`. Pending Balance and Total Received are OWNER-only monetary KPI values.

### Inventory — 8
- Available Now = max(0, Total Quantity − Pickup Pending Qty − Currently Out Qty); Reserved and future active commitments therefore reduce Available Now.
- Pickup Pending Qty = sum of max((Booked Qty − Closed Qty) − Given Qty, 0) across all active non-cancelled Bookings, including Reserved and future committed quantity.
- Currently Out Qty = Given Qty − Returned Qty where positive.
- Total Quantity
- Total Items
- Categories
- Low Stock = active Items with Available Now from 1 through 5.
- Unavailable = active Items with Available Now <= 0.

### Customers — 6
Active Customer Master rows only:
- Total Customers
- New Customers — first non-cancelled Booking is in the current month and the Customer has a current-month Booking.
- Returning Customers — first non-cancelled Booking predates the current month and the Customer has a current-month Booking.
- Frequent Customers — at least 2 non-cancelled Bookings.
- Active Rental Customers — at least one Booking with Return Pending > 0.
- Customer Exceptions — at least one Missed Pickup or Overdue Return condition.

## Dynamic Category-wise Inventory
There is one dynamic card per active Category, with:
- Items
- Total Qty
- Available
- Pickup Pending
- Currently Out

There is **no Subcategory-wise section** and no Owner/Activity Dashboard section.

## Drill-down navigation
KPI clicks reuse existing modules; Dashboard never creates parallel detail screens:
- Today/Booking/Payment booking KPIs → existing Bookings list with the exact temporary filter.
- Draft/Final Bills → existing Bills list with exact status filter.
- Billing/Inventory/Customer analytical KPIs → existing Reports module with the exact temporary Dashboard filter/date preset.
- Total Customers → existing Customers screen.
- Total Quantity/Total Items/Categories → existing Category & Items screen.
- Category card → Category & Items → Items tab with that exact Category selected.

Dashboard drill-down filters are temporary. Back returns directly to Dashboard, restores the existing Dashboard `LazyListState`, clears temporary destination/filter state locally, and does not trigger a wasteful off-screen Booking reload.

## Retired queue payloads
The former expandable Dashboard queues and their per-booking/item payloads remain removed:
`Today Pickups | Today Returns | Missed Pickups | Overdue Returns`.

Their KPI values are computed in the bundled summary query; no dropdown/collapse queue UI, queue booking cards, item image payloads or per-row Dashboard API calls are allowed.
