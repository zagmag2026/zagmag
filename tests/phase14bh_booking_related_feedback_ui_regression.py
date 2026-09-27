#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def require(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

core = read(ROOT / "worker" / "src" / "index.ts")
phase14be = read(ROOT / "worker" / "src" / "phase14be-operational-lifecycle.js")
models = read(ANDROID / "data" / "BookingLifecycleModels.kt")
dashboard_models = read(ANDROID / "data" / "Models.kt")
vm = read(ANDROID / "BookingLifecycleViewModel.kt")
workspace = read(ANDROID / "BookingWorkspaceScreen5.kt")
detail = read(ANDROID / "BookingDetailsScreen6.kt")
summary = read(ANDROID / "BookingSummaryCard.kt")
screen4 = read(ANDROID / "AdminAppScreen4.kt")
dashboard = read(ANDROID / "DashboardScreenV2.kt")
screen5 = read(ANDROID / "Screen5ItemManagement.kt")
legacy = read(ANDROID / "AdminAppScreen3.kt")
components = read(ANDROID / "ui" / "components" / "Components.kt")
cards = read(ANDROID / "ui" / "components" / "CardPatterns.kt")
global_ui = read(ROOT / "docs" / "GLOBAL_UI_RULES.md")
screen4_doc = read(ROOT / "docs" / "ADMIN_SCREEN_04_BOOKINGS.md")
booking_doc = read(ROOT / "docs" / "BOOKING_MODULE.md")

checks = [
    (core, "async function bookingBootstrap", "booking bootstrap"),
    (core, "FROM item_related_items r", "authoritative Related Items table"),
    (core, "relatedItems:relatedItems.results||[]", "bundled Related Item links"),
    (models, "data class BookingRelatedItemLink", "Booking Related Item model"),
    (models, "val relatedItems: List<BookingRelatedItemLink>", "bootstrap relation list"),
    (models, "val itemCode: String", "booking preview Item Code"),
    (models, "val categoryName: String", "booking preview Category"),
    (models, "val givenQty: Int = 0", "booking preview Picked quantity"),
    (models, "val returnedQty: Int = 0", "booking preview Returned quantity"),
    (dashboard_models, "val givenQty: Int", "dashboard Picked quantity"),
    (dashboard_models, "val returnedQty: Int", "dashboard Returned quantity"),
    (phase14be, "'given_qty',x.given_qty,'returned_qty',x.returned_qty", "booking/dashboard operational item JSON"),
    (vm, "fun relatedItemsFor(sourceItemId: String)", "Related suggestions resolver"),
    (vm, "fun addRelatedItem(sourceItemId: String, relatedItemId: String)", "explicit Related Add"),
    (vm, "val added = addOrUpdateItem(relatedItemId, 1)", "normal booking add validation reuse"),
    (workspace, '"Related Items"', "nested Related Items heading"),
    (workspace, "onAdd = { viewModel.addRelatedItem(item.id, relatedItem.id) }", "explicit Related Add wiring"),
    (workspace, "nested = true", "nested Related Item presentation"),
    (workspace, "AppItemDisplayRow(", "canonical Booking item row"),
    (detail, "AppItemDisplayRow(", "canonical Booking Details item row"),
    (detail, 'val cancelled = booking.rawStatus.equals("CANCELLED", true) || status == "CANCELLED"', "cancelled Booking Details action guard"),
    (detail, "if (!cancelled && ((pickupPending && canPickup) || (returnPending && canReturn)))", "cancelled/permission-gated Pickup/Return actions"),
    (screen5, "AppItemDisplayRow(", "canonical Item Management row"),
    (cards, "fun AppItemDisplayRow(", "global Item row"),
    (cards, 'joinToString(" · ")', "Item Code Category separator"),
    (cards, '"× $quantity"', "canonical quantity format"),
    (cards, "fun CompactExpandCollapseAction(", "global expand/collapse action"),
    (cards, 'if (expanded) "Show less" else "+ $hiddenCount more"', "mandatory Show less"),
    (cards, "fun CompactMetaBadge(", "compact booking badge"),
    (cards, "fun ResponsiveCompactPair(", "responsive compact pair"),
    (cards, "placeholder = painterResource(R.drawable.ic_launcher)", "canonical Item placeholder"),
    (summary, "CompactExpandCollapseAction(", "Booking card expansion"),
    (summary, "onToggle = { expanded = !expanded }", "Booking local expand/collapse"),
    (summary, 'title = "Pickup"', "Booking Pickup badge"),
    (summary, 'title = "Current"', "Booking Current badge"),
    (summary, 'title = "Return"', "Booking Return badge"),
    (summary, "bookingPickupStatus(displayStatus)", "Booking Pickup Pending/Done status"),
    (summary, "bookingReturnStatus(displayStatus)", "Booking Return Pending/Done status"),
    (summary, 'label = "Next"', "Booking single Next badge"),
    (screen4, "CompactMetaBadge(", "Booking date/current badge reuse"),
    (screen4, '"Booked ${item.quantity} · Picked ${item.givenQty} · Returned ${item.returnedQty}"', "Booking list Item status order"),
    (dashboard, '"Category-wise Inventory"', "KPI-only Dashboard replaces operational Item rows"),
    (components, "fun FeedbackMessage(", "global feedback component"),
    (components, "fun InlineStatusMessage(", "passive inline status component"),
    (detail, "viewModel.clearActionFeedback()", "Booking Details tab clears stale transient feedback"),
    (detail, '"Pickup completed."', "Pickup passive state copy"),
    (detail, '"No pickup has been recorded."', "Pickup empty passive state copy"),
    (detail, '"Return completed."', "Return passive state copy"),
    (detail, '"No items are ready to return."', "Return empty passive state copy"),
    (components, "MessageTone.SUCCESS", "success feedback tone"),
    (components, "MessageTone.ERROR", "error feedback tone"),
    (components, "MessageTone.WARNING", "warning feedback tone"),
    (components, "MessageTone.INFO", "info feedback tone"),
    (components, "durationMillis: Long = 3500L", "one feedback duration"),
    (components, "AnimatedVisibility(", "feedback animation"),
    (components, 'contentDescription = "Dismiss"', "feedback dismiss"),
    (components, "FeedbackDeduper", "duplicate feedback suppression"),
    (legacy, "CompactExpandCollapseAction(", "legacy applicable compact-card Show less"),
    (global_ui, "## Global Item display row", "global Item display documentation"),
    (global_ui, "same reusable **Show less**", "global Show less rule"),
    (screen4_doc, "## Related Item suggestions in New/Edit Booking", "Booking relation documentation"),
    (booking_doc, "## Related Item suggestions", "Booking module relation contract"),
]
for body, token, label in checks:
    require(body, token, label)

