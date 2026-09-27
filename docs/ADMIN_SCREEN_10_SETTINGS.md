# Admin Android — Screen 10: Settings

Status: **Implemented / staging + Android validated / finalized / locked**

## Scope
- Screen 10 is **Settings** and is visible to **OWNER only**.
- Existing OWNER/STAFF permissions, audit behavior and unrelated locked screens remain unchanged; Billing V1 is the explicit pricing/payment exception defined in `BILLING_MODULE.md`.
- Screen 10 reuses the existing Worker/D1 Settings, Cloudinary signing and Audit Log infrastructure.
- WhatsApp template administration is a separate OWNER-only **More → WhatsApp Centre** module while reusing the same Worker/D1 template infrastructure.
- Android version remains **0.14.5 / versionCode 1**.
- Production remains untouched until an explicit production request.

## Header and tabs
- Immediately below the Main Header, show the reusable **Date | Day | Time** row, then the shared compact Back header with **Settings**.
- Do not show ADMINISTRATION, the former subtitle, or a large standalone Back button; use the shared compact Back icon/header instead.
- Visible Back and Android/system/gesture Back return to More when there are no unsaved settings edits.
- If the Basic/Public settings draft differs from the last loaded/saved settings, Back requires **Discard unsaved changes?** confirmation; Keep editing remains on Settings.
- Back/dismiss is blocked while protected Settings/template mutations or logo upload are in flight.
- Settings tabs remain exactly: **Basic | Public Website | Audit Log**.
- **WhatsApp Templates is not a Settings tab.** Template/list/language management lives in **More → WhatsApp Centre**.
- Tabs use a responsive horizontally scrollable row with non-clipping minimum widths on mobile.

## Basic
Owner can manage Shop Name, Website Title, Website, Business Logo, Contact / Call Number, WhatsApp Number, Address, Default Language, Date Format and day-wise Working Hours.

### Website
- **Website** appears immediately after Website Title and before Business Logo.
- It is optional when no business website is configured.
- When entered, it must be a valid `http://` or `https://` URL with a host.
- Invalid Website input is blocked before Save and shows concise user-facing validation.
- The value is stored inside the existing `site_settings` JSON; no D1 schema migration is required.

### Business Logo
- Logo management is upload-first; there is no user-facing raw Logo URL textbox.
- Owner can select an image from the Android device/gallery/file picker.
- Supported types: **JPG / JPEG, PNG, WebP**.
- Client maximum: **8 MB**; Worker-signed Cloudinary limits remain authoritative.
- Current saved logo is previewed.
- Actions: **Upload Logo / Change Logo / Remove Logo**.
- Uploaded URL is saved through normal Settings persistence.
- Saved logo is reused by the public catalog header and Admin app branding/top bar through the existing branding response.
- Removing the logo clears the saved branding URL; it does not delete Item records or Item images.

### Working Hours
- Working Hours are configured day-wise for **Monday through Sunday**.
- Every day has an **Open / Closed** toggle.
- Open days use a time picker for **Opening Time** and **Closing Time**; manual free-text time entry is not used.
- Closing time must be later than opening time before Basic Settings can be saved.
- Closed days keep their last configured times but do not expose them as active opening hours.
- Quick actions are **Copy Monday to Mon–Fri** and **Copy Monday to All Days**.
- New/unconfigured Working Hours default to **Closed** for safety; the editor retains 09:00–20:00 as the initial time values when a day is enabled.
- Working Hours are stored inside the existing `site_settings` JSON; **no D1 schema migration** is required.
- Saving Working Hours reuses the existing Settings save request and creates no polling or extra background API calls.

## Public Website
Owner can manage Public Catalog, Show WhatsApp, Show Call and Availability Display: **Status Only / Exact Quantity**.

New/unset availability configuration defaults to **Exact Quantity**; an explicitly saved Owner mode is preserved.

### Availability display
The authoritative shared status rule is:
- 0 → **Not Available**
- 1–2 → **Few Left**
- 3–5 → **Limited**
- 6+ → **Available**

