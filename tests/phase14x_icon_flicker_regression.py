#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def main():
    package = json.loads((ROOT / "package.json").read_text(encoding="utf-8"))
    legacy = (ROOT / "apps/admin-web/src/phase14j.js").read_text(encoding="utf-8")
    runtime = (ROOT / "apps/admin-web/src/phase14x-icon-runtime.js").read_text(encoding="utf-8")

    # phase14j keeps copy/placeholder cleanup but must never own icon datasets again.
    assert "PLACEHOLDERS" in legacy
    assert "shouldHideCopy" in legacy
    assert "MENU_GLYPHS" not in legacy
    assert "glyphFor(" not in legacy
    assert "dataset.uiGlyph" not in legacy
    assert 'querySelectorAll?.("button,a")' not in legacy

    # Material runtime is the single icon owner and updates before paint.
    assert "single-owner, idempotent Admin Material icon runtime" in runtime
    assert "setDataset(element, \"uiGlyph\", icon)" in runtime
    assert "if (element.dataset[key] === value) return" in runtime
    assert "queueMicrotask" in runtime
    assert "requestAnimationFrame" not in runtime
    assert "MutationObserver(schedule)" in runtime
    assert "characterData: true" in runtime
    assert "LEGACY_PREFIX" in runtime
    assert "stripLegacyPrefix(element)" in runtime
    assert "stableLabel(element)" in runtime
    assert "phase14arSource" in runtime and "uiLabelKey" in runtime
    assert "iconFor(element, stableLabel(element))" in runtime
    assert 'stableLabel(card.querySelector("span"))' in runtime
    assert "iconFor(element, text(element))" not in runtime
    assert "kpi[text(card.querySelector" not in runtime
    assert "phase14arRendered" in runtime
    assert "fetch(" not in runtime and "api(" not in runtime

    script = package["scripts"]["test:hardening"]
    assert "tests/phase14x_icon_flicker_regression.py" in script
    print("Phase 14X Fix8/Phase14AR stable-language icon flicker regression: PASS")


if __name__ == "__main__":
    main()