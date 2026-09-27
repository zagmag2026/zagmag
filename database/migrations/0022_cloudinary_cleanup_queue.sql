-- Phase 14 hardening: durable, bounded Cloudinary cleanup recovery.
-- No polling: failed destroys are retried only on later explicit media/settings actions.
CREATE TABLE IF NOT EXISTS cloudinary_cleanup_queue (
  public_id TEXT PRIMARY KEY,
  attempts INTEGER NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cloudinary_cleanup_queue_updated
  ON cloudinary_cleanup_queue(updated_at);
