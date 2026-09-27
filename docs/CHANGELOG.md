# Changelog

## 0.14.5 — Shared Order Preview, payment history/status and UI consistency batch (2026-09-21)

- added one shared read-only **Order Preview** reused by Billing Select Order and Bills List; Select Order now uses one-line **Order Preview | Create Draft** actions and removes duplicate Pickup/Return badges;
- Bills List linked cards now use **Order Preview | Edit/View** while legacy standalone Bills keep only their Bill action;
- removed the Splash card/board so the checking state shows only centered logo, brand name and loading spinner;
- fixed Booking Pending/Part/Full Payment tabs: Reserved is excluded, cancelled Bills are ignored, no-active-Bill Advance 0 is Pending and Advance > 0 is Part, while active Bills use authoritative Total Received vs Bill Amount;
- merged Billing/Payment audit events into Booking Details → History with amount/status old → new lines and existing user/time presentation;
- Booking Details → Details now reuses the shared Order Card with all items and no redundant View action;
- shared Order Card top-right now shows **Payment Status only**; lifecycle remains in Current, whose semantic colors are lifecycle-specific and do not reuse Pickup blue or Return amber;
- removed Pickup/Return Status from on-screen Bill Details while finalized Bill PDF now includes Pickup/Return Status + Date + Day + actual event Time;
- rearranged More into paired rows **Category & Items | Reports**, **Bills | WhatsApp Centre**, **Users | Settings** with existing permissions preserved;
- increased only shared tab label font to **16sp** while preserving Run #79 tab height, width, spacing, indicator and scrolling behavior;
- production remains untouched; version remains **0.14.5 / versionCode 1**.

## 0.14.5 — Screen-wise UI audit polish: Billing, Public, Reports and IME (2026-09-21)

- polished Billing list/header consistency: compact top-right Bill Date control, formatted list dates, final **Bill Amount / Balance Due** terminology, shared customer identity rows and Customer-first Select Order cards;
- removed nested Pickup/Return cards inside the Billing labeled section while preserving status/date/day information;
- normalized Public catalog base copy to canonical English so the runtime Gujarati/English i18n layer renders one complete language consistently, including WhatsApp preview actions;
- kept Public item-card **Call** visible on mobile when a Call number is configured and included it in narrow-screen action width;
- cleaned Reports UI by moving **Report Options** to the shared labeled-section pattern and removing the nested card around empty results;
- improved long-form keyboard behavior by expanding Screen 5 Custom Field/Item sheets and adding IME padding to Settings/WhatsApp Centre scrolling roots;
- eligible-order Billing read API now includes Customer Address so the shared identity hierarchy can render it when available; no D1 schema or business-rule change;
- production remains untouched; version remains **0.14.5 / versionCode 1**.

## 0.14.5 — Final Billing UI/PDF + Run #79 tabs + payment lifecycle hardening (2026-09-20)

- finalized Billing screen order as **Bill Header → Booking & Customer → Items (x) → Pickup / Return Status → Amount Summary → Notes → Actions** with compact shared section/card patterns;
- grouped all Bill items into one section with compact item rows, editable Draft Rent Rate and immutable finalized snapshots;
- replaced manual payment-status selection with derived totals: **Advance Received + Other Received = Total Received** and **Bill Amount − Total Received = Balance Due**; Balance Due is emphasized;
- enforced **Reserved = no Bill**, **Edit = Reserved-only**, **Booked onward = Draft Bill allowed**, and **100% Return required for Finalize** across Android + Worker;
- fixed Booking **Pending Payment | Part Payment | Full Payment** tabs to use authoritative received totals and exclude Reserved Orders;
- locked finalized Bill financial/history values and removed post-finalize mutation actions from the Android final view; finalized actions are **View | Share | Download | Print**;
- redesigned finalized Bill PDF to mirror the approved screen hierarchy, including Booking & Customer, Items (x), Pickup/Return, clear amount summary, Notes, branding/footer and print support;
- restored the validated **Admin Android Run #79** shared tab behavior: fixed tabs use equal-width `TabRow`; scrollable tabs use `ScrollableTabRow` with 0dp edge padding and 96dp minimum tab width; Booking Details uses fixed five-tab layout while Bookings/Reports/Settings/WhatsApp Centre remain scrollable where applicable;
- compact Booking identity/status cards now use Name → Mobile → optional Address → Booking No and left-aligned four-line Pickup | Current | Return metadata with icons and no time row;
- added D1 migration `0026_billing_notes.sql`; staging migration/deploy/smoke validation passed in Deploy Staging Run #227;
- production remains untouched; version remains **0.14.5 / versionCode 1**.

## 0.14.5 — Booking/Billing lifecycle + tab/input consistency hardening (2026-09-20)

