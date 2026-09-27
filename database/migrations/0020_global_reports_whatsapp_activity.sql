-- Phase 14BQ Global Rental Reports support
-- Records Admin WhatsApp preparation outcomes so Reports can show prepared/failed activity.
-- No pricing/payment data is introduced.

CREATE TABLE IF NOT EXISTS whatsapp_activity_logs (
  id TEXT PRIMARY KEY,
  user_id TEXT,
  customer_id TEXT,
  booking_id TEXT,
  item_id TEXT,
  context TEXT,
  status_group TEXT,
  outcome TEXT NOT NULL CHECK (outcome IN ('PREPARED','FAILED')),
  template_count INTEGER NOT NULL DEFAULT 0 CHECK (template_count >= 0),
  error_code TEXT,
  error_message TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_whatsapp_activity_created
  ON whatsapp_activity_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS ix_whatsapp_activity_user_created
  ON whatsapp_activity_logs(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_whatsapp_activity_booking_created
  ON whatsapp_activity_logs(booking_id, created_at DESC);

-- Report drill-down support for latest pickup/return handler lookups.
CREATE INDEX IF NOT EXISTS ix_pickup_event_items_booking_item
  ON pickup_event_items(booking_item_id, pickup_event_id);

CREATE INDEX IF NOT EXISTS ix_return_event_items_booking_item
  ON return_event_items(booking_item_id, return_event_id);
