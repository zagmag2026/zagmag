# Deployment

## Default target
Staging is the default deployment and validation target.

Production is never implicit. Production deploy, production D1 migration, production configuration change or production data change requires an explicit user instruction.

## Staging sequence
Use the current repository workflow/scripts and current environment-specific configuration.

Required principles:
1. keep staging D1/config/secrets separate from production;
2. run the cumulative static/hardening regression before deployment;
3. build staging assets;
4. render/validate staging Wrangler configuration;
5. apply **all migrations in `database/migrations/` in order through the latest migration required by current `main`**;
6. deploy staging Worker/static assets;
7. run the staging smoke test;
8. inspect exact workflow/job logs before declaring success.

Current latest migration is `0017_operational_lifecycle.sql`.

Do not copy old migration end ranges such as 0008/0009 from historical docs into current deployment instructions.

## Current GitHub Actions staging workflow
`.github/workflows/deploy-staging.yml` supports:
- manual `workflow_dispatch`;
- a narrowly scoped push trigger on `.github/staging-run-trigger` on `main`.

Normal source/documentation pushes do not automatically deploy staging unless they modify that trigger path or the workflow is manually dispatched.

## Production guardrail
Before any explicitly authorized production deployment:
- staging validation must pass;
- use separate production D1/config/secrets;
- take/export a D1 backup before schema migration;
- apply only the intended current migrations;
- never point production traffic at staging resources;
- never reuse staging/test data as production operational data.

Documentation-only changes do not require Android builds and do not authorize any deployment by themselves.
