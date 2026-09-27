# Zhagmag Dresses 0.14.5 — Phase 14O Final Production Gate

Phase 14O closes the remaining production-readiness gap without changing pricing scope or the D1 schema.

## Strict rental date rule

- Pickup Date remains user-selectable.
- Return Date must be at least one calendar day after Pickup Date.
- Admin UI keeps Return Date at Pickup + 1 day minimum and blocks invalid form submission before React/API handling.
- Public availability keeps Return Date at Pickup + 1 day minimum and blocks invalid availability checks.
- `worker/src/phase14o.js` is the final Worker entry and rejects same-day or reversed ranges for booking create/update, admin availability checks, and public availability checks.

## Release integrity

- `project-manifest.json` is aligned to Phase `14O` while version remains `0.14.5`.
- Staging/local/production Wrangler entrypoints use `src/phase14o.js`.
- Staging preflight and release verification require the Phase 14O Worker entry.
- Phase 14O regression is part of `npm run test:hardening`.
- Direct staging activation script syntax was repaired.

## Deployment impact

- Admin Web: rebuild/redeploy required.
- Public Web: rebuild/redeploy required.
- Worker: redeploy required.
- D1 migration: **not required**; Phase 14O adds no schema migration.

Production deployment remains separate from staging and should only proceed after the Phase 14O staging workflow and smoke test pass.
