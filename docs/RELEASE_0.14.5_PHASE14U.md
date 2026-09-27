# Zhagmag Dresses 0.14.5 — Phase 14U Native Continuity Cleanup

Phase 14U builds on verified Phase 14T and keeps release identity `0.14.5`.

Admin Web:
- Adds richer session-only previous-view memory for Items, Customers, Bookings, Pickup, Returns, Reports and Users.
- Restore is always explicit; Phase 14U performs no direct API fetch of its own.
- The previous page number is remembered and shown as context, but it is not auto-jumped because doing so through the existing React pager could create unnecessary Cloudflare requests.
- During list refreshes, the last rendered rows can remain visible as an inert, dimmed snapshot with a `Refreshing…` indicator instead of a blank content jump.

Public Web:
- Search, category, current page and Pickup/Return dates are initialized from session state before the first catalog request, so returning to the catalog does not require an extra default request followed by a restore request.
- Pickup defaults to Today and Return defaults to Next Day when no valid session state exists.
- Catalog refresh keeps the current item cards visible and temporarily inert while the next result is loading.
- Carousel reduced-motion, hidden-tab and post-interaction pause behavior is now owned directly by the React carousel components.
- The Phase 14T global `window.setInterval` interception is removed; Phase 14T retains only public detail-modal focus containment.

Technical impact:
- Admin Web changed.
- Public Web changed.
- Worker/API business source unchanged.
- D1 schema/migrations unchanged.
- No automatic Cloudflare calls are added by the Phase 14U continuity adapters.
