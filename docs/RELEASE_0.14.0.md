# Release 0.14.0 — Phase 14 Staging Deployment Preparation

## Added
- Same-origin Cloudflare Worker + Static Assets deployment model
- Public site at `/`
- Admin site at `/admin/`
- API at `/api/*`
- Wrangler 4 static-assets configuration templates
- Staging config renderer
- Staging build asset packer
- Staging preflight validation
- Staging smoke-test script
- Manual-only GitHub Actions staging deployment workflow
- Staging deployment guide
- Production handoff template

## Why same-origin
Admin authentication uses secure HttpOnly cookies. Serving Admin and API on the same origin avoids cross-site cookie behavior and keeps staging setup simpler.

## Cloudflare efficiency
Static assets bypass Worker execution; only `/api/*` is configured to run the Worker first.

## Production status
This release is staging-ready preparation only. It does not contain account-specific Cloudflare IDs or secrets and does not deploy production.
