# Category Master + Flexible Custom Fields — Phase 3

## Category API
OWNER only.

- `GET /api/admin/categories` — categories bundled with fields and item counts.
- `POST /api/admin/categories` — add category.
- `PUT /api/admin/categories/:id` — edit/update category.
- `DELETE /api/admin/categories/:id` — hard delete only when no item history exists.

## Custom field API
- `POST /api/admin/categories/:id/fields`
- `PUT /api/admin/category-fields/:fieldId`
- `DELETE /api/admin/category-fields/:fieldId`
- `POST /api/admin/category-fields/:fieldId/delete-action` — controlled `DEACTIVATE` or strongly-confirmed `REMOVE_DATA_AND_DELETE`
- `POST /api/admin/categories/:id/fields/reorder`

Supported types: Text, Number, Dropdown, Multi-select, Yes/No, Date.

Dropdown and Multi-select fields require at least one option.

A field with Item values is not a dead-end delete case. OWNER is shown the affected Item count and chooses either:
- **Deactivate** — keep the field schema inactive and preserve existing Item values.
- **Remove Data & Delete Field** — after strong typed confirmation, delete only that field's rows from `item_field_values`, then delete the field schema. Item records themselves are never deleted.

The legacy direct DELETE endpoint remains conservative; the current Admin Android flow uses the explicit controlled delete-action endpoint.

## Efficiency
The category screen uses one bundled read for categories + custom fields. It does not issue one API request per category.

## Permission rule
- Category/custom-field schema write actions are OWNER-only.
- STAFF with Items / Stock permission may read/use category metadata but cannot add, edit, delete or reorder category fields.