- split the shared Admin Android tab family into two consistent width contracts: equal full-width non-scroll tabs and content-related horizontally scrollable tabs; both use centered 15sp labels, 44dp height and the same 3dp selected indicator;
- made shared numeric amount/quantity fields select the complete current value on focus so replacement typing does not require manual deletion first;
- Booking Advance keeps default actual value 0, uses placeholder **Advance Amount** when cleared and removes the old helper text;
- Booking Details actions use **Edit | Bill | Call | WhatsApp** where applicable, and Booking summary cards gain the lifecycle-aware Bill entry point;
- retired new standalone/direct Bill creation: More → Bills → Add Bill now lists only Orders without a Bill and creates one Booking-linked Draft;
- Full Return idempotently ensures the linked Draft Bill and hands Android to Edit Bill; a Billing handoff failure never rolls back the successful Return;
- Booking cancellation with Advance > 0 now requires Full Refund / valid Partial Refund / No Refund, preserves original Advance, records Refund/Retained settlement, moves any linked Draft/Final Bill to auditable Cancelled history;
- added D1 migration `0024_booking_billing_lifecycle.sql`; production remains untouched; version remains **0.14.5 / versionCode 1**.

## 0.14.5 — Admin Android tab/gallery/Billing shared-UI hardening (2026-09-20)

- unified fixed and scrollable Admin Android tabs behind one 44dp, 14sp, content-width, left-aligned, horizontally-scrollable renderer with one 3dp selected indicator;
- fixed full-screen Item gallery gesture ownership so a 1× one-finger horizontal drag pages images, while multi-touch/zoomed gestures retain pinch/pan behavior;
- aligned Billing V1 with shared Customer form, compact search, canonical form selectors, responsive pairs, shared Item rows, info rows and danger treatments;
- routed Billing WhatsApp launch through the existing failure-safe shared launcher and moved cancellation-reason handling into the shared destructive-confirmation family;
- added regression guards for all three fixes; Worker/D1/API/business behavior and production remain unchanged; version remains **0.14.5 / versionCode 1**.

## 0.14.5 — Billing V1 main integration (2026-09-20)

- merged Billing V1 to `main`: Item Master default Rent, Booking Advance, Booking-linked and Standalone Bills, Draft/Final/Cancelled lifecycle, cash-only Pending/Full Amount Received flow, ₹ Discount, financial-year Bill numbering and finalized Bill snapshots;
- added Billing List/Create/Edit/Details, Booking Details Bill entry, Bill PDF View/Share/Download and central Billing WhatsApp templates/placeholders;
- added OWNER-only Billing Reports while preserving STAFF non-financial report access;
- added D1 migration `0023_billing_v1.sql` and authoritative Worker validation/audit handling;
- staging Worker/D1 deploy and smoke validation passed; merged Admin Android staging APK validation passed;
- production remains untouched; version remains **0.14.5 / versionCode 1**.


## 0.14.5 — Phase 14CI form-validation / required-field hardening (2026-09-19)

- standardized required-field presentation across actively edited Admin Android forms with visible `*` markers plus concise field-local validation after interaction;
- Customer Add/Edit now distinguishes blank Name, blank Mobile and partial/invalid 10-digit Mobile while preserving the existing shared IME-safe form and ViewModel validation;
- Users Add/Edit and Reset Password now show field-local required/minimum-length guidance for Name, Mobile and Password;
- Screen 5 Category, Custom Field and Item editors now expose required/error state for their core fields; custom-field optional default values must remain valid for their selected type;
- New Item Total Quantity now starts blank and must be explicitly entered; explicit `0` remains valid, while blank can no longer silently persist as zero;
- Saved Report Preset Name now uses required marking and field-local blank-name feedback;
- WhatsApp Template Name/Linked Action/Gujarati Message/English Message are visibly required; Template save failures remain visible inside the open editor without a duplicate global error popup;
- existing dynamic custom-field typed validation and authoritative ViewModel/Worker business-rule validation remain intact;
- active docs and static regression guards were updated, including a guard against restoring the blank-quantity-to-zero behavior;
- Admin Android only; Worker/D1/API/schema unchanged; version remains **0.14.5 / versionCode 1**; production untouched.

Validation target: one Admin Android staging APK only.
## 0.14.5 — Phase 14CH confirmation / destructive-action hardening (2026-09-19)

- added shared `AppConfirmDialog` and `AppDestructiveConfirmDialog` primitives with consistent busy-safe outside/back dismissal, confirm/dismiss locking and danger treatment;
- renamed the reversible Customer action from misleading `Delete` wording to **Archive** in both card action and confirmation flow;
- strengthened Archived Customer permanent deletion with explicit irreversible copy plus exact typed **DELETE CUSTOMER** confirmation before `Delete permanently` is enabled;
- migrated Booking Cancel, User Archive, Settings discard/template delete, Reports preset delete, Screen 5 Related Group delete and Screen 5 Item delete to the shared destructive confirmation contract;
- preserved specialized multi-choice dialogs where behavior is materially different: Return `Keep Order Open | Close Remaining Items`, linked-category informational blocking, and custom-field two-stage `DELETE FIELD` removal;
- kept Reset Password as its explicit form flow without adding a redundant stacked confirmation;
- updated active docs and regression guards for confirmation semantics and strong customer deletion safety;
- Admin Android only; Worker/D1/API/schema unchanged; version remains **0.14.5 / versionCode 1**; production untouched.

