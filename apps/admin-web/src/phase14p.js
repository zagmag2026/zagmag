import "./phase14p.css";

const text = (node) => String(node?.textContent || "").trim();
const buttonLabel = (button) => text(button).replace(/\s+/g, " ");
let scanQueued = false;
let itemEditScrollY = 0;
let drawerReturnFocus = null;
let filterReturnFocus = null;

function ensurePortalButton(className, label, onClick) {
  let button = document.querySelector(`body > .${className}`);
  if (!button) {
    button = document.createElement("button");
    button.type = "button";
    button.className = className;
    button.setAttribute("aria-label", label);
    document.body.appendChild(button);
    button.addEventListener("click", onClick);
  }
  return button;
}

function itemEditor() {
  return document.querySelector(".item-editor");
}

function setItemDrawer(open, focus = true) {
  const editor = itemEditor();
  if (!editor) open = false;
  document.body.classList.toggle("phase14p-item-drawer-open", open);
  if (editor) editor.setAttribute("aria-hidden", open ? "false" : "true");
  if (open && focus) {
    window.setTimeout(() => editor?.querySelector("input,select,textarea")?.focus(), 80);
  } else if (!open && focus && drawerReturnFocus?.isConnected) {
    window.setTimeout(() => drawerReturnFocus.focus(), 0);
  }
}

function ensureDrawerControls() {
  ensurePortalButton("phase14p-drawer-backdrop", "Close item editor", () => setItemDrawer(false));
  const close = ensurePortalButton("phase14p-drawer-close", "Close item editor", () => setItemDrawer(false));
  if (close.textContent !== "×") close.textContent = "×";

  const editor = itemEditor();
  if (!editor) {
    setItemDrawer(false, false);
    return;
  }
  const filters = document.querySelector(".item-filters");
  const listCard = filters?.closest(".list-card");
  const head = listCard?.querySelector(":scope > .list-head");
  if (!head) return;
  const actions = ensureHeadActions(head);
  if (!actions.querySelector(".phase14p-add-item")) {
    const add = document.createElement("button");
    add.type = "button";
    add.className = "phase14p-add-item";
    add.textContent = "Add Item";
    add.addEventListener("click", () => {
      drawerReturnFocus = add;
      const reset = [...document.querySelectorAll(".page-head button")].find(button => buttonLabel(button).includes("Add New"));
      if (reset) reset.click();
      requestAnimationFrame(() => setItemDrawer(true));
    });
    actions.prepend(add);
  }
}

function ensureHeadActions(head) {
  let actions = head.querySelector(":scope > .phase14p-head-actions");
  if (!actions) {
    actions = document.createElement("div");
    actions.className = "phase14p-head-actions";
    head.appendChild(actions);
  }
  return actions;
}

const listHeaders = [
  { grid: ".items-grid", cls: "phase14p-items-columns", labels: ["Photo", "Item", "Stock", "Attributes", "Actions"] },
  { grid: ".customer-grid", cls: "phase14p-customers-columns", labels: ["Customer", "Activity", "Contact", "Manage"] },
  { grid: ".booking-grid", cls: "phase14p-bookings-columns", labels: ["Booking / Customer", "Dates & Qty", "Items", "Actions"] }
];

function ensureColumnHeaders() {
  for (const config of listHeaders) {
    for (const grid of document.querySelectorAll(config.grid)) {
      const existing = grid.previousElementSibling;
      if (existing?.classList.contains("phase14p-column-head") && existing.classList.contains(config.cls)) continue;
      const header = document.createElement("div");
      header.className = `phase14p-column-head ${config.cls}`;
      header.setAttribute("aria-hidden", "true");
      for (const label of config.labels) {
        const span = document.createElement("span");
        span.textContent = label;
        header.appendChild(span);
      }
      grid.before(header);
    }
  }
}

function ensureFilterBackdrop() {
  ensurePortalButton("phase14p-filter-backdrop", "Close filters", closeFilters);
}

