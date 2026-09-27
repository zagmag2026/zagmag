# Public Catalog

## Public data boundary
The public catalog exposes only approved public item/catalog/availability/contact information.

Private customer, booking, staff, audit, user, internal permission and admin-only custom-field data must not be returned by public endpoints.

## Availability
Public availability is derived from authoritative inventory plus booking/pickup/return state.

Owner Settings controls the display mode:
- **Exact Quantity**
- **Status Only**

For new/unset configuration the default is **Exact Quantity**. If the Owner has explicitly saved Status Only or Exact Quantity, preserve that saved choice.

Status Only renders only centralized semantic states and never returns/displays the exact quantity: **Not Available = 0**, **Few Left = 1–2**, **Limited = 3–5**, **Available = 6+**. Exact Quantity exposes the actual currently available quantity for the selected date range. These thresholds are a shared Worker rule rather than an editable per-screen threshold.

Do not expose pricing/payment calculations; they remain outside current scope.

## Public contact
Public contact number resolution:
1. saved Settings value;
2. environment fallback (`PUBLIC_WHATSAPP_NUMBER` / `PUBLIC_CALL_NUMBER`);
3. safely hide/disable the related action if neither value exists.

No public phone number is hard-coded.

## WhatsApp inquiry
Public Website WhatsApp Inquiry uses the central active **General Inquiry** template.

Language rendering follows the one global WhatsApp Template Language setting:
- Gujarati = Gujarati message only
- English = English message only
- Both = Gujarati, one blank line, then English

Every template stores both Gujarati and English content.

Do not use a separate hard-coded public inquiry message. The generated message should include only approved placeholders/context and remain user-initiated; no paid/automatic WhatsApp API is introduced.

## Availability request safety
Public availability requests must:
- validate date range and item identifiers;
- use authoritative Worker/D1 calculations;
- avoid returning private/admin-only fields;
- avoid polling.
