-- Phase 13 production hardening
-- Booking idempotency, write-race stock protection, status self-healing, auth throttling.

ALTER TABLE bookings ADD COLUMN request_key TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_bookings_request_key
  ON bookings(request_key) WHERE request_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_booking_items_item_quantities
  ON booking_items(item_id, booked_qty, given_qty, returned_qty);

CREATE TABLE IF NOT EXISTS auth_login_limits (
  throttle_key TEXT PRIMARY KEY,
  failures INTEGER NOT NULL DEFAULT 0 CHECK (failures >= 0),
  window_started_epoch INTEGER NOT NULL,
  blocked_until_epoch INTEGER NOT NULL DEFAULT 0,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_auth_login_limits_blocked
  ON auth_login_limits(blocked_until_epoch);

-- Final stock-capacity protection at database write time. This closes the
-- read-check/write race between two simultaneous booking requests.
CREATE TRIGGER IF NOT EXISTS trg_booking_capacity_guard_insert
BEFORE INSERT ON booking_items
WHEN EXISTS (
  SELECT 1
  FROM bookings nb
  JOIN items i ON i.id = NEW.item_id
  WHERE nb.id = NEW.booking_id
    AND NEW.booked_qty + COALESCE((
      SELECT SUM(CASE
        WHEN b.pickup_date <= nb.return_date AND b.return_date >= nb.pickup_date
          THEN bi.booked_qty
        WHEN b.return_date < nb.pickup_date AND bi.given_qty > bi.returned_qty
          THEN bi.given_qty - bi.returned_qty
        ELSE 0 END)
      FROM booking_items bi
      JOIN bookings b ON b.id = bi.booking_id
      WHERE bi.item_id = NEW.item_id
        AND b.id <> NEW.booking_id
        AND b.status NOT IN ('CANCELLED','RETURNED')
    ), 0) > i.total_quantity
)
BEGIN
  SELECT RAISE(ABORT, 'INSUFFICIENT_AVAILABILITY');
END;

CREATE TRIGGER IF NOT EXISTS trg_booking_capacity_guard_update
BEFORE UPDATE OF booked_qty, item_id, booking_id ON booking_items
WHEN EXISTS (
  SELECT 1
  FROM bookings nb
  JOIN items i ON i.id = NEW.item_id
  WHERE nb.id = NEW.booking_id
    AND NEW.booked_qty + COALESCE((
      SELECT SUM(CASE
        WHEN b.pickup_date <= nb.return_date AND b.return_date >= nb.pickup_date
          THEN bi.booked_qty
        WHEN b.return_date < nb.pickup_date AND bi.given_qty > bi.returned_qty
          THEN bi.given_qty - bi.returned_qty
        ELSE 0 END)
      FROM booking_items bi
      JOIN bookings b ON b.id = bi.booking_id
      WHERE bi.item_id = NEW.item_id
        AND b.id <> NEW.booking_id
        AND b.status NOT IN ('CANCELLED','RETURNED')
    ), 0) > i.total_quantity
)
BEGIN
  SELECT RAISE(ABORT, 'INSUFFICIENT_AVAILABILITY');
END;

-- Booking status is derived from authoritative quantities after any pickup,
-- return or correction update. This protects against concurrent requests that
-- both calculated status from an older snapshot.
CREATE TRIGGER IF NOT EXISTS trg_booking_status_sync_quantities
AFTER UPDATE OF given_qty, returned_qty ON booking_items
BEGIN
  UPDATE bookings
     SET status = CASE
       WHEN status = 'CANCELLED' THEN 'CANCELLED'
       WHEN COALESCE((SELECT SUM(given_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0) = 0
         THEN 'BOOKED'
       WHEN COALESCE((SELECT SUM(returned_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0)
            = COALESCE((SELECT SUM(given_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0)
         THEN 'RETURNED'
       WHEN COALESCE((SELECT SUM(returned_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0) > 0
         THEN 'PARTIALLY_RETURNED'
       WHEN NOT EXISTS (
         SELECT 1 FROM booking_items
          WHERE booking_id = NEW.booking_id AND given_qty < booked_qty
       ) THEN 'GIVEN'
       ELSE 'PARTIALLY_GIVEN'
     END,
     updated_at = CURRENT_TIMESTAMP
   WHERE id = NEW.booking_id AND status <> 'CANCELLED';
END;
