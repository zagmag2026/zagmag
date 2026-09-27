# Zhagmag Dresses 0.14.5 — Phase 14X List-State + KPI Action Hardening

Phase 14X continues from Phase 14W and keeps version `0.14.5`.

## Admin list/result state hardening
- The Phase 14S shimmer/loading class now re-evaluates when React changes only a text node, fixing the case where a settled `0 records` empty state could keep displaying the loading shimmer.
- Related list/search/filter screens receive a consistent settled empty state after loading completes.
- Returns and Pickup show view-specific empty copy such as no returns due today, no overdue returns, or no upcoming pickups.
- Search/filter no-result states clearly report that no matching records were found.
- Pagination is hidden while loading and when the settled result set is empty.
- Any Phase 14U previous-result snapshot/refresh badge is removed when the current settled list is empty.
- Dashboard operational panels use clear no-record copy instead of a generic `No records.` message.

## Dashboard KPI cards
- Dashboard KPI cards are keyboard-accessible clickable cards.
- Each KPI receives a compact icon/glyph and chevron affordance.
- Categories opens Categories; Items, Total Quantity and Available Now open Items; Booked Pending opens Bookings; Given Out and Overdue Returns open Returns.
- Navigation uses the existing Admin tabs and creates no direct API request.

## Preserved rules
- Pricing/payment remains OFF.
- Every booked item remains individually visible; no `+N more` collapsing.
- Worker/API business logic and D1 schema/migrations are unchanged.
- No automatic extra Cloudflare/API calls are introduced.
- Production is NOT deployed.
