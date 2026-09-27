-- Preserve financial history when Customer/Item masters are permanently removed
-- and support Pending / Part Received / Full Received bill collection state.

ALTER TABLE customers ADD COLUMN permanently_deleted_at TEXT;

ALTER TABLE bills ADD COLUMN received_amount INTEGER NOT NULL DEFAULT 0
  CHECK (received_amount >= 0);

UPDATE bills
SET received_amount = CASE
  WHEN payment_status='FULL_AMOUNT_RECEIVED' THEN net_amount
  ELSE advance_amount
END
WHERE received_amount=0;

CREATE INDEX IF NOT EXISTS ix_customers_permanently_deleted
  ON customers(permanently_deleted_at);

CREATE INDEX IF NOT EXISTS ix_bills_received_amount
  ON bills(received_amount);
