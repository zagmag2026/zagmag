# Phase 12 — Settings, Public Controls, WhatsApp Templates & Audit

## Settings
Owner can manage:
- Shop Name
- Website Title
- HTTPS Logo URL
- Contact / Call Number
- WhatsApp Number
- Address
- Default Language
- Date Format

Settings are stored as one JSON row (`settings.key = site_settings`) to keep the schema flexible. Existing Worker environment contact values remain fallbacks until the first Settings save.

## Public Website Controls
- Global Public Catalog On/Off
- Show/Hide WhatsApp
- Show/Hide Call
- Availability display: Status Only or Exact Quantity
- New/unset configuration defaults to Exact Quantity; preserve any existing explicitly saved mode.
- Configurable Few Left threshold

Category and Item public visibility remain controlled in Category Master and Item Master. Pricing remains disabled.

## WhatsApp Templates
Owner can add, edit, enable/disable and delete reusable templates.

Each template has:
- Language: **Gujarati | English | All**
- Linked To: one operational action
- Gujarati Message and/or English Message according to Language
- Active/Inactive state

For **All**, the generated message is Gujarati, one blank line, then English. The shared preview shows the exact generated message before WhatsApp opens.

Linked actions:
- General Inquiry
- Booking Confirmation
- Pickup Reminder
- Missed Pickup Reminder
- Return Reminder
- Overdue Reminder
- Pickup Done
- Part Pickup Done
- Return Done
- Part Return Done
- Thank You

Only one active template can be linked to one action. Inactive alternatives are allowed.

Supported placeholders:
- `{customer_name}`
- `{booking_no}`
- `{pickup_date}`
- `{return_date}`
- `{item_name}`
- `{qty}`
- `{pending_qty}`
- `{overdue_days}`
- `{shop_name}`

WhatsApp remains user-initiated. This does not add a paid WhatsApp API or automatic background messaging service.

## Audit Log
Audit Log is read-only in the UI and supports server-side filtering by:
- Module
- Action
- User
- From / To date
- Search across record/change values

Audit rows display user, role, action, module, record id, timestamp and old/new JSON values.

## API efficiency
- Settings page bootstrap bundles Settings + Templates + audit filter metadata in one API request / D1 batch.
- Audit rows are loaded only when the Audit tab is opened.
- Public catalog remains one HTTP catalog request; its D1 batch also reads public settings.
- No automatic polling is introduced.

## Public inquiry linkage
- Public Website WhatsApp Inquiry resolves through the central active General Inquiry template.
- Public contact number resolution is Saved Settings → environment fallback → hide/disable if unavailable.
- No hard-coded public inquiry message or phone number is allowed.
