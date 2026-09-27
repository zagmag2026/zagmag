# Zhagmag Dresses — Project Rules

This is the mandatory first-read governance file for developers and AI working on this repository.

## Mandatory workflow
1. Read this file first, then audit the current source and relevant active docs before any change.
2. Do not change code or active project documentation until the user explicitly says **Create code** for the current issue.
3. Each new issue requires a fresh **Create code** instruction.
4. Before an approved change, create a rollback-safe backup/branch for the exact pre-change state unless an equivalent exact backup already exists.
5. Normal completion after **Create code** is: implement the approved batch → run the relevant static/regression audit → push the approved batch directly to `main` → run only the single relevant validation workflow when a runtime surface changed → inspect exact job/log evidence → verify artifacts when applicable → report the verified result.
6. Preserve production unless the user explicitly requests a production deploy/change.
7. Validation defaults to staging/non-production.
8. Minimize Cloudflare, D1/API and GitHub Actions usage. No polling.
9. Admin Android default validation is the staging APK workflow only; do not build production APK/AAB unless explicitly requested.
10. Documentation-only consistency changes keep version **0.14.5 / versionCode 1** unchanged and do not require an Android build when runtime/source files are untouched.

## Documentation authority
When active docs conflict, use this order:
1. `PROJECT_RULES.md`
2. current global/UI authority docs
3. current specific Screen docs
4. current module/architecture docs
5. latest hardening docs for the exact behavior they explicitly cover

Additional rules:
- the most specific latest active rule wins for the affected surface;
- preserve production-safe behavior when ambiguity remains;
- `_backups/` and `backups/` are rollback snapshots only;
- `docs/RELEASE_*.md` files are historical records only;
- backup/release-history content must not be rewritten merely to match current behavior;
- historical docs cannot override active current rules.

## Product rules
- Brand: **ઝગમગ ડ્રેસીસ / Zhagmag Dresses**.
- Current project version: **0.14.5**.
- Admin Android versionCode: **1**.
- Billing V1 is an approved exception to the former pricing/payment exclusion: Item Master stores one default `Rent`, Booking stores optional cash `Advance Amount`, and Bills calculate **Item Total, ₹ Discount, Bill Amount, Advance Received, Other Received, Total Received and Balance Due** with derived payment status `Pending | Part Received | Full Received`. New Bill creation is **Booking/Order-linked only**; standalone/direct Bill creation is retired, while existing historical standalone Bills remain preserved read-only. GST, deposit, late fee, damage charge, miscellaneous charges, multi-mode payments, revenue/profit accounting and other pricing features remain out of scope unless explicitly approved later.
- Item Master remains category-driven with dynamic custom fields.
- D1/Worker remains authoritative for permissions, booking state, inventory and business-rule enforcement.
- Customer product UI uses one mandatory Mobile Number only. Alternative Mobile is retired from the current UI/workflow and is not used for Call/WhatsApp.
- Customer permanent deletion removes the Customer from product/customer surfaces and removes eligible operational Booking history, but **never deletes Billing financial history**. Bill/Bill Item snapshots remain authoritative; the hidden Customer tombstone exists only for FK integrity and live Customer PII is scrubbed.
- Item permanent deletion is allowed only when no Booking history exists. Historical Bill Item snapshots remain preserved and their live Item FK is detached before Item Master deletion.
- Public availability supports **Exact Quantity** and **Status Only**. New/unset configuration defaults to **Exact Quantity**; an existing explicit saved mode is preserved. Status Only never exposes exact quantity and uses fixed centralized states: **0 Not Available, 1–2 Few Left, 3–5 Limited, 6+ Available**.
- Public WhatsApp inquiry uses the central **General Inquiry** template.
- Public Call/WhatsApp number resolution is Saved Settings → environment fallback → hide/disable when unavailable.
- Public website base React copy stays canonical English and the runtime i18n layer renders Gujarati or English consistently from the selected/default language. Public item-card **Call** remains visible on mobile whenever a Call number is available; mobile must not silently remove an action that desktop exposes.

## Roles and permissions
Current active roles are exactly **OWNER | STAFF**.
- Legacy `ADMIN` is retired and must not be selectable/exposed by current UI/APIs.
- Existing legacy ADMIN rows are normalized/migrated to STAFF with applicable operational permissions preserved.
- STAFF navigation is permission-aware; inaccessible modules are hidden and Worker authorization remains authoritative.
- Users and Settings are OWNER-only.
- Settings business logo is upload-managed (JPG/PNG/WebP, max 8 MB client limit) and saved branding is reused by public/Admin branding.
- Pickup/Return historical correction actions are OWNER-only.
- Category/Item master write actions are OWNER-only.
- STAFF with Items / Stock permission may access/read/use Item Management but cannot add/edit/delete Categories or Items.

