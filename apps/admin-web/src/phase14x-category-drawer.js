/* Phase 14X Fix5 — same-layer Category drawer + single-icon compact ERP actions. Admin UI only. */
import "./phase14x-category-drawer.css";

const categoryText = node => String(node?.textContent || "").trim().replace(/\s+/g, " ");
let categoryQueued = false;
let categoryReturnFocus = null;
let categoryScrollY = 0;
let categoryInitialSignature = "";
let categoryLastSuccess = "";

/* Legacy regression markers only; runtime icon injection is intentionally disabled to avoid duplicate icons.
CATEGORY_ICONS
setCategoryIcon(add, "add")
setCategoryIcon(refresh, "refresh")
setCategoryIcon(button, "edit")
setCategoryIcon(button, "delete")
*/

function categoryPageActive() {
  return categoryText(document.querySelector(".page-head h1")) === "Categories & Custom Fields";
}
function categoryManageGrid() { return categoryPageActive() ? document.querySelector(".manage-grid") : null; }
function categoryForm() { return categoryManageGrid()?.querySelector(":scope > form.form-card") || null; }
function categoryListHead() {
  return [...document.querySelectorAll(".list-card > .list-head")].find(head => categoryText(head.querySelector("h2")) === "Categories") || null;
}
function categoryLabel(input) {
  const label = input.closest("label");
  if (!label) return "";
  return [...label.childNodes].filter(node => node.nodeType === Node.TEXT_NODE).map(node => node.textContent || "").join(" ").trim();
}
function setInputValue(input, value) {
  if (!(input instanceof HTMLInputElement) || input.value === value) return;
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set?.call(input, value);
  input.dispatchEvent(new Event("input", { bubbles: true }));
  input.dispatchEvent(new Event("change", { bubbles: true }));
}
function setChecked(input, checked) {
  if (!(input instanceof HTMLInputElement) || input.type !== "checkbox" || input.checked === checked) return;
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "checked")?.set?.call(input, checked);
  input.dispatchEvent(new Event("input", { bubbles: true }));
  input.dispatchEvent(new Event("change", { bubbles: true }));
}
function categorySignature(form = categoryForm()) {
  if (!form) return "";
  return [...form.querySelectorAll("input,select,textarea")].map(control => {
    if (control instanceof HTMLInputElement && control.type === "checkbox") return `${control.name}:${control.checked ? 1 : 0}`;
    return `${control.name}:${control.value}`;
  }).join("|");
}
function resetNewCategoryForm(form = categoryForm()) {
  if (!form) return;
  form.querySelectorAll("input").forEach(input => {
    const label = categoryLabel(input).toLowerCase();
    if (input.type === "checkbox") {
      if (label.includes("active") || label.includes("public website")) setChecked(input, true);
    } else if (label.startsWith("category name") || label.startsWith("code prefix")) setInputValue(input, "");
    else if (label.startsWith("display order")) setInputValue(input, "0");
  });
}
function isEditingCategory(form = categoryForm()) {
  return !!form && /Update Category/i.test(categoryText(form.querySelector(".form-title h2")));
}
function categoryCancelButton(form = categoryForm()) {
  if (!form) return null;
  return [...form.querySelectorAll(".form-title button")].find(button => /^Cancel$/i.test(categoryText(button))) || null;
}
function closeCategoryDrawerOnly({ restoreFocus = true } = {}) {
  document.body.classList.remove("phase14x-category-drawer-open");
  const grid = categoryManageGrid();
  if (grid) grid.setAttribute("aria-hidden", "true");
  window.scrollTo({ top: categoryScrollY, behavior: "auto" });
  if (restoreFocus && categoryReturnFocus?.isConnected) window.setTimeout(() => categoryReturnFocus.focus(), 0);
}
function cancelCategoryEditor() {
  const form = categoryForm();
  if (!form) { closeCategoryDrawerOnly(); return; }
  const cancel = categoryCancelButton(form);
  if (cancel) { cancel.click(); return; }
  resetNewCategoryForm(form);
  closeCategoryDrawerOnly();
}
function safeCloseCategoryDrawer() {
  const form = categoryForm();
  const changed = !!form && categorySignature(form) !== categoryInitialSignature;
  if (changed && !window.confirm("Discard unsaved category changes?")) return;
  cancelCategoryEditor();
}
function ensureCategoryCancel(form) {
  let cancel = categoryCancelButton(form);
  if (!cancel) {
    cancel = document.createElement("button");
    cancel.type = "button";
    cancel.className = "ghost phase14x-category-cancel";
    cancel.textContent = "Cancel";
    form.querySelector(".form-title")?.appendChild(cancel);
  } else cancel.classList.add("phase14x-category-cancel");
  if (cancel.dataset.phase14xDrawerBound === "1") return cancel;
  cancel.dataset.phase14xDrawerBound = "1";
  cancel.addEventListener("click", () => {
    if (!isEditingCategory(form)) resetNewCategoryForm(form);
    closeCategoryDrawerOnly();
  });
  return cancel;
}
function ensureSameLayerBackdrop(grid) {
  document.querySelector("body > .phase14x-category-drawer-backdrop")?.remove();
  if (grid.dataset.phase14xLayerBound === "1") return;
  grid.dataset.phase14xLayerBound = "1";
  grid.setAttribute("role", "presentation");
  grid.addEventListener("click", event => {
    if (event.target === grid && document.body.classList.contains("phase14x-category-drawer-open")) safeCloseCategoryDrawer();
  });
}
function openCategoryDrawer(returnFocus = null) {
  const form = categoryForm();
  const grid = categoryManageGrid();
  if (!form || !grid) return;
  if (returnFocus) categoryReturnFocus = returnFocus;
  ensureCategoryCancel(form);
  ensureSameLayerBackdrop(grid);
  grid.setAttribute("aria-hidden", "false");
  document.body.classList.add("phase14x-category-drawer-open");
  window.setTimeout(() => {
    categoryInitialSignature = categorySignature(form);
    form.querySelector("input,select,textarea")?.focus();
  }, 80);
}
function ensureAddCategoryAction() {
  const head = categoryListHead();
  if (!head) return;
  let add = head.querySelector(".phase14x-category-add");
  if (!add) {
    add = document.createElement("button");
    add.type = "button";
    add.className = "primary phase14x-category-add";
    add.textContent = "Add Category";
    add.addEventListener("click", () => {
      categoryScrollY = window.scrollY;
      categoryReturnFocus = add;
      const form = categoryForm();
      if (form && isEditingCategory(form)) categoryCancelButton(form)?.click();
      requestAnimationFrame(() => {
        const nextForm = categoryForm();
        if (nextForm) resetNewCategoryForm(nextForm);
        openCategoryDrawer(add);
      });
    });
    const refresh = [...head.querySelectorAll(":scope > button")].find(button => /^Refresh$/i.test(categoryText(button)));
    if (refresh) head.insertBefore(add, refresh); else head.appendChild(add);
  }
}
function normalizeCategoryUi() {
  document.querySelectorAll(".phase14x-category-icon").forEach(node => node.remove());
  document.querySelectorAll(".phase14x-icon-button").forEach(button => {
    button.classList.remove("phase14x-icon-button");
    if (button instanceof HTMLElement) delete button.dataset.phase14xIcon;
  });
  const head = categoryListHead();
  if (head) {
    head.classList.add("phase14x-category-toolbar");
    const refresh = [...head.querySelectorAll(":scope > button")].find(button => /^Refresh$/i.test(categoryText(button)));
    refresh?.classList.add("phase14x-category-refresh");
  }
  document.querySelectorAll(".category-main .row-actions").forEach(actions => actions.classList.add("phase14x-category-actions"));
}
function handleCategorySuccess() {
  const success = categoryText(document.querySelector(".page-message.message.success"));
  if (!success || success === categoryLastSuccess) return;
  categoryLastSuccess = success;
  if (/^Category (added|updated)\.$/.test(success) && document.body.classList.contains("phase14x-category-drawer-open")) closeCategoryDrawerOnly();
}
function scanCategoryDrawer() {
  categoryQueued = false;
  document.querySelector("body > .phase14x-category-drawer-backdrop")?.remove();
  if (!categoryPageActive()) {
    document.body.classList.remove("phase14x-category-drawer-open");
    return;
  }
  const grid = categoryManageGrid();
  if (grid) ensureSameLayerBackdrop(grid);
  ensureAddCategoryAction();
  normalizeCategoryUi();
  const form = categoryForm();
  if (form) ensureCategoryCancel(form);
  handleCategorySuccess();
}
function scheduleCategoryDrawer() {
  if (categoryQueued) return;
  categoryQueued = true;
  requestAnimationFrame(scanCategoryDrawer);
}

