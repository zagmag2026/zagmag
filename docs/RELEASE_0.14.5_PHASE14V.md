# Zhagmag Dresses 0.14.5 — Phase 14V Request Integrity Hardening

Phase 14V continues from the verified Phase 14U baseline and keeps release identity `0.14.5`.

## Public Web
- Existing catalog and availability requests now receive a 25-second client-side timeout guard.
- Public data interactions are serialized in the UI while one catalog/availability request is active, preventing overlapping user-triggered requests from racing and overwriting newer UI state.
- Search/category/pagination/availability actions are temporarily guarded while the active request is running; date inputs are temporarily disabled and restored afterward.
- The guard wraps only requests already initiated by the React application. It creates no additional Cloudflare/API request.
- Timeout recovery returns controls to an interactive state and shows the existing page error surface.

## Admin Web
- Phase 14U stale-while-refresh snapshots are now sanitized before caching.
- Snapshot IDs and ARIA/label reference attributes are removed so a stale visual copy cannot duplicate live DOM identities.
- Cached snapshots are explicitly `inert`, `aria-hidden`, keyboard-disabled and pointer-disabled by the existing Phase 14U styling.
- Snapshot images use lazy/async decoding to keep refresh presentation lightweight.

## Preserved rules
- Pricing/payment/deposit features remain OFF.
- Every booked item remains individually visible with its own thumbnail/details; no `+N more` collapsing is introduced.
- Compact ERP-style responsive Admin UI remains unchanged in business behavior.
- Phase 14U continuity/session behavior is preserved.

## Technical impact
- Admin Web changed.
- Public Web changed.
- Worker/API business source unchanged from Phase 14O.
- D1 schema/migrations unchanged.
- No new Cloudflare/API call path was added.
- Production is **NOT deployed** by this release work.
