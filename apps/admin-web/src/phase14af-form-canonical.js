/* Phase 14AF — canonical Create/Edit form behavior.
   No API calls, polling or business-rule changes. */
import "./phase14af-form-canonical.css";

const formConfigs = {
  item: {
    label: "item",
    isOpen: () => document.body.classList.contains("phase14p-item-drawer-open"),
    form: () => document.querySelector(".item-editor > form.form-card"),
    close: ".phase14p-drawer-close",
    backdrop: ".phase14p-drawer-backdrop"
  },
  customer: {
    label: "customer",
    isOpen: () => document.body.classList.contains("phase14q-customer-drawer-open"),
    form: () => document.querySelector(".customer-layout > form.form-card"),
    close: ".phase14q-form-close",
    backdrop: ".phase14q-form-backdrop"
  },
  booking: {
    label: "booking",
    isOpen: () => document.body.classList.contains("phase14q-booking-drawer-open"),
    form: () => document.querySelector(".booking-layout > form.form-card"),
    close: ".phase14q-form-close",
    backdrop: ".phase14q-form-backdrop"
  },
  category: {
    label: "category",
    isOpen: () => document.body.classList.contains("phase14x-category-drawer-open"),
    form: () => document.querySelector(".manage-grid > form.form-card"),
    close: ".phase14x-category-cancel",
    backdrop: null
  },
  user: {
    label: "user",
    isOpen: () => Boolean(document.querySelector(".history-backdrop > form.user-modal")),
    form: () => document.querySelector(".history-backdrop > form.user-modal"),
    close: ".user-modal > .form-title > button",
    backdrop: ".history-backdrop"
  }
};

let activeState = null;
let syncQueued = false;
let bypassCloseGuard = false;

function ignoredControl(control) {
  if (!(control instanceof HTMLInputElement || control instanceof HTMLSelectElement || control instanceof HTMLTextAreaElement)) return true;
  if (control instanceof HTMLInputElement && ["button", "submit", "reset", "file"].includes(control.type)) return true;
  if (control.closest(".customer-picker, .booking-items-head")) return true;
  return false;
}

function controlValue(control) {
  if (control instanceof HTMLInputElement && (control.type === "checkbox" || control.type === "radio")) return control.checked ? "1" : "0";
  if (control instanceof HTMLSelectElement && control.multiple) return [...control.selectedOptions].map(option => option.value).join("\u001f");
  return control.value;
}

function formSignature(form) {
  const parts = [];
  let index = 0;
  form.querySelectorAll("input,select,textarea").forEach(control => {
    if (ignoredControl(control)) return;
    parts.push(`${control.tagName}:${control.getAttribute("type") || ""}:${index++}:${controlValue(control)}`);
  });
  return parts.join("\u001e");
}

function captureSnapshot(kind, form) {
  const controls = [];
  form.querySelectorAll("input,select,textarea").forEach(control => {
    if (ignoredControl(control)) return;
    controls.push({
      node: control,
      value: control instanceof HTMLInputElement && (control.type === "checkbox" || control.type === "radio") ? null : control.value,
      checked: control instanceof HTMLInputElement && (control.type === "checkbox" || control.type === "radio") ? control.checked : null
    });
  });
  return {
    kind,
    form,
    signature: formSignature(form),
    controls,
    bookingLineCount: kind === "booking" ? form.querySelectorAll(".booking-line").length : 0
  };
}

function setNativeValue(control, value) {
  if (control instanceof HTMLInputElement) Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set?.call(control, value);
  else if (control instanceof HTMLTextAreaElement) Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")?.set?.call(control, value);
  else if (control instanceof HTMLSelectElement) Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, "value")?.set?.call(control, value);
}

function setNativeChecked(control, checked) {
  if (!(control instanceof HTMLInputElement)) return;
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "checked")?.set?.call(control, checked);
}

function dispatchControl(control) {
  control.dispatchEvent(new Event("input", { bubbles: true }));
  control.dispatchEvent(new Event("change", { bubbles: true }));
}