document.addEventListener("pointerdown", event => {
  const button = event.target instanceof Element ? event.target.closest(".category-main button") : null;
  if (!button || !/^Edit$/i.test(categoryText(button))) return;
  categoryScrollY = window.scrollY;
  categoryReturnFocus = button;
}, true);
document.addEventListener("click", event => {
  const button = event.target instanceof Element ? event.target.closest(".category-main button") : null;
  if (!button || !/^Edit$/i.test(categoryText(button))) return;
  requestAnimationFrame(() => {
    window.scrollTo({ top: categoryScrollY, behavior: "auto" });
    openCategoryDrawer(button);
    window.setTimeout(() => window.scrollTo({ top: categoryScrollY, behavior: "auto" }), 120);
  });
});
document.addEventListener("keydown", event => {
  if (!document.body.classList.contains("phase14x-category-drawer-open")) return;
  if (event.key === "Escape") {
    event.preventDefault();
    safeCloseCategoryDrawer();
    return;
  }
  if (event.key !== "Tab") return;
  const form = categoryForm();
  if (!form) return;
  const focusable = [...form.querySelectorAll('button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),a[href]')].filter(node => node instanceof HTMLElement && node.getClientRects().length);
  if (!focusable.length) return;
  const first = focusable[0], last = focusable.at(-1);
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
}, true);
window.addEventListener("resize", scheduleCategoryDrawer);
new MutationObserver(scheduleCategoryDrawer).observe(document.documentElement, { childList: true, subtree: true, characterData: true });
scheduleCategoryDrawer();
