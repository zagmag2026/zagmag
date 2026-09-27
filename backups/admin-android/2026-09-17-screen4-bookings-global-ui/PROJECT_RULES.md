# Zhagmag Dresses — Project Rules

This is the first-read governance file for developers and AI working on this repository.

## Mandatory workflow
1. Audit the current source and relevant docs before changing code.
2. Do not change code until the user explicitly says **Create code** for the current issue.
3. Each new issue requires a fresh **Create code** instruction.
4. Preserve production unless the user explicitly requests a production deploy/change.
5. Minimize Cloudflare, D1/API calls and GitHub Actions runs.
6. After a code batch, run the relevant static/regression audit before the single validation build/deploy.
7. Check the exact workflow log after the validation run.
8. Rebuild the Admin Android app screen-by-screen. Finalize and validate one screen before starting implementation of the next screen.

## Product rules
- Brand: **ઝગમગ ડ્રેસીસ / Zhagmag Dresses**.
- Current project version remains **0.14.5** until explicitly changed.
- Pricing/payment calculations are outside current scope: no rent amount, deposit, discount, late fee, damage charge or other pricing calculations.
- Item Master remains category-driven with dynamic custom fields managed without code changes.
- Dashboard operational queues must include customer context and quick Call/WhatsApp actions.
- D1/Worker remains authoritative for business rules, permissions, booking state and inventory calculations.

## UI governance
All new screens, buttons, forms, cards and dialogs must follow `UI_DESIGN_SYSTEM.md` and reusable components. For the **Admin Android app**, `UI_GUIDELINES.md` is the app-specific authority and takes precedence when a generic UI rule conflicts with the latest Android requirements. Run a UI consistency audit after every UI batch.

## Android Admin rules
See `ANDROID_ADMIN_APP.md` and `UI_GUIDELINES.md`.
- Native Kotlin + Jetpack Compose.
- Reuse the existing Worker/D1 backend; do not create a second database/backend.
- Admin Android visible UI is English only unless explicitly changed later.
- Keep vertical height compact while preserving usability/accessibility.
- Do not add unnecessary explanatory/helper labels or info cards.
- Staging is the default build environment.
- Production API base URL must be supplied explicitly; it is not hard-coded.
- Avoid polling. Data loads on screen entry, explicit search/page change, approved mutation-stale refresh, or manual refresh.
- Screen 1 App Entry/Login is locked by `ADMIN_SCREEN_01_LOGIN.md` unless the user explicitly revises it.
- Screen 2 Dashboard follows `ADMIN_SCREEN_02_DASHBOARD.md`. Its deferred summary-card navigation/filter requirements must be completed when the destination screens are rebuilt.

## Related docs
- `BUSINESS_RULES.md` / existing module docs — business behavior.
- `UI_DESIGN_SYSTEM.md` — shared UI tokens and reusable component rules.
- `UI_GUIDELINES.md` — Admin Android-specific compact UI and text/action rules.
- `ADMIN_SCREEN_01_LOGIN.md` — approved Screen 1 dynamic-branding, mobile-only login contract.
- `ADMIN_SCREEN_02_DASHBOARD.md` — approved Screen 2 Dashboard contract and deferred cross-screen navigation requirements.
- `DASHBOARD_MODULE.md` — Dashboard backend/data behavior.
- `ARCHITECTURE.md` — system architecture.
- `AUTH.md` — authentication/session rules.
- `API_CALL_AUDIT.md` — call-minimization rules.
- `DO_NOT_CHANGE.md` — frozen/critical behavior.
- `PENDING_WORK.md` — current pending work.
- `CHANGELOG.md` — project change record.
- `DEPLOYMENT.md` — deployment rules.

When rules conflict, preserve existing production-safe behavior and use the most specific latest rule for the affected surface. Ask only when the conflict cannot be resolved from the repository or explicit user instructions.
