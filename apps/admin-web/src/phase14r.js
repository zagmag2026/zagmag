import "./phase14r.css";

/* Phase 14R — User Experience Hardening. Admin-only interaction layer; zero extra API calls. */
const rText = (node) => String(node?.textContent || "").trim().replace(/\s+/g, " ");
const rButtonLabel = (button) => rText(button);
const rSafeImageUrl = (value) => {
  const url = String(value || "").trim();
  return /^https?:\/\//i.test(url) ? url : "";
};
let rScanQueued = false;
let rBookingItems = new Map();
let rAvailability = new Map();
let rPickerSelect = null;
let rDiscardCallback = null;
let rBypassCloseGuard = false;
let rReportReturnFocus = null;
let rLastMappedServerError = "";
const rDirty = { item: false, customer: false, booking: false };

/* Capture existing bootstrap/availability responses without creating any new Cloudflare request. */
const rNativeFetch = window.fetch.bind(window);
window.fetch = async (...args) => {
  const response = await rNativeFetch(...args);
  try {
    const input = args[0];
    const url = typeof input === "string" ? input : String(input?.url || "");
    if (/\/api\/admin\/bookings\/bootstrap(?:\?|$)/.test(url) && response.ok) {
      response.clone().json().then(data => {
        if (Array.isArray(data?.items)) {
          rBookingItems = new Map(data.items.map(item => [String(item.id), item]));
          rScheduleScan();
        }
      }).catch(() => {});
    }
    if (/\/api\/admin\/bookings\/availability(?:\?|$)/.test(url) && response.ok) {
      response.clone().json().then(data => {
        if (Array.isArray(data?.availability)) {
          for (const item of data.availability) rAvailability.set(String(item.itemId), item);
          rScheduleScan();
        }
      }).catch(() => {});
    }
  } catch {}
  return response;
};

function rPortalButton(className, label, handler) {
  let button = document.querySelector(`body > .${className}`);
  if (!button) {
    button = document.createElement("button");
    button.type = "button";
    button.className = className;
    button.setAttribute("aria-label", label);
    document.body.appendChild(button);
    button.addEventListener("click", handler);
  }
  return button;
}

function rOpenKind() {
  if (document.body.classList.contains("phase14p-item-drawer-open")) return "item";
  if (document.body.classList.contains("phase14q-customer-drawer-open")) return "customer";
  if (document.body.classList.contains("phase14q-booking-drawer-open")) return "booking";
  return null;
}

function rDrawerSelector(kind) {
  if (kind === "item") return ".item-editor";
  if (kind === "customer") return ".customer-layout";
  if (kind === "booking") return ".booking-layout";
  return "";
}

function rClosePortalSelector(kind) {
  return kind === "item" ? ".phase14p-drawer-close" : ".phase14q-form-close";
}

function rMarkDirty(kind) {
  if (kind in rDirty) rDirty[kind] = true;
  document.body.classList.toggle("phase14r-unsaved", Object.values(rDirty).some(Boolean));
}

function rClearDirty(kind) {
  if (kind in rDirty) rDirty[kind] = false;
  document.body.classList.toggle("phase14r-unsaved", Object.values(rDirty).some(Boolean));
}

function rEnsureDiscardDialog() {
  if (document.querySelector(".phase14r-discard-backdrop")) return;
  const backdrop = document.createElement("div");
  backdrop.className = "phase14r-discard-backdrop";
  const dialog = document.createElement("section");
  dialog.className = "phase14r-discard-dialog";
  dialog.setAttribute("role", "dialog");
  dialog.setAttribute("aria-modal", "true");
  dialog.setAttribute("aria-labelledby", "phase14r-discard-title");
  dialog.innerHTML = `<div class="phase14r-dialog-icon">!</div><h2 id="phase14r-discard-title">Discard unsaved changes?</h2><p>Your changes have not been saved. Keep editing to avoid losing them.</p><div class="phase14r-dialog-actions"><button type="button" class="ghost phase14r-keep-editing">Keep Editing</button><button type="button" class="phase14r-discard-confirm">Discard Changes</button></div>`;
  backdrop.appendChild(dialog);
  backdrop.addEventListener("click", event => { if (event.target === backdrop) rHideDiscardDialog(); });
  dialog.querySelector(".phase14r-keep-editing")?.addEventListener("click", rHideDiscardDialog);
  dialog.querySelector(".phase14r-discard-confirm")?.addEventListener("click", () => {
    const callback = rDiscardCallback;
    rDiscardCallback = null;
    backdrop.classList.remove("open");
    callback?.();
  });
  document.body.appendChild(backdrop);
}

