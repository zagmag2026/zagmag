# Admin Android Screen 5 — Category & Items

Status: **Implemented / finalized / locked**

Screen 5 is the `More` destination rebuilt as one compact item-management workspace. Screens 1–4 remain locked and unchanged except for the minimum More-destination hook.

## Tabs
`Categories | Items | Related Items`

## Categories
- Reuses the existing Worker/D1 Category Master and Custom Field business rules used by Admin Website.
- OWNER can add/edit/delete eligible categories and custom fields. Category delete remains blocked while any linked Item exists; the UI explains that linked Items must be deleted first.
- STAFF with Items / Stock permission is read-only.
- Category cards show Active/Inactive, Public/Hidden, item count and custom-field count.
- Category search is local after the bundled Screen 5 load and must not create a request per keystroke.
- Add Category labels display order as **Website Display Order** and prefills the next local order as current maximum + 1; the value remains editable.
- Edit Category shows the category's saved Website Display Order. A duplicate order used by another category shows the other category name and blocks Save; the edited category's own current order is not treated as a duplicate.
- Screen 5 create actions use the Booking-style compact add family with labels **New Category**, **New Custom Field** and **New Item**.

## Items
- Reuses the existing Worker/D1 Item Master and dynamic category-field validation.
- OWNER can add/edit items; STAFF with Items / Stock permission is read-only.
- Item cards show the existing rounded primary image, item identity/category, semantic status and authoritative **Total / Available / Booked / Given** quantities on one compact line.
- Item search/category filtering is server-side and paged and continues to use the global reusable filter sheet. The Items tab loads 10 records initially and appends the next 10 at bottom scroll while preserving the current search/category filter; search is debounced and does not request on every keystroke. The bootstrap keeps Categories/Custom Fields/Related Item mappings plus a lightweight Item catalog for Related Items, while full Item field/image payloads are fetched only for the visible Item pages. The current category is always visible; applied non-default filtering remains explicitly indicated.
- Add/Edit Item renders custom fields by configured type: TEXT textbox, NUMBER numeric input, DROPDOWN single selector, MULTI_SELECT multiple selector, YES_NO selector and DATE picker. Required fields show `*` and block normal Save while invalid/empty. Edit Item also provides an explicit **Clear Value** action; only this controlled removal path may clear a required field value.
- DATE custom/default fields reuse the global `AppDatePickerField`; optional DATE values may be cleared, required DATE values retain required validation, and stored values remain ISO `yyyy-MM-dd`.
- Dropdown and multi-select values are restricted to configured options; Edit Item rehydrates saved values into the proper control and category changes rebuild the field controls for the newly selected category.
- Android Add/Edit Item reuses the existing signed Cloudinary upload flow used by Admin Website: up to 8 optional images, max 8 MB each, first image is Primary, with preview, Make Primary and Remove actions. Existing image URLs remain editable in the same list; no second storage/backend is introduced.

## Form validation
- Category Name, Code Prefix and Website Display Order are required. Duplicate Website Display Order continues to identify the conflicting category and blocks Save.
- Custom Field Name, Field Type and Display Order are required. DROPDOWN/MULTI_SELECT definitions also require at least one Option; optional default values must still be valid for their selected field type.
- Item Code, Item Name, Category and Total Quantity are required. New Item opens Total Quantity blank so the operator must enter an explicit whole quantity.
- Explicit **0** Total Quantity is valid; a blank quantity is not. Blank must never be silently persisted as zero.
- Required/invalid fields use field-local supporting text while Worker/D1 validation remains authoritative.
## Confirmation consistency
- Straightforward Related Group and Item deletes reuse the shared destructive confirmation dialog.
- Category-linked blocking remains a specialized informational dialog because no delete mutation is allowed while linked Items exist.
- Custom-field removal keeps its stronger specialized two-stage flow with exact `DELETE FIELD` confirmation.
## Delete and unlink rules

- Category **Delete** is always tappable for OWNER. If the category has linked Items, deletion is blocked with a clear linked-Item count/message; an empty category requires confirmation and can then be deleted.
- Custom-field delete shows the affected Item count and offers **Cancel**, **Deactivate**, or **Remove Data & Delete Field**.
- **Deactivate** preserves existing Item values. **Remove Data & Delete Field** requires strong typed confirmation, removes only that field's values from affected Items, then deletes the field schema; Item records are never deleted by this flow.
- Changing a configured value (for example Dropdown Black → Red) is a normal edit, not unlinking. Only explicit **Clear Value** removes that Item's stored value and therefore removes it from the field usage count.
- Item list cards expose **View | Edit | Delete** for OWNER and **View** for read-only STAFF. Delete still uses the existing Item business rule: unused Items may be permanently deleted; Items with booking history are blocked and must use the existing archive policy.

