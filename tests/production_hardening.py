#!/usr/bin/env python3
from __future__ import annotations
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / "database" / "migrations"


def apply_migrations(conn: sqlite3.Connection) -> None:
    for path in sorted(MIGRATIONS.glob("*.sql")):
        conn.executescript(path.read_text(encoding="utf-8"))


def insert_booking(conn: sqlite3.Connection, booking_id: str, no: str, customer_id: str, user_id: str,
                   pickup: str, ret: str, item_id: str, qty: int, request_key: str) -> str:
    conn.execute(
        "INSERT INTO bookings (id,booking_no,customer_id,booking_date,pickup_date,return_date,status,created_by_user_id,request_key) VALUES (?,?,?,?,?,?,'BOOKED',?,?)",
        (booking_id, no, customer_id, "2026-09-13", pickup, ret, user_id, request_key),
    )
    line_id = f"line-{booking_id}"
    conn.execute(
        "INSERT INTO booking_items (id,booking_id,item_id,booked_qty,given_qty,returned_qty) VALUES (?,?,?,?,0,0)",
        (line_id, booking_id, item_id, qty),
    )
    return line_id


def main() -> None:
    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    apply_migrations(conn)

    conn.execute("INSERT INTO users (id,name,email,role,is_active) VALUES ('u1','Owner','owner@example.com','OWNER',1)")
    conn.execute("INSERT INTO customers (id,name,mobile,is_active) VALUES ('c1','Customer','9999999999',1)")
    conn.execute("INSERT INTO items (id,item_code,item_name,category_id,total_quantity,is_active,public_visible) VALUES ('i1','CH-001','Test Choli','cat_choli',20,1,1)")

    # 1) Overlap guard: 15 reserved means another overlapping 6 must fail.
    insert_booking(conn, "b1", "BK-1", "c1", "u1", "2026-09-20", "2026-09-22", "i1", 15, "request-booking-0001")
    conn.commit()
    try:
        insert_booking(conn, "b2", "BK-2", "c1", "u1", "2026-09-21", "2026-09-23", "i1", 6, "request-booking-0002")
        raise AssertionError("over-capacity overlapping booking was accepted")
    except sqlite3.IntegrityError as exc:
        assert "INSUFFICIENT_AVAILABILITY" in str(exc)
        conn.rollback()

    # 2) Non-overlapping booking succeeds.
    insert_booking(conn, "b3", "BK-3", "c1", "u1", "2026-10-01", "2026-10-02", "i1", 20, "request-booking-0003")
    conn.commit()

    # 3) Idempotency request key is unique.
    try:
        conn.execute("INSERT INTO bookings (id,booking_no,customer_id,booking_date,pickup_date,return_date,status,created_by_user_id,request_key) VALUES ('dup','BK-DUP','c1','2026-09-13','2026-11-01','2026-11-02','BOOKED','u1','request-booking-0003')")
        raise AssertionError("duplicate booking request key was accepted")
    except sqlite3.IntegrityError:
        conn.rollback()

    # Recreate clean test state for quantity/status tests.
    conn.close()
    conn = sqlite3.connect(":memory:")
    conn.execute("PRAGMA foreign_keys=ON")
    apply_migrations(conn)
    conn.execute("INSERT INTO users (id,name,email,role,is_active) VALUES ('u1','Owner','owner@example.com','OWNER',1)")
    conn.execute("INSERT INTO customers (id,name,mobile,is_active) VALUES ('c1','Customer','9999999999',1)")
    conn.execute("INSERT INTO items (id,item_code,item_name,category_id,total_quantity,is_active,public_visible) VALUES ('i1','CH-001','Test Choli','cat_choli',20,1,1)")

    line = insert_booking(conn, "status1", "BK-S1", "c1", "u1", "2026-09-20", "2026-09-22", "i1", 2, "request-status-0001")
    conn.execute("UPDATE booking_items SET given_qty=1 WHERE id=?", (line,))
    assert conn.execute("SELECT status FROM bookings WHERE id='status1'").fetchone()[0] == "PARTIALLY_GIVEN"
    conn.execute("UPDATE booking_items SET given_qty=2 WHERE id=?", (line,))
    assert conn.execute("SELECT status FROM bookings WHERE id='status1'").fetchone()[0] == "GIVEN"
    conn.execute("UPDATE booking_items SET returned_qty=1 WHERE id=?", (line,))
    assert conn.execute("SELECT status FROM bookings WHERE id='status1'").fetchone()[0] == "PARTIALLY_RETURNED"
    conn.execute("UPDATE booking_items SET returned_qty=2 WHERE id=?", (line,))
    assert conn.execute("SELECT status FROM bookings WHERE id='status1'").fetchone()[0] == "RETURNED"

    # 4) Overdue pending quantity blocks a later booking even without date overlap.
    overdue_line = insert_booking(conn, "old1", "BK-OLD", "c1", "u1", "2026-08-01", "2026-08-02", "i1", 4, "request-old-0001")
    conn.execute("UPDATE booking_items SET given_qty=4 WHERE id=?", (overdue_line,))
    conn.commit()
    try:
        insert_booking(conn, "future1", "BK-FUT", "c1", "u1", "2026-12-01", "2026-12-02", "i1", 17, "request-future-0001")
        raise AssertionError("overdue pending stock was not reserved")
    except sqlite3.IntegrityError as exc:
        assert "INSUFFICIENT_AVAILABILITY" in str(exc)
        conn.rollback()

    # 5) Cloudflare Workers PBKDF2 compatibility is pinned and bounded.
    auth_source = (ROOT / "worker" / "src" / "auth.ts").read_text(encoding="utf-8")
    assert "const PASSWORD_ITERATIONS = 210_000;" in auth_source
    assert "const MIN_SUPPORTED_PBKDF2_ITERATIONS = 100_000;" in auth_source
    assert "const MAX_SUPPORTED_PBKDF2_ITERATIONS = 600_000;" in auth_source
    assert "iterations < MIN_SUPPORTED_PBKDF2_ITERATIONS" in auth_source
    assert "iterations > MAX_SUPPORTED_PBKDF2_ITERATIONS" in auth_source
    assert "passwordNeedsRehash" in auth_source

    # 6) Auth throttle table and foreign keys are structurally healthy.
    conn.execute("INSERT INTO auth_login_limits (throttle_key,failures,window_started_epoch,blocked_until_epoch) VALUES ('k',8,1,2)")
    assert conn.execute("SELECT failures FROM auth_login_limits WHERE throttle_key='k'").fetchone()[0] == 8
    assert conn.execute("PRAGMA foreign_key_check").fetchall() == []

    # 7) Hardened worker chain + strict boundary regression gates.
    admin_enhancer = (ROOT / "apps" / "admin-web" / "src" / "phase14d.js").read_text(encoding="utf-8")
    admin_date_enhancer = (ROOT / "apps" / "admin-web" / "src" / "phase14k.js").read_text(encoding="utf-8")
    admin_ui_entry = (ROOT / "apps" / "admin-web" / "src" / "phase14j.js").read_text(encoding="utf-8")
    public_enhancer = (ROOT / "apps" / "public-web" / "src" / "phase14c.js").read_text(encoding="utf-8")
    worker_wrapper = (ROOT / "worker" / "src" / "phase14d.js").read_text(encoding="utf-8")
    strict_worker = (ROOT / "worker" / "src" / "phase14o.js").read_text(encoding="utf-8")
    phase14ak_worker = (ROOT / "worker" / "src" / "phase14ak-partial-return.js").read_text(encoding="utf-8")
    admin_html = (ROOT / "apps" / "admin-web" / "index.html").read_text(encoding="utf-8")
    public_html = (ROOT / "apps" / "public-web" / "index.html").read_text(encoding="utf-8")
    wrangler = (ROOT / "worker" / "wrangler.toml").read_text(encoding="utf-8")
    main_line = next((line.strip() for line in wrangler.splitlines() if line.strip().startswith("main = ")), "")
    assert main_line.startswith('main = "src/') and main_line.endswith('"')
    current_relative = main_line.split('"', 2)[1]
    current_worker_path = ROOT / "worker" / current_relative
    assert current_worker_path.exists(), f"Configured Worker entry does not exist: {current_relative}"
    current_worker = current_worker_path.read_text(encoding="utf-8")

    assert "phase14e.js" in admin_html and "phase14c.js" in public_html
    assert "core.fetch(request,env,ctx)" in current_worker.replace(" ", ""), "Configured Worker must continue delegating unmatched routes to the prior hardened core."
    assert 'import core from "./phase14o.js";' in phase14ak_worker
    assert 'return core.fetch(request, env, ctx);' in phase14ak_worker
    assert 'import "./phase14k.js";' in admin_ui_entry
    assert "nextDateValue(pickup.value)" in admin_date_enhancer
    assert "returnDate.min=minimum" in admin_date_enhancer
    assert 'document.addEventListener("submit"' in admin_date_enhancer
    assert "nextDateValue(pickup.value)" in public_enhancer
    assert "returnDate.min=minimum" in public_enhancer
    assert 'document.addEventListener("click"' in public_enhancer
    assert "INVALID_RENTAL_DATE_RANGE" in strict_worker
    assert "returnDate <= pickupDate" in strict_worker
    assert "/api/public/availability" in strict_worker
    assert "/api/admin/bookings/availability" in strict_worker
    assert "item_lines" in worker_wrapper and "loadVisualItems" in worker_wrapper
    assert "phase14d-item-card" in admin_enhancer

    print("Cumulative production hardening regression: PASS")


if __name__ == "__main__":
    main()
