import "./phase14x-booking-item-state.css";

/* Phase 14X Fix7 + Phase 14AA stability hardening — New Booking item search/availability clarity.
   Reuses the existing availability request only; never creates an API call. */
const bText = node => String(node?.textContent || "").trim().replace(/\s+/g, " ");
let bQueued = false;
let bAvailabilityPending = false;
let bAvailabilityAwaiting = false;
let bAvailabilityError = "";
let bAvailabilitySerial = 0;
let bAvailabilityItems = new Map();
let bAvailabilityRequested = new Set();

function bSetText(node, value) {
  if (!node) return;
  const next = String(value ?? "");
  if (node.textContent !== next) node.textContent = next;
}
function bSetClass(node, name, enabled) {
  if (!node) return;
  const next = !!enabled;
  if (node.classList.contains(name) !== next) node.classList.toggle(name, next);
}
function bSetTitle(node, value) {
  if (!node) return;
  const next = String(value || "");
  if (node.getAttribute("title") !== next) node.setAttribute("title", next);
}
function bIsAvailabilityUrl(value) {
  return /\/api\/admin\/bookings\/availability(?:\?|$)/.test(String(value || ""));
}
function bInputUrl(input) {
  return typeof input === "string" ? input : String(input?.url || "");
}
function bRequestIds(init) {
  try {
    const body = typeof init?.body === "string" ? JSON.parse(init.body) : null;
    return new Set(Array.isArray(body?.itemIds) ? body.itemIds.map(String) : []);
  } catch { return new Set(); }
}

/* Observe the existing bundled availability request without adding another call. */
const bPriorFetch = window.fetch.bind(window);
window.fetch = async (...args) => {
  const [input, init] = args;
  if (!bIsAvailabilityUrl(bInputUrl(input))) return bPriorFetch(...args);
  const serial = ++bAvailabilitySerial;
  bAvailabilityPending = true;
  bAvailabilityAwaiting = false;
  bAvailabilityError = "";
  bAvailabilityItems = new Map();
  bAvailabilityRequested = bRequestIds(init);
  bSchedule();
  try {
    const response = await bPriorFetch(...args);
    if (serial !== bAvailabilitySerial) return response;
    if (!response.ok) {
      bAvailabilityError = "Availability check failed.";
      return response;
    }
    try {
      const data = await response.clone().json();
      bAvailabilityItems = new Map((Array.isArray(data?.availability) ? data.availability : []).map(item => [String(item.itemId), item]));
    } catch {
      bAvailabilityError = "Availability response unavailable.";
      bAvailabilityItems = new Map();
    }
    return response;
  } catch (error) {
    if (serial === bAvailabilitySerial) {
      bAvailabilityError = error instanceof Error ? error.message : "Availability check failed.";
      bAvailabilityItems = new Map();
    }
    throw error;
  } finally {
    if (serial === bAvailabilitySerial) {
      bAvailabilityPending = false;
      bSchedule();
    }
  }
};