function rShowDiscardDialog(callback) {
  rEnsureDiscardDialog();
  rDiscardCallback = callback;
  const backdrop = document.querySelector(".phase14r-discard-backdrop");
  backdrop?.classList.add("open");
  window.setTimeout(() => backdrop?.querySelector(".phase14r-keep-editing")?.focus(), 0);
}

function rHideDiscardDialog() {
  document.querySelector(".phase14r-discard-backdrop")?.classList.remove("open");
  rDiscardCallback = null;
}

function rConfirmClose(kind, closeAction) {
  if (!kind || !rDirty[kind] || rBypassCloseGuard) return false;
  rShowDiscardDialog(() => {
    rClearDirty(kind);
    rBypassCloseGuard = true;
    try { closeAction(); } finally { rBypassCloseGuard = false; }
  });
  return true;
}

/* Inline validation and first-error guidance. */
function rErrorHost(control) {
  return control.closest("label") || control.closest(".booking-line") || control.parentElement;
}

function rClearInlineError(control) {
  if (!(control instanceof HTMLElement)) return;
  control.removeAttribute("aria-invalid");
  delete control.dataset.phase14rServerError;
  const host = rErrorHost(control);
  host?.querySelector(":scope > .phase14r-inline-error")?.remove();
}

function rSetInlineError(control, message) {
  if (!(control instanceof HTMLElement)) return;
  rClearInlineError(control);
  control.setAttribute("aria-invalid", "true");
  const host = rErrorHost(control);
  if (!host) return;
  const error = document.createElement("small");
  error.className = "phase14r-inline-error";
  error.textContent = String(message || "Check this field and try again.");
  host.appendChild(error);
}

function rValidityMessage(control) {
  if (control.validity?.valueMissing) return "This field is required.";
  if (control.validity?.typeMismatch) return "Enter a valid value.";
  if (control.validity?.rangeUnderflow) return `Minimum allowed is ${control.min || "required value"}.`;
  if (control.validity?.rangeOverflow) return `Maximum allowed is ${control.max || "allowed value"}.`;
  if (control.validity?.tooShort) return `Enter at least ${control.minLength} characters.`;
  return "Check this field and try again.";
}

function rFocusError(control) {
  if (!(control instanceof HTMLElement)) return;
  control.scrollIntoView({ behavior: "smooth", block: "center" });
  window.setTimeout(() => control.focus(), 180);
}

function rMapServerError() {
  const kind = rOpenKind();
  if (!kind) { rLastMappedServerError = ""; return; }
  const messageNode = [...document.querySelectorAll(".page-message.message.error")].find(node => rText(node));
  const message = rText(messageNode);
  if (!message) { rLastMappedServerError = ""; return; }
  if (message === rLastMappedServerError) return;
  const drawer = document.querySelector(rDrawerSelector(kind));
  if (!drawer) return;
  let target = null;
  if (kind === "item") {
    if (/code|duplicate/i.test(message)) target = drawer.querySelector("input");
    else if (/quantity|stock/i.test(message)) target = drawer.querySelector('input[type="number"]');
    else if (/category/i.test(message)) target = drawer.querySelector("select");
  } else if (kind === "customer") {
    if (/mobile|phone|duplicate/i.test(message)) target = drawer.querySelector('input[inputmode="tel"]');
    else if (/name/i.test(message)) target = drawer.querySelector("input");
  } else {
    if (/date|range|return|pickup/i.test(message)) target = [...drawer.querySelectorAll('input[type="date"]')].pop();
    else if (/availability|stock|quantity|item/i.test(message)) target = drawer.querySelector(".booking-line select, .booking-line input[type=number]");
    else if (/customer/i.test(message)) target = drawer.querySelector("form > label select");
  }
  if (target) {
    rSetInlineError(target, message);
    target.dataset.phase14rServerError = message;
    rLastMappedServerError = message;
    rFocusError(target);
  }
}

/* Mobile booking steps: Customer → Dates → Items → Review & Save. */
function rBookingForm() { return document.querySelector(".booking-layout .booking-form"); }

