\
# Staging Activation Candidate

This package does not silently create Cloudflare resources.

## First activation

1. Authenticate Wrangler or set Cloudflare credentials.
2. Create the staging D1 database once:
   ```bash
   npx wrangler d1 create zhagmag-dresses-staging --location apac
   ```
3. Confirm Wrangler can discover it:
   ```bash
   npm run staging:discover
   ```
4. Export a strong one-time Owner setup token:
   ```bash
   export INITIAL_OWNER_SETUP_TOKEN='use-a-long-random-value-here'
   ```
5. Run a no-cloud dry run:
   ```bash
   npm run staging:activate -- --dry-run
   ```
6. Activate staging:
   ```bash
   npm run staging:activate
   ```
7. Copy the deployed HTTPS URL into:
   ```bash
   export STAGING_BASE_URL='https://...workers.dev'
   ```
8. Run:
   ```bash
   npm run smoke:staging
   ```

## Existing staging database

If `zhagmag-dresses-staging` already exists, do not create it again.
`npm run staging:discover` resolves its UUID through `wrangler d1 list --json`.

## Owner already created

After the initial Owner account has been created, future deploys can use:

```bash
npm run staging:activate -- --skip-owner-secret
```

The Initial Owner API itself remains one-time only.

## Safety

- No production database is touched.
- No production config is used.
- No cloud resource is created automatically.
- Remote migrations occur only when `--dry-run` is not present.
- Deployment remains manual.
