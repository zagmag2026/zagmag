/* Phase 14AG — secondary canonical Create/Edit behavior.
   WhatsApp Template, Category Custom Field and Reset Password.
   No polling, fetch/API calls or business-rule changes. */
import "./phase14ag-secondary-form-canonical.css";

let queued = false;
let fieldRequestedOpen = false;
let active = null;
let bypassGuard = false;
let lastFieldSuccess = "";

const text = node => String(node?.textContent || "").trim().replace(/\s+/g, " ");

function ensurePortal(className, label, handler) {
  let node = document.querySelector(`body > .${className}`);
  if (!node) {
    node = document.createElement("button");
    node.type = "button";
    node.className = className;
    node.setAttribute("aria-label", label);
    document.body.appendChild(node);
    node.addEventListener("click", handler);
  }
  return node;
}

function ensurePortalControls() {
  ensurePortal("phase14ag-backdrop", "Close editor", () => requestClose());
  ensurePortal("phase14ag-close", "Close editor", () => requestClose());
}

function controlValue(control) {
  if (control instanceof HTMLInputElement && (control.type === "checkbox" || control.type === "radio")) return control.checked ? "1" : "0";
  if (control instanceof HTMLSelectElement && control.multiple) return [...control.selectedOptions].map(option => option.value).join("\u001f");
  return control.value;
}

function signature(form) {
  const values = [];
  let index = 0;
  form.querySelectorAll("input,select,textarea").forEach(control => {
    if (control instanceof HTMLInputElement && ["button", "submit", "reset", "file"].includes(control.type)) return;
    values.push(`${control.tagName}:${control.getAttribute("type") || ""}:${index++}:${controlValue(control)}`);
  });
  return values.join("\u001e");
}

function markForm(kind, form) {
  form.classList.add("phase14af-canonical-form", "phase14ag-secondary-form");
  form.dataset.phase14agKind = kind;
  form.setAttribute("role", "dialog");
  form.setAttribute("aria-modal", "true");
  if (kind === "template") {
    form.classList.add("phase14ag-drawer-form");
    form.querySelector(":scope > .settings-title")?.classList.add("form-title");
  } else if (kind === "field") {
    form.classList.remove("phase14ag-secondary-idle");
    form.classList.add("phase14ag-drawer-form");
  }
}

function clearKindClasses(kind) {
  document.body.classList.toggle("phase14ag-template-open", kind === "template");
  document.body.classList.toggle("phase14ag-field-open", kind === "field");
  document.body.classList.toggle("phase14ag-password-open", kind === "password");
}

function detect() {
  const password = document.querySelector(".history-backdrop > form.password-modal");
  if (password) return { kind: "password", form: password, label: "password" };

  const template = document.querySelector("form.template-editor");
  if (template) return { kind: "template", form: template, label: "WhatsApp template" };

  const field = document.querySelector("form.field-form");
  if (field && fieldRequestedOpen) return { kind: "field", form: field, label: "custom field" };
  return null;
}

function sync() {
  queued = false;
  ensurePortalControls();

  const field = document.querySelector("form.field-form");
  if (field && !fieldRequestedOpen) {
    field.classList.add("phase14ag-secondary-idle");
    field.classList.remove("phase14ag-drawer-form", "phase14ag-secondary-form", "phase14af-canonical-form");
    delete field.dataset.phase14agKind;
  }

  const found = detect();
  if (!found) {
    active = null;
    clearKindClasses("");
    return;
  }

  markForm(found.kind, found.form);
  clearKindClasses(found.kind);
  if (!active || active.kind !== found.kind || active.form !== found.form) {
    active = { ...found, initial: signature(found.form) };
    window.setTimeout(() => found.form.querySelector("input,select,textarea,button")?.focus(), 60);
  }

  const success = text(document.querySelector(".page-message.message.success"));
  if (found.kind === "field" && success && success !== lastFieldSuccess && /^Field (added|updated)\.$/.test(success)) {
    lastFieldSuccess = success;
    closeField({ force: true });
  }
}

function schedule() {
  if (queued) return;
  queued = true;
  requestAnimationFrame(sync);
}