function rMarkStepGroup(node, step) {
  if (node instanceof HTMLElement) node.dataset.phase14rStepGroup = String(step);
}

function rEnsureBookingStepper() {
  const form = rBookingForm();
  if (!form) return;
  if (!form.dataset.phase14rStep) form.dataset.phase14rStep = "1";

  let stepper = form.querySelector(":scope > .phase14r-stepper");
  if (!stepper) {
    stepper = document.createElement("div");
    stepper.className = "phase14r-stepper";
    ["Customer", "Dates", "Items", "Review"].forEach((label, index) => {
      const node = document.createElement("span");
      node.dataset.step = String(index + 1);
      const number = document.createElement("b"); number.textContent = String(index + 1);
      const name = document.createElement("em"); name.textContent = label;
      node.append(number, name); stepper.appendChild(node);
    });
    form.querySelector(":scope > .form-title")?.after(stepper);
  }

  rMarkStepGroup(form.querySelector(":scope > .customer-picker"), 1);
  const directLabels = [...form.children].filter(node => node instanceof HTMLLabelElement);
  const customerLabel = directLabels.find(label => label.querySelector("select"));
  rMarkStepGroup(customerLabel, 1);
  const dateGrid = [...form.querySelectorAll(":scope > .form-grid")].find(grid => grid.querySelector('input[type="date"]'));
  rMarkStepGroup(dateGrid, 2);
  rMarkStepGroup(form.querySelector(":scope > .booking-items-head"), 3);
  rMarkStepGroup(form.querySelector(":scope > .booking-lines"), 3);
  rMarkStepGroup(form.querySelector(":scope > .add-line"), 3);
  const notesLabel = directLabels.find(label => /^Notes/i.test(rText(label)));
  rMarkStepGroup(notesLabel, 4);
  rMarkStepGroup(form.querySelector(":scope > .booking-save-note"), 4);
  rMarkStepGroup(form.querySelector(":scope > .primary.full"), 4);

  let review = form.querySelector(":scope > .phase14r-booking-review");
  if (!review) {
    review = document.createElement("section");
    review.className = "phase14r-booking-review";
    review.dataset.phase14rStepGroup = "4";
    (notesLabel || form.querySelector(":scope > .booking-save-note"))?.before(review);
  }

  let controls = form.querySelector(":scope > .phase14r-step-controls");
  if (!controls) {
    controls = document.createElement("div");
    controls.className = "phase14r-step-controls";
    const back = document.createElement("button"); back.type = "button"; back.className = "ghost phase14r-step-back"; back.textContent = "Back";
    const next = document.createElement("button"); next.type = "button"; next.className = "primary phase14r-step-next"; next.textContent = "Next";
    controls.append(back, next); form.appendChild(controls);
    back.addEventListener("click", () => rSetBookingStep(Number(form.dataset.phase14rStep || 1) - 1));
    next.addEventListener("click", () => {
      const current = Number(form.dataset.phase14rStep || 1);
      if (rValidateBookingStep(current)) rSetBookingStep(current + 1);
    });
  }
  rSetBookingStep(Number(form.dataset.phase14rStep || 1), false);
  rUpdateBookingReview();
}

function rSetBookingStep(step, focus = true) {
  const form = rBookingForm();
  if (!form) return;
  const next = Math.min(4, Math.max(1, Number(step) || 1));
  form.dataset.phase14rStep = String(next);
  form.querySelectorAll("[data-phase14r-step-group]").forEach(node => node.classList.toggle("phase14r-step-active", Number(node.dataset.phase14rStepGroup) === next));
  form.querySelectorAll(".phase14r-stepper [data-step]").forEach(node => {
    const value = Number(node.dataset.step);
    node.classList.toggle("active", value === next);
    node.classList.toggle("done", value < next);
  });
  const back = form.querySelector(".phase14r-step-back");
  const forward = form.querySelector(".phase14r-step-next");
  if (back) back.disabled = next === 1;
  if (forward) forward.hidden = next === 4;
  if (focus && window.matchMedia("(max-width:760px)").matches) {
    const active = form.querySelector(".phase14r-step-active input, .phase14r-step-active select, .phase14r-step-active textarea, .phase14r-step-active button");
    window.setTimeout(() => active?.focus(), 40);
  }
}

