# Item Master — Phase 4

## Common fields
- Item Code
- Item Name
- Category
- Total Quantity
- Active/Inactive
- Public Show/Hide
- Up to 8 photo URLs

## Dynamic fields
The item form renders active fields configured in Category Master. Required, dropdown, multi-select, yes/no, date and numeric validation is enforced on the Worker, not only in the browser.

## Quantity safety
Total Quantity cannot be reduced below the maximum quantity required by existing non-cancelled/non-returned bookings or currently outstanding given quantity.


## Explicit custom-field value removal
On Edit Item, changing one valid value to another is a normal edit. Removing a stored custom-field value requires the explicit **Clear Value** action. Normal required-field validation remains enforced; only the controlled edit-removal path may clear a required stored value. Once saved, the corresponding `item_field_values` row is absent and the Item no longer counts as using that field.

## Delete policy
Unused items can be permanently deleted after confirmation. Items with history must be archived; the Delete action remains blocked by the authoritative Worker rule. An item with active/pending bookings cannot be archived.

## Photos
Current Item Master image storage uses signed Cloudinary uploads. Up to 8 optional images are supported per item. D1 stores Cloudinary metadata/public IDs/references only; binary image content is not stored in D1. The first image is Primary unless another saved image is made Primary. R2 is not the current active image implementation.

## Current write permission
- Category/Item master write actions are OWNER-only.
- STAFF with Items / Stock permission may read/use Item Master but cannot add, edit or delete items.
