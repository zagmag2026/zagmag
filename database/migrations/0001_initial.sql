PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT,
  mobile TEXT,
  password_hash TEXT,
  role TEXT NOT NULL CHECK (role IN ('OWNER','ADMIN','STAFF')),
  is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
  last_login_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email
  ON users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_mobile
  ON users(mobile) WHERE mobile IS NOT NULL;

CREATE TABLE IF NOT EXISTS categories (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  code_prefix TEXT NOT NULL,
  is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
  public_visible INTEGER NOT NULL DEFAULT 1 CHECK (public_visible IN (0,1)),
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_name ON categories(name);
CREATE UNIQUE INDEX IF NOT EXISTS ux_categories_prefix ON categories(code_prefix);

CREATE TABLE IF NOT EXISTS category_fields (
  id TEXT PRIMARY KEY,
  category_id TEXT NOT NULL,
  field_name TEXT NOT NULL,
  field_type TEXT NOT NULL CHECK (
    field_type IN ('TEXT','NUMBER','DROPDOWN','MULTI_SELECT','YES_NO','DATE')
  ),
  is_required INTEGER NOT NULL DEFAULT 0 CHECK (is_required IN (0,1)),
  default_value TEXT,
  options_json TEXT,
  public_visible INTEGER NOT NULL DEFAULT 1 CHECK (public_visible IN (0,1)),
  is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX IF NOT EXISTS ix_category_fields_category
  ON category_fields(category_id, is_active, display_order);

CREATE TABLE IF NOT EXISTS items (
  id TEXT PRIMARY KEY,
  item_code TEXT NOT NULL,
  item_name TEXT NOT NULL,
  category_id TEXT NOT NULL,
  total_quantity INTEGER NOT NULL DEFAULT 0 CHECK (total_quantity >= 0),
  is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
  public_visible INTEGER NOT NULL DEFAULT 1 CHECK (public_visible IN (0,1)),
  archived_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_items_code ON items(item_code);
CREATE INDEX IF NOT EXISTS ix_items_category ON items(category_id, is_active, public_visible);

CREATE TABLE IF NOT EXISTS item_field_values (
  id TEXT PRIMARY KEY,
  item_id TEXT NOT NULL,
  category_field_id TEXT NOT NULL,
  value_text TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES items(id),
  FOREIGN KEY (category_field_id) REFERENCES category_fields(id),
  UNIQUE(item_id, category_field_id)
);

CREATE TABLE IF NOT EXISTS item_images (
  id TEXT PRIMARY KEY,
  item_id TEXT NOT NULL,
  image_url TEXT NOT NULL,
  is_primary INTEGER NOT NULL DEFAULT 0 CHECK (is_primary IN (0,1)),
  display_order INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE INDEX IF NOT EXISTS ix_item_images_item ON item_images(item_id, display_order);

CREATE TABLE IF NOT EXISTS customers (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  mobile TEXT NOT NULL,
  alternate_mobile TEXT,
  address TEXT,
  notes TEXT,
  is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
  archived_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_customers_mobile ON customers(mobile);

CREATE TABLE IF NOT EXISTS bookings (
  id TEXT PRIMARY KEY,
  booking_no TEXT NOT NULL,
  customer_id TEXT NOT NULL,
  booking_date TEXT NOT NULL,
  pickup_date TEXT NOT NULL,
  return_date TEXT NOT NULL,
  status TEXT NOT NULL CHECK (
    status IN ('BOOKED','PARTIALLY_GIVEN','GIVEN','PARTIALLY_RETURNED','RETURNED','CANCELLED')
  ),
  notes TEXT,
  created_by_user_id TEXT NOT NULL,
  cancelled_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES customers(id),
  FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_bookings_no ON bookings(booking_no);
CREATE INDEX IF NOT EXISTS ix_bookings_customer ON bookings(customer_id, booking_date);
CREATE INDEX IF NOT EXISTS ix_bookings_dates ON bookings(pickup_date, return_date, status);

CREATE TABLE IF NOT EXISTS booking_items (
  id TEXT PRIMARY KEY,
  booking_id TEXT NOT NULL,
  item_id TEXT NOT NULL,
  booked_qty INTEGER NOT NULL CHECK (booked_qty > 0),
  given_qty INTEGER NOT NULL DEFAULT 0 CHECK (given_qty >= 0),
  returned_qty INTEGER NOT NULL DEFAULT 0 CHECK (returned_qty >= 0),
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id),
  FOREIGN KEY (item_id) REFERENCES items(id),
  UNIQUE(booking_id, item_id)
);

CREATE INDEX IF NOT EXISTS ix_booking_items_item ON booking_items(item_id, booking_id);

CREATE TABLE IF NOT EXISTS pickup_events (
  id TEXT PRIMARY KEY,
  booking_id TEXT NOT NULL,
  given_to_customer_id TEXT NOT NULL,
  handled_by_user_id TEXT NOT NULL,
  pickup_at TEXT NOT NULL,
  notes TEXT,
  correction_of_event_id TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id),
  FOREIGN KEY (given_to_customer_id) REFERENCES customers(id),
  FOREIGN KEY (handled_by_user_id) REFERENCES users(id),
  FOREIGN KEY (correction_of_event_id) REFERENCES pickup_events(id)
);

CREATE TABLE IF NOT EXISTS pickup_event_items (
  id TEXT PRIMARY KEY,
  pickup_event_id TEXT NOT NULL,
  booking_item_id TEXT NOT NULL,
  qty_given INTEGER NOT NULL CHECK (qty_given > 0),
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (pickup_event_id) REFERENCES pickup_events(id),
  FOREIGN KEY (booking_item_id) REFERENCES booking_items(id)
);

CREATE TABLE IF NOT EXISTS return_events (
  id TEXT PRIMARY KEY,
  booking_id TEXT NOT NULL,
  received_by_user_id TEXT NOT NULL,
  return_at TEXT NOT NULL,
  notes TEXT,
  correction_of_event_id TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (booking_id) REFERENCES bookings(id),
  FOREIGN KEY (received_by_user_id) REFERENCES users(id),
  FOREIGN KEY (correction_of_event_id) REFERENCES return_events(id)
);

CREATE TABLE IF NOT EXISTS return_event_items (
  id TEXT PRIMARY KEY,
  return_event_id TEXT NOT NULL,
  booking_item_id TEXT NOT NULL,
  qty_returned INTEGER NOT NULL CHECK (qty_returned > 0),
  condition_note TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (return_event_id) REFERENCES return_events(id),
  FOREIGN KEY (booking_item_id) REFERENCES booking_items(id)
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value_json TEXT NOT NULL,
  updated_by_user_id TEXT,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS whatsapp_templates (
  id TEXT PRIMARY KEY,
  template_key TEXT NOT NULL,
  template_name TEXT NOT NULL,
  message_text TEXT NOT NULL,
  is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_whatsapp_template_key
  ON whatsapp_templates(template_key);

CREATE TABLE IF NOT EXISTS audit_logs (
  id TEXT PRIMARY KEY,
  user_id TEXT,
  action TEXT NOT NULL,
  module TEXT NOT NULL,
  record_id TEXT,
  old_value_json TEXT,
  new_value_json TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS ix_audit_logs_module_record
  ON audit_logs(module, record_id, created_at);

CREATE INDEX IF NOT EXISTS ix_audit_logs_user
  ON audit_logs(user_id, created_at);

INSERT OR IGNORE INTO categories (
  id, name, code_prefix, is_active, public_visible, display_order
) VALUES (
  'cat_choli', 'Choli', 'CH', 1, 1, 1
);

INSERT OR IGNORE INTO category_fields (
  id, category_id, field_name, field_type, is_required, default_value,
  options_json, public_visible, is_active, display_order
) VALUES (
  'field_choli_size', 'cat_choli', 'Size', 'TEXT', 1, 'Free Size',
  NULL, 1, 1, 1
);
