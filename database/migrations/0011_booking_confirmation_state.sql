ALTER TABLE bookings
  ADD COLUMN confirmation_state TEXT NOT NULL DEFAULT 'BOOKED'
  CHECK (confirmation_state IN ('RESERVED','BOOKED'));

CREATE INDEX IF NOT EXISTS ix_bookings_confirmation_state
  ON bookings(confirmation_state, status, pickup_date);