function isDirty(state = active) {
  return Boolean(state?.form?.isConnected && signature(state.form) !== state.initial);
}

function allowClose(state = active) {
  if (!state || !isDirty(state)) return true;
  return window.confirm(`Discard unsaved ${state.label} changes?`);
}

function closeTemplate({ force = false } = {}) {
  if (!force && !allowClose()) return;
  const form = document.querySelector("form.template-editor");
  const clear = form ? [...form.querySelectorAll(":scope > .form-title button, :scope > .settings-title button")].find(button => /^Clear$/i.test(text(button))) : null;
  bypassGuard = true;
  clear?.click();
  bypassGuard = false;
  document.body.classList.remove("phase14ag-template-open");
  active = null;
  schedule();
}

function closeField({ force = false } = {}) {
  if (!force && !allowClose()) return;
  const form = document.querySelector("form.field-form");
  const cancel = form ? [...form.querySelectorAll(":scope > .form-title button")].find(button => /^Cancel$/i.test(text(button))) : null;
  bypassGuard = true;
  cancel?.click();
  bypassGuard = false;
  fieldRequestedOpen = false;
  document.body.classList.remove("phase14ag-field-open");
  active = null;
  schedule();
}

function closePassword({ force = false } = {}) {
  if (!force && !allowClose()) return;
  const form = document.querySelector(".history-backdrop > form.password-modal");
  const close = form?.querySelector(":scope > .form-title > button");
  if (!close) return;
  bypassGuard = true;
  close.click();
  bypassGuard = false;
  active = null;
  schedule();
}

function requestClose() {
  if (!active) return;
  if (active.kind === "template") closeTemplate();
  else if (active.kind === "field") closeField();
  else if (active.kind === "password") closePassword();
}

function focusable(state = active) {
  if (!state?.form) return [];
  const nodes = [...state.form.querySelectorAll('button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])')];
  if (["template", "field"].includes(state.kind)) {
    const close = document.querySelector("body > .phase14ag-close");
    if (close) nodes.unshift(close);
  }
  return [...new Set(nodes)].filter(node => node instanceof HTMLElement && node.getClientRects().length > 0);
}

/* Open the custom-field sheet only from Add Field / Edit Field. Merely
   expanding a category remains a read/browse action. */
document.addEventListener("click", event => {
  const button = event.target instanceof Element ? event.target.closest("button") : null;
  if (!button) return;
  if (button.closest(".fields-head") && /Add Field/i.test(text(button))) {
    fieldRequestedOpen = true;
    requestAnimationFrame(schedule);
    return;
  }
  if (button.closest(".field-row") && /^Edit$/i.test(text(button))) {
    fieldRequestedOpen = true;
    requestAnimationFrame(schedule);
  }
}, true);

/* Password uses its React backdrop/Close source, so guard those direct close
   attempts before React receives them. */
document.addEventListener("click", event => {
  if (bypassGuard || active?.kind !== "password") return;
  const target = event.target instanceof Element ? event.target : null;
  if (!target) return;
  const backdrop = target.closest(".history-backdrop");
  const close = target.closest(".password-modal > .form-title > button");
  const isBackdropClick = backdrop && event.target === backdrop;
  if (!close && !isBackdropClick) return;
  if (allowClose()) return;
  event.preventDefault();
  event.stopImmediatePropagation();
}, true);

window.addEventListener("keydown", event => {
  if (!active) return;
  if (event.key === "Escape") {
    event.preventDefault();
    event.stopImmediatePropagation();
    requestClose();
    return;
  }
  if (event.key !== "Tab") return;
  const nodes = focusable();
  if (!nodes.length) return;
  const first = nodes[0], last = nodes.at(-1);
  if (event.shiftKey && (document.activeElement === first || !active.form.contains(document.activeElement) && document.activeElement !== first)) {
    event.preventDefault(); last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault(); first.focus();
  }
}, true);

new MutationObserver(schedule).observe(document.documentElement, {
  childList: true,
  subtree: true,
  characterData: true
});
schedule();
