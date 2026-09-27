-- Phase 14BE operational lifecycle hardening
-- booked_qty remains the immutable/original booked quantity.
-- closed_qty records unpicked quantity explicitly closed by the operator.
-- Active/effective booked quantity = booked_qty - closed_qty.

ALTER TABLE booking_items
  ADD COLUMN closed_qty INTEGER NOT NULL DEFAULT 0 CHECK (closed_qty >= 0);

CREATE TABLE IF NOT EXISTS booking_close_events (
  id TEXT PRIMARY KEY,
  booking_id TEXT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  handled_by_user_id TEXT NOT NULL REFERENCES users(id),
  closed_at TEXT NOT NULL,
  request_key TEXT UNIQUE,
  notes TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_close_event_items (
  id TEXT PRIMARY KEY,
  close_event_id TEXT NOT NULL REFERENCES booking_close_events(id) ON DELETE CASCADE,
  booking_item_id TEXT NOT NULL REFERENCES booking_items(id) ON DELETE RESTRICT,
  qty_closed INTEGER NOT NULL CHECK (qty_closed > 0)
);

CREATE INDEX IF NOT EXISTS ix_booking_close_events_booking_created
  ON booking_close_events(booking_id, created_at);

CREATE INDEX IF NOT EXISTS ix_booking_close_event_items_event
  ON booking_close_event_items(close_event_id);

-- Quantity safety now also protects the explicit closed quantity.
DROP TRIGGER IF EXISTS trg_booking_items_quantity_guard_update;
DROP TRIGGER IF EXISTS trg_booking_items_quantity_guard_insert;

CREATE TRIGGER trg_booking_items_quantity_guard_update
BEFORE UPDATE OF booked_qty, given_qty, returned_qty, closed_qty ON booking_items
WHEN NEW.booked_qty <= 0
   OR NEW.given_qty < 0
   OR NEW.returned_qty < 0
   OR NEW.closed_qty < 0
   OR NEW.given_qty + NEW.closed_qty > NEW.booked_qty
   OR NEW.returned_qty > NEW.given_qty
BEGIN
  SELECT RAISE(ABORT, 'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE');
END;

CREATE TRIGGER trg_booking_items_quantity_guard_insert
BEFORE INSERT ON booking_items
WHEN NEW.booked_qty <= 0
   OR NEW.given_qty < 0
   OR NEW.returned_qty < 0
   OR NEW.closed_qty < 0
   OR NEW.given_qty + NEW.closed_qty > NEW.booked_qty
   OR NEW.returned_qty > NEW.given_qty
BEGIN
  SELECT RAISE(ABORT, 'BOOKING_ITEM_QUANTITY_OUT_OF_RANGE');
END;

-- Capacity must reserve only active quantity. Closing unpicked quantity must
-- release it immediately without changing original booked_qty/history.
DROP TRIGGER IF EXISTS trg_booking_capacity_guard_insert;
DROP TRIGGER IF EXISTS trg_booking_capacity_guard_update;

CREATE TRIGGER trg_booking_capacity_guard_insert
BEFORE INSERT ON booking_items
WHEN EXISTS (
  SELECT 1
  FROM bookings nb
  JOIN items i ON i.id = NEW.item_id
  WHERE nb.id = NEW.booking_id
    AND (NEW.booked_qty - NEW.closed_qty) + COALESCE((
      SELECT SUM(CASE
        WHEN b.pickup_date <= nb.return_date AND b.return_date >= nb.pickup_date
          THEN bi.booked_qty - bi.closed_qty
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

CREATE TRIGGER trg_booking_capacity_guard_update
BEFORE UPDATE OF booked_qty, closed_qty, item_id, booking_id ON booking_items
WHEN EXISTS (
  SELECT 1
  FROM bookings nb
  JOIN items i ON i.id = NEW.item_id
  WHERE nb.id = NEW.booking_id
    AND (NEW.booked_qty - NEW.closed_qty) + COALESCE((
      SELECT SUM(CASE
        WHEN b.pickup_date <= nb.return_date AND b.return_date >= nb.pickup_date
          THEN bi.booked_qty - bi.closed_qty
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

-- A return of everything picked so far is NOT a full return while active
-- pickup quantity is still pending.
DROP TRIGGER IF EXISTS trg_booking_status_sync_quantities;

CREATE TRIGGER trg_booking_status_sync_quantities
AFTER UPDATE OF booked_qty, given_qty, returned_qty, closed_qty ON booking_items
BEGIN
  UPDATE bookings
     SET status = CASE
       WHEN status = 'CANCELLED' THEN 'CANCELLED'
       WHEN COALESCE((SELECT SUM(given_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0) = 0
         THEN 'BOOKED'
       WHEN EXISTS (
         SELECT 1 FROM booking_items
          WHERE booking_id = NEW.booking_id
            AND given_qty < (booked_qty - closed_qty)
       )
         THEN CASE
           WHEN COALESCE((SELECT SUM(given_qty-returned_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0) > 0
                AND COALESCE((SELECT SUM(returned_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0) > 0
             THEN 'PARTIALLY_RETURNED'
           ELSE 'PARTIALLY_GIVEN'
         END
       WHEN COALESCE((SELECT SUM(given_qty-returned_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0) = 0
         THEN 'RETURNED'
       WHEN COALESCE((SELECT SUM(returned_qty) FROM booking_items WHERE booking_id = NEW.booking_id),0) > 0
         THEN 'PARTIALLY_RETURNED'
       ELSE 'GIVEN'
     END,
     updated_at = CURRENT_TIMESTAMP
   WHERE id = NEW.booking_id AND status <> 'CANCELLED';
END;

-- Repair existing rows that were previously marked RETURNED only because
-- everything picked so far had been returned while unpicked quantity remained.
UPDATE bookings
SET status = CASE
  WHEN status = 'CANCELLED' THEN 'CANCELLED'
  WHEN COALESCE((SELECT SUM(given_qty) FROM booking_items WHERE booking_id = bookings.id),0) = 0
    THEN 'BOOKED'
  WHEN EXISTS (
    SELECT 1 FROM booking_items
     WHERE booking_id = bookings.id
       AND given_qty < (booked_qty - closed_qty)
  )
    THEN CASE
      WHEN COALESCE((SELECT SUM(given_qty-returned_qty) FROM booking_items WHERE booking_id = bookings.id),0) > 0
           AND COALESCE((SELECT SUM(returned_qty) FROM booking_items WHERE booking_id = bookings.id),0) > 0
        THEN 'PARTIALLY_RETURNED'
      ELSE 'PARTIALLY_GIVEN'
    END
  WHEN COALESCE((SELECT SUM(given_qty-returned_qty) FROM booking_items WHERE booking_id = bookings.id),0) = 0
    THEN 'RETURNED'
  WHEN COALESCE((SELECT SUM(returned_qty) FROM booking_items WHERE booking_id = bookings.id),0) > 0
    THEN 'PARTIALLY_RETURNED'
  ELSE 'GIVEN'
END,
updated_at = CURRENT_TIMESTAMP
WHERE status <> 'CANCELLED';
