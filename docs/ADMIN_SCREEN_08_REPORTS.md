# Admin Android — Screen 8: Global Rental Reports

Status: **Global Rental Reports implemented / staging validated; shared Report Options UI refinement implemented**

## Scope
Screen 8 is the category-agnostic **Global Rental Reports** workspace for OWNER and STAFF users with the **Reports** permission.

The Reports architecture must work for every current/future rental category. No category name is hard-coded into report behavior. Category Master and its dynamic custom fields remain authoritative.

Android identity remains **0.14.5 / versionCode 1**. Billing V1 is the approved monetary-report exception; laundry, repair, maintenance and broader revenue/profit/accounting reporting remain out of scope.

## Main View
Top layout is **Main Header → Date | Day | Time → Reports Back Header → section tabs → Report Options → Results**.

Reports uses the shared compact Back header. Visible Back and Android system/gesture Back return to the More hub. Back is consumed while preset/PDF export work is protected so it cannot fall through to app exit.

Top sections are:
- **Overview**
- **Operations**
- **Inventory**
- **Customers**
- **Billing** — OWNER only
- **Activity**

A section may contain multiple report types. The selected report uses one reusable Report Options/result flow rather than a separate duplicated screen per report.

The first entry loads one bundled Reports bootstrap request and then the default **Operations Overview** report. There is no polling.

## Report catalog

### Overview
- Operations Overview

Operations Overview summarizes the selected period and surfaces:
- Bookings
- Pickup Pending
- Return Pending
- Missed Pickup
- Overdue Return
- Completed

### Operations
- Bookings
- Upcoming Bookings
- Cancelled Bookings
- Pickup Report
- Missed Pickup
- Return Report
- Overdue Return

### Inventory
- Availability
- Currently Out
- Item Utilization
- Low-use / Idle Items
- Item Rental History
- Category Summary

### Customers
- Customer Rental History
- Active Rentals
- Frequent Customers
- New vs Returning
- Customer Exceptions

### Billing — OWNER only
- Billing Overview
- Bills Report
- Pending Balance
- Full Amount Received

Billing monetary data is OWNER-only. STAFF must not see this section or receive these report types through direct API access or saved presets, even when STAFF has Reports permission.

Billing summary metrics may include Bills, Total Rent, Discount, Net Amount, Cash Received and Pending Balance. Payment collection is Cash only and payment status is Pending or Full Amount Received.

### Activity
- Staff Activity
- WhatsApp Activity
- Audit Report
- Exception Report

**Audit Report is OWNER-only.** STAFF with Reports permission may use the other Reports surfaces but must not receive Audit Report through UI, presets or direct API access.

### Audit Report timezone
- Audit history remains stored in D1 as **UTC**.
- Audit Report Date / Time values are returned/displayed in **Asia/Kolkata (IST)**.
- Audit Report From/To dates represent Asia/Kolkata calendar days; Worker converts them to UTC start and next-day exclusive boundaries before querying.
- This keeps Screen 8 Audit Report aligned with Settings → Audit Log, including records created between 00:00 and 05:29 IST.

## Authoritative rental calculations
All report status and quantities are derived from Worker/D1 state.

- Original booked quantity = booked_qty.
- Active booked quantity = booked_qty - closed_qty.
- Pickup Pending = active booked quantity - given_qty, minimum zero.
- Return Pending = given_qty - returned_qty, minimum zero.
- Reserved bookings do not enter Pickup Pending / Missed Pickup queues until confirmed.
- **Missed Pickup** = confirmed booking + Pickup Pending > 0 + pickup date has passed.
- **Overdue Return** = Return Pending > 0 + return date has passed.
- Missed Pickup and Overdue Return are separate operational conditions.
- Returning all quantity picked so far does not become Full Returned while active unpicked quantity remains.
- Closing remaining unpicked quantity releases it from active reservation without changing original booking history.

These rules remain aligned with Booking/Dashboard operational lifecycle rules.

## Availability
Availability accepts a selected date range and uses server-authoritative inventory commitments.

For each item it returns:
- Total Qty
- Committed Qty
- Available Qty
- Available / Partially Available / Fully Booked

The calculation accounts for active overlapping booking quantity and quantity already issued but still outstanding beyond its scheduled return. Availability is a report/read surface only and cannot mutate stock.

## Currently Out
Currently Out is based on actual outstanding issued quantity:

given_qty - returned_qty > 0

It is not inferred only from booking status.

## Utilization
Item Utilization is period-aware and includes:
- Rental Cycles
- Issued Qty
- Currently Out
- Available Now
- Last Rental

Low-use / Idle Items reuses the same authoritative item rollup and identifies items with zero or low rental cycles for the selected period.

Rental operational reports do not add monetary metrics. Monetary values are confined to the OWNER-only Billing section. Revenue/profit, deposit, GST, late-fee, damage-charge and broader accounting metrics remain out of scope.

