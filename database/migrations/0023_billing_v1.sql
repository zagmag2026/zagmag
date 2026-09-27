-- Billing V1: Item Rent, Booking Advance, bills, bill items, numbering and billing WhatsApp context.
-- Production is intentionally untouched; this migration is for the feature branch/staging validation only.

ALTER TABLE items ADD COLUMN rent_amount INTEGER NOT NULL DEFAULT 0 CHECK (rent_amount >= 0);
ALTER TABLE bookings ADD COLUMN advance_amount INTEGER NOT NULL DEFAULT 0 CHECK (advance_amount >= 0);

CREATE TABLE IF NOT EXISTS bill_sequences (
  financial_year TEXT PRIMARY KEY,
  last_number INTEGER NOT NULL DEFAULT 0 CHECK (last_number >= 0),
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bills (
  id TEXT PRIMARY KEY,
  request_key TEXT UNIQUE,
  bill_no TEXT UNIQUE,
  booking_id TEXT UNIQUE REFERENCES bookings(id),
  customer_id TEXT NOT NULL REFERENCES customers(id),
  bill_date TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','FINAL','CANCELLED')),
  payment_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING','FULL_AMOUNT_RECEIVED')),
  customer_name_snapshot TEXT NOT NULL,
  customer_mobile_snapshot TEXT NOT NULL,
  customer_address_snapshot TEXT,
  booking_no_snapshot TEXT,
  total_rent INTEGER NOT NULL DEFAULT 0 CHECK (total_rent >= 0),
  discount_amount INTEGER NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
  net_amount INTEGER NOT NULL DEFAULT 0 CHECK (net_amount >= 0),
  advance_amount INTEGER NOT NULL DEFAULT 0 CHECK (advance_amount >= 0),
  balance_amount INTEGER NOT NULL DEFAULT 0 CHECK (balance_amount >= 0),
  created_by_user_id TEXT REFERENCES users(id),
  finalized_by_user_id TEXT REFERENCES users(id),
  cancelled_by_user_id TEXT REFERENCES users(id),
  finalized_at TEXT,
  cancelled_at TEXT,
  cancellation_reason TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bill_items (
  id TEXT PRIMARY KEY,
  bill_id TEXT NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
  item_id TEXT REFERENCES items(id),
  item_code_snapshot TEXT NOT NULL,
  item_name_snapshot TEXT NOT NULL,
  category_name_snapshot TEXT NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  rent_rate INTEGER NOT NULL CHECK (rent_rate >= 0),
  amount INTEGER NOT NULL CHECK (amount >= 0),
  display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_bills_date ON bills(bill_date DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_bills_customer_date ON bills(customer_id, bill_date DESC);
CREATE INDEX IF NOT EXISTS ix_bills_status_date ON bills(status, bill_date DESC);
CREATE INDEX IF NOT EXISTS ix_bills_payment_date ON bills(payment_status, bill_date DESC);
CREATE INDEX IF NOT EXISTS ix_bill_items_bill ON bill_items(bill_id, display_order, id);

-- Allocate the financial-year sequence inside the same SQLite statement that moves
-- a Bill from Draft to Final. This avoids a separately committed sequence increment.
CREATE TRIGGER IF NOT EXISTS trg_bills_final_sequence_before
BEFORE UPDATE OF status ON bills
WHEN OLD.status='DRAFT' AND NEW.status='FINAL' AND OLD.bill_no IS NULL
BEGIN
  INSERT INTO bill_sequences(financial_year,last_number,updated_at)
  VALUES(
    printf(
      '%04d-%02d',
      CASE
        WHEN CAST(substr(NEW.bill_date,6,2) AS INTEGER) >= 4
          THEN CAST(substr(NEW.bill_date,1,4) AS INTEGER)
        ELSE CAST(substr(NEW.bill_date,1,4) AS INTEGER)-1
      END,
      (
        CASE
          WHEN CAST(substr(NEW.bill_date,6,2) AS INTEGER) >= 4
            THEN CAST(substr(NEW.bill_date,1,4) AS INTEGER)+1
          ELSE CAST(substr(NEW.bill_date,1,4) AS INTEGER)
        END
      ) % 100
    ),
    1,
    CURRENT_TIMESTAMP
  )
  ON CONFLICT(financial_year) DO UPDATE
    SET last_number=last_number+1, updated_at=CURRENT_TIMESTAMP;
END;

CREATE TRIGGER IF NOT EXISTS trg_bills_final_number_after
AFTER UPDATE OF status ON bills
WHEN OLD.status='DRAFT' AND NEW.status='FINAL' AND OLD.bill_no IS NULL
BEGIN
  UPDATE bills
  SET bill_no =
      'ZD/' ||
      printf(
        '%02d-%02d',
        (
          CASE
            WHEN CAST(substr(NEW.bill_date,6,2) AS INTEGER) >= 4
              THEN CAST(substr(NEW.bill_date,1,4) AS INTEGER)
            ELSE CAST(substr(NEW.bill_date,1,4) AS INTEGER)-1
          END
        ) % 100,
        (
          CASE
            WHEN CAST(substr(NEW.bill_date,6,2) AS INTEGER) >= 4
              THEN CAST(substr(NEW.bill_date,1,4) AS INTEGER)+1
            ELSE CAST(substr(NEW.bill_date,1,4) AS INTEGER)
          END
        ) % 100
      ) ||
      '/' ||
      printf(
        '%06d',
        (
          SELECT last_number
          FROM bill_sequences
          WHERE financial_year = printf(
            '%04d-%02d',
            CASE
              WHEN CAST(substr(NEW.bill_date,6,2) AS INTEGER) >= 4
                THEN CAST(substr(NEW.bill_date,1,4) AS INTEGER)
              ELSE CAST(substr(NEW.bill_date,1,4) AS INTEGER)-1
            END,
            (
              CASE
                WHEN CAST(substr(NEW.bill_date,6,2) AS INTEGER) >= 4
                  THEN CAST(substr(NEW.bill_date,1,4) AS INTEGER)+1
                ELSE CAST(substr(NEW.bill_date,1,4) AS INTEGER)
              END
            ) % 100
          )
        )
      )
  WHERE id=NEW.id AND bill_no IS NULL;
END;

ALTER TABLE whatsapp_activity_logs ADD COLUMN bill_id TEXT;
ALTER TABLE whatsapp_activity_logs ADD COLUMN bill_no_snapshot TEXT;
CREATE INDEX IF NOT EXISTS ix_whatsapp_activity_bill_created
  ON whatsapp_activity_logs(bill_id, created_at DESC);

INSERT OR IGNORE INTO whatsapp_templates
  (id,template_key,template_name,message_text,is_active,message_gu,message_en,language_mode,linked_action)
VALUES
  (
    'tpl_bill_details',
    'bill_details',
    'Bill Details',
    'નમસ્તે {customer_name}, Bill {bill_no}: Net ₹{net_amount}, Advance ₹{advance_amount}, Balance ₹{balance_amount}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Bill {bill_no}: Net ₹{net_amount}, Advance ₹{advance_amount}, Balance ₹{balance_amount}. — {shop_name}',
    'Hello {customer_name}, Bill {bill_no}: Net ₹{net_amount}, Advance ₹{advance_amount}, Balance ₹{balance_amount}. — {shop_name}',
    'ALL',
    'BILL_DETAILS'
  ),
  (
    'tpl_payment_pending',
    'payment_pending',
    'Payment Pending',
    'નમસ્તે {customer_name}, Bill {bill_no} માં ₹{balance_amount} બાકી છે. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Bill {bill_no} માં ₹{balance_amount} બાકી છે. — {shop_name}',
    'Hello {customer_name}, ₹{balance_amount} is pending for Bill {bill_no}. — {shop_name}',
    'ALL',
    'PAYMENT_PENDING'
  ),
  (
    'tpl_full_amount_received',
    'full_amount_received',
    'Full Amount Received',
    'નમસ્તે {customer_name}, Bill {bill_no} માટે સંપૂર્ણ રકમ ₹{net_amount} પ્રાપ્ત થઈ છે. આપનો આભાર. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Bill {bill_no} માટે સંપૂર્ણ રકમ ₹{net_amount} પ્રાપ્ત થઈ છે. આપનો આભાર. — {shop_name}',
    'Hello {customer_name}, full amount ₹{net_amount} has been received for Bill {bill_no}. Thank you. — {shop_name}',
    'ALL',
    'FULL_AMOUNT_RECEIVED'
  ),
  (
    'tpl_bill_cancelled',
    'bill_cancelled',
    'Bill Cancelled',
    'નમસ્તે {customer_name}, Bill {bill_no} cancel કરવામાં આવ્યું છે. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Bill {bill_no} cancel કરવામાં આવ્યું છે. — {shop_name}',
    'Hello {customer_name}, Bill {bill_no} has been cancelled. — {shop_name}',
    'ALL',
    'BILL_CANCELLED'
  );
