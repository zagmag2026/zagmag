# API Call Audit

## Objective
Minimize Cloudflare Worker, D1 and network calls while preserving correct fresh business state.

## Global rules
- No polling.
- Prefer one bundled/bootstrap request over per-card/per-row requests.
- Load on screen entry, explicit search/page/filter change, approved mutation-stale refresh or manual refresh.
- Prevent duplicate concurrent requests for the same action/state.
- Keep valid previously loaded data visible during refresh when safe.
- Use local UI state for purely presentational actions.

## Admin Android
- Dashboard uses the shared Main Header refresh; no separate page-level refresh request path.
- Dashboard/Booking card Expand/Collapse uses already-loaded data and performs **zero API calls**.
- Booking item selection/search/filter reuses already-loaded/approved paged data as defined by the Screen 4 contract.
- Screen 5 uses the bundled item-management bootstrap endpoint.
- Settings uses a bundled bootstrap; Audit rows load only when Audit Log is opened.
- WhatsApp composition makes no per-card background call; one bounded template-resolution request is made only after the user taps WhatsApp.
- The shared WhatsApp busy state prevents overlapping requests from repeated taps; a completed request resolves to selector, preview or visible error.
- No API call is made merely because a card becomes visible or expands.

## Mutation freshness
After successful booking/customer/item/pickup/return/settings/user mutations, refresh only the affected stale domain/surface. Do not globally reload unrelated modules without a documented need.

## Screen 8 Global Rental Reports
- Screen entry uses one bundled bootstrap request for category/custom-field/staff/preset metadata.
- The default Operations Overview may make one report request immediately after successful bootstrap.
- Editing section/report/filter/group/sort criteria makes no report API call until Generate, except the one initial Overview load.
- Generate/refresh uses one request for the current 10-row page.
- Bottom scroll appends the next 10 with one request and duplicate-load protection.
- Dynamic Category custom-field filtering is bundled into the same report request; no per-field/per-row request.
- PDF export performs one complete export request only after an explicit PDF action.
- Reuse the cached export for View/Download/Share while generated filters remain unchanged.
- WhatsApp Activity adds one bounded D1 activity-log write only after an explicit Admin WhatsApp preparation request; it does not add template polling/background calls.
- No per-row requests and no polling.

## Billing V1
- More → Bills initial entry uses one paged Bills request (10 rows).
- Bottom scroll appends the next 10 with one request; there are no Previous/Page/Next controls and no polling.
- Search text editing is local; the Bills request is made only when Search is applied. Payment/Bill Status changes request page 1 once.
- Create Bill uses one bundled Billing bootstrap request. Booking-linked bootstrap returns Customer, Item/Rent defaults, Booking Advance and existing linked-Bill resolution together; there are no per-item Rent calls.
- Draft editing, Qty/Rate edits, Discount/Advance edits and Payment Status selection are local-only until Save.
- Save/Finalize/Full Amount Received/Cancel/Delete are explicit mutations with duplicate/stale-write protection.
- Booking → Bill deep link performs one Billing bootstrap and opens the existing linked Bill when present; it does not load the Bills list first.
- Bill PDF View/Share/Download is generated on-device from already-loaded final Bill detail and causes no additional Worker/D1 request.
- Billing WhatsApp uses one explicit central-template preparation request and one bounded activity-log write; system Share of a PDF does not create a false WhatsApp activity record.
- OWNER-only Billing Reports reuse the existing Screen 8 request/export/cache rules; there are no per-row or background monetary-data calls.
- No polling.

## Public Website
- No polling.
- Public availability/contact/catalog requests occur only for actual user/navigation/date/filter needs.
- Item/general WhatsApp inquiry resolves the central General Inquiry template only after an explicit WhatsApp tap; there is no background template polling or screen-local message builder.

## Admin Web
- Dashboard, Customers, Bookings, Pickup, Return and Reports share one on-demand WhatsApp template flow.
- Each explicit WhatsApp tap makes one status-filtered template request; repeated taps are blocked while that request is active.
- Page rendering/list scrolling never generates WhatsApp/template calls.

## Review checklist
Before merging a runtime batch:
- count new request paths;
- verify no timer/poll loop was added;
- verify duplicate taps cannot create duplicate mutations;
- verify bundled endpoints are reused where available;
- verify local presentation state causes no backend call.
