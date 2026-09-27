from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JS = (ROOT / "apps/admin-web/src/phase14af-form-canonical.js").read_text()
CSS = (ROOT / "apps/admin-web/src/phase14af-form-canonical.css").read_text()
ENTRY = (ROOT / "apps/admin-web/src/phase14k.js").read_text()
PKG = (ROOT / "package.json").read_text()


def must(text: str, needle: str, label: str) -> None:
    assert needle in text, f"missing {label}: {needle}"


def must_not(text: str, needle: str, label: str) -> None:
    assert needle not in text, f"unexpected {label}: {needle}"


must(ENTRY, 'import "./phase14af-form-canonical.js";', "canonical form runtime import")
must(JS, 'import "./phase14af-form-canonical.css";', "canonical form css import")

for kind in ["item", "customer", "booking", "category", "user"]:
    must(JS, f"{kind}:", f"{kind} runtime config")
    must(CSS, f'data-phase14af-kind="{kind}"', f"{kind} canonical CSS ownership")

must(JS, "formSignature", "dirty-form signature")
must(JS, "captureSnapshot", "initial form snapshot")
must(JS, "restoreSnapshot", "discard restore")
must(JS, "Discard unsaved", "dirty-close confirmation")
must(JS, 'event.key === "Escape"', "Escape handling")
must(JS, 'event.key !== "Tab"', "focus trap")
must(JS, "MutationObserver", "event-driven open-state sync")
must_not(JS, "setInterval", "polling")
must_not(JS, "fetch(", "network call")

must(CSS, ".phase14af-canonical-form>.primary.full", "shared primary footer")
must(CSS, "position:sticky!important", "shared inset sticky action")
must(CSS, ".phase14p-drawer-close", "item close parity")
must(CSS, ".phase14q-form-close", "customer/booking close parity")
must(CSS, ".multi-options>.check", "dynamic multi-select parity")
must(CSS, 'data-phase14af-kind="booking"', "booking footer/control parity")
must(CSS, "body.phase14p-item-drawer-open>.phase14p-drawer-close", "closed item X guard")
must(CSS, "body.phase14q-form-drawer-open>.phase14q-form-close", "closed form X guard")

must(PKG, "phase14af_form_canonical_regression.py", "hardening registration")

print("Phase 14AF canonical Create/Edit forms regression: PASS")