function rValidateBookingStep(step) {
  const form = rBookingForm();
  if (!form) return true;
  let target = null;
  let message = "";
  if (step === 1) {
    const customer = [...form.children].find(node => node instanceof HTMLLabelElement && node.querySelector("select"))?.querySelector("select");
    if (!customer?.value) { target = customer; message = "Select a customer before continuing."; }
  } else if (step === 2) {
    const dates = [...form.querySelectorAll('input[type="date"]')];
    const pickup = dates[0], ret = dates[1];
    if (!pickup?.value) { target = pickup; message = "Select a pickup date."; }
    else if (!ret?.value) { target = ret; message = "Select a return date."; }
    else if (ret.value <= pickup.value) { target = ret; message = "Return date must be after pickup date."; }
  } else if (step === 3) {
    for (const line of form.querySelectorAll(".booking-line")) {
      const select = line.querySelector("select");
      const qty = line.querySelector('input[type="number"]');
      const availability = line.querySelector(".availability-pill");
      const availabilityText = rText(availability);
      if (!select?.value) { target = select; message = "Select an item for every line."; break; }
      if (!qty?.value || Number(qty.value) < 1) { target = qty; message = "Quantity must be at least 1."; break; }
      if (/Checking/i.test(availabilityText)) { target = select; message = "Wait for the availability check to finish."; break; }
      if (availability?.classList.contains("bad")) { target = qty; message = availabilityText || "Requested quantity is not available."; break; }
    }
  }
  if (target) {
    rSetInlineError(target, message);
    rFocusError(target);
    return false;
  }
  return true;
}

function rUpdateBookingReview() {
  const form = rBookingForm();
  const review = form?.querySelector(":scope > .phase14r-booking-review");
  if (!form || !review) return;
  review.replaceChildren();
  const title = document.createElement("div"); title.className = "phase14r-review-title";
  const strong = document.createElement("strong"); strong.textContent = "Review Booking";
  const hint = document.createElement("span"); hint.textContent = "Confirm customer, dates and every selected dress before saving.";
  title.append(strong, hint);
  const grid = document.createElement("div"); grid.className = "phase14r-review-grid";
  const customer = [...form.children].find(node => node instanceof HTMLLabelElement && node.querySelector("select"))?.querySelector("select");
  const dates = [...form.querySelectorAll('input[type="date"]')];
  [["Customer", customer?.selectedOptions?.[0]?.textContent || "Not selected"], ["Pickup", dates[0]?.value || "—"], ["Return", dates[1]?.value || "—"]].forEach(([label, value]) => {
    const cell = document.createElement("span"); cell.textContent = label;
    const b = document.createElement("b"); b.textContent = value; cell.appendChild(b); grid.appendChild(cell);
  });
  const list = document.createElement("ul");
  const selected = [...form.querySelectorAll(".booking-line")].filter(line => line.querySelector("select")?.value);
  if (!selected.length) {
    const li = document.createElement("li"); const text = document.createElement("span"); text.textContent = "No item selected"; li.appendChild(text); list.appendChild(li);
  } else {
    selected.forEach(line => {
      const select = line.querySelector("select"); const qty = line.querySelector('input[type="number"]');
      const li = document.createElement("li"); const text = document.createElement("span"); text.textContent = select?.selectedOptions?.[0]?.textContent || "Item";
      const count = document.createElement("b"); count.textContent = `Qty ${qty?.value || 1}`; li.append(text, count); list.appendChild(li);
    });
  }
  review.append(title, grid, list);
}

/* Visual item picker — uses already-loaded booking bootstrap data only. */
function rFallbackItems(select) {
  return [...select.options].filter(option => option.value).map(option => {
    const parts = String(option.textContent || "").split("·").map(v => v.trim());
    return { id: option.value, item_code: parts[0] || "", item_name: parts[1] || parts[0] || "Item", category_name: parts[2] || "", total_quantity: null, image_url: null };
  });
}

function rItemSource(select) {
  return rBookingItems.size ? [...rBookingItems.values()] : rFallbackItems(select);
}

