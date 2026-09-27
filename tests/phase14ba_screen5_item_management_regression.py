from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"
WORKER = ROOT / "worker" / "src" / "phase14ba-screen5-item-management.js"
MIGRATION = ROOT / "database" / "migrations" / "0013_related_items.sql"
DOC = ROOT / "docs" / "ADMIN_SCREEN_05_ITEM_MANAGEMENT.md"
GLOBAL_UI = ROOT / "docs" / "GLOBAL_UI_RULES.md"
WRANGLER = ROOT / "worker" / "wrangler.toml"
STAGING = ROOT / "worker" / "wrangler.staging.toml.template"
CURRENT_WRAPPER = ROOT / "worker" / "src" / "phase14bc-screen10-settings.js"

errors = []

def text(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

models = text(ANDROID / "Screen5ItemManagementModels.kt")
repo = text(ANDROID / "Screen5ItemManagementRepository.kt")
vm = text(ANDROID / "Screen5ItemManagementViewModel.kt")
ui = text(ANDROID / "Screen5ItemManagement.kt")
components = text(ANDROID / "ui" / "components" / "Components.kt")
root = text(ANDROID / "AdminAppScreen4.kt")
worker = text(WORKER)
migration = text(MIGRATION)
doc = text(DOC)
global_ui = text(GLOBAL_UI)
wrangler = text(WRANGLER)
staging = text(STAGING)
current_wrapper = text(CURRENT_WRAPPER)

checks = [
    (doc, "Categories | Items | Related Items", "Screen 5 tabs contract"),
    (doc, "A → B` never automatically creates `B → A", "one-way relation contract"),
    (repo, '"/api/admin/item-management/bootstrap"', "single Screen 5 bootstrap endpoint"),
    (repo, 'api.request("PUT", "/api/admin/related-items/$sourceItemId", body)', "exact-replace related endpoint"),
    (models, 'archived = !isNull("archived_at") && optString("archived_at").isNotBlank()', "null-safe active item archive parsing"),
    (models, "internal data class Screen5UploadedAsset", "uploaded image asset model"),
    (models, "val valueCount: Int", "custom-field usage count"),
    (models, "val linkedItemCount: Int", "category linked-item count"),
    (ui, 'CATEGORIES("Categories"), ITEMS("Items"), RELATED("Related Items")', "Android tabs"),
    (ui, 'title = "Category & Items"', "Screen 5 visible Back header title"),
    (ui, "AppCompactFixedTabs(", "shared compact Screen 5 tabs"),
    (ui, "ActivityResultContracts.GetMultipleContents()", "Android multiple image picker"),
    (ui, 'Text("Item Photos"', "item photo editor"),
    (ui, 'Text("Make Primary")', "primary image action"),
    (ui, 'contentDescription = "Remove photo"', "remove image action"),
    (ui, "private const val MAX_ITEM_PHOTOS = 8", "Android eight-photo limit"),
    (repo, 'api.post("/api/admin/cloudinary/signature")', "existing signed Cloudinary upload endpoint"),
    (repo, '.put("cloudinaryAssets", assetPayload)', "Cloudinary asset metadata persistence"),
    (repo, '"/api/admin/category-fields/$id/delete-action"', "controlled custom-field delete action"),
    (repo, '.put("clearFieldIds", JSONArray(clearFieldIds.toList()))', "explicit Item field-value clear payload"),
    (repo, 'api.request("DELETE", "/api/admin/items/$id")', "Item delete action"),
    (repo, 'const val CLIENT_MAX_IMAGE_BYTES = 8 * 1024 * 1024', "Android 8 MB image limit"),
    (doc, "Visible page heading is `Category & Items`.", "documented Screen 5 heading"),
    (doc, "existing signed Cloudinary upload flow", "documented Cloudinary parity"),
    (ui, 'placeholder = "Search categories"', "local category search UI"),
    (ui, 'placeholder = "Search items"', "local item search UI"),
    (ui, 'Screen5Tab.CATEGORIES -> "New Category"', "New Category top action"),
    (ui, 'Screen5Tab.ITEMS -> "New Item"', "New Item top action"),
    (ui, 'Screen5Tab.RELATED -> "New Group"', "New Group top action"),
    (ui, 'label = "New Custom Field"', "Booking-style custom-field create label"),
    (ui, 'CompactNewActionButton(', "shared Booking-style add/new action"),
    (ui, 'label = "Website Display Order *"', "required Website Display Order label"),
    (ui, '(categories.maxOfOrNull { it.displayOrder } ?: 0) + 1', "Add Category next display order"),
    (ui, 'it.id != existing?.id && it.displayOrder == selectedOrder', "Edit Category own-order exclusion"),
    (ui, 'is already used by', "duplicate display-order category message"),
    (ui, 'duplicateCategory == null', "duplicate display-order blocks Save"),
    (ui, 'private fun Screen5DropdownField(', "reusable Screen 5 dropdown"),
    (ui, 'prompt = "Select Category"', "category dropdown prompt"),
    (ui, 'prompt = "Select Field Type"', "field-type dropdown prompt"),
    (ui, 'AppSelectField(', "Screen 5 canonical shared dropdown"),
    (components, 'modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)', "full-width shared dropdown"),
    (components, 'Icon(Icons.Rounded.Check, contentDescription = null', "selected shared dropdown check state"),
    (ui, 'private fun Screen5MultiSelectField(', "multi-select control"),
    (ui, 'private fun Screen5DateField(', "date picker control"),
    (ui, 'AppDatePickerField(', "shared date picker"),
    (ui, '"TEXT" -> AppTextField(', "TEXT dynamic renderer"),
    (ui, '"NUMBER" -> {', "NUMBER dynamic renderer"),
    (ui, "keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)", "NUMBER decimal keyboard"),
    (ui, '"DROPDOWN" -> Screen5DropdownField(', "DROPDOWN dynamic renderer"),
    (ui, '"MULTI_SELECT" -> Screen5MultiSelectField(', "MULTI_SELECT dynamic renderer"),
    (ui, '"YES_NO" -> Screen5DropdownField(', "YES_NO dynamic renderer"),
    (ui, '"DATE" -> Screen5DateField(', "DATE dynamic renderer"),
    (ui, 'screen5NormalizeFieldValues(fields, fieldValues)', "typed field-value normalization"),
    (ui, 'requiredFieldsValid', "required dynamic-field validation"),
    (ui, 'label = "Category Name *"', "required Category Name label"),
    (ui, 'label = "Code Prefix *"', "required Code Prefix label"),
    (ui, 'label = "Field Name *"', "required Field Name label"),
    (ui, 'label = "Options *"', "required options label"),
    (ui, 'label = "Item Code *"', "required Item Code label"),
    (ui, 'label = "Item Name *"', "required Item Name label"),
    (ui, 'label = "Category *"', "required Item Category label"),
    (ui, 'label = "Total Quantity *"', "required quantity label"),
    (ui, 'mutableStateOf(existing?.totalQuantity?.toString().orEmpty())', "new Item quantity has no silent zero default"),
    (ui, 'val quantityNumber = quantity.toIntOrNull()', "explicit quantity parsing"),
    (ui, 'quantityNumber != null', "blank quantity blocks Save"),
    (ui, '"Quantity is required. Enter 0 if there is currently no stock."', "blank quantity guidance"),
    (ui, 'placeholder = "Enter category name"', "generic category placeholder"),
    (ui, 'placeholder = "Enter code prefix"', "generic code-prefix placeholder"),
    (ui, 'placeholder = "Enter item name"', "generic item-name placeholder"),
    (ui, 'placeholder = "Enter quantity"', "generic quantity placeholder"),
    (ui, 'StatusBadge("Category · $selectedCategoryName"', "visible selected category filter"),
    (ui, 'AppSingleSelectFilter(', "shared category filter sheet"),
    (ui, 'AppFilterOption("", "All Categories")', "all-categories filter option"),
    (ui, "AppItemDisplayRow(", "global reusable Item display"),
    (ui, '"Available ${item.availableQuantity} · Booked ${item.bookedQuantity} · Given ${item.givenQuantity}"', "contextual Item status line"),
    (doc, "prefills the next local order as current maximum + 1", "documented Website Display Order behavior"),
    (doc, "renders custom fields by configured type", "documented typed custom-field rendering"),
    (doc, "global reusable filter sheet", "documented category-filter pattern"),
    (global_ui, "Every normal editable textbox/input field", "global textbox leading-icon rule"),
    (global_ui, "Placeholders must never contain actual, sample or business-specific example data", "global generic-placeholder rule"),
    (global_ui, "NUMBER, DATE, DROPDOWN, MULTI_SELECT and YES_NO values use their proper controls", "global typed-control rule"),
    (global_ui, "Closed dropdown state shows a generic prompt before selection", "global dropdown prompt rule"),
    (global_ui, "selected option is visibly identified in the menu with a check/highlight", "global dropdown selected-state rule"),
    (ui, 'text = "Save Related Items"', "related item commit action"),
    (ui, 'text = "Remove Data & Delete Field"', "custom-field destructive option"),
    (ui, 'strongDeleteText == "DELETE FIELD"', "strong custom-field delete confirmation"),
    (ui, 'Text("Clear Value")', "explicit custom-field value clear"),
    (ui, 'label = "View"', "Item View action"),
    (ui, 'label = "Delete"', "Item Delete action"),
    (ui, 'AppBackHeader(', "shared related-editor back header"),
    (ui, '"Related Groups (', "Related Groups list heading"),
    (ui, 'Screen5Tab.RELATED -> "New Group"', "new group action"),
    (ui, 'private fun Screen5RelatedGroupCard(', "related group card"),
    (ui, 'label = "View"', "related group View action"),
    (ui, 'label = "Edit"', "related group Edit action"),
    (ui, 'label = "Delete"', "related group Delete action"),
    (ui, 'private fun Screen5RelatedGroupViewSheet(', "read-only related group view"),
    (ui, 'title = "Delete related group?"', "delete group confirmation"),
    (ui, 'vm.replaceRelatedItems(sourceId, emptyList())', "delete group clears mapping by exact replace"),
    (doc, "default Related Items tab shows a **Related Groups** list", "documented grouped list"),
    (doc, "**Delete** means delete/clear the relation group only", "documented relation-only delete"),
    (ui, 'title = "Main Item"', "Main Item relation section"),
    (ui, 'private fun Screen5MainItemPickerSheet(', "single Main Item picker"),
    (ui, 'Text("Select Main Item"', "Main Item picker prompt"),
    (ui, 'private fun Screen5RelationSectionHeader(', "shared Main/Related selector card"),
    (ui, 'onRemoveSelection = onRemove', "compact side Remove action"),
    (ui, 'private fun Screen5RelatedItemsPickerSheet(', "multi-select Related Items picker"),
    (ui, 'Text("Add Related Items"', "Related Items picker title"),
    (ui, 'text = "Add Selected"', "multi-select apply action"),
    (ui, 'selectedIds + item.id', "multi-select add behavior"),
    (ui, 'selectedIds.filterNot { it == item.id }', "related selection remove behavior"),
    (doc, "tap Select/Change → bottom sheet → Select Category", "documented Main Item picker flow"),
    (doc, "choose multiple eligible items → Add Selected", "documented Related Items multi-select flow"),
    (ui, "rememberModalBottomSheetState(skipPartiallyExpanded = true)", "expanded form sheets"),
    (ui, ".imePadding()", "IME safety"),
    (ui, "AppItemDisplayRow(", "shared reusable item-display treatment"),
    (vm, "if ((!state.loaded || stale) && !state.loading) load(keepContent = state.loaded)", "cache-first/stale screen load"),
    (vm, "val refreshed = repository.bootstrap()", "mutation refreshes bundled Screen 5 data"),
    (worker, 'url.pathname === "/api/admin/item-management/bootstrap"', "Worker bundled bootstrap route"),
    (worker, '/^\\/api\\/admin\\/related-items\\/([^/]+)$/', "Worker related route"),
    (worker, '"RELATED_ITEM"', "related-item audit module"),
    (worker, "sourceItemId", "directional source item"),
    (worker, "linked_item_count", "linked Item count"),
    (worker, "value_count", "custom-field Item usage count"),
    (migration, "PRIMARY KEY (source_item_id, related_item_id)", "directional relation primary key"),
    (migration, "CHECK (source_item_id <> related_item_id)", "self-relation protection"),
    (root, "Screen5ItemManagement(", "minimum More destination hook"),
    (root, "destination == Screen4Destination.MORE", "Screen 5 destination-aware chrome"),
    (root, "onLogout = adminViewModel::logout", "confirmed logout remains available on Screen 5"),
    (wrangler, 'main = "src/phase14bq-global-rental-reports.js"', "current staging Worker entry"),
    (staging, 'main = "src/phase14bq-global-rental-reports.js"', "generated staging entry"),
    (current_wrapper, 'import core from "./phase14bb-screen9-users.js"', "Screen 10 wrapper preserves the cumulative Screen 9 / Screen 5 chain"),
]
for body, token, label in checks:
    if token not in body:
        errors.append(f"{label} missing: {token}")

for forbidden, label in [
    ("fixedRateTimer", "polling timer"),
    ("while (true)", "polling loop"),
    ("Previous", "Previous pagination control"),
    ("Page X", "Page X pagination control"),
    ("sourceMenuOpen", "legacy Related Items source dropdown"),
    ('placeholder = "Search related items"', "legacy always-visible related search"),
    ("import androidx.compose.foundation.layout.weight", "invalid explicit weight import"),
]:
    if forbidden in ui:
        errors.append(f"Screen 5 must not contain {label}.")

if "INSERT INTO item_related_items(source_item_id,related_item_id" not in worker:
    errors.append("Worker must insert directional related-item rows.")
if "related_item_id,source_item_id" in worker:
    errors.append("Worker appears to create an automatic reverse relation; relations must remain one-way.")

if errors:
    print("Screen 5 regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Screen 5 regression PASS")
