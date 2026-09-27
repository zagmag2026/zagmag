# Release 0.12.0 — Phase 12

## Added
- Owner/Admin Settings screen
- Dynamic Shop/Public branding
- Global Public Catalog On/Off
- Public WhatsApp and Call toggles
- Status-only or exact availability display mode
- Configurable Few Left threshold
- WhatsApp Template CRUD + Active/Inactive
- Five default free WhatsApp templates
- Read-only Audit Log screen
- Audit server-side filters and pagination
- Audit performance indexes

## Availability integrity fix
- Overdue items with pending physical return now continue to block future availability.
- Overlap reservations still reserve booked quantity; overdue non-overlap records reserve only `given - returned`.

## Preserved
- Generic rental architecture
- Choli current category + Free Size seed
- Quantity-based inventory
- No pricing/payment/deposit module
- No laundry/repair/maintenance module
- Public catalog without customer login
- Existing Dashboard, Booking, Pickup, Return and Reports behavior

## Migration
- `0006_settings_templates_audit.sql`

## Free-source rule
No paid WhatsApp API is added. Templates are reusable text for the existing free WhatsApp deep-link workflow.
