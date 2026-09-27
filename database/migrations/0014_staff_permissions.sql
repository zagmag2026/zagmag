-- Screen 9 Users: two-role model and per-Staff module access.
ALTER TABLE users ADD COLUMN staff_permissions_json TEXT NOT NULL DEFAULT '["DASHBOARD","ITEMS","CUSTOMERS","BOOKINGS","PICKUPS","RETURNS","REPORTS"]';

-- Retire the legacy ADMIN role without granting additional privilege.
UPDATE users
SET role='STAFF',
    staff_permissions_json='["DASHBOARD","ITEMS","CUSTOMERS","BOOKINGS","PICKUPS","RETURNS","REPORTS"]',
    updated_at=CURRENT_TIMESTAMP
WHERE role='ADMIN';

-- Owners always have full access; the stored permission list is intentionally empty.
UPDATE users
SET staff_permissions_json='[]'
WHERE role='OWNER';

CREATE INDEX IF NOT EXISTS ix_users_mobile_active
  ON users(mobile, is_active, archived_at);