Validation target: one Admin Android staging APK only.
## 0.14.5 — Phase 14CG loading / empty / error / retry hardening (2026-09-19)

- added the shared `LoadFailureState` for first-load failures with an explicit safe Retry action;
- hardened Booking Detail first View/Edit so the Date row and Back header stay visible, Loading is explicit, failure shows Error + Retry, and Retry targets the same booking id;
- corrected Booking List initial failure so it is not marked as a successful empty load and cannot show `No bookings found.` after a failed request;
- added first-load Retry states to Customers, Category & Items, Reports, Users and Settings, plus Audit Log-specific retry handling;
- gated customer/booking/user/audit empty states and initial counts so a failed or not-yet-completed read is never presented as a valid empty/zero result;
- preserved already loaded content across refresh/load-more failures and stopped automatic load-more retry loops after append errors until an explicit user-driven recovery starts a new request;
- removed empty load-more placeholder text rows from Customers and Bookings;
- added static regression guards and active documentation for loading/content/error/empty transitions;
- Admin Android only; Worker/D1/API/schema unchanged; version remains **0.14.5 / versionCode 1**; production untouched.

Validation target: one Admin Android staging APK only.
## 0.14.5 — Phase 14CF shared date + responsive action hardening (2026-09-19)

- expanded shared `AppDatePickerField` with readable `DD-MM-YYYY` display, optional weekday, min/max constraints and clear support while preserving ISO `yyyy-MM-dd` stored/API values;
- migrated active New/Edit Booking Pickup/Return dates, Category & Items dynamic DATE fields and Settings → Audit Log From/To away from screen-local date-picker implementations and onto the shared calendar field;
- preserved Booking date business rules (Pickup not before the business day; Return strictly after Pickup), made Reports/Settings From/To pairs responsive and mutually constrained, and added Audit Log reversed-range validation;
- hardened shared `StatusBadge` and `CompactMetaBadge` to remain single-line with safe ellipsis;
- added shared responsive soft-action grid and three-action layout primitives, preserving 44dp touch targets and semantic tones while moving crowded actions onto additional rows on narrow phones;
- applied responsive actions/badges to Customer cards, Booking/Dashboard quick actions, Booking Details, Dashboard overdue badges, Users identity/status badges and Reports saved-preset actions;
- no Worker/D1/API/schema/business-rule change; version remains **0.14.5 / versionCode 1**; production untouched.

Validation target: one Admin Android staging APK only.
## 0.14.5 — Phase 14CE Booking Detail refresh continuity (2026-09-19)

- removed the short blank-screen interval after Pickup/Return and other awaited Booking mutations by preserving the currently loaded Booking Detail while the authoritative refresh is in progress;
- kept the mutation busy lock and button spinner active through the refresh, so the previous double-submit fix remains intact while the visible content stays stable;
- added stale-refresh fail-safe behavior: if the post-mutation detail refresh fails, existing detail stays visible, further booking mutations are blocked on that stale state, an inline refresh error is shown, and `Retry Refresh` is available;
- disabled Pickup/Return quantity editing, All, notes and confirm actions while a mutation is busy or the detail is stale-blocked;
- added regression guards preventing `reloadDetailAwaited()` from clearing loaded detail data during refresh;
- Admin Android only; Worker/D1/API/schema unchanged; version remains **0.14.5 / versionCode 1**; production untouched.

Validation target: one Admin Android staging APK only.
## 0.14.5 — Phase 14CD mutation/customer-delete hardening (2026-09-19)

- globally hardened active Admin Android async mutation entry points so protected busy/loading state is acquired synchronously before coroutine launch, closing rapid/double-tap request races across Customers, Bookings, Reports, Users and Settings; Screen 5 keeps its already-safe synchronous mutation guard;
- fixed Pickup/Return stale-success behavior: save now awaits authoritative Booking Detail reload/apply before success feedback and before the busy lock is released, so old quantity controls cannot become clickable underneath a success popup;
- applied the same refresh-before-success pattern to other Booking lifecycle mutations that depend on updated detail state;
- added confirmation before restoring an Archived customer; confirmed restore continues to show the existing `Customer restored successfully.` transient success feedback;
- fixed Archived Customer Permanent Delete for the operational-lifecycle FK graph by deleting `booking_close_event_items` / `booking_close_events` in dependency order before booking items/bookings/customer;
- moved Archive/Restore lifecycle audit writes and Permanent Delete audit insertion into their respective D1 mutation batches and replaced misleading booking-archived/restored audit flags with booking-history-preserved metadata;
- added regression guards for restore confirmation, close-event cleanup, atomic customer audit batching, synchronous mutation latching and Pickup/Return refresh-before-success ordering;
- no D1 schema migration; version remains **0.14.5 / versionCode 1**; production remains untouched.

Validation targets: one staging Worker deploy and one Admin Android staging APK.
## 0.14.5 — Phase 14CC bottom-sheet consistency hardening (2026-09-19)

