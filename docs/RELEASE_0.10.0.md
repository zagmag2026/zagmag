# Release 0.10.0 — Phase 10 Public Customer Website

## Added
- Live public category catalog
- Public item cards with photos
- Category-driven dynamic public fields
- Search and category filters
- Local item details modal without extra API call
- Bulk date-wise availability check
- Available / Few Left / Not Available only; exact stock is hidden
- WhatsApp inquiry with item/date prefill
- Call action
- Public pagination
- Mobile-first responsive catalog
- Optional contact numbers from Worker environment

## Efficiency
- Initial catalog = one bundled public API request
- Availability = one bulk request for all visible page items
- No auto polling
- Item details modal uses already-loaded catalog data
- Short cache for public catalog; no cache for availability

## Security
- No public customer login required
- No customer/booking/staff/audit private data returned
- Only active + public categories/items/fields are exposed
- Pricing remains disabled

## Database
No new D1 migration is required for Phase 10.
