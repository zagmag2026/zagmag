-- Phase 8 return idempotency and return-query indexes

ALTER TABLE return_events ADD COLUMN request_key TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_return_events_request_key
  ON return_events(request_key) WHERE request_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_return_events_booking_created
  ON return_events(booking_id, created_at);

CREATE INDEX IF NOT EXISTS ix_booking_items_pending_return
  ON booking_items(booking_id, given_qty, returned_qty);