## Related Items
- Relation direction is one-way. `A → B` never automatically creates `B → A`.
- The default Related Items tab shows a **Related Groups** list with the one-line dynamic heading `Related Groups (N Groups)`. One group is one Main Item plus its saved related-item mappings; the same Main Item cannot create a second visible group because relations are grouped by source item.
- Every saved group card shows the Main Item identity and `N Related`, with **View**, **Edit** and **Delete** actions for OWNER. STAFF with Items / Stock permission remains read-only and receives View only.
- **View** opens a read-only group sheet containing the Main Item and all saved Related Items.
- **Edit** opens the existing Main Item + Related Items editor with the saved mapping preselected.
- **Delete** means delete/clear the relation group only: it uses the existing exact-replace mutation with an empty related-item list. It never deletes the Main Item or any Item Master record and requires confirmation.
- **New Group** is the reusable compact top-right action beside the `Category & Items` title and opens the existing editor with no Main Item selected.
- Main Item uses a compact item-card flow: tap Select/Change → bottom sheet → Select Category → choose exactly one active, non-archived item. The selected Main Item is shown as an item card with compact Change and Remove-selection actions.
- Related Items use the same reusable relation-selection card pattern as Main Item (link icon, heading/helper spacing and selection treatment). The section-level **Add** action reuses the global compact Add/New component: Add → bottom sheet → Select Category → choose multiple eligible items → Add Selected. Selection persists while switching categories.
- Selected related items are shown as normal item cards with a compact destructive Remove action on the side of the card; removing here never deletes the Item Master record.
- An item cannot relate to itself; archived/unavailable items cannot be newly related. Existing related selections remain editable under the existing business rules.
- OWNER saves the complete selected set through the existing exact-replace relation mutation. The Worker/D1 relation remains authoritative and all changes are audited.
- Booking Screen 4 now consumes this existing directional relation as **suggestions only** in New/Edit Booking after the user's separate explicit change. Suggestions are nested under the selected Main Item and require explicit Add; no Item is auto-added and the relation configuration itself remains owned by Item Management.

## Data / API discipline
- Screen entry uses `GET /api/admin/item-management/bootstrap` for Categories/Custom Fields/Related Item mappings and the lightweight Item identity/stock catalog used by Related Items. The catalog may include Item image URL metadata so the shared gallery can show all configured photos; image binaries are never returned by this bootstrap.
- The **Items** tab uses paged `GET /api/admin/item-management/items` data. Search/category filters are server-side, debounced and page from 10 records onward; full custom-field/image metadata for visible Item pages remains paged.
- Pull-to-refresh refreshes this module only. An Items page-1 failure has a targeted **Retry Items** path without requiring a full bootstrap reload; next-page failure preserves loaded Items and shows **Retry loading more**.
- Relation mutation uses idempotent exact-replace `PUT /api/admin/related-items/:sourceItemId` and then refreshes the bundled workspace once.

## UI contract
- The page uses the shared compact Back header with `Category & Items`; visible Back and Android system/gesture Back return to the More hub when no nested editor is open.
- In Related Items edit mode, system/gesture Back first closes the nested editor and returns to the Related Groups list; it must not skip directly to More.
- Visible page heading is `Category & Items`.
- `Categories | Items | Related Items` uses one equal-width full-width tab row.
- Screen 5 textboxes use relevant Material Rounded leading icons and generic placeholders only; they never embed sample/business values in placeholders.
- Screen 5 dropdowns use the shared full-width prompt pattern: non-selectable `Select ...` first row, selected check state, relevant icon and dropdown arrow.
Follow `GLOBAL_UI_RULES.md`, `UI_DESIGN_SYSTEM.md` and `UI_GUIDELINES.md`: English-only visible Android UI, compact shared cards/tabs/search/buttons/badges, Material Rounded icons, transient messages, IME/navigation-inset-safe sheets, first 10 rows then +10 incremental display, using server paging for the Items tab and local incremental display for the bounded Categories/Related Groups lists, with no Previous/Page/Next controls.

## Locked permission rule
- Category/Item master write actions are OWNER-only.
- STAFF with Items / Stock permission may access/read/use this module but cannot add, edit or delete Categories, custom-field schemas, Items or Related Item groups.

## Loading / empty / retry behavior
- Initial Category & Items bootstrap failure remains `loaded = false` and shows the normal Date row, Back header and shared Error + Retry state.
- Initial load failure must not open an empty workspace that can be mistaken for `No categories/items found`.
- Pull-to-refresh after a successful load keeps current Category/Item/Related Item content visible when refresh fails and renders the passive error inline.

## Billing V1 Rent extension
This locked screen is explicitly extended only for the approved Billing V1 requirement.
- Item Add/Edit includes one fixed numeric **Rent (₹)** field, default 0.
- Rent is the Item Master default rate used to seed a Bill Draft.
- Bill Draft may override the seeded Rent Rate without changing Item Master.
- Final Bills store their own actual used rate snapshot.
- Rent is not displayed in the Booking flow.
- Deposit, GST, discount, late fee, damage charge and miscellaneous pricing fields are not added to Item Master.


## Dirty editor protection
- Category, Custom Field and Item Add/Edit sheets compare against their opening values and require **Discard unsaved changes?** only after a real change.
- Item discard also cleans up newly uploaded, unsaved image assets through the existing discard-upload path.
- Related Group editing tracks Main Item and selected Related Items; Back/gesture/header Back prompts only when that mapping changed.
- Unchanged forms and read-only views close immediately.
- Related Item cards reuse the shared full-screen image gallery with all available Item image URLs.
