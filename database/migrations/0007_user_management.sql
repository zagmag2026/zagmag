ALTER TABLE users ADD COLUMN archived_at TEXT;

CREATE INDEX IF NOT EXISTS ix_users_role_status
  ON users(role, is_active, archived_at);

CREATE INDEX IF NOT EXISTS ix_users_last_login
  ON users(last_login_at);
