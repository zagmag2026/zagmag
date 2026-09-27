# Admin Android — Screen 2: Dashboard

Status: **Implemented / finalized / locked after Issue 6 KPI redesign**

This file records the current Screen 2 UI/navigation contract.

## Main screen chrome
- Fixed signed-in top app bar remains unchanged.
- Business date stays below the shared header and scrolls with Dashboard content.
- Title row is `Dashboard` with compact `+ New Booking` at right when Booking access is allowed.
- Shared Main Header refresh is the only Dashboard refresh action.
- Bottom navigation remains Dashboard | Customers | Bookings | More during normal top-level navigation.

## Dashboard sections
Exact visible section order:
1. Today Overview
2. Booking Status
3. Payment & Billing
4. Inventory
5. Customers
6. Category-wise Inventory

There is no Subcategory-wise section, no Owner/Activity section, and no dropdown/collapse queue section.

The five fixed groups contain exactly **34 fixed KPI cards**:
- Today Overview: 4
- Booking Status: 8
- Payment & Billing: 8
- Inventory: 8
- Customers: 6

Category-wise Inventory is dynamic and is not part of the 34 fixed-card count.

## Responsive card layout
- Mobile/narrow: 2 KPI cards per row.
- Wide/tablet at the Dashboard breakpoint: 4 KPI cards per row.
- KPI cards reuse one Dashboard KPI card component with compact semantic pastel styling.
- Category cards use the same responsive 2/4-column geometry.
- Zero values remain visible for authorized KPI cards.

Permission-aware rendering is required. Staff must not be shown KPI destinations or sensitive values they are not authorized to use. OWNER-only Billing monetary KPI values are never exposed as usable Staff values by the Worker.

## Exact drill-down behavior
A KPI tap opens the related existing module directly with the exact temporary filter; there is no popup or intermediate Dashboard menu.

- Today Pickups / Today Returns / Missed Pickups / Overdue Returns → Bookings exact filter.
- Reserved / Booked / Part Picked Up / Full Picked Up / Part Return / Full Returned / Cancelled / Active Rental Orders → Bookings exact filter.
- Pending / Part / Full Payment → Bookings payment filter.
- Draft Bills / Final Bills → Bills exact status filter.
- Bills Today / Pending Balance / Total Received → Reports exact Billing filter.
- Available Now / Pickup Pending Qty / Currently Out Qty / Low Stock / Unavailable → Reports exact Inventory filter.
- Total Quantity / Total Items → Category & Items → Items.
- Categories → Category & Items → Categories.
- Total Customers → Customers.
- New / Returning / Frequent / Active Rental / Exception Customers → Reports exact Customer filter.
- Dynamic Category card → Category & Items → Items with that Category selected.

Back from any Dashboard drill-down returns directly to Dashboard and restores the existing Dashboard scroll position/state. Temporary drill-down filters are cleared without an unnecessary off-screen reload.

## Data/freshness
- One optimized bundled Dashboard API.
- No per-KPI calls and no aggressive polling.
- Relevant mutations advance Dashboard freshness; Billing mutations are included.
- Dashboard reloads on normal return/entry only when stale, or by explicit shared-header refresh.
- Refresh keeps old valid content visible where practical.
- Initial load failure shows retry; refresh failure shows concise inline feedback.

## Retired UI
The former expandable detail queues remain permanently absent from Dashboard:
- Today Pickups
- Today Returns
- Missed Pickups
- Overdue Returns

Their per-booking cards, item rows, image payloads, quick Call/WhatsApp actions and local expand/collapse state are not part of the KPI Dashboard.