- added shared Admin Android `AppFormSheetScaffold` for standard editor-sheet title/close, scrollable body, fixed `Cancel | Save` footer, IME/navigation insets and busy-state action locking;
- migrated Category/Custom Field/Item editors, Users editor/reset-password and Settings WhatsApp template editor to the shared form-sheet structure where applicable;
- blocked Screen 5 Category/Custom Field and Users Reset Password sheet dismissal while their async mutation is busy;
- made Screen 5 Item Details and Related Group view sheets short-screen safe with a scrollable body and fixed full-width Close action;
- preserved existing form fields, validation, role/business rules, Worker/D1/API/schema behavior and production;
- added regression guards for shared scaffold ownership, busy-dismiss protection and long-sheet overflow safety;
- version remains **0.14.5 / versionCode 1**.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14CB button consistency hardening (2026-09-19)

- hardened shared Admin Android Primary/Secondary actions with visible **18dp** loading indicators instead of disabled-only busy states;
- constrained shared normal action icons to **18dp**, kept labels single-line with safe ellipsis, and enforced a **44dp** minimum interactive height;
- added reusable danger button/text-button treatments and applied them to destructive customer, booking, Item Management, Reports, Users and Settings confirmations;
- moved the generic compact New/Add action into the shared UI component layer while preserving its approved compact sizing and booking-specific wrapper;
- made Reports PDF **View / Share / Download** loading action-specific and blocked repeat PDF actions through the full export + local PDF operation;
- added regression guards for loading, icon sizing, danger treatment, shared New/Add ownership and Reports PDF busy-state behavior;
- Admin Android UI/component behavior only; Worker/D1/API/schema/business rules unchanged; version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14CA global popup, tabs & top-spacing consistency (2026-09-19)

- centralized applicable action feedback through the shared root-level `AppFeedbackHost`, preventing LazyColumn feedback items from shifting content or making popup placement appear screen-dependent;
- kept passive load failures inline and kept Customer Add/Edit form errors inside the open form instead of duplicating the same error as a page popup;
- clarified Customer success feedback to **Customer added successfully.** / **Customer updated successfully.**;
- added shared compact fixed/scrollable tab components and migrated Customers, Bookings, Booking Details, Category & Items, Reports, Settings main tabs and Settings WhatsApp sections;
- standardized compact tab typography/height and scrollable edge/min-width behavior;
- standardized the signed-in top rhythm to **8dp** between Main Header → Date row → Screen Title/Action → first content, removed empty feedback-row spacing, and added the Date row to the More hub;
- aligned the More-header logout action height with the shared refresh action treatment;
- preserved Date-row sizing/content, business logic, Worker/D1/API/schema and production; version remains **0.14.5 / versionCode 1**.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BZ compact Screen Title + New/Add hierarchy (2026-09-19)

- standardized applicable signed-in Admin Android page titles through shared `AppScreenTitle` at **20sp / 24sp / Bold**;
- standardized Back-header titles to the same typography and reduced the Back arrow glyph to **20dp**;
- compacted shared New/Add actions to **12sp / 16sp SemiBold**, **16dp** Add icon, **10dp × 6dp** padding and **4dp** icon/text gap so the action remains visually smaller than the title;
- moved Customers and New/Edit Booking away from duplicate one-off New Customer button styling to the shared compact action;
- unified Booking Details with the shared Back header and applied the title standard to Dashboard, Customers, Bookings, More, New/Edit Booking, Category & Items, Reports, Users, Settings and Booking Details;
- preserved the already-validated compact Date | Day | Time row unchanged;
- Worker/D1/API/schema/business rules unchanged; version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BY compact Date Day Time row (2026-09-19)

- compacted the shared Admin Android **Date | Day | Time** row used across applicable signed-in module pages;
- reduced the visual row from about 40dp to about 32dp using 11sp/14sp SemiBold text, 14dp icons, 24dp icon boxes/dividers, 4dp vertical padding and 4dp icon/text gap;
- preserved Date/Day/Time equal-width alignment, DD-MM-YYYY, full English day name, 12-hour am/pm display and Asia/Kolkata behavior;
- added no timer/polling and made no title/button, Worker, D1, API/schema or business-rule change;
- version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BX Back navigation hardening (2026-09-19)

- prevented Android system/gesture Back from falling through to Activity/app exit while Booking editor/detail actions are busy;
- added a safe Back path while Edit Booking detail is still loading or failed before content appears;
- aligned Related Items nested editor system Back with its visible Back action so it returns to Related Groups before More;
- added the shared compact Back header to Category & Items, Reports, Users and Settings More sub-pages;
- blocked busy User/WhatsApp-template sheet dismissal during protected mutations;
- added Settings unsaved-draft confirmation before leaving to More;
- Admin Android/navigation only; Worker/D1/API/schema/report calculations unchanged;
- version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BW Reports PDF action layout (2026-09-19)

- fixed the Screen 8 generated-report action area for narrow Android screens;
- first row is now **View | Share** using two equal-width buttons with short single-line labels;
- second row is a full-width **Download** button;
- preserved the generated PDF presentation, summary cards, report calculations, export caching, Worker/D1/API/schema and permissions;
- Admin Android UI only; version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BV Audit IST timezone correction (2026-09-19)

