# Release 0.14.3 — Phase 14D: Item Image Cards Across Booking Flow

Every booking item is shown individually with its own primary image and item details across Dashboard Today Bookings/Pickups/Returns/Overdue, Booking cards, Pickup cards, Return cards, Customer History, booking-related Reports, and detail rows.

Multiple items are not collapsed into a `+N more` summary.

The Worker uses one bundled D1 enrichment query per affected API response, with no extra browser API calls.

Database/schema unchanged. Pricing remains disabled.
