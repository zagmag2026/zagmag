from pathlib import Path
from datetime import datetime, timedelta, timezone

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
admin_web = read(ROOT / "apps" / "admin-web" / "src" / "main.tsx")
core = read(ROOT / "worker" / "src" / "index.ts")
reports = read(ROOT / "worker" / "src" / "phase14bq-global-rental-reports.js")
doc10 = read(ROOT / "docs" / "ADMIN_SCREEN_10_SETTINGS.md")
doc8 = read(ROOT / "docs" / "ADMIN_SCREEN_08_REPORTS.md")

for token in [
    'ZoneId.of("Asia/Kolkata")',
    '.atOffset(ZoneOffset.UTC)',
    '.atZoneSameInstant(',
    'DateTimeFormatter.ofPattern("dd-MM-yyyy · h:mm a"',
]:
    need(ui, token, "Audit Log IST display")

for token in [
    "function formatAuditTimestamp(value:string)",
    "timeZone:'Asia/Kolkata'",
    "normalized+'Z'",
    "{formatAuditTimestamp(log.created_at)}",
]:
    need(admin_web, token, "Admin Web Audit Log IST display")


for token in [
    "function istDayStartUtc(value:string): string",
    'T00:00:00+05:30',
    "where.push('a.created_at>=?')",
    "binds.push(istDayStartUtc(fromDate))",
    "where.push('a.created_at<?')",
    "binds.push(istDayStartUtc(addIsoDays(toDate,1)))",
]:
    need(core, token, "Audit Log IST date boundaries")

if "date(a.created_at)>=date(?)" in core or "date(a.created_at)<=date(?)" in core:
    errors.append("Audit Log must not filter UTC timestamps by raw UTC calendar date.")

for token in [
    "function istDayStartUtc(value)",
    'T00:00:00+05:30',
    'where.push("a.created_at>=?")',
    "binds.push(istDayStartUtc(filters.fromDate))",
    'where.push("a.created_at<?")',
    "binds.push(istDayStartUtc(addIsoDays(filters.toDate, 1)))",
    "datetime(a.created_at,'+5 hours','+30 minutes') AS activity_at",
]:
    need(reports, token, "Audit Report IST contract")

if 'substr(a.created_at,1,10)>=?' in reports or 'substr(a.created_at,1,10)<=?' in reports:
    errors.append("Audit Report must not filter UTC timestamps by raw UTC calendar date.")

for token in [
    "Asia/Kolkata",
    "UTC",
    "From/To",
]:
    need(doc10, token, "Audit Log timezone documentation")

for token in [
    "Audit Report",
    "Asia/Kolkata",
    "UTC",
]:
    need(doc8, token, "Audit Report timezone documentation")

# Behavioral reference: the screenshot case 05:01 UTC is 10:31 IST.
utc = datetime(2026, 9, 19, 5, 1, tzinfo=timezone.utc)
ist = utc.astimezone(timezone(timedelta(hours=5, minutes=30)))
if ist.strftime("%H:%M") != "10:31":
    errors.append("Reference UTC→IST conversion is incorrect.")

# IST 2026-09-19 begins at 2026-09-18 18:30 UTC and ends exclusively at 2026-09-19 18:30 UTC.
ist_offset = timezone(timedelta(hours=5, minutes=30))
start_local = datetime(2026, 9, 19, 0, 0, tzinfo=ist_offset)
end_local = datetime(2026, 9, 20, 0, 0, tzinfo=ist_offset)
if start_local.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M:%S") != "2026-09-18 18:30:00":
    errors.append("IST day-start UTC boundary reference is incorrect.")
if end_local.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M:%S") != "2026-09-19 18:30:00":
    errors.append("IST next-day exclusive UTC boundary reference is incorrect.")

if errors:
    print("Phase 14BV Audit IST timezone regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Phase 14BV Audit IST timezone regression PASS")
