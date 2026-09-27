-- Phase 14AL — daily, human-readable booking number sequence.
-- One row per India business date. Allocation is updated inside the same D1
-- batch transaction that creates the booking so failed/duplicate writes roll back.

CREATE TABLE IF NOT EXISTS booking_daily_sequences (
  business_date TEXT PRIMARY KEY,
  sequence_value INTEGER NOT NULL CHECK (sequence_value >= 1),
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
