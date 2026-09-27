import "./phase14t-public.css";
import "./phase14u-public.css";
import "./phase14v-public.js";

/* Phase 14T modal focus continuity. Carousel motion is React-owned from Phase 14U onward. */
const ptFocusable = 'button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])';
let ptOpener = null;
let ptModalWasOpen = false;
let ptScanQueued = false;

function ptVisibleModal() {
  const modal = document.querySelector(".detail-modal");
  return modal instanceof HTMLElement && modal.getClientRects().length ? modal : null;
}
function ptScan() {
  ptScanQueued = false;
  const modal = ptVisibleModal();
  if (modal) {
    document.body.classList.add("phase14t-public-modal-open");
    if (!modal.dataset.phase14tFocus) {
      modal.dataset.phase14tFocus = "1";
      window.setTimeout(() => modal.querySelector(".close-btn")?.focus(), 0);
    }
    ptModalWasOpen = true;
  } else if (ptModalWasOpen) {
    document.body.classList.remove("phase14t-public-modal-open");
    ptModalWasOpen = false;
    const returnTo = ptOpener;
    ptOpener = null;
    if (returnTo instanceof HTMLElement && returnTo.isConnected) window.setTimeout(() => returnTo.focus(), 0);
  }
}
function ptSchedule() {
  if (ptScanQueued) return;
  ptScanQueued = true;
  requestAnimationFrame(ptScan);
}
document.addEventListener("click", event => {
  const target = event.target instanceof Element ? event.target : null;
  if (!target) return;
  const opener = target.closest(".image-button.public-card-carousel,.card-actions button");
  if (opener && (opener.matches(".image-button.public-card-carousel") || /details/i.test(String(opener.textContent || "")))) ptOpener = opener;
}, true);
document.addEventListener("keydown", event => {
  const modal = ptVisibleModal();
  if (!modal) return;
  if (event.key === "Escape") {
    event.preventDefault();
    event.stopImmediatePropagation();
    modal.querySelector(".close-btn")?.click();
    return;
  }
  if (event.key !== "Tab") return;
  const nodes = [...modal.querySelectorAll(ptFocusable)].filter(node => node instanceof HTMLElement && node.getClientRects().length);
  if (!nodes.length) return;
  const first = nodes[0], last = nodes.at(-1);
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
}, true);
new MutationObserver(ptSchedule).observe(document.documentElement, { childList: true, subtree: true });
ptSchedule();