function restoreSnapshot(state) {
  if (!state?.form?.isConnected) return;

  /* A new booking starts with one line. If unsaved extra lines were added,
     remove them before restoring the captured control values. */
  if (state.kind === "booking") {
    let lines = [...state.form.querySelectorAll(".booking-line")];
    while (lines.length > state.bookingLineCount && lines.length > 1) {
      const remove = lines.at(-1)?.querySelector("button.icon-btn");
      if (!remove) break;
      remove.click();
      lines = [...state.form.querySelectorAll(".booking-line")];
    }
  }

  state.controls.forEach(entry => {
    const control = entry.node;
    if (!control?.isConnected) return;
    if (entry.checked !== null) {
      if (control.checked === entry.checked) return;
      setNativeChecked(control, entry.checked);
    } else {
      if (control.value === entry.value) return;
      setNativeValue(control, entry.value ?? "");
    }
    dispatchControl(control);
  });
}

function detectActive() {
  for (const kind of ["user", "category", "item", "customer", "booking"]) {
    const config = formConfigs[kind];
    if (!config.isOpen()) continue;
    const form = config.form();
    if (form) return { kind, config, form };
  }
  return null;
}

function syncActiveForm() {
  syncQueued = false;
  const found = detectActive();
  if (!found) {
    activeState = null;
    return;
  }

  found.form.classList.add("phase14af-canonical-form");
  found.form.dataset.phase14afKind = found.kind;
  found.form.setAttribute("role", "dialog");
  found.form.setAttribute("aria-modal", "true");

  if (!activeState || activeState.kind !== found.kind || activeState.form !== found.form) {
    activeState = { ...found, ...captureSnapshot(found.kind, found.form) };
  }
}

function scheduleSync() {
  if (syncQueued) return;
  syncQueued = true;
  requestAnimationFrame(syncActiveForm);
}

function isDirty(state = activeState) {
  return Boolean(state?.form?.isConnected && formSignature(state.form) !== state.signature);
}

function allowDiscard(state = activeState) {
  if (!state || !isDirty(state)) return true;
  const allowed = window.confirm(`Discard unsaved ${state.config.label} changes?`);
  if (allowed) restoreSnapshot(state);
  return allowed;
}

function isCloseAttempt(event, state) {
  const target = event.target instanceof Element ? event.target : null;
  if (!target || !state) return false;
  if (state.config.close && target.closest(state.config.close)) return true;
  if (state.config.backdrop) {
    const backdrop = target.closest(state.config.backdrop);
    if (backdrop && event.target === backdrop) return true;
  }
  return false;
}

document.addEventListener("click", event => {
  if (bypassCloseGuard || !activeState || !isCloseAttempt(event, activeState)) return;
  /* Category backdrop already owns a dirty-confirmation path in Phase14X.
     Keep that single owner and guard only its explicit Close/Cancel control. */
  if (activeState.kind === "category" && activeState.config.backdrop) return;
  if (allowDiscard(activeState)) return;
  event.preventDefault();
  event.stopImmediatePropagation();
}, true);

function visibleFocusable(state) {
  if (!state?.form) return [];
  const nodes = [...state.form.querySelectorAll('button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])')];
  if (["item", "customer", "booking"].includes(state.kind) && state.config.close) {
    const portalClose = document.querySelector(state.config.close);
    if (portalClose) nodes.unshift(portalClose);
  }
  return [...new Set(nodes)].filter(node => node instanceof HTMLElement && node.getClientRects().length > 0);
}

window.addEventListener("keydown", event => {
  const state = activeState;
  if (!state) return;

  if (event.key === "Escape") {
    /* Category already has its own proven dirty-close guard, so do not prompt twice. */
    if (state.kind === "category") return;
    if (!allowDiscard(state)) {
      event.preventDefault();
      event.stopImmediatePropagation();
      return;
    }
    if (state.kind === "user") {
      event.preventDefault();
      event.stopImmediatePropagation();
      const close = state.form.querySelector(":scope > .form-title > button");
      if (close) {
        bypassCloseGuard = true;
        close.click();
        bypassCloseGuard = false;
      }
    }
    return;
  }

  if (event.key !== "Tab") return;
  const focusable = visibleFocusable(state);
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable.at(-1);
  if (event.shiftKey && (document.activeElement === first || !state.form.contains(document.activeElement) && document.activeElement !== first)) {
    event.preventDefault();
    event.stopPropagation();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    event.stopPropagation();
    first.focus();
  }
}, true);

/* Body-class changes open the existing structural drawers; child-list changes
   cover the React User modal. No polling is needed. */
new MutationObserver(scheduleSync).observe(document.documentElement, {
  childList: true,
  subtree: true,
  attributes: true,
  attributeFilter: ["class"]
});
scheduleSync();