function rEnsureItemPicker() {
  if (document.querySelector(".phase14r-item-picker-backdrop")) return;
  const backdrop = document.createElement("div"); backdrop.className = "phase14r-item-picker-backdrop";
  const panel = document.createElement("section"); panel.className = "phase14r-item-picker"; panel.setAttribute("role", "dialog"); panel.setAttribute("aria-modal", "true"); panel.setAttribute("aria-labelledby", "phase14r-picker-title");
  panel.innerHTML = `<header><div><span>Visual Item Picker</span><h2 id="phase14r-picker-title">Choose Dress / Item</h2></div><button type="button" class="ghost phase14r-picker-close">Close</button></header><div class="phase14r-picker-search"><input type="search" placeholder="Search item code, name or category" autocomplete="off"></div><div class="phase14r-picker-grid"></div>`;
  backdrop.appendChild(panel);
  backdrop.addEventListener("click", event => { if (event.target === backdrop) rCloseItemPicker(); });
  panel.querySelector(".phase14r-picker-close")?.addEventListener("click", rCloseItemPicker);
  panel.querySelector("input")?.addEventListener("input", rRenderItemPicker);
  document.body.appendChild(backdrop);
}

function rPickerVisual(item) {
  const url = rSafeImageUrl(item?.image_url);
  if (url) {
    const img = document.createElement("img"); img.src = url; img.alt = ""; img.loading = "lazy"; return img;
  }
  const placeholder = document.createElement("span"); placeholder.className = "phase14r-picker-placeholder"; placeholder.textContent = String(item?.item_code || "?").slice(0, 2).toUpperCase(); return placeholder;
}

function rRenderItemPicker() {
  const backdrop = document.querySelector(".phase14r-item-picker-backdrop");
  const grid = backdrop?.querySelector(".phase14r-picker-grid");
  if (!grid || !rPickerSelect) return;
  const query = String(backdrop.querySelector("input")?.value || "").trim().toLowerCase();
  const selectedElsewhere = new Set([...document.querySelectorAll(".booking-line select")].filter(select => select !== rPickerSelect).map(select => select.value).filter(Boolean));
  const items = rItemSource(rPickerSelect).filter(item => !query || `${item.item_code} ${item.item_name} ${item.category_name}`.toLowerCase().includes(query));
  grid.replaceChildren();
  if (!items.length) {
    const empty = document.createElement("div"); empty.className = "phase14r-picker-empty"; empty.textContent = "No matching items."; grid.appendChild(empty); return;
  }
  for (const item of items) {
    const button = document.createElement("button"); button.type = "button"; button.className = "phase14r-picker-card"; button.disabled = selectedElsewhere.has(String(item.id));
    const availability = rAvailability.get(String(item.id));
    const stock = availability ? `Available ${availability.availableQuantity}` : item.total_quantity != null ? `Total ${item.total_quantity}` : "Select to check availability";
    const copy = document.createElement("span"); copy.className = "phase14r-picker-copy";
    const name = document.createElement("strong"); name.textContent = item.item_name || "Item";
    const meta = document.createElement("small"); meta.textContent = `${item.item_code || ""}${item.category_name ? ` · ${item.category_name}` : ""}`;
    const qty = document.createElement("em"); qty.textContent = button.disabled ? "Already selected" : stock;
    copy.append(name, meta, qty); button.append(rPickerVisual(item), copy);
    button.addEventListener("click", () => rChooseVisualItem(String(item.id))); grid.appendChild(button);
  }
}

function rOpenItemPicker(select) {
  rEnsureItemPicker(); rPickerSelect = select;
  const backdrop = document.querySelector(".phase14r-item-picker-backdrop"); backdrop?.classList.add("open"); document.body.classList.add("phase14r-picker-open");
  const search = backdrop?.querySelector("input"); if (search) search.value = ""; rRenderItemPicker(); window.setTimeout(() => search?.focus(), 0);
}

function rCloseItemPicker() {
  document.querySelector(".phase14r-item-picker-backdrop")?.classList.remove("open"); document.body.classList.remove("phase14r-picker-open");
  const returnTo = rPickerSelect; rPickerSelect = null; window.setTimeout(() => returnTo?.focus(), 0);
}

function rSetSelectValue(select, value) {
  const setter = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, "value")?.set; setter?.call(select, value);
  select.dispatchEvent(new Event("input", { bubbles: true })); select.dispatchEvent(new Event("change", { bubbles: true }));
}

function rChooseVisualItem(id) {
  if (!rPickerSelect) return;
  rSetSelectValue(rPickerSelect, id); rMarkDirty("booking"); rClearInlineError(rPickerSelect); rCloseItemPicker(); rScheduleScan();
}