- kept D1 audit timestamps stored in UTC and corrected Admin Android + Admin Web Settings → Audit Log display to **Asia/Kolkata (IST)**;
- corrected Audit Log From/To filters to use IST calendar-day boundaries converted to UTC, including the 00:00–05:29 IST edge window;
- aligned Screen 8 **Audit Report** date filtering to the same IST→UTC boundary rule;
- converted Audit Report Date / Time output to IST while preserving chronological ordering and historical records;
- added regression coverage for the observed 05:01 UTC → 10:31 IST case and IST day-boundary conversion;
- no D1 schema migration or historical data rewrite; version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: staging Worker/web validation plus one Admin Android staging APK.


## 0.14.5 — Phase 14BV Global Reports PDF presentation (2026-09-19)

- redesigned the actual generated Screen 8 PDF while preserving report data and export behavior;
- added a branded first-page header with logo, shop/report identity and compact Generated / Generated By metadata;
- moved Applied Filters into a wrapped bordered block so long criteria no longer collapse into one truncated line;
- rendered Dynamic Summary as compact PDF KPI cards (3 columns portrait / 4 landscape);
- changed the Detailed Report table from equal-width columns to content-aware widths with wrapped headers and multi-line cells;
- formatted ISO date/date-time values for readability and reused human-readable semantic status/action labels;
- added alternating row backgrounds, repeated continuation-page report/table headers and page-number footers;
- Worker/D1/API/schema/report calculations/permissions unchanged; version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BU day-wise Working Hours (2026-09-19)

- added Settings → Basic → **Working Hours** for Monday through Sunday;
- each day supports Open / Closed plus opening and closing time through a Material time picker;
- added **Copy Mon → Weekdays** and **Copy Mon → All** quick actions;
- blocks invalid open-day ranges where closing time is not later than opening time;
- stores Working Hours inside the existing `site_settings` JSON with safe Closed defaults for unconfigured days;
- Worker validates the seven fixed day keys and HH:mm values; no new table or D1 schema migration;
- reused the existing Settings bootstrap/save request, with no polling or extra background API calls;
- version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: staging Worker/web validation plus one Admin Android staging APK.


## 0.14.5 — Phase 14BT compact Global Reports summary cards (2026-09-19)

- compacted Screen 8 dynamic summary KPI cards without changing the two-column grid or report data;
- changed each summary card to a single-row label/value hierarchy with compact shared padding;
- kept labels readable up to two lines for font scaling while keeping the metric value bold and single-line;
- no Report Options, result rows, PDF actions, Worker/D1/API/schema or business-rule changes;
- Admin Android/UI only; version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BS Global Reports reused filter UI (2026-09-19)

- replaced Screen 8 local dropdown/menu controls with the existing shared searchable single-select filter sheet;
- added reusable shared multi-select and calendar/date-picker controls and used them for dynamic report custom fields and Custom date ranges;
- hid redundant Date Basis controls when only one basis is valid and show From/To only when Date Range is Custom;
- added local Clear Filters for the current report while preserving no-API-on-filter-edit behavior;
- retained Item/Customer/general text search through the shared AppTextField and preserved all Global Rental Reports calculations, Worker endpoints, presets, pagination and PDF behavior;
- added regression guards preventing Screen 8-specific dropdowns from returning;
- Admin Android/UI only; Worker/D1/schema/version unchanged; production untouched.

Validation target: one Admin Android staging APK only.

## 0.14.5 — Phase 14BR WhatsApp template editor performance (2026-09-19)

- reorganized WhatsApp Templates into **Reservation | Booking | Pickup | Return | Overdue | Item | General | Language Settings** sections with Language Settings last;
- filtered template cards by selected section and scoped Add Template to non-language sections;
- replaced the full 37-action Linked To dropdown in Add/Edit with compact category-scoped linked-action chips;
- replaced eager 12–32 placeholder rows and Add GU/Add EN buttons with per-message **Insert Placeholder** dropdowns;
- placeholder dropdown options retain token + authoritative source and are composed only when opened, reducing initial popup/retyping recomposition work;
- preserved bilingual content, global language persistence, linked-action validation and one-active-template-per-action rules;
- Admin Android only; no Worker/D1/schema/API/version change; production untouched.

Validation target: Admin Android staging APK only.


## 0.14.5 — Phase 14BQ Global Rental Reports replacement (2026-09-19)

- replaced the previous eight-type Screen 8 report generator scope with category-agnostic Global Rental Reports sections: Overview, Operations, Inventory, Customers and Activity;
- added Operations Overview, Upcoming/Cancelled/Missed Pickup, Availability, Currently Out, Item Utilization, Low-use/Idle, Active Rentals, Frequent Customers, New vs Returning, Customer Exceptions, WhatsApp Activity, Owner-only Audit and Exception reports while retaining core Booking/Pickup/Return/Overdue/History/Category/Staff reports;
- made report lifecycle quantities authoritative from active booked quantity (booked minus closed), picked quantity and returned quantity, keeping Missed Pickup separate from Overdue Return;
- added dynamic Category custom-field filtering without category-specific report code and preserved those filters in Saved Presets/PDF filter context;
- added migration 0020_global_reports_whatsapp_activity.sql for Admin WhatsApp preparation success/failure reporting only; no external delivery/read claim or background polling;
- kept 10+10 result loading, explicit cached complete PDF export, Owner My/Shared and Staff My-only presets, REPORTS permission enforcement and Owner-only Audit access;
- activated cumulative Worker wrapper src/phase14bq-global-rental-reports.js and added Phase 14BP regression coverage;
- no pricing/payment/revenue scope was added; version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation target: staging Worker/D1 deploy and one Admin Android staging APK build.

