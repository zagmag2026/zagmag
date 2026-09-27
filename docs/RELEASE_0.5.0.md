# Release 0.5.0 — Phase 5 Customer Master

## Added
- Customer Add / View / Edit / Update
- Primary mobile duplicate prevention, including +91 / 10-digit normalization
- Alternate mobile, address and notes
- Active / Inactive
- Safe Archive / Restore / Permanent Delete
- WhatsApp and Call quick actions
- Customer booking history foundation
- Server-side search, status filter and pagination
- Audit logs for customer mutations
- Staff can add/view/history; Owner/Admin control edits and destructive actions

## Preserved
- Generic category/item architecture
- Choli + Free Size seed
- Pricing/payment disabled
- Laundry/repair/maintenance disabled

## Validation
- D1 migrations 0001 + 0002: PASS
- Foreign-key check: PASS
- Choli / Free Size seed: PASS
- Duplicate mobile database constraint: PASS
- Customer history SQL smoke test: PASS
- Admin/Public/Worker TypeScript syntax+symbol check: PASS (temporary local stubs used because external npm packages were unavailable in the execution environment)
