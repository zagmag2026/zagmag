#!/usr/bin/env python3
from pathlib import Path
import json
import sqlite3

ROOT = Path(__file__).resolve().parents[1]

def require(condition, message):
    if not condition:
        raise AssertionError(message)

def main():
    migration = (ROOT / "database/migrations/0010_booking_daily_sequence.sql").read_text(encoding="utf-8")
    worker = (ROOT / "worker/src/phase14al-sequential-weekday.js").read_text(encoding="utf-8")
    outer_worker = (ROOT / "worker/src/phase14ar-bilingual-branding.js").read_text(encoding="utf-8")
    lifecycle_worker = (ROOT / "worker/src/phase14at-booking-lifecycle.js").read_text(encoding="utf-8")
    dashboard_worker = (ROOT / "worker/src/phase14au-dashboard-screen2.js").read_text(encoding="utf-8")
    customers_worker = (ROOT / "worker/src/phase14av-customers-screen3.js").read_text(encoding="utf-8")
    screen4_worker = (ROOT / "worker/src/phase14aw-screen4-bookings.js").read_text(encoding="utf-8")
    admin = (ROOT / "apps/admin-web/src/phase14aj-business-time.js").read_text(encoding="utf-8")
    public_runtime = (ROOT / "apps/public-web/src/phase14al-full-weekdays.js").read_text(encoding="utf-8")
    public_entry = (ROOT / "apps/public-web/src/phase14c.js").read_text(encoding="utf-8")
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    local = (ROOT / "worker/wrangler.toml").read_text(encoding="utf-8")
    staging = (ROOT / "worker/wrangler.staging.toml.template").read_text(encoding="utf-8")
    production = (ROOT / "worker/wrangler.production.toml.template").read_text(encoding="utf-8")

    require("CREATE TABLE IF NOT EXISTS booking_daily_sequences" in migration, "Daily booking sequence table is required")
    require("business_date TEXT PRIMARY KEY" in migration, "Booking sequence must be unique per business date")
    require("sequence_value INTEGER NOT NULL CHECK (sequence_value >= 1)" in migration, "Booking sequence must remain positive")

    conn = sqlite3.connect(":memory:")
    conn.executescript(migration)
    sql = """INSERT INTO booking_daily_sequences (business_date,sequence_value,updated_at)
             VALUES (?,1,CURRENT_TIMESTAMP)
             ON CONFLICT(business_date) DO UPDATE SET sequence_value=booking_daily_sequences.sequence_value+1,updated_at=CURRENT_TIMESTAMP
             RETURNING sequence_value"""
    first = conn.execute(sql, ("2026-09-15",)).fetchone()[0]
    second = conn.execute(sql, ("2026-09-15",)).fetchone()[0]
    next_day = conn.execute(sql, ("2026-09-16",)).fetchone()[0]
    require((first, second, next_day) == (1, 2, 1), "Daily sequence must increment and reset by business date")

    require('import core from "./phase14ak-partial-return.js";' in worker, "Phase14AL must preserve the Phase14AK Worker chain")
    for marker in ['url.pathname === BOOKING_PATH', "ON CONFLICT(business_date) DO UPDATE SET", "RETURNING sequence_value", "printf('%03d', sequence_value)", "json_set(COALESCE(?, '{}'), '$.bookingNo'", "results.slice(1)", "SELECT booking_no FROM bookings WHERE id=? LIMIT 1"]:
        require(marker in worker, f"Missing sequential booking guard: {marker}")
    require("MAX(" not in worker.upper(), "Booking allocation must not use race-prone MAX()+1")
    require("crypto.randomUUID" not in worker, "Phase14AL booking-number wrapper must have no random fallback")
    require('/^BK-\\d{8}-\\d{3,}$/' in worker, "Returned booking number must be sequential-format validated")
    require('url.pathname === PUBLIC_CATALOG_PATH' in worker, "Public catalog response must carry existing display preferences")
    require("state.displayPreferences = parseDisplayPreferences(value)" in worker, "Display preferences must be captured from the existing catalog DB batch")
    require("data.shop.defaultLanguage" in worker and "data.shop.dateFormat" in worker, "Catalog shop payload must expose language/date format without a new endpoint")

    full_names = ["રવિવાર", "સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    for name in full_names:
        require(name in admin, f"Admin full weekday missing: {name}")
        require(name in public_runtime, f"Public full weekday missing: {name}")
    require('weekday: "long"' in admin and 'weekday: "short"' not in admin, "Admin weekday formatter must never render short weekday names")
    require("phase14al-full-weekday" in public_runtime, "Public date inputs must show the full weekday label")
    require("fetch(" not in public_runtime, "Public weekday runtime must not issue a standalone API call")
    require('import "./phase14al-full-weekdays.js";' in public_entry, "Public entry must load the full weekday runtime before the app")

    require('import core from "./phase14al-sequential-weekday.js";' in outer_worker, "Phase14AR must preserve Phase14AL sequential booking behavior")
    require('import core from "./phase14ar-bilingual-branding.js";' in lifecycle_worker, "Phase14AT must preserve Phase14AR and Phase14AL sequential booking behavior")
    require('import core from "./phase14at-booking-lifecycle.js";' in dashboard_worker, "Phase14AU must preserve Phase14AT and Phase14AL behavior")
    require('import core from "./phase14au-dashboard-screen2.js";' in customers_worker, "Phase14AV must preserve Phase14AU and the full sequence chain")
    require('import core from "./phase14av-customers-screen3.js";' in screen4_worker, "Phase14AW must preserve Phase14AV and the full sequence chain")
    expected_staging = 'main = "src/phase14aw-screen4-bookings.js"'
    expected_production = 'main = "src/phase14at-booking-lifecycle.js"'
    require(expected_staging in local and expected_staging in staging, "Local/staging configs must use the cumulative Screen 4 wrapper")
    require(expected_production in production, "Production config must remain pinned to Phase14AT")
    require(package.get("version") == "0.14.5", "Version must remain 0.14.5")
    require("tests/phase14al_sequence_weekday_regression.py" in package["scripts"]["test:hardening"], "Phase14AL regression must run in test:hardening")

    print("Phase 14AL daily sequential booking + full weekday preserved through Screen 4 staging chain: PASS")

if __name__ == "__main__":
    main()
