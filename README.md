# Zhagmag Dresses

Current project version: **0.14.5**  
Admin Android versionCode: **1**

Zhagmag Dresses is a traditional-dress rental management system with a public catalog, authenticated Admin Web, native Admin Android, Cloudflare Worker APIs and D1 as the authoritative business datastore.

## Current product scope
- Brand: **ઝગમગ ડ્રેસીસ / Zhagmag Dresses**.
- Pricing/payment calculations are out of scope.
- Item Master is category-driven with dynamic custom fields.
- Customer workflow uses one mandatory Mobile Number; Alternative Mobile is not part of the current UI/workflow.
- Booking lifecycle supports reserve/confirm, direct pickup, partial/full pickup, partial/full return, cancellation before pickup, audit history and derived overdue state.
- Operational Call/WhatsApp actions use the single customer Mobile Number.
- Admin operational WhatsApp messages use the central linked-template engine; preview text must equal the text passed to WhatsApp.
- Public WhatsApp inquiry uses the central **General Inquiry** template.
- Public availability supports **Exact Quantity** or **Status Only**. For new/unset configuration the default is **Exact Quantity**; an existing explicitly saved mode is preserved.

## Current roles
Exactly two active roles exist:
- **OWNER**
- **STAFF**

Legacy `ADMIN` is retired. Existing legacy ADMIN rows are normalized/migrated to STAFF with applicable operational permissions preserved. Users and Settings remain OWNER-only.

## Admin Android status
- Screen 1 Login — implemented / finalized / locked.
- Screen 2 Dashboard — implemented / finalized / locked.
- Screen 3 Customers — implemented / finalized / locked.
- Screen 4 Bookings — implemented / finalized / locked.
- Screen 5 Category & Items — implemented / finalized / locked.
- Screens 6 Pickup, 7 Returns and 8 Reports — intentionally skipped for the current Android rebuild.
- Screen 9 Users — implemented / staging validated / finalized / locked.
- Screen 10 Settings — implemented / staging validated / finalized / locked.

Screen 10 validation evidence includes Admin Android Run #46 SUCCESS and Deploy Staging Run #153 SUCCESS. Staging migration `0015_whatsapp_template_linking.sql`, Worker deployment and staging smoke test were verified successful.

## Reports
Admin Android Screen 8 provides a **Flexible Report Generator** with report-aware filters/grouping/sort, +10 incremental results, saved presets, dynamic summaries and device-side Summary + Detailed Table PDF export with View/Download/Share.

## Database migrations
Apply **all migrations in `database/migrations/` in filename order through the latest migration required by current `main`**.

Current latest migration: `0016_report_presets.sql`.

Do not rely on an old hard-coded migration end number from historical release notes.

## Item images
Current Item Master image flow uses signed Cloudinary uploads:
- up to 8 optional images per item;
- D1 stores asset metadata/public IDs/references;
- binary image data is not stored in D1;
- first image is primary unless another saved image is made primary.

## Public contact resolution
For public Call/WhatsApp contact numbers:
1. saved Settings value;
2. environment fallback (`PUBLIC_CALL_NUMBER` / `PUBLIC_WHATSAPP_NUMBER`);
3. safely hide/disable the related action if neither exists.

No phone number is hard-coded.

## Deployment safety
- Default deployment/validation target is **staging**.
- Production is never implicit.
- Production deploy, production migration or production data change requires an explicit user instruction.
- Documentation-only changes do not require an Android build and do not trigger a production deploy.
- Minimize GitHub Actions, Cloudflare and D1/API usage.

See `docs/PROJECT_RULES.md` first before any change.

## Documentation authority
Current active rules are governed by:
`PROJECT_RULES.md` → current global/UI docs → current specific Screen docs → current module/architecture docs.

`_backups/`, `backups/` and `docs/RELEASE_*.md` are historical/rollback records only and do not override current active rules.
