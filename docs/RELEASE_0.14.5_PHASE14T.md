# Zhagmag Dresses 0.14.5 — Phase 14T

Phase 14T improves task continuity and public mobile browsing on top of verified Phase 14S staging.

Admin Web improvements:
- Unsaved-change protection now also covers Category and Custom Field forms, Settings changes, and Pickup/Return quantity or note edits.
- Admin modules remember scroll position for the current browser session.
- Previous list search text can be restored with one explicit tap.
- Pickup and Return drawers show a persistent footer with selected quantity, pending quantity, and the primary Save action.

Public Web improvements:
- Mobile dress cards keep a portrait 4:5 photo ratio.
- The mobile hero is shorter so browsing starts sooner.
- Category chips stay available as a compact sticky rail.
- Item search stays visible while browsing the item list.
- Carousel motion pauses for reduced-motion preference, hidden tabs, and briefly after user interaction.
- Item details keep keyboard focus inside the dialog, support Escape to close, and return focus to the opening item.

Release identity remains 0.14.5. Admin Web and Public Web require staging rebuild/deploy. Worker and database schema are unchanged.