function closeFilters() {
  document.querySelectorAll(".list-card.phase14p-filters-open").forEach(card => card.classList.remove("phase14p-filters-open"));
  document.body.classList.remove("phase14p-mobile-filter-open");
  if (filterReturnFocus?.isConnected) window.setTimeout(() => filterReturnFocus.focus(), 0);
  filterReturnFocus = null;
}

function filterCount(filters) {
  let count = 0;
  filters.querySelectorAll("input,select").forEach(control => {
    if (control instanceof HTMLInputElement) {
      if (control.type === "checkbox") count += control.checked ? 1 : 0;
      else count += control.value.trim() ? 1 : 0;
    } else if (control instanceof HTMLSelectElement && control.value) count += 1;
  });
  return count;
}

function ensureFilterToggles() {
  ensureFilterBackdrop();
  for (const selector of [".item-filters", ".customer-filters", ".booking-filters"]) {
    for (const filters of document.querySelectorAll(selector)) {
      const card = filters.closest(".list-card");
      const head = card?.querySelector(":scope > .list-head");
      if (!card || !head) continue;
      const actions = ensureHeadActions(head);
      let toggle = actions.querySelector(".phase14p-filter-toggle");
      if (!toggle) {
        toggle = document.createElement("button");
        toggle.type = "button";
        toggle.className = "phase14p-filter-toggle";
        toggle.addEventListener("click", () => {
          const willOpen = !card.classList.contains("phase14p-filters-open");
          closeFilters();
          if (willOpen) filterReturnFocus = toggle;
          card.classList.toggle("phase14p-filters-open", willOpen);
          document.body.classList.toggle("phase14p-mobile-filter-open", willOpen);
          if (willOpen) window.setTimeout(() => filters.querySelector("input,select")?.focus(), 60);
        });
        actions.appendChild(toggle);
      }
      const active = filterCount(filters);
      const next = active ? `Filters (${active})` : "Filters";
      if (toggle.textContent !== next) toggle.textContent = next;
    }
  }
}

function actionIcon(label) {
  const value = label.toLowerCase();
  if (value.includes("edit")) return "✎";
  if (value.includes("archive")) return "▣";
  if (value.includes("restore")) return "↺";
  if (value.includes("delete") || value.includes("cancel")) return "×";
  return "›";
}

function closeOverflowMenus(except = null) {
  document.querySelectorAll(".phase14p-overflow.open").forEach(wrapper => {
    if (wrapper === except) return;
    wrapper.classList.remove("open");
    wrapper.querySelector(".phase14p-more-toggle")?.setAttribute("aria-expanded", "false");
  });
}

function buildOverflow(target, sources, sourceGroup = null) {
  const labels = sources.map(buttonLabel).filter(Boolean);
  const signature = sources.map(source => `${buttonLabel(source)}:${source.disabled ? 1 : 0}`).join("|");
  let wrapper = target.querySelector(":scope > .phase14p-overflow");
  if (!labels.length) {
    wrapper?.remove();
    sourceGroup?.classList.remove("phase14p-source-group");
    return;
  }
  sources.forEach(source => source.classList.add("phase14p-overflow-source"));
  if (sourceGroup) sourceGroup.classList.add("phase14p-source-group");
  if (wrapper?.dataset.signature === signature) return;
  wrapper?.remove();

  wrapper = document.createElement("span");
  wrapper.className = "phase14p-overflow";
  wrapper.dataset.signature = signature;
  const toggle = document.createElement("button");
  toggle.type = "button";
  toggle.className = "phase14p-more-toggle";
  toggle.textContent = "⋯ More";
  toggle.setAttribute("aria-haspopup", "menu");
  toggle.setAttribute("aria-expanded", "false");
  const menu = document.createElement("span");
  menu.className = "phase14p-more-menu";
  menu.setAttribute("role", "menu");

  sources.forEach(source => {
    const label = buttonLabel(source);
    const item = document.createElement("button");
    item.type = "button";
    item.disabled = source.disabled;
    item.setAttribute("role", "menuitem");
    item.textContent = `${actionIcon(label)} ${label}`;
    if (source.classList.contains("danger-link") || /delete|cancel/i.test(label)) item.classList.add("danger");
    item.addEventListener("click", event => {
      event.stopPropagation();
      wrapper.classList.remove("open");
      toggle.setAttribute("aria-expanded", "false");
      source.click();
    });
    menu.appendChild(item);
  });
  toggle.addEventListener("click", event => {
    event.stopPropagation();
    const next = !wrapper.classList.contains("open");
    closeOverflowMenus(wrapper);
    wrapper.classList.toggle("open", next);
    toggle.setAttribute("aria-expanded", next ? "true" : "false");
  });
  wrapper.append(toggle, menu);
  target.appendChild(wrapper);
}

