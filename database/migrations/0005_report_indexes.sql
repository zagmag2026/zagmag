-- Phase 11 report/history read-performance indexes
CREATE INDEX IF NOT EXISTS ix_booking_items_booking ON booking_items(booking_id, item_id);
CREATE INDEX IF NOT EXISTS ix_pickup_events_booking_created ON pickup_events(booking_id, created_at);
CREATE INDEX IF NOT EXISTS ix_pickup_event_items_booking_item ON pickup_event_items(booking_item_id, pickup_event_id);
CREATE INDEX IF NOT EXISTS ix_return_event_items_booking_item ON return_event_items(booking_item_id, return_event_id);
CREATE INDEX IF NOT EXISTS ix_bookings_status_booking_date ON bookings(status, booking_date);
