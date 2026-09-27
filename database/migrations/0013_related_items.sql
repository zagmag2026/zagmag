-- Screen 5 Related Items: directional (one-way) item relationships.
CREATE TABLE IF NOT EXISTS item_related_items (
  source_item_id TEXT NOT NULL,
  related_item_id TEXT NOT NULL,
  display_order INTEGER NOT NULL DEFAULT 0,
  created_by_user_id TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (source_item_id, related_item_id),
  CHECK (source_item_id <> related_item_id),
  FOREIGN KEY (source_item_id) REFERENCES items(id) ON DELETE CASCADE,
  FOREIGN KEY (related_item_id) REFERENCES items(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_item_related_items_source_order
  ON item_related_items(source_item_id, display_order, related_item_id);

CREATE INDEX IF NOT EXISTS idx_item_related_items_related
  ON item_related_items(related_item_id);
