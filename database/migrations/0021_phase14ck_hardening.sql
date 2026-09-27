-- Phase 14CK hardening
-- Concurrency invariants, report/activity indexes and durable WhatsApp identity snapshots.

CREATE INDEX IF NOT EXISTS ix_bookings_created_by_booking_date
  ON bookings(created_by_user_id, booking_date);

CREATE INDEX IF NOT EXISTS ix_pickup_events_handler_at
  ON pickup_events(handled_by_user_id, pickup_at);

CREATE INDEX IF NOT EXISTS ix_return_events_receiver_at
  ON return_events(received_by_user_id, return_at);

CREATE INDEX IF NOT EXISTS ix_whatsapp_activity_customer_created
  ON whatsapp_activity_logs(customer_id, created_at);

CREATE UNIQUE INDEX IF NOT EXISTS ux_cloudinary_item_assets_public_id
  ON cloudinary_item_assets(public_id);

ALTER TABLE whatsapp_activity_logs ADD COLUMN customer_name_snapshot TEXT;
ALTER TABLE whatsapp_activity_logs ADD COLUMN customer_mobile_snapshot TEXT;
ALTER TABLE whatsapp_activity_logs ADD COLUMN booking_no_snapshot TEXT;
ALTER TABLE whatsapp_activity_logs ADD COLUMN item_name_snapshot TEXT;

-- Prevent two concurrent Owner mutations from leaving the installation without an active Owner.
CREATE TRIGGER IF NOT EXISTS trg_users_last_owner_guard_update
BEFORE UPDATE OF role, is_active, archived_at ON users
WHEN OLD.role='OWNER'
 AND OLD.is_active=1
 AND OLD.archived_at IS NULL
 AND NOT (NEW.role='OWNER' AND NEW.is_active=1 AND NEW.archived_at IS NULL)
 AND NOT EXISTS (
   SELECT 1 FROM users other
   WHERE other.id<>OLD.id
     AND other.role='OWNER'
     AND other.is_active=1
     AND other.archived_at IS NULL
 )
BEGIN
  SELECT RAISE(ABORT, 'LAST_OWNER');
END;

-- Item quantity is an inventory invariant too. Booking-write triggers already protect new
-- reservations; this guard protects a simultaneous Item quantity reduction.
CREATE TRIGGER IF NOT EXISTS trg_items_total_quantity_guard_update
BEFORE UPDATE OF total_quantity ON items
WHEN NEW.total_quantity < MAX(
  COALESCE((
    SELECT MAX(overlap_qty) FROM (
      SELECT anchor.id AS booking_id,
             COALESCE((
               SELECT SUM(MAX(0, bi2.booked_qty-bi2.closed_qty))
               FROM booking_items bi2
               JOIN bookings b2 ON b2.id=bi2.booking_id
               WHERE bi2.item_id=OLD.id
                 AND b2.status NOT IN ('CANCELLED','RETURNED')
                 AND b2.pickup_date<=anchor.return_date
                 AND b2.return_date>=anchor.pickup_date
             ),0) AS overlap_qty
      FROM booking_items anchor_bi
      JOIN bookings anchor ON anchor.id=anchor_bi.booking_id
      WHERE anchor_bi.item_id=OLD.id
        AND anchor.status NOT IN ('CANCELLED','RETURNED')
    )
  ),0),
  COALESCE((
    SELECT SUM(MAX(0, bi.given_qty-bi.returned_qty))
    FROM booking_items bi
    JOIN bookings b ON b.id=bi.booking_id
    WHERE bi.item_id=OLD.id
      AND b.status<>'CANCELLED'
  ),0)
)
BEGIN
  SELECT RAISE(ABORT, 'ITEM_QUANTITY_IN_USE');
END;

-- D1 cleanup fallback. Worker captures public IDs before Item deletion so external Cloudinary
-- cleanup can still run after this local referential cleanup.
CREATE TRIGGER IF NOT EXISTS trg_items_cloudinary_asset_map_cleanup
AFTER DELETE ON items
BEGIN
  DELETE FROM cloudinary_item_assets WHERE item_id=OLD.id;
END;