function rSelectedVisual(item, fallbackText) {
  const url = rSafeImageUrl(item?.image_url);
  if (url) { const img = document.createElement("img"); img.src = url; img.alt = ""; img.loading = "lazy"; return img; }
  const placeholder = document.createElement("span"); placeholder.className = "phase14r-selected-placeholder"; placeholder.textContent = String(item?.item_code || fallbackText || "?").slice(0, 2).toUpperCase(); return placeholder;
}

function rEnsureVisualItemControls() {
  document.querySelectorAll(".booking-line").forEach(line => {
    const label = line.querySelector("label"); const select = label?.querySelector("select"); if (!label || !select) return;
    let button = label.querySelector(":scope > .phase14r-visual-picker-button");
    if (!button) { button = document.createElement("button"); button.type = "button"; button.className = "phase14r-visual-picker-button"; button.textContent = "Browse Items"; button.addEventListener("click", () => rOpenItemPicker(select)); label.appendChild(button); }
    let preview = line.querySelector(":scope > .phase14r-selected-item");
    if (!preview) { preview = document.createElement("div"); preview.className = "phase14r-selected-item"; line.appendChild(preview); }
    preview.replaceChildren();
    const id = select.value;
    if (!id) {
      preview.classList.add("empty"); const mark = document.createElement("span"); mark.className = "phase14r-selected-placeholder"; mark.textContent = "+";
      const copy = document.createElement("div"); const strong = document.createElement("strong"); strong.textContent = "No item selected"; const small = document.createElement("small"); small.textContent = "Use Browse Items for a visual selection."; copy.append(strong, small); preview.append(mark, copy); return;
    }
    preview.classList.remove("empty");
    const item = rBookingItems.get(String(id)); const optionText = select.selectedOptions?.[0]?.textContent || "Selected item"; const availability = line.querySelector(".availability-pill");
    const copy = document.createElement("div"); const strong = document.createElement("strong"); strong.textContent = item?.item_name || optionText;
    const small = document.createElement("small"); small.textContent = item ? `${item.item_code} · ${item.category_name}` : optionText;
    const stock = document.createElement("em"); stock.textContent = rText(availability) || "Availability will appear here"; copy.append(strong, small, stock); preview.append(rSelectedVisual(item, optionText), copy);
  });
}

/* Reports use the same mobile filter-sheet language as operational lists. */
function rReportFilterCount(filters) {
  let count = 0;
  filters.querySelectorAll("input,select").forEach(control => {
    if (control instanceof HTMLSelectElement) count += control.value ? 1 : 0;
    else if (control instanceof HTMLInputElement) count += control.value.trim() ? 1 : 0;
  });
  return count;
}

function rCloseReportFilters() {
  document.querySelectorAll(".report-shell.phase14r-report-filters-open").forEach(shell => shell.classList.remove("phase14r-report-filters-open"));
  document.body.classList.remove("phase14r-report-filter-open");
  if (rReportReturnFocus?.isConnected) window.setTimeout(() => rReportReturnFocus.focus(), 0); rReportReturnFocus = null;
}

function rEnsureReportFilters() {
  rPortalButton("phase14r-report-filter-backdrop", "Close report filters", rCloseReportFilters);
  document.querySelectorAll(".report-shell").forEach(shell => {
    const filters = shell.querySelector(":scope > .report-filters"); const tabs = shell.querySelector(":scope > .report-tabs"); if (!filters || !tabs) return;
    let bar = shell.querySelector(":scope > .phase14r-report-filter-bar");
    if (!bar) {
      bar = document.createElement("div"); bar.className = "phase14r-report-filter-bar";
      const button = document.createElement("button"); button.type = "button"; button.className = "phase14r-report-filter-toggle";
      button.addEventListener("click", () => {
        const willOpen = !shell.classList.contains("phase14r-report-filters-open"); rCloseReportFilters();
        if (willOpen) { rReportReturnFocus = button; shell.classList.add("phase14r-report-filters-open"); document.body.classList.add("phase14r-report-filter-open"); window.setTimeout(() => filters.querySelector("input,select")?.focus(), 40); }
      });
      bar.appendChild(button); tabs.after(bar);
    }
    const active = rReportFilterCount(filters); const button = bar.querySelector(".phase14r-report-filter-toggle"); if (button) button.textContent = active ? `Filters (${active})` : "Filters";
  });
}