- **Status Only** returns/displays only the status label and never exposes the exact available quantity.
- **Exact Quantity** returns/displays the actual available quantity.
- Thresholds are centralized in Worker logic; the previous editable Few Left Threshold control is retired from current Settings UI.
- Pricing/payment fields remain out of scope.

## More → WhatsApp Centre
- This OWNER-only module contains the existing template list, Add/Edit/Delete, linked-action/status grouping, Gujarati + English message content, Active/Inactive state and Global Template Language.
- Moving the module does not change Dashboard/Customer/Booking/Pickup/Return WhatsApp composition, filtering, preview or launch behavior.


### Global WhatsApp Template Language
Language is configured **once globally**, not per template.
- **Gujarati** → Gujarati message only.
- **English** → English message only.
- **Both** → Gujarati message, one blank line, then English message.

Every template always stores **both Gujarati and English content**. The legacy per-template language_mode column remains compatibility storage only and is not exposed as an editable setting.

The generated message is shown in the shared WhatsApp Preview before external WhatsApp is launched. Previewed and sent text must be identical.

### Template list UI
- Do not show the former Gujarati · English · All summary line.
- Do not show a list-level placeholder information card.
- Inside WhatsApp Templates, use the horizontally scrollable sections in this exact order: **Reservation | Booking | Pickup | Return | Overdue | Item | General | Language Settings**.
- **Language Settings is always the final section** and contains the one global Gujarati / English / Both setting.
- Template sections show only templates whose linked action belongs to that section; Language Settings does not show Add Template.
- The large standalone **WhatsApp Templates** heading is not shown; the page title is **WhatsApp Centre**.
- **Add Template** uses the global reusable compact New/Add action in the top **WhatsApp Centre** back-header row on the right for template sections; Language Settings does not show Add Template.
- Add/Edit uses compact linked-action chips scoped to the current section instead of the former all-actions Linked To dropdown.
- Existing cards keep established badges and Edit/Delete actions.
- Templates are marked bilingual because both message bodies are required.

### Template form validation
- Template Name, Linked Action, Gujarati Message and English Message are required; required labels are visibly marked with `*`.
- Add/Edit Template Save remains disabled until all required values are valid.
- After interaction, blank Template Name / Gujarati Message / English Message show field-local required messages.
- Worker placeholder/linked-action validation remains authoritative. When Template Save fails while the editor is open, the server validation message is shown inside the editor and is not duplicated as a second global error popup.
### Status-group template mapping
Operational templates are grouped centrally by the Worker registry. The template table continues to store a linked action; status-group membership is derived from that registry and is not duplicated into a per-row status_group column.

- **Reserved** → Reservation Confirmation, Reservation Reminder, Reservation Expiry Reminder
- **Confirmed / Booked** → Booking Confirmation, Booking Details, Pickup Reminder, Pickup Today, Pickup Ready
- **Part Pickup** → Partial Pickup Confirmation, Remaining Pickup Reminder, Remaining Pickup Today, Remaining Pickup Ready
- **Picked Up** → Pickup Confirmation, Return Reminder, Return Today, Return Date/Time Update
- **Part Return** → Partial Return Confirmation, Remaining Return Reminder, Remaining Return Today, Pending Return Reminder
- **Returned** → Return Confirmation, Thank You, Feedback / Review
- **Overdue** → Overdue Reminder, Urgent Reminder, Follow-up Reminder, Final Reminder
- **Cancelled** → Cancellation Confirmation, Cancellation Details
- **Other** → Shop Address, Working Hours, General Information, Thank You, Holiday / Shop Closed, Contact Us, Custom General Message

**Other is general-only. Order-status-specific templates must not be mapped into Other.**

Only one **active** template may be linked to one linked action at a time. Inactive alternatives remain allowed. Pricing/payment/deposit/balance template types are excluded.
New alternatives use an internal unique template key derived from Linked To + Template Name so inactive alternatives do not collide with the seeded primary template key.