## UI governance
All new UI follows `GLOBAL_UI_RULES.md`, `UI_DESIGN_SYSTEM.md` and reusable components. For Admin Android, `UI_GUIDELINES.md` is app-specific authority. Run a UI consistency audit after every UI batch.

## Admin Android locked state
- Screen 1 Login — **implemented / finalized / locked**.
- Screen 2 Dashboard — **implemented / finalized / locked**.
- Screen 3 Customers — **implemented / finalized / locked**.
- Screen 4 Bookings — **implemented / finalized / locked**.
- Screen 5 Category & Items — **implemented / finalized / locked**.
- Screen 6 Pickup — intentionally skipped.
- Screen 7 Returns — intentionally skipped.
- Screen 8 Reports — **implemented / staging validated**; follows `ADMIN_SCREEN_08_REPORTS.md`. It is not locked until the user explicitly locks it.
- Screen 9 Users — **implemented / staging validated / finalized / locked**.
- Screen 10 Settings — **implemented / staging validated / finalized / locked**.

Locked screens are not modified unless the user explicitly unlocks or requests a specific change.

## Admin Android shared rules
- Native Kotlin + Jetpack Compose.
- Visible Admin Android UI is English-only unless explicitly changed.
- More screen uses compact paired rows in this order: **Category & Items | Reports**, **Bills | WhatsApp Centre**, **Users | Settings**, while preserving role/permission visibility and leaving a clean half-width placeholder when only one member of a pair is available.
- Shared Admin tabs preserve the validated Run #79 geometry (44dp height, fixed/scroll behavior, indicator, spacing and 96dp minimum scroll width); only the shared label font is increased to **16sp**.
- Reuse the existing Worker/D1 backend; do not create a second backend/database.
- Keep UI compact while preserving accessibility.
- Avoid unnecessary helper/explanatory cards.
- Avoid polling. Load on entry, approved mutation-stale refresh, explicit search/page change, or manual refresh.
- Compact Booking summary cards show first 2 item rows, then tappable `+ N more`; expanded state must support **Collapse / Show less** back to the first 2 rows. Expansion/collapse uses already-loaded data and causes no API call.
- Booking Details and confirmation/review show all items and do not use compact collapse.
- Dashboard has no separate page-level Refresh action; shared Main Header refresh is used.
- Dashboard is KPI-only and uses exactly **34 fixed KPIs + dynamic Category-wise Inventory cards**. Fixed groups are **Today Overview (4) | Booking Status (8) | Payment & Billing (8) | Inventory (8) | Customers (6)**; there is no Subcategory-wise, Owner/Activity or dropdown/collapse section. Mobile uses 2 KPI cards/row and wide Dashboard uses 4/row. One bundled Dashboard summary API supplies the KPI data; no per-card calls or polling. KPI taps reuse existing filtered Bookings/Bills/Reports/Customers/Category & Items screens, and Back returns directly to Dashboard with its scroll state preserved. Dynamic Category cards open Category & Items → Items with that exact Category selected. Former operational queue detail/card payloads remain retired. OWNER-only Billing monetary KPI values are not exposed as usable STAFF values.
- Booking summary/list quick actions use icon + label in exact order **View | Edit | Bill | Call | WhatsApp** where valid; Bill follows the linked-Billing visibility rules.
- Booking lifecycle is quantity-derived. Active Pickup Pending = active booked qty − picked qty; Return Pending = picked qty − returned qty. Returning everything picked so far does not make the booking Full Returned while pickup remains pending.
- When Return is saved after Part Pickup while unpicked quantity remains, the operator must choose **Keep Order Open** or **Close Remaining Items**. Closing remaining quantity releases it from active inventory reservation while preserving original booked/history quantities.
- Booking List tabs are exactly: **All | Reserved | Booked | Part Picked Up | Full Picked Up | Part Return | Full Returned | Overdue | Cancelled | Pending Payment | Part Payment | Full Payment**. Payment tabs are Billing-authoritative and exclude Reserved Orders. A cancelled Bill is ignored for active payment classification. One shared authoritative classifier is reused by Booking tabs/list cards and Booking Details: for a confirmed Order with no active Bill, Advance = 0 → Pending Payment and Advance > 0 → Part Payment; for a linked active Bill, `Total Received = Advance Received + Other Received`, `0` = Pending, `0 < Total Received < Bill Amount` = Part Payment, and `Total Received >= Bill Amount` = Full Payment. These states are mutually exclusive and a stale stored Bill payment-status label must not override the authoritative totals.
- Reserved Booking Details keeps **Cancel Booking | Confirm** and adds a full-width **Directly Pickup** action below; it confirms the same booking and then opens quantity-selectable Pickup.
- Booking Advance cancellation settlement is authoritative: when Advance > 0, cancellation requires Full Refund / valid Partial Refund / No Refund; original Advance remains historical, Refund is separate and Retained = Advance − Refund.
- Booking Edit is **Reserved-only** in all applicable UI and Worker mutation paths. Reserved Orders do not expose Bill. After Confirm, Bill becomes available and Edit is removed. Booking summary/list actions preserve **View | Edit | Bill | Call | WhatsApp** ordering while showing only actions valid for the current lifecycle; Cancelled Orders show Bill only when a linked Bill or Advance settlement exists.
- Booking/Order numbers are daily atomic sequences in format **BK-YYYYMMDD-001, 002, ...**; a new business date starts again at 001 and request-key retries must not allocate duplicates.
- Compact Booking cards show one top metadata row of **Pickup | Current | Return**. Every card uses the same four-line structure: **Label → Status → Date → Day**. Pickup/Return status is `Pending | Done`; Current status is the lifecycle label. Time is not shown. Pickup keeps its blue family, Return keeps its amber family, and Current uses a lifecycle-specific color family that does not reuse Pickup/Return colors. The top-right badge shows **Payment Status only** (`Pending Payment | Part Payment | Full Payment`); Reserved has no payment badge because lifecycle status is already shown in Current.
- New/Edit Booking uses exactly **Customer → Details → Items → Preview → Payment**. Preview is read-only review; `Advance Amount` and the final `Reserve | Confirm | Directly Pickup` actions live only on Payment. More → Bills → Add Bill opens an eligible Order selector containing only Orders without a Bill. Eligible Order cards do not repeat Pickup/Return badges; their bottom actions are **Order Preview | Create Draft** on one line. **Order Preview** is one shared read-only component reused by Select Order and Bills List. Booking Details tabs are **Details | Pickup | Return | Bill | History**; Details reuses the same shared Order Card without a redundant View action, and History merges Booking lifecycle + Billing/Payment history. Bill is the fourth tab and hosts the linked Bill Generate/Edit/Detail workspace inline. Full Return idempotently ensures the linked Draft Bill and switches to the Bill tab without rolling back a successful Return if Billing handoff fails.
- Draft Bill editor order remains **Bill Header → Booking & Customer → Items (x) → Pickup / Return Status → Amount Summary → Notes → Actions**. **Bill Details** order is **Bill Header → Booking & Customer → Items (x) → Amount Summary → Notes → Actions**; Pickup/Return status is intentionally omitted from the on-screen Bill Details view. The finalized Bill PDF uses a compact professional hierarchy and retains Pickup/Return with **Status + Date + Day + actual event Time** only; fake `00:00:00` is never shown. Bill Header shows Bill No., top-right Bill Date and Draft/Finalized badge. Booking & Customer uses Name → Mobile → optional Address → Booking No. All Bill items live in one section/card as separate compact rows with image, Qty × Rent Rate and line Amount; Draft Rent Rate stays editable and Finalized values are historical snapshots. Amount Summary shows one top-right **Pending Payment | Part Payment | Full Payment** badge, then totals; after Total Received a separator precedes a normal emphasized Balance Due row, with no nested Balance Due card or duplicate bottom payment-status row. Finalized Bill Details actions are unboxed shared buttons in two rows: **View | Share** then **Download | Print**. Bills List linked cards use **Order Preview | Edit/View** on one line; legacy standalone Bills hide Order Preview.
- Reserved Orders have **no Bill**. From Booked onward, a linked Bill may be created/edited/saved as Draft before Return completion. **Bill Finalize is allowed only after authoritative 100% Return completion**: at least one quantity was picked up, Pickup Pending = 0, and Return Pending = 0. Draft Amount Summary is `Item Total − Discount = Bill Amount; Advance Received + Other Received = Total Received; Bill Amount − Total Received = Balance Due`. Payment Status is derived automatically from those totals. Android disables Finalize early and the Worker/D1 endpoint enforces the same rule. Finalized Bill monetary/item/customer/date/notes snapshot values are immutable.
- Admin operational WhatsApp uses the central linked-template composer; no screen-local hard-coded message text. Template language is one global Settings value (**Gujarati | English | Both**), every template stores both languages, and action-specific placeholder validation blocks unknown/unresolved placeholders.

