# Release 0.12.1 — Phase 12B User Management

## Added
- User Management Admin screen
- User list/search/filter/pagination
- Add/Edit/Activate/Deactivate
- Role management
- Password reset
- Archive/Restore
- Last-login and activity counts
- Last active Owner protection
- Session revocation on security-sensitive account changes
- User management audit events

## Migration
- `0007_user_management.sql` adds `users.archived_at` and management indexes.

## Preserved
- Pricing/payment/deposit remains OFF.
- Laundry/repair/maintenance remains OFF.
- Existing booking/pickup/return/public/report/settings behavior remains unchanged.
