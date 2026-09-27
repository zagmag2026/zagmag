# Authentication and Authorization

## Active roles
The current product has exactly two active roles:
- **OWNER** — full authorized administration.
- **STAFF** — only assigned operational module permissions.

The legacy **ADMIN** role is retired. It must not be selectable or exposed by current UI/APIs. Existing legacy ADMIN rows are normalized/migrated to STAFF with applicable operational permissions preserved; this must never silently grant OWNER privilege.

## STAFF permissions
Supported operational permission capabilities include:
- Dashboard
- Items / Stock
- Customers
- Bookings
- Pickup
- Returns
- Reports

Admin Android Screens 6 Pickup, 7 Returns and 8 Reports are currently skipped/hidden even when the corresponding capability exists. Those permissions remain valid for backend/Web/future Android reuse.

Users and Settings are always OWNER-only.

## Session security
- authenticated Admin API requests require a valid server session;
- password/raw credentials are never persisted in plain text;
- explicit logout invalidates the server session and clears local authenticated state;
- closing the app is not logout;
- session revocation applies after sensitive user lifecycle changes where required;
- expired/invalid sessions return the user to Login rather than leaving protected UI in a stale authenticated state.

## Admin Android
Admin Android uses the approved secure mobile session flow from `ADMIN_SCREEN_01_LOGIN.md`.
- mobile-based login;
- server-authenticated session;
- secure local session persistence only as approved by the Login contract;
- Worker authorization remains authoritative for every protected API.

## Permission visibility
STAFF navigation hides modules without assigned permission. Direct/deep-link/API access must still be denied by the Worker when permission is absent.

## Sensitive operations
- Users and Settings: OWNER-only.
- Category/Item master writes: OWNER-only.
- Pickup/Return historical correction actions: OWNER-only.
- STAFF with valid module permission may perform normal operational actions allowed by that module contract.
