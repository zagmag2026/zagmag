# Zhagmag Dresses 0.14.5 — Phase 14S Final UX Reliability

Phase 14S is an Admin Web UX reliability batch on top of verified Phase 14R staging.

- Internal tab navigation and Logout now respect unsaved Item, Customer, Booking, Add/Edit User and Reset Password work.
- User/password modals protect dirty fields on Close, backdrop, Escape and browser unload.
- Booking dates are canonicalized to Pickup = Today and Return = Next Day for new/reset forms, with Return >= Pickup + 1 day.
- The visible top bar shows `Admin Panel` only; the stale internal phase label is hidden from users.
- Booking Detail `Go to Pickup` / `Go to Returns` targets that booking. It uses the loaded queue first and only falls back to the existing All/Search controls when needed.
- Availability UI shows `Checking…` while date/item context is being refreshed, and the visual picker does not expose old exact quantities as current.
- Modal keyboard focus is contained and loading placeholders receive compact skeleton feedback with reduced-motion support.

Technical impact: version remains `0.14.5`; manifest phase is `14S`; Admin Web rebuild/deploy required; Public Web, Worker/API and D1 schema/migrations are unchanged. Phase 14S performs no direct API fetch of its own; the exact operational handoff can reuse one bounded existing list/search action when the target is not already loaded.