/* Existing More menu becomes a labelled mobile action sheet with a backdrop. */
function rCloseActionSheet() {
  document.querySelectorAll(".phase14p-overflow.open").forEach(wrapper => { wrapper.classList.remove("open"); wrapper.querySelector(".phase14p-more-toggle")?.setAttribute("aria-expanded", "false"); });
  document.body.classList.remove("phase14r-action-sheet-open");
}

function rEnsureActionSheet() {
  rPortalButton("phase14r-action-backdrop", "Close actions", rCloseActionSheet);
  const open = document.querySelector(".phase14p-overflow.open");
  if (!open || !window.matchMedia("(max-width:760px)").matches) { document.body.classList.remove("phase14r-action-sheet-open"); return; }
  const menu = open.querySelector(".phase14p-more-menu"); if (!menu) return;
  let title = menu.querySelector(":scope > .phase14r-action-title");
  if (!title) { title = document.createElement("div"); title.className = "phase14r-action-title"; const label = document.createElement("span"); label.textContent = "Actions"; const record = document.createElement("strong"); title.append(label, record); menu.prepend(title); }
  const card = open.closest(".item-card,.customer-card,.booking-card,.user-card"); title.querySelector("strong").textContent = rText(card?.querySelector("strong")) || "Selected record"; document.body.classList.add("phase14r-action-sheet-open");
}

/* Contextual next action in booking detail reduces back-and-forth navigation. */
function rFindNav(label) { return [...document.querySelectorAll("button,a")].find(node => !node.closest(".history-panel") && rText(node) === label) || null; }
function rCloseDetail(panel) { panel.querySelector(".history-title button")?.click(); }

function rEnsureContextActions() {
  document.querySelectorAll(".history-panel.booking-detail").forEach(panel => {
    const bar = panel.querySelector(":scope > .phase14q-detail-bar"); if (!bar) return;
    let actions = bar.querySelector(":scope > .phase14r-context-actions"); if (!actions) { actions = document.createElement("div"); actions.className = "phase14r-context-actions"; bar.prepend(actions); }
    const statusNode = [...panel.querySelectorAll(".detail-meta span")].find(node => /^Status/i.test(rText(node)));
    const status = rText(statusNode?.querySelector("b") || statusNode).replace(/^Status\s*/i, "").trim().toUpperCase();
    const bookingNo = rText(panel.querySelector(".history-title h2"));
    const card = [...document.querySelectorAll(".booking-card")].find(node => rText(node.querySelector(".booking-card-head strong")) === bookingNo);
    const signature = `${status}:${bookingNo}:${Boolean(card)}`; if (actions.dataset.signature === signature) return; actions.dataset.signature = signature; actions.replaceChildren();
    if (status === "BOOKED" && card) {
      const editSource = [...card.querySelectorAll("button")].find(button => rButtonLabel(button) === "Edit");
      if (editSource) { const edit = document.createElement("button"); edit.type = "button"; edit.className = "ghost"; edit.textContent = "Edit Booking"; edit.addEventListener("click", () => { rCloseDetail(panel); window.setTimeout(() => editSource.click(), 70); }); actions.appendChild(edit); }
    }
    const destination = status === "BOOKED" || status === "PARTIALLY_GIVEN" ? "Pickup" : (status === "GIVEN" || status === "PARTIALLY_RETURNED" ? "Returns" : "");
    if (destination) {
      const go = document.createElement("button"); go.type = "button"; go.className = "primary"; go.textContent = destination === "Pickup" ? "Go to Pickup" : "Go to Returns";
      go.addEventListener("click", () => { const target = rFindNav(destination); rCloseDetail(panel); window.setTimeout(() => target?.click(), 70); }); actions.appendChild(go);
    }
  });
}

function rScan() {
  rScanQueued = false;
  rEnsureDiscardDialog(); rEnsureBookingStepper(); rEnsureVisualItemControls(); rEnsureReportFilters(); rEnsureActionSheet(); rEnsureContextActions(); rMapServerError(); rUpdateBookingReview();
  if (!document.body.classList.contains("phase14p-item-drawer-open")) rClearDirty("item");
  if (!document.body.classList.contains("phase14q-customer-drawer-open")) rClearDirty("customer");
  if (!document.body.classList.contains("phase14q-booking-drawer-open")) { rClearDirty("booking"); const form = rBookingForm(); if (form) form.dataset.phase14rStep = "1"; }
}