function enhanceActions() {
  document.querySelectorAll(".item-card").forEach(card => {
    const actions = card.querySelector(".row-actions");
    if (!actions) return;
    const sources = [...actions.querySelectorAll(":scope > button")].filter(button => /^(Archive|Delete)$/i.test(buttonLabel(button)));
    buildOverflow(actions, sources);
  });

  document.querySelectorAll(".customer-card").forEach(card => {
    const quick = card.querySelector(".quick-actions");
    const row = card.querySelector(".row-actions");
    if (!quick || !row) return;
    const sources = [...row.querySelectorAll(":scope > button")].filter(button => !button.classList.contains("phase14p-more-toggle"));
    buildOverflow(quick, sources, row);
  });

  document.querySelectorAll(".booking-card").forEach(card => {
    const quick = card.querySelector(".quick-actions");
    if (!quick) return;
    const sources = [...quick.querySelectorAll(":scope > button")].filter(button => /^(Edit|Cancel)$/i.test(buttonLabel(button)));
    buildOverflow(quick, sources);
  });
}

function scan() {
  scanQueued = false;
  ensureDrawerControls();
  ensureColumnHeaders();
  ensureFilterToggles();
  enhanceActions();
  const saved = [...document.querySelectorAll(".page-message.message.success")].some(node => /^Item (added|updated)\.$/.test(text(node)));
  if (saved && document.body.classList.contains("phase14p-item-drawer-open")) setItemDrawer(false);
}

function scheduleScan() {
  if (scanQueued) return;
  scanQueued = true;
  requestAnimationFrame(scan);
}

document.addEventListener("pointerdown", event => {
  const button = event.target instanceof Element ? event.target.closest("button") : null;
  if (button?.closest(".items-grid") && buttonLabel(button) === "Edit") itemEditScrollY = window.scrollY;
}, true);

document.addEventListener("click", event => {
  const button = event.target instanceof Element ? event.target.closest("button") : null;
  if (!button) {
    closeOverflowMenus();
    return;
  }
  if (button.closest(".items-grid") && buttonLabel(button) === "Edit") {
    drawerReturnFocus = button;
    requestAnimationFrame(() => {
      window.scrollTo({ top: itemEditScrollY, behavior: "auto" });
      setItemDrawer(true);
    });
  }
  if (button.closest(".item-editor") && buttonLabel(button) === "Cancel") {
    requestAnimationFrame(() => setItemDrawer(false));
  }
  if (!button.closest(".phase14p-overflow")) closeOverflowMenus();
});

document.addEventListener("input", scheduleScan, true);
document.addEventListener("change", scheduleScan, true);
document.addEventListener("keydown", event => {
  if (event.key !== "Escape") return;
  if (document.querySelector(".phase14p-overflow.open")) closeOverflowMenus();
  else if (document.body.classList.contains("phase14p-mobile-filter-open")) closeFilters();
  else if (document.body.classList.contains("phase14p-item-drawer-open")) setItemDrawer(false);
});
window.addEventListener("resize", () => {
  if (window.innerWidth > 760) closeFilters();
});
new MutationObserver(scheduleScan).observe(document.documentElement, { childList: true, subtree: true });
scheduleScan();
