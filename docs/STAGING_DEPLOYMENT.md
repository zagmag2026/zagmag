# Phase 14 — Staging Deployment

## Architecture
Staging is intentionally deployed as one Cloudflare Worker origin:

- `/` — public customer website
- `/admin/` — authenticated Admin website for OWNER/STAFF access
- `/api/*` — Worker API
- D1 binding — `DB`

This avoids cross-site session-cookie problems between separate `pages.dev` and `workers.dev` origins. Static assets are served by Workers Static Assets; only `/api/*` is configured to run the Worker first.

## One-time Cloudflare setup
1. Authenticate Wrangler:
   `npx wrangler login`
2. Create the staging D1 database:
   `npx wrangler d1 create zhagmag-dresses-staging --location apac`
3. Copy the returned D1 database UUID.
4. Put it in environment variable `STAGING_D1_DATABASE_ID`.
5. Create a long random `INITIAL_OWNER_SETUP_TOKEN`.
6. Render config:
   `npm run render:staging-config`
7. Build:
   `npm run build:staging`
8. Preflight:
   `npm run preflight:staging`
9. Apply all migrations in `database/migrations/` in order through the latest migration required by current `main`:
   `npx wrangler d1 migrations apply DB --remote --config worker/wrangler.staging.generated.toml`
   Current latest migration is `0016_report_presets.sql`.
10. Set setup token:
    `printf '%s' "$INITIAL_OWNER_SETUP_TOKEN" | npx wrangler secret put INITIAL_OWNER_SETUP_TOKEN --config worker/wrangler.staging.generated.toml`
11. Deploy:
    `npx wrangler deploy --config worker/wrangler.staging.generated.toml`

## GitHub manual deployment
The included workflow `.github/workflows/deploy-staging.yml` supports manual `workflow_dispatch` and a narrowly scoped push trigger on `.github/staging-run-trigger` on `main`. Normal source/documentation pushes do not automatically deploy staging.
It uses `npm install` with exact pinned direct dependency versions because this release does not ship a generated lockfile.
Use `configure_owner_setup_secret=true` only on the first deploy or when deliberately rotating that setup secret.

Required GitHub repository secrets:
- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`
- `STAGING_D1_DATABASE_ID`
- `INITIAL_OWNER_SETUP_TOKEN`

Optional repository variable:
- `STAGING_BASE_URL`

Set `STAGING_BASE_URL` after the first deploy if you want the workflow to run the smoke test.
The smoke-test input defaults to false so the first deployment does not fail before that URL is known.

## First Owner
After deployment, open `/admin/`.
Use the Initial Owner setup form once with the setup token. Once a user exists, the API refuses additional initial-owner creation.

## Production guardrail
Do not point production traffic at staging. Production must use a separate D1 database and production Wrangler config. Production deploy/migration/data change requires an explicit user instruction.


## Phase 14B activation helper

For the guided activation path, see `docs/STAGING_ACTIVATION.md`.

The helper intentionally discovers the D1 UUID using `wrangler d1 list --json`.
It will not auto-create the D1 database because resource creation should remain an explicit owner action.
