from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin"
WORKER = ROOT / "worker/src/phase14ba-screen5-item-management.js"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")

screen5 = read(ANDROID / "Screen5ItemManagement.kt")
screen5_vm = read(ANDROID / "Screen5ItemManagementViewModel.kt")
screen5_repo = read(ANDROID / "Screen5ItemManagementRepository.kt")
screen5_models = read(ANDROID / "Screen5ItemManagementModels.kt")
settings = read(ANDROID / "Screen10Settings.kt")
billing = read(ANDROID / "Screen11Billing.kt")
billing_vm = read(ANDROID / "Screen11BillingViewModel.kt")
worker = read(WORKER)
doc5 = read(ROOT / "docs/ADMIN_SCREEN_05_ITEM_MANAGEMENT.md")
doc10 = read(ROOT / "docs/ADMIN_SCREEN_10_SETTINGS.md")
billing_doc = read(ROOT / "docs/BILLING_MODULE.md")

checks = [
    (settings, "enabled = tab == Screen10Tab.AUDIT", "Settings editable tabs must not pull-refresh"),
    (settings, "enabled = section != Screen10WhatsAppSection.LANGUAGE", "WhatsApp Language Settings must not pull-refresh"),
    (billing, "PullToRefreshBox(", "Bills list pull-to-refresh"),
    (billing, "onRefresh = vm::refreshList", "Bills refresh callback"),
    (billing_vm, "fun refreshList() = loadList(1, refreshing = true)", "Bills page-1 refresh"),
    (screen5_repo, '"/api/admin/item-management/items"', "paged Items endpoint"),
    (screen5_repo, "pageSize.coerceIn(10, 50)", "Items page size"),
    (screen5_models, "internal data class Screen5ItemPage(", "Items page model"),
    (screen5_vm, "fun loadMoreItems()", "Items auto-load VM"),
    (screen5_vm, "delay(300)", "debounced Item search"),
    (screen5, "state.itemItems.forEach", "paged Item list rendering"),
    (screen5, "vm.loadMoreItems()", "bottom-scroll Item auto-load"),
    (worker, 'url.pathname === "/api/admin/item-management/items"', "Worker paged Items route"),
    (worker, "LIMIT ? OFFSET ?", "Worker Item pagination"),
    (worker, "field_values: []", "lightweight bootstrap catalog"),
    (doc5, "server-side and paged", "Screen 5 paging documentation"),
    (doc10, "Pull-to-refresh is intentionally disabled", "Settings refresh-scope documentation"),
    (billing_doc, "main Bills list supports pull-to-refresh", "Billing refresh documentation"),
]

errors = [label for body, token, label in checks if token not in body]
if errors:
    raise SystemExit("Phase14CL refresh/pagination regression failed: " + "; ".join(errors))
print("Phase14CL refresh/pagination regression PASS")