## 0.14.5 — Phase 14BP WhatsApp template audit hardening (2026-09-19)

- hardened status-aware WhatsApp selection so Pickup Today / Return Today are date-gated and Missed Pickup / Reservation Cancelled special contexts are reachable from the generic selector;
- exposed Booking Updated and Booking Completed in the applicable status flows while preserving one-active-template-per-action enforcement;
- added website, customer-address, booking-notes/creator and availability placeholders with authoritative source metadata;
- blocked referenced placeholders whose runtime value is unavailable/blank instead of silently sending malformed customer-facing text;
- populated Public Inquiry related-items and requested-date availability data, preserving Status Only exact-quantity privacy;
- made operational chooser names honor configured Template Name;
- improved Android Settings template editing with grouped Linked To labels, placeholder source descriptions, explicit Add GU / Add EN insertion, and collision-safe internal keys for inactive alternatives;
- strengthened WhatsApp/Screen 10 regression coverage for the audited behavior;
- no D1 schema migration; production remains untouched; version stays **0.14.5 / versionCode 1**.

Validation target: staging Worker/web smoke plus Admin Android staging APK only.


## 0.14.5 — Phase 14BO Reports reliability hardening (2026-09-19)

- re-resolved saved relative report presets (`Today`, `Yesterday`, `This Week`, `This Month`) from the current authoritative business date when applied, preventing stale saved From/To dates;
- cleared stale report title/columns/summary metadata at the start of a new Generate action and stopped failed generation from being treated as a valid empty result;
- sanitized local PDF generation/view/download/share failures so raw platform exception messages are not shown to operators;
- aligned Reports lifecycle wording with the current app contract: Part Picked Up, Full Picked Up and Full Returned;
- strengthened the cumulative Admin Android foundation audit with regression guards for all four fixes;
- Admin Android only; no Worker/D1/schema/version change; production remains untouched; version stays **0.14.5 / versionCode 1**.

Validation target: one Admin Android staging APK build only.

## 0.14.5 — Phase 14BN WhatsApp no-silent-failure + web parity hardening (2026-09-19)

- made Android Dashboard, Booking List and Booking Details render a visible shared WhatsApp action error instead of silently stopping after template/API failure;
- propagated WhatsApp busy state into Booking List/Details and blocked rapid/repeated template requests;
- hardened status-group rendering so invalid/empty/unresolved templates are skipped while valid applicable siblings remain available; zero valid templates now returns a clear error;
- kept the canonical Reserved / Confirmed-Booked / Part Pickup / Picked Up / Part Return / Returned / Overdue / Cancelled / Other filtering rules unchanged;
- moved Public Website WhatsApp Inquiry to the central General Inquiry template endpoint with loading, visible error, preview and explicit Open WhatsApp/manual-send flow;
- removed page-local direct WhatsApp bypasses from supported Admin Web Dashboard, Customers, Bookings, Pickup, Return and Reports in favor of one reusable status-filtered selector/preview/error flow;
- added report identity fields needed by Admin Web WhatsApp composition without changing report calculations;
- added cumulative regression coverage for no silent exit, invalid-template skipping, zero-valid failure, status filtering and web parity;
- no new D1 migration; production remains untouched; version stays **0.14.5 / versionCode 1**.

Validation target: staging Worker/web smoke plus Admin Android staging APK only.


## 0.14.5 — Phase 14BM status-group WhatsApp + shared contact flow (2026-09-19)

- added authoritative WhatsApp status groups for Reserved, Confirmed / Booked, Part Pickup, Picked Up, Part Return, Returned, Overdue, Cancelled and general-only Other;
- changed generic Booking/Dashboard WhatsApp actions to detect current status and return only applicable active templates, with immediate loading, template selection when multiple apply, direct Preview when exactly one applies, and clear failure when none apply;
- added the requested status-group template set and new default templates through migration `0019_whatsapp_status_groups.sql`;
- added preferred double-brace placeholder aliases such as `{{customer_name}}`, `{{booking_id}}`, `{{items}}`, `{{status}}` and `{{business_name}}` while retaining legacy single-brace compatibility;
- standardized Call confirmation as Person/User icon + Customer Name, Call icon + Mobile Number, Cancel | Call;
- changed Booking Details contact row to Call left / WhatsApp right and removed the standalone Thank You button; Returned Thank You remains available as a WhatsApp template;
- preserved one bounded WhatsApp D1 batch, no polling, no pricing/payment scope, and version **0.14.5 / versionCode 1**;
- production remains untouched.

