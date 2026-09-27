from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "apps" / "admin-android" / "app" / "src" / "main" / "java" / "com" / "nimsdeveloper" / "zhagmagdresses" / "admin"

errors = []

def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"Missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

def need(body: str, token: str, label: str):
    if token not in body:
        errors.append(f"{label} missing: {token}")

ui = read(ANDROID / "Screen10Settings.kt")
models = read(ANDROID / "Screen10SettingsModels.kt")
repo = read(ANDROID / "Screen10SettingsRepository.kt")
worker = read(ROOT / "worker" / "src" / "index.ts")
doc = read(ROOT / "docs" / "ADMIN_SCREEN_10_SETTINGS.md")

for token in [
    '"MONDAY" to "Monday"',
    '"TUESDAY" to "Tuesday"',
    '"WEDNESDAY" to "Wednesday"',
    '"THURSDAY" to "Thursday"',
    '"FRIDAY" to "Friday"',
    '"SATURDAY" to "Saturday"',
    '"SUNDAY" to "Sunday"',
    "data class Screen10WorkingDay",
    'val isOpen: Boolean = false',
    'val openTime: String = "09:00"',
    'val closeTime: String = "20:00"',
    'val workingHours: Map<String, Screen10WorkingDay>',
    'optJSONObject("workingHours")',
]:
    need(models, token, "Android Working Hours model")

for token in [
    '"workingHours"',
    '.put("isOpen", day.isOpen)',
    '.put("openTime", day.openTime)',
    '.put("closeTime", day.closeTime)',
]:
    need(repo, token, "Android Working Hours persistence")

for token in [
    'Text("Working Hours"',
    '"Set opening and closing time for each day."',
    "rememberTimePickerState(",
    "TimePicker(state = pickerState)",
    '"Copy Mon → Weekdays"',
    '"Copy Mon → All"',
    '"Closing time must be later than opening time."',
    "screen10WorkingHoursValid(draft.workingHours)",
    "enabled = !busy && websiteValid && workingHoursValid",
]:
    need(ui, token, "Android Working Hours UI")

for token in [
    'const WORKING_DAY_KEYS = ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"] as const',
    "type WorkingHoursDay = { isOpen:boolean; openTime:string; closeTime:string }",
    "workingHours: WorkingHours;",
    "function defaultWorkingHours()",
    'isOpen:false,openTime:"09:00",closeTime:"20:00"',
    "function validWorkingTime",
    "function parseWorkingHoursBody",
    "closing time must be later than opening time",
    "workingHours: defaultWorkingHours()",
    "workingHours:storedWorkingHours(raw.workingHours,defaults.workingHours)",
    "const workingHours=parseWorkingHoursBody(body.workingHours,base.workingHours)",
    "defaultLanguage, dateFormat, workingHours",
]:
    need(worker, token, "Worker Working Hours contract")

if "workingHours" not in worker:
    errors.append("Worker must persist Working Hours inside site_settings JSON.")
if "CREATE TABLE" in worker[worker.find("type WorkingHoursDay"):worker.find("type WhatsAppTemplateRow")]:
    errors.append("Working Hours must not introduce a separate database table.")
if 'val isOpen: Boolean = true' in models or 'isOpen:true,openTime:"09:00",closeTime:"20:00"' in worker:
    errors.append("Unconfigured Working Hours must default to Closed.")

for token in [
    "### Working Hours",
    "Open / Closed",
    "Copy Monday to Mon–Fri",
    "existing `site_settings` JSON",
    "no D1 schema migration",
]:
    need(doc, token, "Working Hours documentation")

if errors:
    print("Phase 14BU Working Hours regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14BU Working Hours regression PASS")
