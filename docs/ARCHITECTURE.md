# Architecture

## Runtime surfaces
Zhagmag Dresses uses shared business infrastructure across:
- Public Website
- authenticated Admin Website
- authenticated native Admin Android
- Cloudflare Worker API
- Cloudflare D1

Admin Android and Admin Web reuse the same Worker/D1 backend. Do not create a second backend or database for Android.

## Authentication and authorization
Admin surfaces call authenticated Worker APIs. Worker-side session, role and module-permission checks are authoritative.

Current active roles are exactly:
- OWNER
- STAFF

Legacy ADMIN exists only as migration compatibility and is normalized/migrated to STAFF with applicable operational permissions preserved.

Users and Settings are OWNER-only. STAFF navigation is permission-aware, but UI hiding is only convenience; Worker authorization must independently reject unauthorized direct/deep/API access.

## Data authority
D1 is authoritative for:
- users/permissions;
- categories/custom fields/items/relations;
- customers;
- booking lifecycle;
- pickup/return quantities and history;
- inventory/availability calculations;
- settings, WhatsApp templates and audit history.

Availability is derived from inventory and booking/pickup/return state. Do not maintain a separately editable availability number.

## Public API boundary
Public endpoints expose only approved public catalog/availability/contact information. Private customer, booking, staff, audit and admin-only data must remain behind authenticated APIs.

Public availability display obeys Settings:
- Exact Quantity
- Status Only

New/unset configuration defaults to Exact Quantity; existing explicitly saved mode is preserved.

## Media
Item images use signed Cloudinary uploads. D1 stores metadata/public IDs/references; binary image content is not stored in D1.

## API efficiency
- no polling;
- prefer bundled/bootstrap endpoints;
- load on screen entry, explicit search/page change, approved mutation-stale refresh or manual refresh;
- local expand/collapse state never calls the backend.

## Deployment separation
Staging is the default validation target. Production deploy/migration/data changes require explicit user authorization and use separate production configuration/data.