function rScheduleScan() { if (rScanQueued) return; rScanQueued = true; requestAnimationFrame(rScan); }

function rKindForTarget(target) {
  if (!(target instanceof Element)) return null;
  if (target.closest(".item-editor")) return "item";
  if (target.closest(".customer-layout")) return "customer";
  if (target.closest(".booking-layout")) return "booking";
  return null;
}

/* Dirty tracking: Item image enhancer emits programmatic input after trusted image actions, so Item tracks both. */
document.addEventListener("input", event => {
  const kind = rKindForTarget(event.target); if (kind && (kind === "item" || event.isTrusted)) rMarkDirty(kind);
  if (event.target instanceof HTMLElement) rClearInlineError(event.target); rScheduleScan();
}, true);

document.addEventListener("change", event => {
  const kind = rKindForTarget(event.target); if (kind && (kind === "item" || event.isTrusted)) rMarkDirty(kind);
  if (event.target instanceof HTMLElement) rClearInlineError(event.target); rScheduleScan();
}, true);

document.addEventListener("invalid", event => {
  const control = event.target;
  if (!(control instanceof HTMLInputElement || control instanceof HTMLSelectElement || control instanceof HTMLTextAreaElement)) return;
  if (!control.closest(".item-editor,.customer-layout,.booking-layout")) return;
  event.preventDefault(); rSetInlineError(control, rValidityMessage(control)); rFocusError(control);
}, true);

/* Capture close attempts before Phase 14P/14Q target/bubble handlers. */
document.addEventListener("click", event => {
  const target = event.target instanceof Element ? event.target : null; if (!target) return;
  const kind = rOpenKind(); const button = target.closest("button");
  const portalClose = target.closest(".phase14p-drawer-close,.phase14p-drawer-backdrop,.phase14q-form-close,.phase14q-form-backdrop");
  const internalCancel = kind === "item" && button?.closest(".item-editor") && /^Cancel$/.test(rButtonLabel(button))
    || kind === "customer" && button?.closest(".customer-layout") && /^(Cancel|\+ Add New)$/.test(rButtonLabel(button))
    || kind === "booking" && button?.closest(".booking-layout") && /^Cancel Edit$/.test(rButtonLabel(button));
  if ((portalClose || internalCancel) && kind && rConfirmClose(kind, () => document.querySelector(rClosePortalSelector(kind))?.click())) {
    event.preventDefault(); event.stopImmediatePropagation(); return;
  }
  if (kind === "booking" && button?.closest(".booking-layout") && /Add Another Item|Remove item/i.test(`${rButtonLabel(button)} ${button.getAttribute("aria-label") || ""}`) && event.isTrusted) rMarkDirty("booking");
  if (button?.closest(".report-filters") && /^(Apply|Reset)$/.test(rButtonLabel(button))) window.setTimeout(rCloseReportFilters, 0);
  window.setTimeout(rEnsureActionSheet, 0);
}, true);

document.addEventListener("keydown", event => {
  if (event.key !== "Escape") return;
  if (document.querySelector(".phase14r-discard-backdrop.open")) { event.preventDefault(); event.stopImmediatePropagation(); rHideDiscardDialog(); return; }
  if (document.querySelector(".phase14r-item-picker-backdrop.open")) { event.preventDefault(); event.stopImmediatePropagation(); rCloseItemPicker(); return; }
  if (document.body.classList.contains("phase14r-report-filter-open")) { event.preventDefault(); event.stopImmediatePropagation(); rCloseReportFilters(); return; }
  if (document.body.classList.contains("phase14r-action-sheet-open")) { event.preventDefault(); event.stopImmediatePropagation(); rCloseActionSheet(); return; }
  const kind = rOpenKind();
  if (kind && rDirty[kind] && !rBypassCloseGuard) { event.preventDefault(); event.stopImmediatePropagation(); rConfirmClose(kind, () => document.querySelector(rClosePortalSelector(kind))?.click()); }
}, true);

window.addEventListener("beforeunload", event => {
  const kind = rOpenKind(); if (kind && rDirty[kind]) { event.preventDefault(); event.returnValue = ""; }
});

window.addEventListener("resize", () => { if (window.innerWidth > 760) { rCloseReportFilters(); rCloseActionSheet(); } rScheduleScan(); });
new MutationObserver(rScheduleScan).observe(document.documentElement, { childList: true, subtree: true });
rScheduleScan();
