-- Screen 8 Reports: saved report presets for OWNER/STAFF.
CREATE TABLE IF NOT EXISTS report_presets (
  id TEXT PRIMARY KEY,
  owner_user_id TEXT NOT NULL,
  name TEXT NOT NULL,
  visibility TEXT NOT NULL DEFAULT 'MY' CHECK (visibility IN ('MY','SHARED')),
  config_json TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS ix_report_presets_owner_updated
  ON report_presets(owner_user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS ix_report_presets_visibility_updated
  ON report_presets(visibility, updated_at DESC);
