# Admin Android App

## Identity
- Native Kotlin + Jetpack Compose.
- Version: **0.14.5**
- versionCode: **1**
- Visible UI: English-only unless explicitly changed.
- Backend: existing Cloudflare Worker + D1; no second backend/database.

## Screen status
- Screen 1 Login — implemented / finalized / locked.
- Screen 2 Dashboard — implemented / finalized / locked.
- Screen 3 Customers — implemented / finalized / locked.
- Screen 4 Bookings — implemented / finalized / locked.
- Screen 5 Category & Items — implemented / finalized / locked.
- Screen 6 Pickup — intentionally skipped.
- Screen 7 Returns — intentionally skipped.
- Screen 8 Reports — implemented / staging validated.
- Screen 9 Users — implemented / staging validated / finalized / locked.
- Screen 10 Settings — implemented / staging validated / finalized / locked.

## Roles/navigation
Active roles: OWNER and STAFF.
- OWNER has authorized full administration.
- STAFF sees only assigned operational modules.
- Users and Settings are OWNER-only.
- More is a hub destination: tapping bottom **More** from any More sub-page reopens the More hub.
- Screens 6 Pickup and 7 Returns stay hidden until implemented. Screen 8 Reports is visible only to OWNER or STAFF with Reports permission.
- Worker permission enforcement is authoritative.

## Shared UI rules
- Follow `GLOBAL_UI_RULES.md`, `UI_GUIDELINES.md` and specific Screen docs.
- Applicable More module pages such as Users, Settings and Reports reuse the Date | Day | Time row immediately below the Main Header before page content.
- Final-field Done uses the shared reliable keyboard-dismiss flow and must preserve values without closing the form/sheet.
- Shared `AppTextField` centrally enforces reliable Done-hide behavior for unspecified single-line final inputs while preserving explicit Next; `CompactSearchField` uses the same Done-hide contract.
- Visible errors are sanitized: raw server/network/DNS/exception/endpoint details are never shown to the operator.
- Shared transient Success/Error/Warning/Info popup feedback is action-result only. Passive page/tab/navigation/filter/expand/read state uses inline status/content and clears stale action feedback.
- Keep layouts compact and accessible.
- Avoid unnecessary helper cards/text.
- Dashboard uses shared Main Header refresh only.
- Dashboard operational section order is fixed: **Today Pickups → Today Returns → Missed Pickups → Overdue Returns**; sections are pending-work queues rather than completed-activity history.
- Booking tabs are **All | Reserved | Booked | Part Picked Up | Full Picked Up | Part Return | Full Returned | Overdue | Cancelled**.
- Reserved Booking Details includes full-width **Directly Pickup** below Cancel/Confirm.
- Return after Part Picked Up with unpicked quantity requires Keep Order Open / Close Remaining Items; history shows actual event item + qty.
- Dashboard/Booking shared quick actions use icon + label.
- Compact booking cards show first 2 item rows, +N more, Expand, and Collapse/Show less.
- Expand/collapse is local state with zero API calls.

## Contact/WhatsApp
- One customer Mobile Number only.
- Call is confirmation-first everywhere through the same shared popup: Person/User icon + Customer Name, Call icon + Mobile Number, Cancel | Call.
- Booking WhatsApp uses Loading → current-status group filtering → applicable template selection → shared preview → external WhatsApp/manual send; a single applicable template skips selection.
- Customer WhatsApp without a Booking uses the general-only Other group.
- Every WhatsApp tap must end in selector, preview or visible action error; Dashboard, Booking List and Booking Details reuse the same shared feedback contract as Customers.
- Shared WhatsApp busy state blocks rapid/repeated taps while templates are being prepared.
- Preview text equals text sent to WhatsApp.
- No screen-local hard-coded operational WhatsApp text.

## API/freshness
- No polling.
- Prefer bundled/bootstrap endpoints.
- Refresh only stale/affected domains after mutations.
- Local presentation changes do not call the backend.

## Validation
Admin Android runtime changes default to the staging APK workflow only. Documentation-only changes do not require an Android build when runtime/source files are untouched.

## Screen 8 Global Rental Reports
- Top sections: **Overview | Operations | Inventory | Customers | Activity**.
- Category-agnostic report catalog covers operations, availability/current-out/utilization, customer history/active/frequent/exception views, staff/WhatsApp activity, Owner-only Audit and operational exceptions.
- Category selection can expose dynamic custom-field filters without category-specific Android code.
- Missed Pickup and Overdue Return are separate quantity-derived operational conditions.
- Results load 10 then append +10; no polling or per-row calls.
- Saved report presets remain Owner My/Shared and Staff My-only and now preserve dynamic custom filters.
- Device-side Summary + Detailed Table PDF keeps auto Portrait/Landscape and View/Download/Share.
- Worker/D1 report data and calculations remain authoritative; Audit Report is OWNER-only.
- WhatsApp Activity reports preparation success/failure only, not external delivery/read state.
- Pricing/payment/revenue reporting remains out of scope.
