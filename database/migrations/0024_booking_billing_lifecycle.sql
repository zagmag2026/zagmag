-- Booking cancellation Advance settlement + booking-linked Billing lifecycle hardening.
-- Production remains untouched until an explicit production deploy is authorized.

ALTER TABLE bookings
  ADD COLUMN advance_refund_amount INTEGER NOT NULL DEFAULT 0 CHECK (advance_refund_amount >= 0);

ALTER TABLE bookings
  ADD COLUMN advance_settlement_status TEXT
  CHECK (advance_settlement_status IS NULL OR advance_settlement_status IN ('FULL_REFUND','PARTIAL_REFUND','NO_REFUND'));

ALTER TABLE bookings
  ADD COLUMN advance_settled_at TEXT;

ALTER TABLE bookings
  ADD COLUMN advance_settled_by_user_id TEXT REFERENCES users(id);

CREATE INDEX IF NOT EXISTS ix_bookings_advance_settlement
  ON bookings(advance_settlement_status, cancelled_at);