for forbidden in [
    "relatedItemsFor(item.id).forEach { addOrUpdateItem",
    "relatedItems.forEach { addOrUpdateItem",
]:
    if forbidden in vm or forbidden in workspace:
        errors.append(f"Related suggestions must not auto-add: {forbidden}")

cards_expand = cards[cards.find("fun CompactExpandCollapseAction("):]
for forbidden in ["api.", "repository.", "http", "fetch("]:
    if forbidden in cards_expand:
        errors.append(f"Expand/collapse must remain local-only: {forbidden}")

if "MessageTone.SUCCESS -> 2500L" in components or "MessageTone.ERROR -> 5000L" in components:
    errors.append("Feedback tones must not restore separate per-tone durations.")

if 'InlineMessage(\n                if (detail.items.any { it.givenQty > 0 })' in detail:
    errors.append("Pickup passive state must remain InlineStatusMessage, not transient feedback.")
if 'InlineMessage(\n                if (detail.booking.displayStatus.equals("FULL_RETURN", true))' in detail:
    errors.append("Return passive state must remain InlineStatusMessage, not transient feedback.")

for body, label in [(workspace, "Booking UI"), (vm, "Booking ViewModel"), (components, "shared components")]:
    if "fixedRateTimer" in body or "while (true)" in body:
        errors.append(f"{label} introduced polling.")

if errors:
    print("Phase 14BH Booking Related/Feedback/Item UI regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14BH Booking Related/Feedback/Item UI regression PASS")
