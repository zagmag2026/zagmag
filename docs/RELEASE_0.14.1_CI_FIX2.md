# Release 0.14.1 — Phase 14B CI Fix 2

## Fix
Cloudflare Workers rejected the previous PBKDF2 password-hash iteration count of 210,000 with `NotSupportedError` because the Workers Web Crypto implementation supports at most 100,000 iterations.

This correction:
- changes new password hashes to PBKDF2-HMAC-SHA-256 with 100,000 iterations;
- bounds verification to the same supported maximum so malformed or incompatible stored hashes fail closed instead of causing a Worker runtime exception;
- updates the authentication documentation and regression checks.

## Deployment impact
- Worker code changed.
- Admin/Public frontend code did not change.
- D1 schema did not change; no migration is required.
- Version remains 0.14.1 / Phase 14B.

After deployment, retry Initial Owner Setup using the existing `INITIAL_OWNER_SETUP_TOKEN`. The previous failed request did not create a user because password hashing failed before user creation completed.
