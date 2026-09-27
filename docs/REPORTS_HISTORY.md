# Reports + History

This file defines high-level report/business behavior. Admin Android Screen 8 UI/PDF behavior is authoritative in `ADMIN_SCREEN_08_REPORTS.md`.

## Current report generator
The current Android Report Generator supports:
- Bookings
- Pickups
- Returns
- Overdue
- Item-wise History
- Customer-wise History
- Category Stock
- Staff Activity

## Filters
Relevant report types may use:
- Date Basis
- From Date / To Date
- relative date presets
- Category
- Item
- Customer
- Status
- Staff/User
- Grouping
- Sort
- Search

Only filters meaningful to the selected report should be shown.

## Quantity rules
- Current given quantity = `given_qty - returned_qty` when positive.
- Overdue = Return Date before the current business date with outstanding return quantity.
- Category Available = Total active quantity - currently given-out quantity.
- Category Booked = booked quantity not yet given for non-cancelled/non-returned bookings.
- Pickup/Return activity reports use event quantities/timestamps and preserve correction/audit history.

## Staff activity
Staff Activity may summarize Bookings created, Pickup events, Return events and total activities by OWNER/STAFF user for the selected period.

## Pagination/API efficiency
- Initial Android result batch = 10.
- Bottom scroll appends the next 10.
- No polling.
- Bootstrap metadata is bundled.
- Each page/filter refresh uses one report request.
- Complete PDF export is fetched only after an explicit PDF action and is cached for the unchanged generated report.

## PDF
Android generates a local Summary + Detailed Table PDF. The PDF uses the same generated filters and complete authoritative export data. See `ADMIN_SCREEN_08_REPORTS.md`.