- Applicable Item displays reuse the shared Item row: thumbnail → Item Name → Item Code · Category → optional contextual status line → × N. Operational status lines keep consistent inline order/separators, e.g. `Booked 2 · Picked 0 · Returned 0`.
- Compact `+ N more` expansion is local-only and every expanded state provides the same reusable **Show less** action to restore the compact view.
- Operational Success/Error/Warning/Info feedback uses one shared semantic transient component with one placement, timing, animation and dismiss pattern; duplicate feedback for one action is suppressed.
- New/Edit Booking shows configured directional Related Items directly beneath a selected Main Item as suggestions only. Related Items never auto-add; explicit **Add** is required and normal availability/duplicate rules stay authoritative.

- Applicable Admin list/workspace filters use the shared global filter sheet/modal pattern with visible current selection, consistent Cancel/Clear/Apply actions, applied-state indication, and search for large option sets. Reusing this pattern must not change each screen's filter options or semantics.

## Screen 8 Global Rental Reports rules
- Reports are visible to OWNER and STAFF with the REPORTS permission.
- Screen 8 is the category-agnostic **Global Rental Reports** workspace.
- Top sections are exactly **Overview | Operations | Inventory | Customers | Billing | Activity** for OWNER. STAFF with Reports permission sees **Overview | Operations | Inventory | Customers | Activity**; Billing is hidden and its monetary APIs remain OWNER-only.
- Overview defaults to Operations Overview and may auto-load once after the single bootstrap request.
- Report definitions are generic; no current/future rental category may be hard-coded into report behavior.
- Selecting a Category exposes its active dynamic custom fields as filters using the authoritative Category Master metadata.
- Active booked quantity = booked_qty - closed_qty.
- Pickup Pending = active booked quantity - given_qty.
- Return Pending = given_qty - returned_qty.
- Missed Pickup = confirmed booking + Pickup Pending > 0 + pickup date passed.
- Overdue Return = Return Pending > 0 + return date passed. Missed Pickup and Overdue remain separate.
- Currently Out is derived from outstanding issued quantity, not status alone.
- Availability uses the selected date range and server-authoritative active commitments/outstanding issued quantity.
- Audit Report is OWNER-only even when STAFF has Reports permission.
- WhatsApp Activity represents system preparation success/failure only; it must not claim external WhatsApp delivery/read status.
- Billing V1 reports are allowed only in the OWNER-only Billing section. STAFF must not receive Billing monetary report data even with Reports permission. Broader revenue/profit/accounting reports remain out of scope.
- Initial results load 10 records and append the next 10 on bottom scroll.
- PDF format is Summary + Detailed Table with automatic Portrait/Landscape orientation.
- Generated PDF presentation uses a branded first-page header, Generated/Generated By metadata, wrapped Applied Filters, compact KPI summary cards, a Detailed Report table with content-aware column widths and wrapped rows, readable date/status labels, repeated continuation headers/table headers, and page-number footers.
- PDF layout changes must not alter report calculations, filtering, permissions or export request behavior.
- PDF is generated on-device from one explicit complete export request; reuse the cached export for unchanged filters.
- Saved presets support Owner My/Shared and Staff My-only visibility and include dynamic custom filters.
- Report Options reuse the shared/global filter UI: single-select criteria use the canonical searchable filter sheet, multi-select uses the shared multi-select sheet, and dates use the shared calendar field. Screen-local dropdown implementations are not allowed.
- Relative Date Range presets keep From/To hidden; From/To calendar fields appear only for Custom. A single valid Date Basis is hidden instead of rendering a redundant selector.
- Clear Filters resets only the current report criteria locally; filter editing/resetting must not cause report API calls until Generate/refresh.
- Reports use one bootstrap request, one request per generated page, no per-row calls, and no polling.