function bBookingForm() {
  if (bText(document.querySelector(".page-head h1")) !== "Bookings") return null;
  return document.querySelector(".booking-layout .booking-form");
}
function bItemSearch(form) { return form?.querySelector(".booking-items-head input") || null; }
function bItemSelects(form) { return [...(form?.querySelectorAll(".booking-line select") || [])]; }
function bCacheSelections(form) {
  bItemSelects(form).forEach(select => {
    if (!(select instanceof HTMLSelectElement) || !select.value) return;
    const option = select.selectedOptions?.[0];
    if (!option || !option.value) return;
    if (select.dataset.phase14xSelectedValue !== option.value) select.dataset.phase14xSelectedValue = option.value;
    const label = bText(option);
    if (select.dataset.phase14xSelectedLabel !== label) select.dataset.phase14xSelectedLabel = label;
  });
}
function bRestoreSelections(form) {
  bItemSelects(form).forEach(select => {
    if (!(select instanceof HTMLSelectElement)) return;
    const value = select.dataset.phase14xSelectedValue || "";
    const label = select.dataset.phase14xSelectedLabel || "";
    if (!value || !label || [...select.options].some(option => option.value === value)) return;
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    option.dataset.phase14xRetained = "1";
    select.add(option, Math.min(1, select.options.length));
    select.value = value;
  });
}
function bEmptyMessage(form) {
  const search = bItemSearch(form);
  if (!(search instanceof HTMLInputElement)) return "";
  const query = search.value.trim();
  const selects = bItemSelects(form).filter(select => select instanceof HTMLSelectElement);
  const selected = new Set(selects.map(select => select.value).filter(Boolean));
  const candidates = new Set();
  for (const select of selects) {
    for (const option of select.options) {
      if (!option.value || option.dataset.phase14xRetained === "1" || selected.has(option.value)) continue;
      candidates.add(option.value);
    }
  }
  if (query && candidates.size === 0) return "No matching items found.";
  if (!query && selects.some(select => !select.value) && candidates.size === 0) return selected.size ? "All available items are already selected." : "No items available to select.";
  return "";
}
function bSyncEmptyState(form) {
  const head = form.querySelector(".booking-items-head");
  if (!head) return;
  let node = form.querySelector(".phase14x-booking-item-empty");
  const message = bEmptyMessage(form);
  if (!message) { node?.remove(); return; }
  if (!node) {
    node = document.createElement("div");
    node.className = "phase14x-booking-item-empty";
    node.setAttribute("role", "status");
    node.setAttribute("aria-live", "polite");
    head.insertAdjacentElement("afterend", node);
  }
  bSetText(node, message);
}
function bAvailabilityCopy(itemId, qty) {
  if (bAvailabilityAwaiting || bAvailabilityPending) return { text: "Checking availability…", kind: "checking" };
  if (bAvailabilityError) return { text: "Availability unavailable", kind: "unavailable" };
  const data = bAvailabilityItems.get(String(itemId));
  if (!data && bAvailabilityRequested.has(String(itemId))) return { text: "Availability unavailable", kind: "unavailable" };
  if (!data) return null;
  const available = Math.max(0, Number(data.availableQuantity) || 0);
  const total = Math.max(0, Number(data.totalQuantity) || 0);
  if (available === 0) return { text: "Not available for selected dates", kind: "bad" };
  if (qty > available) return { text: `Only ${available} available`, kind: "bad" };
  return { text: `Available ${available} / ${total}`, kind: "ok" };
}
function bSyncAvailability(form) {
  form.querySelectorAll(".booking-line").forEach(line => {
    const select = line.querySelector("select");
    const qty = line.querySelector('input[type="number"]');
    const pill = line.querySelector(".availability-pill");
    if (!(select instanceof HTMLSelectElement) || !(qty instanceof HTMLInputElement) || !(pill instanceof HTMLElement)) return;
    if (!select.value) {
      bSetClass(pill, "phase14x-checking", false);
      bSetClass(pill, "phase14x-unavailable", false);
      return;
    }
    const copy = bAvailabilityCopy(select.value, Math.max(1, Number(qty.value) || 1));
    if (!copy) return;
    bSetText(pill, copy.text);
    bSetClass(pill, "bad", copy.kind === "bad");
    bSetClass(pill, "phase14x-checking", copy.kind === "checking");
    bSetClass(pill, "phase14x-unavailable", copy.kind === "unavailable");
    bSetTitle(pill, copy.kind === "unavailable" ? "Change the item or dates to retry availability." : "");
  });
  let error = form.querySelector(".phase14x-booking-availability-error");
  const selected = bItemSelects(form).some(select => select.value);
  if (!bAvailabilityError || !selected) { error?.remove(); return; }
  if (!error) {
    error = document.createElement("div");
    error.className = "phase14x-booking-availability-error";
    error.setAttribute("role", "status");
    form.querySelector(".booking-lines")?.insertAdjacentElement("afterend", error);
  }
  bSetText(error, "Availability could not be checked. Change the item or dates to retry.");
}
function bScan() {
  bQueued = false;
  const form = bBookingForm();
  if (!form) return;
  bRestoreSelections(form);
  bSyncEmptyState(form);
  bSyncAvailability(form);
}
function bSchedule() {
  if (bQueued) return;
  bQueued = true;
  requestAnimationFrame(bScan);
}

document.addEventListener("input", event => {
  const form = bBookingForm();
  if (!form) return;
  const target = event.target;
  if (target === bItemSearch(form)) bCacheSelections(form);
  if (target instanceof HTMLInputElement && target.type === "number" && target.closest(".booking-line")) bSchedule();
}, true);
document.addEventListener("change", event => {
  const form = bBookingForm();
  if (!form) return;
  const target = event.target;
  if (target instanceof HTMLSelectElement && target.closest(".booking-line")) {
    if (target.value) {
      const option = target.selectedOptions?.[0];
      if (option?.value) {
        target.dataset.phase14xSelectedValue = option.value;
        target.dataset.phase14xSelectedLabel = bText(option);
      }
    } else {
      delete target.dataset.phase14xSelectedValue;
      delete target.dataset.phase14xSelectedLabel;
    }
    bAvailabilityAwaiting = bItemSelects(form).some(select => !!select.value);
    bAvailabilityError = "";
    bAvailabilityItems = new Map();
    bSchedule();
    return;
  }
  if (target instanceof HTMLInputElement && target.type === "date" && target.closest(".booking-layout")) {
    bAvailabilityAwaiting = bItemSelects(form).some(select => !!select.value);
    bAvailabilityError = "";
    bAvailabilityItems = new Map();
    bSchedule();
  }
}, true);
/* Phase 14AA: textContent writes are idempotent, and text-node mutations no longer
   reschedule full form scans. Child additions/removals still refresh the UI. */
new MutationObserver(bSchedule).observe(document.documentElement, { childList: true, subtree: true });
bSchedule();