### Placeholder registry
Preferred operator-facing syntax is double-brace. Supported placeholders include:
`{{shop_name}}`, `{{business_name}}`, `{{shop_phone}}`, `{{shop_whatsapp}}`, `{{shop_address}}`, `{{website_title}}`, `{{website_url}}`, `{{customer_name}}`, `{{customer_mobile}}`, `{{customer_address}}`, `{{booking_no}}`, `{{booking_id}}`, `{{booking_status}}`, `{{status}}`, `{{booking_date}}`, `{{booking_notes}}`, `{{booked_by}}`, `{{pickup_date}}`, `{{return_date}}`, `{{pickup_time}}`, `{{return_time}}`, `{{item_name}}`, `{{items}}`, `{{item_code}}`, `{{category_name}}`, `{{qty}}`, `{{availability_status}}`, `{{available_qty}}`, `{{pending_qty}}`, `{{picked_qty}}`, `{{returned_qty}}`, `{{overdue_days}}`, `{{related_items}}`, `{{today_date}}`, `{{staff_name}}`.

The Worker accepts legacy single-brace placeholders for backward compatibility, but Settings displays the double-brace form.

Rules:
- Each placeholder has one fixed authoritative source in the Worker registry.
- The editor shows only placeholders relevant to the selected linked action.
- Gujarati Message and English Message each have a compact **Insert Placeholder** dropdown. Opening it shows token + authoritative source and inserts into that specific message.
- Placeholder option rows are composed only while the dropdown is open; the Edit/Add popup must not eagerly render the full 12–32 placeholder list on initial open or while typing.
- `{{available_qty}}` is populated only when Exact Quantity mode is active; Status Only never exposes exact quantity.
- Unknown placeholders are rejected on save.
- Known placeholders that are not valid for the selected action are rejected.
- An unresolved raw placeholder is never sent; composition fails safely instead.
- A referenced placeholder whose authoritative value is unavailable/blank also fails safely instead of silently sending malformed text.
- Multiple items use a readable line/list summary.
- No pricing/payment placeholders are supported.

### Operational composition
- Booking/Dashboard WhatsApp button uses the authoritative current Booking status to select one status group and returns only active templates from that group.
- `Pickup Today` and `Return Today` are offered only when the matching authoritative date is today.
- Past-due pickup work exposes `Missed Pickup Reminder`; cancelled reservations expose `Reservation Cancelled`; returned bookings may expose `Booking Completed`; confirmed/booked records may expose `Booking Updated`.
- Item-context composition may expose `Item Availability Reply`; requested-date availability placeholders use the same inventory semantics as Public Availability.
- Customer-card WhatsApp without a Booking uses the **Other** general-message group.
- Flow: WhatsApp Button → Current Status → Filtered Template List → Select Template → Message Preview → Open WhatsApp → Manual Send.
- If exactly one applicable template is active, skip the template-selection sheet and open Preview directly.
- If no applicable active template exists, fail visibly and do not launch WhatsApp.
- The existing explicit linked-action composition path remains available for system-specific/manual contexts and backward compatibility.
- Public Website inquiry remains linked to General Inquiry.
- Composition remains one bounded D1 batch after explicit user action; no per-card/background polling.

## Audit Log
- Read-only server-side search/filter.
- D1 audit timestamps remain stored in **UTC**.
- Admin Android and Admin Web convert each Audit Log timestamp from UTC to **Asia/Kolkata (IST)** before display.
- Module, Action and User filters; From/To date filters use date pickers.
- Audit Log From/To reuse the shared responsive `AppDatePickerField`, support clear/reset, constrain each other, and block Apply when From is after To.
- From/To dates are interpreted as **Asia/Kolkata local calendar days**. Worker converts the local day start and next-day exclusive boundary to UTC before querying `audit_logs.created_at`, preventing midnight 00:00–05:29 IST records from falling into the previous displayed day.
- Initial batch is 10 rows and load-more appends up to 10.
- No polling.
- Old/New values expand in place.