## Deployment and API safety
- Default deployment target is staging.
- Production deploy/migration/data change requires explicit user authorization.
- Staging D1 migrations are applied in order through the latest migration required by current `main`.
- Do not introduce polling.
- Prefer bundled/bootstrap APIs and cached/loaded state.
- UI-only expand/collapse and similar local presentation state must not generate backend calls.

## Related active docs
- `GLOBAL_UI_RULES.md`
- `UI_DESIGN_SYSTEM.md`
- `UI_GUIDELINES.md`
- `ADMIN_SCREEN_01_LOGIN.md`
- `ADMIN_SCREEN_02_DASHBOARD.md`
- `CUSTOMER_MASTER.md`
- `ADMIN_SCREEN_04_BOOKINGS.md`
- `ADMIN_ANDROID_SCREEN4_HARDENING.md`
- `ADMIN_SCREEN_05_ITEM_MANAGEMENT.md`
- `ADMIN_SCREEN_08_REPORTS.md`
- `ADMIN_SCREEN_09_USERS.md`
- `ADMIN_SCREEN_10_SETTINGS.md`
- `BOOKING_MODULE.md`
- `BILLING_MODULE.md`
- `DASHBOARD_MODULE.md`
- `ARCHITECTURE.md`
- `AUTH.md`
- `API_CALL_AUDIT.md`
- `DO_NOT_CHANGE.md`
- `PENDING_WORK.md`
- `CHANGELOG.md`
- `ANDROID_ADMIN_APP.md`
- `DEPLOYMENT.md`