## Category-driven custom filters
When a specific Category is selected, its active dynamic custom fields are returned in the existing Reports bootstrap and rendered as report filters without category-specific code.

Supported field types reuse their proper controls:
- TEXT
- NUMBER
- DROPDOWN
- MULTI_SELECT
- YES_NO
- DATE

Changing Category clears custom-field filters that belong to the previous Category.

Custom-field criteria are saved inside report presets and are included in the PDF applied-filter summary.

## Report Options UI
Report Options uses the existing shared/global Admin controls instead of Screen 8-specific dropdown implementations.

- Report, Grouping, Sort, Date Range, Date Basis, Category, Status and Staff/User use the canonical shared searchable **single-select filter sheet** with Cancel / Clear / Apply behavior.
- Dynamic **MULTI_SELECT** fields use the shared multi-select filter sheet with search for large option sets and explicit Apply.
- Date inputs use the shared calendar/date-picker field.
- Relative Date Range presets (Today / Yesterday / This Week / This Month) keep From/To fields hidden because their dates are resolved automatically.
- Selecting **Custom** reveals From Date and To Date calendar fields.
- Custom From/To fields use the shared responsive date pair; From is capped by an existing To and To is bounded by an existing From while ViewModel validation still blocks reversed ranges.
- Date Basis is hidden when a report has only one valid basis.
- TEXT/NUMBER custom fields and Item/Customer/general search continue to reuse AppTextField; no extra lookup/API call is introduced merely to edit criteria.
- **Clear Filters** restores the current report default criteria locally (This Month where dates apply, Newest sort and the report default grouping) and clears stale generated results.
- Filter selection, clearing and date picking are presentation/state operations only. The report API runs only on Generate/refresh/load-more/export under the existing rules.

## Common criteria
Only relevant criteria are shown for each report:
- Date Basis
- Date Range
- From / To
- Category
- Category dynamic custom fields
- Item
- Customer
- Status / Outcome / Action
- Staff / User
- Grouping
- Sort
- Search

Relative date presets:
- Today
- Yesterday
- This Week
- This Month
- Custom

Upcoming Bookings uses Pickup Date as its report date basis. Cancelled Bookings uses Booking Date. Relative Saved Presets are re-resolved from the current authoritative business date when applied; Custom preserves the saved From/To dates.

## Grouping
Grouping is report-aware and may include:
- None
- Date
- Category
- Item
- Customer
- Staff
- Status / Action

Invalid grouping choices are not shown for the selected report.

## Result loading
- Initial generated page: **10 rows**.
- Bottom scroll appends the next **10 rows**.
- No Previous/Page/Next controls.
- Duplicate load-more calls are blocked.
- No polling.
- Filter/section/report changes clear the previous generated result instead of presenting stale rows under new criteria.
- Generate failure shows an action error and does not render a false valid-empty result.
- Manual report refresh reloads only the current generated configuration.

## Dynamic Summary
Dynamic summary metrics render in the existing two-column grid using compact single-row cards: label on the left, prominent value on the right, shared compact spacing, and no unnecessary vertical whitespace. Labels may wrap to two lines for accessibility/font scaling while values remain single-line.

Each report returns only relevant summary metrics. Examples:
- Operations Overview → Bookings / Pickup Pending / Return Pending / Missed Pickup / Overdue Return / Completed
- Pickup → Picked Qty / Pending Pickup
- Overdue / Currently Out → Pending Return
- Availability → Total / Committed / Available / Fully Booked
- Item Utilization → Rental Cycles / Issued Qty / Currently Out
- Customer reports → Customers / Bookings / Active Qty / Overdue
- WhatsApp Activity → Attempts / Prepared / Failed
- Audit Report → Actions / Created / Updated / Deleted

## WhatsApp Activity
WhatsApp Activity records the result of the system's **explicit Admin WhatsApp preparation request**:
- Prepared
- Failed
- customer / booking context
- user
- status group / context
- template count
- visible failure reason where applicable

It does **not** claim that WhatsApp was delivered, read or sent successfully by the external WhatsApp application. The system has no authoritative delivery/read receipt for manual external sending.

Logging occurs only after an explicit Admin WhatsApp action. It adds no polling/background template calls, and logging failure must never break the WhatsApp action itself.

Historical WhatsApp preparation events from before migration 0020_global_reports_whatsapp_activity.sql are not backfilled.

## Audit Report
Audit Report reuses existing audit_logs as the authoritative source. It does not create a duplicate audit store.

It supports date/user/action/search filtering and is OWNER-only.

## Saved Presets
Presets remain stored in the existing report_presets table.

A preset stores:
- Report Type
- Date basis/range rule
- Category
- Category custom filters
- Item
- Customer
- Status
- Staff
- Grouping
- Sort
- Search

