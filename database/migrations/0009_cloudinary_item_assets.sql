CREATE TABLE IF NOT EXISTS cloudinary_item_assets (
  image_url TEXT PRIMARY KEY,
  public_id TEXT NOT NULL,
  item_id TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_cloudinary_item_assets_item ON cloudinary_item_assets(item_id);
