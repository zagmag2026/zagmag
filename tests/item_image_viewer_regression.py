from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GALLERY = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/ui/components/ItemImageGallery.kt"
CARDS = ROOT / "apps/admin-android/app/src/main/java/com/nimsdeveloper/zhagmagdresses/admin/ui/components/CardPatterns.kt"
RULES = ROOT / "docs/GLOBAL_UI_RULES.md"

gallery = GALLERY.read_text(encoding="utf-8")
cards = CARDS.read_text(encoding="utf-8")
rules = RULES.read_text(encoding="utf-8")
errors=[]

def need(body, token, label):
    if token not in body:
        errors.append(f"{label}: missing {token}")

for token in [
    "usePlatformDefaultWidth = false",
    "decorFitsSystemWindows = false",
    "dismissOnClickOutside = false",
    ".background(Color.Black)",
    "ContentScale.Fit",
    "HorizontalPager(",
    "userScrollEnabled = zoomedPage == null",
    "awaitEachGesture",
    "awaitFirstDown(requireUnconsumed = false)",
    "pressedPointers >= 2",
    "transformOwnsGesture = scale > 1.01f",
    "event.calculateZoom()",
    "event.calculatePan()",
    "change.consume()",
    "detectTapGestures",
    "onDoubleTap",
    "coerceIn(1f, 4f)",
    "scale = 2.5f",
    "graphicsLayer(",
    "translationX = offset.x",
    "translationY = offset.y",
    "controlsVisible = !controlsVisible",
    "onLoading =",
    "onSuccess =",
    "onError =",
    '"Image unavailable"',
    '"Close image gallery"',
    ' / ${images.size}"',
]:
    need(gallery, token, "Full-screen Item image viewer")

if "detectTransformGestures" in gallery:
    errors.append("Gallery must not let the old single-finger transform detector steal 1x horizontal pager swipes.")
if "scrim.copy(alpha" in gallery:
    errors.append("Gallery must not use the old translucent scrim over the underlying Admin screen.")
if ".padding(horizontal = 12.dp, vertical = 72.dp)" in gallery:
    errors.append("Gallery image must not remain constrained inside the old modal-style padding.")

for token in [
    "RoundedItemImage(",
    "Modifier.clickable(",
    "ItemImageGalleryDialog(",
    "galleryUrls = imageUrls",
]:
    need(cards, token, "Shared clickable Item image")

for token in [
    "true edge-to-edge full-screen viewer",
    "solid black background",
    "Pinch-to-zoom",
    "double-tap zoom/reset",
    "local-only",
]:
    need(rules, token, "Active gallery UI rule")

if errors:
    print("Item image viewer regression FAILED")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("Item image viewer regression PASS")
