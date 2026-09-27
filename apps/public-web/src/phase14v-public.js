import "./phase14v-public.css";

/* Phase 14V — serialize public data interactions around the existing fetches.
   No request is created by this layer; it only adds timeout + UI concurrency guards. */
const PV_TIMEOUT_MS = 25000;
const pvNativeFetch = window.fetch.bind(window);
const pvAriaState = new WeakMap();
const pvDisabledState = new WeakMap();
let pvBusyCount = 0;
let pvScanQueued = false;

function pvUrl(input) {
  if (typeof input === "string") return input;
  if (input instanceof URL) return input.href;
  return String(input?.url || "");
}
function pvIsPublicDataRequest(input) {
  try {
    const path = new URL(pvUrl(input), window.location.href).pathname;
    return path === "/api/public/catalog" || path === "/api/public/availability";
  } catch {
    return false;
  }
}
function pvGuardedControls() {
  return document.querySelectorAll(".category-chip,.search-form button,.pager button,.availability-box button");
}
function pvDateInputs() {
  return document.querySelectorAll('.availability-box input[type="date"]');
}
function pvSetAriaDisabled(element, busy) {
  if (!(element instanceof HTMLElement)) return;
  if (busy) {
    if (!pvAriaState.has(element)) pvAriaState.set(element, element.getAttribute("aria-disabled"));
    element.setAttribute("aria-disabled", "true");
    return;
  }
  if (!pvAriaState.has(element)) return;
  const previous = pvAriaState.get(element);
  if (previous === null) element.removeAttribute("aria-disabled");
  else element.setAttribute("aria-disabled", previous);
  pvAriaState.delete(element);
}
function pvSetDateDisabled(input, busy) {
  if (!(input instanceof HTMLInputElement)) return;
  if (busy) {
    if (!pvDisabledState.has(input)) pvDisabledState.set(input, input.disabled);
    input.disabled = true;
    return;
  }
  if (!pvDisabledState.has(input)) return;
  input.disabled = Boolean(pvDisabledState.get(input));
  pvDisabledState.delete(input);
}
function pvSyncBusyUi() {
  pvScanQueued = false;
  const busy = pvBusyCount > 0;
  document.body.classList.toggle("phase14v-public-data-busy", busy);
  pvGuardedControls().forEach(element => pvSetAriaDisabled(element, busy));
  pvDateInputs().forEach(input => pvSetDateDisabled(input, busy));
}
function pvScheduleSync() {
  if (pvScanQueued) return;
  pvScanQueued = true;
  requestAnimationFrame(pvSyncBusyUi);
}
function pvBegin() {
  pvBusyCount += 1;
  pvSyncBusyUi();
}
function pvEnd() {
  pvBusyCount = Math.max(0, pvBusyCount - 1);
  pvSyncBusyUi();
}

window.fetch = async (input, init = {}) => {
  if (!pvIsPublicDataRequest(input)) return pvNativeFetch(input, init);

  const controller = new AbortController();
  const upstreamSignal = init?.signal;
  let timedOut = false;
  let detachAbort = null;
  if (upstreamSignal) {
    const forwardAbort = () => controller.abort();
    if (upstreamSignal.aborted) forwardAbort();
    else {
      upstreamSignal.addEventListener("abort", forwardAbort, { once: true });
      detachAbort = () => upstreamSignal.removeEventListener("abort", forwardAbort);
    }
  }
  const timeout = window.setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, PV_TIMEOUT_MS);

  pvBegin();
  try {
    return await pvNativeFetch(input, { ...init, signal: controller.signal });
  } catch (error) {
    if (timedOut) throw new Error("Request timed out. Please try again.");
    throw error;
  } finally {
    window.clearTimeout(timeout);
    detachAbort?.();
    pvEnd();
  }
};

function pvBusyTarget(event) {
  if (pvBusyCount < 1 || !(event.target instanceof Element)) return null;
  return event.target.closest(".category-chip,.search-form button,.pager button,.availability-box button");
}
document.addEventListener("click", event => {
  if (!pvBusyTarget(event)) return;
  event.preventDefault();
  event.stopImmediatePropagation();
}, true);
document.addEventListener("submit", event => {
  if (pvBusyCount < 1 || !(event.target instanceof HTMLFormElement) || !event.target.matches(".search-form")) return;
  event.preventDefault();
  event.stopImmediatePropagation();
}, true);

new MutationObserver(() => {
  if (pvBusyCount > 0) pvScheduleSync();
}).observe(document.documentElement, { childList: true, subtree: true });
pvSyncBusyUi();