Validation target: staging Worker/D1 plus Admin Android staging APK only.


## 0.14.5 — Phase 14BL global IME / keyboard Done hardening (2026-09-19)

- moved reliable keyboard dismissal into the shared Admin UI component layer so final-field Done no longer depends on screen-local wiring;
- shared `AppTextField` now resolves unspecified single-line IME actions to Done and performs Compose focus clear + keyboard hide + Android IME fallback + post-focus retry before any caller-supplied Done action;
- shared `CompactSearchField` now uses Done with the same reliable dismiss behavior;
- preserved explicit Next navigation and added explicit Next/Done intent to applicable Users and Category & Items sequential fields;
- existing booking quantity Done handling reuses the shared helper through the compatibility delegate;
- no Worker/D1/business-rule/version change; production remains untouched and version stays **0.14.5 / versionCode 1**.

Validation target: Admin Android staging APK only.


## 0.14.5 — Phase 14BK passive feedback / notification hardening (2026-09-18)

- reserved the shared transient Success/Error/Warning/Info popup surface for explicit actions/mutations and their failures;
- prevented page open, tab/navigation changes, expand/collapse, filters/search/sort and other passive presentation changes from generating or replaying popup feedback;
- added a reusable normal-layout **InlineStatusMessage** with the existing semantic feedback colors/icons for passive state and read/load errors;
- changed Booking Details Pickup/Return completed/empty-state messages to inline status content and clear stale action feedback on manual tab switch;
- separated passive load/read errors from action/mutation errors across Customers, Category & Items, Reports, Users and Settings;
- preserved existing business logic, API contracts and server-authoritative state; no Worker/D1/version change;
- production remains untouched; version stays **0.14.5 / versionCode 1**.

Validation target: Admin Android staging APK only.

## 0.14.5 — Phase 14BJ UI validation + settings hardening (2026-09-18)

- added the reusable Date | Day | Time row below the Main Header on Reports, Users and Settings;
- simplified Reports to a title-only Report Generator header, removed the old eyebrow/subtitle/visible Back action, added relevant field icons and compacted selector/date controls;
- added optional Basic Settings **Website** with validated http/https URL persistence in the existing site_settings JSON;
- centralized user-facing network/server error sanitization so raw DNS/host/5xx/technical exception text is never rendered directly;
- changed Login validity so Sign in enables only after a valid 10-digit mobile and at least 8 password characters;
- strengthened the shared final-field Done keyboard dismiss flow with focus clear, keyboard hide, platform IME fallback and post-focus retry;
- changed first-use Admin branding fallback from **Zhagmag Dresses** to **Your Shop Name**;
- no D1 schema migration; production remains untouched; version stays **0.14.5 / versionCode 1**.

Validation target: one staging Worker deploy/smoke plus one Admin Android staging APK build.

## 0.14.5 — Phase 14BI More/Users compact navigation follow-up (2026-09-18)

- shortened the Screen 5 Related Items header action from **New Related Group** to **New Group** while keeping the shared compact New/Add component unchanged;
- simplified Screen 9 to a title-only **Users** header with the shared compact **Add User** action on the right, removing the old eyebrow/subtitle/visible Back button;
- made bottom **More** act as a hub action even while a More sub-page is already open, so tapping it again returns to the More options hub;
- no Worker/D1/business-rule/version changes; production remains untouched.

Validation target: Admin Android staging APK only; version remains **0.14.5 / versionCode 1**.

## 0.14.5 — Phase 14BH Booking related suggestions + reusable feedback/item UI (2026-09-18)

- added explicit-only Related Item suggestions nested beneath selected Main Items in New/Edit Booking using the existing directional Item relation mapping;
- bundled relation links into the existing Booking bootstrap so suggestions add no per-item network calls;
- added one canonical Item display row for thumbnail, Item Name, Item Code · Category, optional contextual status and × N quantity;
- standardized lifecycle Item status text such as `Booked N · Picked N · Returned N` across applicable Booking/Dashboard/Item surfaces;
- added reusable `+ N more` / `Show less` compact expansion with local-only state;
- unified Success/Error/Warning/Info transient feedback with semantic icon/color, consistent timing/animation/dismiss and duplicate suppression;
- compacted Pickup/Return and Current/Next booking badges with shared responsive side-by-side/wrapped presentation.

Validation: pending for this batch. Production remains untouched; version stays 0.14.5 / versionCode 1.


## 0.14.5 — Phase 14BG Settings / WhatsApp revision (2026-09-18)

- simplified Settings to a title-only header and hardened mobile scrollable tabs;
- replaced user-facing Logo URL editing with validated device image upload, preview/change/remove and branding refresh;
- centralized public availability states: Not Available / Few Left / Limited / Available, while Status Only hides exact quantity and Exact Quantity shows it;
- moved WhatsApp language to one global Gujarati / English / Both setting while requiring both message bodies on every template;
- expanded linked template actions with Reservation, booking update/cancel, due/pending reminders, final overdue follow-up, item availability and booking completion;
- added fixed action-specific placeholder registry and unresolved-placeholder blocking;
- refined the reusable compact New/Add action to content-fit width with balanced padding.