Visibility:
- OWNER: My Preset or Shared Preset.
- STAFF: My Preset only.
- STAFF cannot edit/delete another user's preset.
- OWNER may edit/delete Shared presets.
- Audit Report presets are not exposed/accepted for STAFF.

Existing supported presets remain safe to parse; invalid/retired report types fall back to the current default report rather than breaking Screen 8.

## Preset form validation
- Saved Preset Name is required and is labelled `Preset Name *`.
- Save stays disabled while the name is blank; after interaction the field shows `Preset name is required.`.
## PDF
Generated result actions use a compact two-row mobile layout:
- first row: **View | Share**
- second row: full-width **Download**

The action order is therefore View → Share → Download, with short single-line labels. This changes only the Android result-card presentation; the generated PDF content/export behavior is unchanged.
Saved Preset Use/Edit/Delete actions use the shared responsive triple layout so Delete moves to a full-width second row on narrow phones without changing preset behavior or permissions.

PDF format remains **Summary + Detailed Table** with:
- branded Shop/app identity + logo and Report name on the first page;
- Generated date/time and Generated by user/role in a compact metadata row;
- Applied Filters in a wrapped bordered block instead of one truncated line;
- Dynamic Summary in compact KPI cards (3 columns in portrait, 4 in landscape);
- a **Detailed Report** table with content-aware column widths rather than equal-width columns;
- wrapped table headers and up to three visible lines per cell before safe ellipsis;
- readable date/date-time values and human-readable semantic status/action labels;
- alternating row background for scanability;
- repeated compact report header + table header on continuation pages;
- report title + page number footer on every page.

Portrait/Landscape is selected automatically by column count. PDF presentation changes do not change report calculations, rows, filters, permissions or export caching.

PDF is generated on-device from one explicit complete Worker export request. Export is cached for the unchanged generated configuration so View/Download/Share do not repeat the export request. More than 10,000 matching rows requires narrower filters.

Local PDF failures use sanitized user-facing feedback and do not expose raw platform exception messages.

## Permissions
- OWNER has Reports access.
- STAFF requires Reports permission.
- Worker authorization is authoritative.
- Audit Report additionally requires OWNER.
- Direct API access without permission returns forbidden.

## API / D1 efficiency
- One Reports bootstrap request on entry.
- Initial Overview uses one report request after bootstrap.
- User criteria editing causes no report API call.
- Generate/refresh uses one request for the current page.
- Bottom scroll uses one request per next 10 rows.
- PDF export uses one explicit complete request and is cached for unchanged filters.
- Category custom filtering is part of the same report request; no per-field/per-row request.
- WhatsApp activity logging is one bounded D1 write after an explicit Admin WhatsApp preparation request.
- No polling.

## Backend
Current cumulative feature-branch Worker entry for Billing V1:
src/phase14br-billing-v1.js

It wraps the existing Global Rental Reports Worker and preserves the cumulative report architecture.

It wraps the existing cumulative Settings/WhatsApp Worker and preserves all unrelated APIs.

Endpoints remain:
- GET /api/admin/report-generator/bootstrap
- GET /api/admin/report-generator
- POST /api/admin/report-presets
- PUT /api/admin/report-presets/:id
- DELETE /api/admin/report-presets/:id

Migrations:
- 0016_report_presets.sql — existing saved presets
- 0020_global_reports_whatsapp_activity.sql — Admin WhatsApp preparation activity
- 0023_billing_v1.sql — Billing V1 tables, Rent/Advance and Billing WhatsApp context

No second backend/database is introduced.

## Production safety
- Default validation is staging only.
- Production remains untouched unless explicitly requested.
- Required validation for this replacement is the staging Worker/D1 deployment plus Admin Android staging APK.
- Version remains **0.14.5 / versionCode 1** unless separately changed.

## Validation
Validation evidence will be added after the Phase 14BQ staging deploy and Admin Android staging APK complete successfully.

Screen 8 remains unlocked until the user explicitly locks it.

## Confirmation consistency
- Saved Preset Delete uses the shared destructive confirmation dialog with busy-safe dismiss behavior.
- Save/Edit Preset remains a normal editor confirmation and is not danger-styled.
## Loading / empty / retry behavior
- Reports bootstrap failure shows the shared Error + Retry state; retry reruns the normal bootstrap flow and may restore the default Overview auto-load behavior.
- A report empty state is shown only after a successful generated response with zero rows.
- Load-more failure keeps generated rows visible, shows a passive load error and stops automatic page retries until Generate/Refresh starts a new request.
- Generate/Refresh clears a prior append-load error so pagination can resume after a successful recovery.


## UI consistency polish
- Report Options uses the shared labeled-section card pattern instead of an inside-card duplicate heading.
- Empty report results render one shared Empty State surface; do not wrap Empty State in another AppCard.
- Report filters/forms remain IME-safe through the screen-level scroll container.
