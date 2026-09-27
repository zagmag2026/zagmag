-- Phase 7 pickup idempotency and quantity-integrity guards

ALTER TABLE pickup_events ADD COLUMN request_key TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_pickup_events_request_key
  ON pickup_events(request_key) WHERE request_key IS NOT NULL;

CREATE TRIGGER IF NOT EXISTS trg_booking_items_quantity_guard_update
BEFORE UPDATE OF booked_qty, given_qty, returned_qty ON booking_items
WHEN NEW.booked_qty <= 0
   OR NEW.given_qty < 0
   OR NEW.returned_qty < 0
   OR NEW.given_qty > NEW.booked_qty
   OR NEW.returned_qty > NEW.given_qty
BEGIN
  SELECT RAISE(ABORT, 'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE');
END;

CREATE TRIGGER IF NOT EXISTS trg_booking_items_quantity_guard_insert
BEFORE INSERT ON booking_items
WHEN NEW.booked_qty <= 0
   OR NEW.given_qty < 0
   OR NEW.returned_qty < 0
   OR NEW.given_qty > NEW.booked_qty
   OR NEW.returned_qty > NEW.given_qty
BEGIN
  SELECT RAISE(ABORT, 'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE');
END;
