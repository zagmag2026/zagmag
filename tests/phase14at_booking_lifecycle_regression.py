#!/usr/bin/env python3
from pathlib import Path
import sqlite3

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    migration = (ROOT / "database/migrations/0011_booking_confirmation_state.sql").read_text(encoding="utf-8")
    wrapper = (ROOT / "worker/src/phase14at-booking-lifecycle.js").read_text(encoding="utf-8")
    dashboard_wrapper = (ROOT / "worker/src/phase14au-dashboard-screen2.js").read_text(encoding="utf-8")
    customers_wrapper = (ROOT / "worker/src/phase14av-customers-screen3.js").read_text(encoding="utf-8")
    screen4_wrapper = (ROOT / "worker/src/phase14aw-screen4-bookings.js").read_text(encoding="utf-8")
    staging = (ROOT / "worker/wrangler.staging.toml.template").read_text(encoding="utf-8")
    production = (ROOT / "worker/wrangler.production.toml.template").read_text(encoding="utf-8")

    assert "confirmation_state" in migration
    assert "RESERVED" in migration and "BOOKED" in migration
    assert 'import core from "./phase14ar-bilingual-branding.js";' in wrapper
    assert "/confirm" in wrapper
    assert "BOOKING_NOT_CONFIRMED" in wrapper
    assert "confirmationState" in wrapper
    assert "PART_PICKUP" in wrapper and "FULL_RETURN" in wrapper
    assert 'import core from "./phase14at-booking-lifecycle.js";' in dashboard_wrapper
    assert 'import core from "./phase14au-dashboard-screen2.js";' in customers_wrapper
    assert 'import core from "./phase14av-customers-screen3.js";' in screen4_wrapper
    assert 'main = "src/phase14aw-screen4-bookings.js"' in staging
    # Screen 4 remains staging-only until an explicit production release is approved.
    assert 'main = "src/phase14at-booking-lifecycle.js"' in production

    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    for path in sorted((ROOT / "database/migrations").glob("*.sql")):
        conn.executescript(path.read_text(encoding="utf-8"))
    columns = {row[1]: row for row in conn.execute("PRAGMA table_info(bookings)").fetchall()}
    assert "confirmation_state" in columns
    assert str(columns["confirmation_state"][4]).strip("'") == "BOOKED"

    print("Phase 14AT booking lifecycle preserved through Screen 4 staging wrapper: PASS")


if __name__ == "__main__":
    main()
