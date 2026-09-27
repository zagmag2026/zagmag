# Release 0.4.0 — Phase 4 Item Master

## Added
- Complete Item Master CRUD
- Category-driven dynamic item fields
- Quantity-based inventory
- Multiple photo URL records; first photo is primary
- Active / Inactive
- Public Show / Hide
- Search, filters and server-side pagination
- Safe archive / restore / permanent delete validation
- Quantity reduction protection against existing booking / given obligations
- Audit logging for item create/update/archive/restore/delete
- Staff read-only Item Master access
- Owner/Admin write permissions

## Preserved
- Choli initial category
- Size = Free Size default
- Pricing/payment disabled
- Laundry/repair/maintenance disabled
- Phase 2 authentication and role protection
- Phase 3 Category + Flexible Field Builder

## Storage note
Binary photos are not stored in D1. Phase 4 stores photo URLs so a free-tier source such as Cloudflare R2 can be connected later without redesigning the Item schema.

## Validation
- D1 migrations 0001 + 0002: PASS
- Foreign-key check: PASS
- Choli / Free Size seed: PASS
- Item inventory SQL smoke test: PASS
- Worker TypeScript syntax: PASS
- Admin TSX syntax: PASS
- Public TSX syntax: PASS
