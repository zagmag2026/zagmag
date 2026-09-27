import "./phase14q.css";

const qText = (node) => String(node?.textContent || "").trim();
const qLabel = (button) => qText(button).replace(/\s+/g, " ");
let qScanQueued = false;
let qReturnFocus = null;
let qEditScrollY = 0;
let qPendingBookingOpen = false;

const drawerConfig = {
  customer: { selector: ".customer-layout", bodyClass: "phase14q-customer-drawer-open" },
  booking: { selector: ".booking-layout", bodyClass: "phase14q-booking-drawer-open" }
};

function qPortalButton(className, label, handler) {
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

function qOpenKind() {
  if (document.body.classList.contains(drawerConfig.customer.bodyClass)) return "customer";
  if (document.body.classList.contains(drawerConfig.booking.bodyClass)) return "booking";
  return null;
}

function qCloseDrawers({ restoreFocus = true } = {}) {
  Object.values(drawerConfig).forEach(config => {
    document.body.classList.remove(config.bodyClass);
    document.querySelector(config.selector)?.setAttribute("aria-hidden", "true");
  });
  document.body.classList.remove("phase14q-form-drawer-open");
  if (restoreFocus && qReturnFocus?.isConnected) window.setTimeout(() => qReturnFocus.focus(), 0);
  qReturnFocus = null;
}

function qSetDrawer(kind, open, focus = true) {
  const config = drawerConfig[kind];
  if (!config) return;
  const drawer = document.querySelector(config.selector);
  if (!drawer) {
    qCloseDrawers({ restoreFocus: false });
    return;
  }
  if (!open) {
    qCloseDrawers({ restoreFocus: focus });
    return;
  }
  Object.entries(drawerConfig).forEach(([key, value]) => {
    document.body.classList.toggle(value.bodyClass, key === kind);
    document.querySelector(value.selector)?.setAttribute("aria-hidden", key === kind ? "false" : "true");
  });
  document.body.classList.add("phase14q-form-drawer-open");
  if (focus) window.setTimeout(() => drawer.querySelector("input,select,textarea,button")?.focus(), 80);
}

function qEnsurePortalControls() {
  qPortalButton("phase14q-form-backdrop", "Close editor", () => qCloseDrawers());
  const close = qPortalButton("phase14q-form-close", "Close editor", () => qCloseDrawers());
  if (close.textContent !== "×") close.textContent = "×";
}

function qHeadActions(head) {
  let actions = head.querySelector(":scope > .phase14p-head-actions");
  if (!actions) {
    actions = document.createElement("div");
    actions.className = "phase14p-head-actions";
    head.appendChild(actions);
  }
  return actions;
}

function qResetButton(kind) {
  const page = document.querySelector(drawerConfig[kind].selector)?.closest("main") || document;
  const labels = kind === "customer" ? ["+ Add New"] : ["+ New Booking"];
  return [...page.querySelectorAll(".page-head button")].find(button => labels.includes(qLabel(button))) || null;
}

function qAddListButton(kind, listSelector, className, label) {
  const list = document.querySelector(listSelector)?.closest(".list-card");
  const head = list?.querySelector(":scope > .list-head");
  if (!head) return;
  const actions = qHeadActions(head);
  if (actions.querySelector(`.${className}`)) return;
  const add = document.createElement("button");
  add.type = "button";
  add.className = `phase14q-add-button ${className}`;
  add.textContent = label;
  add.addEventListener("click", () => {
    qEditScrollY = window.scrollY;
    qReturnFocus = add;
    qResetButton(kind)?.click();
    requestAnimationFrame(() => {
      window.scrollTo({ top: qEditScrollY, behavior: "auto" });
      qSetDrawer(kind, true);
    });
  });
  actions.prepend(add);
}

function qEnsureAddButtons() {
  qAddListButton("customer", ".customer-filters", "phase14q-add-customer", "Add Customer");
  qAddListButton("booking", ".booking-filters", "phase14q-add-booking", "New Booking");
}

function qPhoneFromPanel(panel) {
  const existing = panel.querySelector('a[href^="tel:"]');
  if (existing) return existing.getAttribute("href")?.replace(/^tel:/, "") || "";
  const titleText = qText(panel.querySelector(".history-title"));
  const matches = titleText.match(/(?:\+?\d[\d\s-]{7,}\d)/g) || [];
  if (!matches.length) return "";
  return matches[matches.length - 1].replace(/[^\d+]/g, "");
}

function qWaHref(phone) {
  const digits = String(phone || "").replace(/\D/g, "");
  if (!digits) return "";
  return `https://wa.me/${digits.length === 10 ? `91${digits}` : digits}`;
}

function qEnsureDetailBars() {
  document.querySelectorAll(".history-panel").forEach(panel => {
    if (panel.querySelector(":scope > .phase14q-detail-bar")) return;
    const phone = qPhoneFromPanel(panel);
    const closeSource = panel.querySelector(".history-title button");
    if (!phone && !closeSource) return;
    const bar = document.createElement("div");
    bar.className = "phase14q-detail-bar";
    if (phone) {
      const wa = document.createElement("a");
      wa.className = "wa-button";
      wa.href = qWaHref(phone);
      wa.target = "_blank";
      wa.rel = "noreferrer";
      wa.textContent = "WhatsApp";
      const call = document.createElement("a");
      call.className = "call-button";
      call.href = `tel:${phone}`;
      call.textContent = "Call";
      bar.append(wa, call);
    }
    if (closeSource) {
      const close = document.createElement("button");
      close.type = "button";
      close.className = "ghost";
      close.textContent = "Close";
      close.addEventListener("click", () => closeSource.click());
      bar.appendChild(close);
    }
    panel.appendChild(bar);
  });
}

function qSavedNotice(kind) {
  const patterns = kind === "customer"
    ? [/^Customer (added|updated)\.$/]
    : [/^Booking (created|updated)\.$/, /^Booking saved\.$/];
  return [...document.querySelectorAll(".page-message.message.success")].some(node => patterns.some(pattern => pattern.test(qText(node))));
}

function qEnsureCategoryFocus() {
  document.querySelectorAll(".category-row").forEach(row => row.classList.toggle("phase14q-category-expanded", Boolean(row.querySelector(":scope > .fields-zone"))));
}

function qScan() {
  qScanQueued = false;
  qEnsurePortalControls();
  qEnsureAddButtons();
  qEnsureDetailBars();
  qEnsureCategoryFocus();

  if (!document.querySelector(".customer-layout") && qOpenKind() === "customer") qCloseDrawers({ restoreFocus: false });
  if (!document.querySelector(".booking-layout") && qOpenKind() === "booking") qCloseDrawers({ restoreFocus: false });

  if (qPendingBookingOpen && document.querySelector(".booking-layout")) {
    qPendingBookingOpen = false;
    qEditScrollY = window.scrollY;
    requestAnimationFrame(() => qSetDrawer("booking", true));
  }

  if (qOpenKind() === "customer" && qSavedNotice("customer")) qCloseDrawers();
  if (qOpenKind() === "booking" && qSavedNotice("booking")) qCloseDrawers();
}

function qScheduleScan() {
  if (qScanQueued) return;
  qScanQueued = true;
  requestAnimationFrame(qScan);
}

// Capture before React handlers that scroll legacy forms to the top.
document.addEventListener("click", event => {
  const button = event.target instanceof Element ? event.target.closest("button") : null;
  if (!button) return;
  const label = qLabel(button);

  if (button.closest(".customer-card") && label === "Edit") {
    qEditScrollY = window.scrollY;
    qReturnFocus = button.closest(".customer-card")?.querySelector(".phase14p-more-toggle") || button;
    requestAnimationFrame(() => {
      window.scrollTo({ top: qEditScrollY, behavior: "auto" });
      qSetDrawer("customer", true);
    });
  }
  if (button.closest(".booking-card") && label === "Edit") {
    qEditScrollY = window.scrollY;
    qReturnFocus = button.closest(".booking-card")?.querySelector(".phase14p-more-toggle") || button;
    requestAnimationFrame(() => {
      window.scrollTo({ top: qEditScrollY, behavior: "auto" });
      qSetDrawer("booking", true);
    });
  }

  if (!document.querySelector(".booking-layout") && /New Booking/.test(label)) qPendingBookingOpen = true;
}, true);

document.addEventListener("click", event => {
  const button = event.target instanceof Element ? event.target.closest("button") : null;
  if (!button) return;
  const kind = qOpenKind();
  if (kind === "customer" && button.closest(".customer-layout") && /^(Cancel|\+ Add New)$/.test(qLabel(button))) requestAnimationFrame(() => qSetDrawer("customer", false));
  if (kind === "booking" && button.closest(".booking-layout") && /^Cancel Edit$/.test(qLabel(button))) requestAnimationFrame(() => qSetDrawer("booking", false));
});

document.addEventListener("keydown", event => {
  const kind = qOpenKind();
  if (!kind) return;
  const drawer = document.querySelector(drawerConfig[kind].selector);
  if (event.key === "Escape") {
    event.preventDefault();
    qCloseDrawers();
    return;
  }
  if (event.key !== "Tab" || !drawer) return;
  const focusable = [...drawer.querySelectorAll('button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])')].filter(node => node.getClientRects().length > 0);
  if (!focusable.length) return;
  const first = focusable[0], last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
});

document.addEventListener("input", qScheduleScan, true);
document.addEventListener("change", qScheduleScan, true);
new MutationObserver(qScheduleScan).observe(document.documentElement, { childList: true, subtree: true });
qScheduleScan();