Validation:
- Settings WhatsApp Premerge Audit Run #6 / `35352230024` SUCCESS.
- Deploy Staging #157 / `35352526497` SUCCESS; migration `0018_settings_whatsapp_expansion.sql` applied; staging Worker `8952c29e-90a9-4e53-b5b8-6211950de811`; smoke PASS.
- Admin Android Build #52 / `35353584922` SUCCESS; artifact `zhagmag-admin-android-52`, ID `10551177341`, SHA256 `63a60b570905469454cdbb6178067f43cff3020319e047635f86c5a5bbb7f68f`.
- Production untouched; version remains 0.14.5 / versionCode 1.


## 0.14.5 — Pending improvements batch (2026-09-18)

- simplified More header to title-only;
- added linked-Item-aware Category delete confirmation/blocking;
- added affected-count Custom Field Deactivate / strongly-confirmed Remove Data & Delete Field flow;
- added explicit Edit Item Clear Value handling without weakening ordinary required-field validation;
- added Item View/Edit/Delete list actions while preserving authoritative history/archive delete rules;
- introduced one reusable bottom-sheet list-filter pattern and applied it to applicable Admin Android filters without changing filter semantics;
- moved New Related Group to the page header, added one-line dynamic Related Groups count, reused the shared back-header pattern, and unified Main/Related selection cards with compact side Remove.

Validation: pending for this batch.


Current product version remains **0.14.5 / versionCode 1**.

## 2026-09-18 — Screen 2/4 operational lifecycle hardening
- fixed Dashboard operational meaning/order to **Today Pickups → Today Returns → Missed Pickups → Overdue Returns** and made all four sections pending-work queues;
- excluded Reserved bookings from Today/Missed Pickup until confirmed;
- added quantity-derived Pickup Pending / Return Pending lifecycle behavior so returning everything picked so far no longer incorrectly closes an order with unpicked quantity;
- added explicit **Keep Order Open | Close Remaining Items** decision before Return when unpicked quantity remains, with auditable item-level remaining-item closure;
- added item + quantity details to Pickup/Return/closure History events;
- split Booking tabs into Part/Full Picked Up and Part/Full Returned states;
- added Reserved Booking Details **Directly Pickup** using the same booking;
- fixed Screen 8 PDF action cancellation so normal Compose coroutine cancellation is not surfaced as an error;
- added migration `0017_operational_lifecycle.sql` and cumulative Phase 14BE staging Worker wrapper;
- version remains **0.14.5 / versionCode 1** and production remains untouched.

Validation: pending for this batch.

## 2026-09-18 — Documentation consistency hardening
- reconciled active documentation against finalized Screen 1–5/9/10 rules;
- documented Screen 10 staging validation evidence from Deploy Staging Run #153;
- retired stale active-role references to ADMIN; current roles are OWNER/STAFF;
- removed active Alternative Mobile behavior;
- aligned Dashboard operational sections and shared actions;
- documented Android first-2-items + Expand/Collapse compact-card behavior;
- aligned New Booking search-first Category bottom-sheet and Directly Pickup label;
- aligned central WhatsApp template behavior, including Public Website General Inquiry;
- set new/unset public availability default to Exact Quantity while preserving explicit saved settings;
- made Category/Item writes and Pickup/Return historical corrections OWNER-only;
- aligned current migration instructions through latest current main (0015 at this change);
- clarified staging-default / production-explicit deployment policy;
- added missing governance docs referenced by PROJECT_RULES;
- preserved backup and historical RELEASE docs unchanged.

No runtime/source behavior, app version, versionCode or production deployment is changed by this documentation-only batch.

## 2026-09-18 — Screen 8 Reports implementation
- added permission-aware Admin Android Screen 8 Flexible Report Generator;
- added Bookings, Pickups, Returns, Overdue, Item History, Customer History, Category Stock and Staff Activity reports;
- added dynamic filters, grouping, sorting, summaries and +10 incremental result loading;
- added OWNER My/Shared and STAFF My-only saved report presets;
- added device-side Summary + Detailed Table PDF with automatic Portrait/Landscape plus View/Download/Share;
- added cumulative staging Worker report-generator APIs and migration 0016_report_presets.sql;
- production remains untouched; version stays 0.14.5 / versionCode 1.

### Screen 8 validation
- Premerge static audit Run #3 SUCCESS.
- Deploy Staging Run #154 SUCCESS; migration 0016 applied and staging smoke PASS.
- Staging Worker version: `81edac37-a1c3-44ce-8a7a-68ec2556bc39`.
- Admin Android Run #47 failed only on Kotlin visibility exposure; fixed without behavior change.
- Admin Android Run #48 SUCCESS.
- Artifact `zhagmag-admin-android-48`, ID `10540074427`, SHA256 `3ef67a6c806dcbf8379d71fcdb9d25c99731e8c74d77aeb29db0d1ef560f8700`.
- Screen 8 remains unlocked pending explicit user lock.