## API / D1 efficiency
- Settings bootstrap remains one bundled request.
- Audit rows load only after opening Audit Log.
- WhatsApp composition performs one bounded D1 batch only after user action.
- No per-card polling or background composition.
- Logo upload reuses signed Cloudinary upload; no binary image is stored in D1.

## Safety
- Settings/WhatsApp Templates/Audit APIs remain OWNER-only.
- Worker/D1 linked-action and placeholder validation are authoritative.
- Template preview is required before WhatsApp launch.
- Billing V1 is the approved pricing/payment exception. Broader pricing/accounting remains out of scope.
- Production remains untouched until explicit production request.

## Current validation
- Baseline before this explicit revision: Phase 14BF main `6015da277736fbfa95dcea04f4c8ec3b735f4434`.
- Settings WhatsApp Premerge Audit Run #6 / `35352230024` — SUCCESS.
- Deploy Staging #157 / `35352526497` — SUCCESS; migration `0018_settings_whatsapp_expansion.sql` applied; staging Worker version `8952c29e-90a9-4e53-b5b8-6211950de811`; cumulative hardening + Phase 14BG regression PASS; public/API/browser smoke PASS.
- Admin Android staging APK Build #52 / `35353584922` — SUCCESS; job `105627359721`; `:app:assembleStagingDebug` BUILD SUCCESSFUL.
- Artifact `zhagmag-admin-android-52`, ID `10551177341`, SHA256 `63a60b570905469454cdbb6178067f43cff3020319e047635f86c5a5bbb7f68f`, size 20,419,645 bytes, expires 2026-09-25.
- Production remained untouched; version remains 0.14.5 / versionCode 1.

## Locked public contact/template behavior
- Public Website WhatsApp Inquiry uses the central General Inquiry template; no screen-local hard-coded public inquiry message is allowed.
- Public Call/WhatsApp number resolution remains Saved Settings → environment fallback → safely hide/disable when unavailable.
- Phase 14BG validation is complete; this revised Screen 10 contract is protected current behavior.

## Confirmation consistency
- `Discard unsaved changes?` and WhatsApp-template Delete reuse the shared destructive confirmation contract.
- Destructive dialogs keep their dismiss action disabled while a protected mutation is busy.
- Working-hours time picker and other non-destructive Apply dialogs are not danger confirmations.
## Loading / empty / retry behavior
- Initial Settings bootstrap failure shows Error + Retry while keeping the Back path available.
- Audit Log initial failure shows Error + Retry; `No audit records found.` and the audit record count appear only after a successful audit load.
- Settings/Audit refresh failures preserve already loaded content and render passive inline errors.
- Audit load-more failure preserves existing rows, stops automatic append retries and shows **Retry loading more** for the failed next page.

## Billing V1 WhatsApp Centre extension
The existing OWNER-only **More → WhatsApp Centre** adds a **Billing** section without creating a second template system.

Billing linked actions:
- Bill Details
- Payment Pending
- Full Amount Received
- Bill Cancelled

Supported Billing placeholders include Bill No/Date, Total Rent, Discount, Net Amount, Advance Amount, Balance Amount and Payment Status. Bill preparation reuses the central bilingual templates and global language setting. WhatsApp Activity continues to record only Prepared/Failed system preparation and never external delivery/read status.


## Manual refresh scope
- Pull-to-refresh is enabled for Audit Log and WhatsApp template-list sections.
- Pull-to-refresh is intentionally disabled on editable Basic, Public Website and WhatsApp Language Settings drafts.


## WhatsApp Template editor interaction hardening
- Add/Edit Template compares Template Name/Key, Linked Action, Gujarati/English messages and Active state against the opening values; a changed editor requires **Discard unsaved changes?** before dismiss.
- The Linked Action row is a horizontal LazyRow. Opening/editing a template automatically scrolls the currently selected Linked Action into view while preserving manual horizontal scroll.
- Gujarati/English message fields stay multiline; a visible **Hide Keyboard** action provides dependable keyboard dismissal without removing newline support. Save also dismisses the keyboard through the shared form scaffold.
