# Production Handoff

Production deployment is intentionally separate from staging.

Before production:
1. Complete the full staging smoke-test checklist.
2. Create a separate `zhagmag-dresses-production` D1 database.
3. Use `worker/wrangler.production.toml.template`.
4. Generate a new production-only Owner setup token.
5. Keep production and staging D1 IDs/secrets separate.
6. Take/export a D1 backup before future schema migrations.
7. Do not reuse test customer data in production.
