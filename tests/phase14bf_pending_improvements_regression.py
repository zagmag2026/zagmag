#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"
CORE = ROOT / "worker" / "src" / "index.ts"
SCREEN5_WORKER = ROOT / "worker" / "src" / "phase14ba-screen5-item-management.js"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def require(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

components = read(ANDROID / "ui" / "components" / "Components.kt")
actions = read(ANDROID / "BookingCardActions.kt")
root = read(ANDROID / "AdminAppScreen4.kt")
screen5 = read(ANDROID / "Screen5ItemManagement.kt")
screen5_models = read(ANDROID / "Screen5ItemManagementModels.kt")
screen5_repo = read(ANDROID / "Screen5ItemManagementRepository.kt")
booking = read(ANDROID / "BookingWorkspaceScreen5.kt")
users = read(ANDROID / "Screen9Users.kt")
settings = read(ANDROID / "Screen10Settings.kt")
core = read(CORE)
screen5_worker = read(SCREEN5_WORKER)
global_ui = read(ROOT / "docs" / "GLOBAL_UI_RULES.md")
screen5_doc = read(ROOT / "docs" / "ADMIN_SCREEN_05_ITEM_MANAGEMENT.md")
category_doc = read(ROOT / "docs" / "CATEGORY_FIELDS.md")
item_doc = read(ROOT / "docs" / "ITEM_MASTER.md")

checks = [
    (root, 'AppScreenTitle("More")', "More title-only header"),
    (components, "fun AppBackHeader(", "shared back header"),
    (components, "fun AppSingleSelectFilter(", "shared filter component"),
    (components, 'Text("Cancel")', "filter Cancel action"),
    (components, 'text = "Clear"', "filter Clear action"),
    (components, 'text = "Apply"', "filter Apply action"),
    (components, 'placeholder = "Search options"', "large-filter search"),
    (components, "fun CompactNewActionButton(", "shared New/Add action"),
    (screen5, 'Screen5Tab.RELATED -> "New Group"', "New Group page action"),
    (screen5, '"Related Groups (', "one-line Related Groups count"),
    (screen5, "AppBackHeader(", "Related editor back header"),
    (screen5, "Screen5RelationSectionHeader(", "shared Main/Related selector card"),
    (screen5, 'actionLabel = if (canEdit && sourceItemId.isNotBlank()) "Add" else null', "shared Related Add action"),
    (screen5, "onRemoveSelection = if (canEdit)", "side destructive related Remove"),
    (screen5, 'label = "View"', "Item View action"),
    (screen5, 'label = "Edit"', "Item Edit action"),
    (screen5, 'label = "Delete"', "Item Delete action"),
    (screen5, 'AppDestructiveConfirmDialog(', "shared destructive confirmation"),
    (screen5, 'title = "Delete item?"', "Item delete confirmation"),
    (screen5, 'Text(if (linked > 0) "Category has linked Items" else "Delete category?")', "linked-category delete block"),
    (screen5, "category.linkedItemCount", "linked-category count"),
    (screen5, "field.valueCount", "affected Item field count"),
    (screen5, 'Text("Deactivate")', "field Deactivate option"),
    (screen5, 'text = "Remove Data & Delete Field"', "field remove-data option"),
    (screen5, 'strongDeleteText == "DELETE FIELD"', "strong field deletion confirmation"),
    (screen5, 'Text("Clear Value")', "explicit Item custom-field clear"),
    (screen5, "clearFieldIds = clearFieldIds + field.id", "controlled field unlink state"),
    (screen5_models, "val valueCount: Int", "field usage model"),
    (screen5_models, "val linkedItemCount: Int", "category linked Item model"),
    (screen5_repo, '"/api/admin/category-fields/$id/delete-action"', "field delete-action API"),
    (screen5_repo, '.put("clearFieldIds", JSONArray(clearFieldIds.toList()))', "field clear payload"),
    (screen5_repo, 'api.request("DELETE", "/api/admin/items/$id")', "Item delete API"),
    (core, "async function fieldDeleteAction(", "Worker controlled field delete"),
    (core, 'action === "DEACTIVATE"', "Worker deactivate action"),
    (core, 'action === "REMOVE_DATA_AND_DELETE"', "Worker remove-data action"),
    (core, 'text(body, "confirmation") !== "DELETE FIELD"', "Worker strong confirmation"),
    (core, 'DELETE FROM item_field_values WHERE category_field_id=?', "field-only data removal"),
    (core, "clearFieldIds", "Worker controlled Item-field clear"),
    (core, "Delete those Items first, then delete the category.", "clear linked-category message"),
    (screen5_worker, "linked_item_count", "bootstrap linked Item count"),
    (screen5_worker, "value_count", "bootstrap affected Item count"),
    (screen5, "AppSingleSelectFilter(", "Screen 5 shared Item filter"),
    (booking, "AppSingleSelectFilter(", "New Booking shared category filter"),
    (users, "AppSingleSelectFilter(", "Users shared filters"),
    (settings, "AppSingleSelectFilter(", "Settings Audit shared filters"),
    (global_ui, "## Global filter pattern", "global filter documentation"),
    (screen5_doc, "## Delete and unlink rules", "Screen 5 delete/unlink documentation"),
    (category_doc, "Remove Data & Delete Field", "field delete documentation"),
    (item_doc, "## Explicit custom-field value removal", "Item clear documentation"),
]
for body, token, label in checks:
    require(body, token, label)

for forbidden, label in [
    ('eyebrow = "Management"', "More Management eyebrow"),
    ('subtitle = "Manage your store and access"', "More subtitle"),
]:
    if forbidden in root:
        errors.append(f"Retired More header text present: {label}")

if "private fun Screen5CompactNewButton(" in screen5:
    errors.append("Screen 5 must reuse the shared CompactNewActionButton instead of a duplicate local add component.")
if "itemFilterOpen" in screen5:
    errors.append("Screen 5 must not restore the raw category-filter dropdown state.")
if "CategoryRadioRow5" in booking:
    errors.append("New Booking must use the global category filter component.")
if "setInterval(" in screen5 or "fixedRateTimer" in screen5:
    errors.append("Pending improvements must not introduce polling.")

if errors:
    print("Phase 14BF pending improvements regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14BF pending improvements regression PASS")
