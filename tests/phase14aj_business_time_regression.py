#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    entry = (ROOT / "apps/admin-web/src/phase14k.js").read_text(encoding="utf-8")
    admin = (ROOT / "apps/admin-web/src/phase14aj-business-time.js").read_text(encoding="utf-8")
    public = (ROOT / "apps/public-web/src/phase14c.js").read_text(encoding="utf-8")
    worker = (ROOT / "worker/src/phase14o.js").read_text(encoding="utf-8")
    schema = (ROOT / "database/migrations/0001_initial.sql").read_text(encoding="utf-8")

    assert 'import "./phase14aj-business-time.js";' in entry
    assert 'const BUSINESS_TZ = "Asia/Kolkata"' in admin
    assert 'zhagmag:display-prefs:v1' in admin
    assert 'DD-MM-YYYY' in admin and 'YYYY-MM-DD' in admin
    for name in ["રવિવાર", "સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર",
                 "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]:
        assert name in admin
    assert 'weekday: "long"' in admin
    assert 'longForm ? "long" : "short"' not in admin
    assert '`${text.replace(" ", "T")}Z`' in admin
    assert 'formatDateTime' in admin and 'formatDateOnly' in admin
    assert '/\\/api\\/auth\\/(?:me|login)' in admin
    assert '\\/api\\/admin\\/settings\\/bootstrap' in admin
    assert 'input,textarea,select,option,pre,code,script,style' in admin

    assert 'const BUSINESS_TZ="Asia/Kolkata"' in public
    assert 'timeZone:BUSINESS_TZ' in public
    assert 'new Date(Date.UTC(year,month-1,day))' in public

    assert 'const BUSINESS_TZ = "Asia/Kolkata"' in worker
    assert 'const IST_OFFSET_MINUTES = 330' in worker
    assert 'function businessDayStartUtcSql' in worker
    assert 'async function listAuditLogsBusinessTime' in worker
    assert 'a.created_at>=?' in worker
    assert 'a.created_at<?' in worker
    assert 'displayPreferences' in worker
    assert 'readDisplayPreferences' in worker
    assert 'url.pathname === "/api/admin/audit-logs"' in worker
    assert 'url.pathname === "/api/auth/me"' in worker
    assert 'url.pathname === "/api/auth/login"' in worker

    # Persist UTC in D1; conversion belongs at query/display boundaries.
    assert 'created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP' in schema
    assert 'updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP' in schema

    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    assert "tests/phase14aj_business_time_regression.py" in package["scripts"]["test:hardening"]
    print("Phase 14AJ/14AL Asia/Kolkata + full weekday regression: PASS")


if __name__ == "__main__":
    main()
