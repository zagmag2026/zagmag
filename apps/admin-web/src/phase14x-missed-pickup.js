/* Phase 14X Fix10 — missed/pending pickup operational queue.
   Dashboard Missed Pickups is rendered natively by React from the existing bundled response.
   This runtime owns only the dedicated Pickup-tab missed queue and its cross-page actions. */
import "./phase14x-missed-pickup.css";

const normalize = value => String(value || "").replace(/\s+/g, " ").trim();
const text = node => normalize(node?.textContent);
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

let pickupMode = false;
let pickupPage = 1;
let pickupPages = 1;
let pickupTotal = 0;
let pickupRows = [];
let pickupLoading = false;
let pickupError = "";
let pickupSerial = 0;
let pickupRenderSignature = "";
let queued = false;

function pageName() {
  return text(document.querySelector(".page-head h1"));
}
function todayIndia() {
  try {
    return new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata", year: "numeric", month: "2-digit", day: "2-digit"
    }).format(new Date());
  } catch {
    return new Date().toISOString().slice(0, 10);
  }
}
function dayDiff(fromIso, toIso = todayIndia()) {
  const from = Date.parse(`${fromIso}T00:00:00Z`);
  const to = Date.parse(`${toIso}T00:00:00Z`);
  if (!Number.isFinite(from) || !Number.isFinite(to)) return 1;
  return Math.max(1, Math.round((to - from) / 86400000));
}
function missedDays(row) {
  return Math.max(1, Number(row?.missed_days || 0) || dayDiff(String(row?.pickup_date || "")));
}
function wa(mobile) {
  const value = String(mobile || "").replace(/\D/g, "");
  return `https://wa.me/${value.length === 10 ? `91${value}` : value}`;
}
function tel(mobile) {
  return `tel:${String(mobile || "")}`;
}
function make(tag, className = "", content = "") {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (content !== "") node.textContent = String(content);
  return node;
}
function actionButton(label, action, bookingNo, className = "ghost") {
  const button = make("button", className, label);
  button.type = "button";
  button.dataset.missedAction = action;
  button.dataset.bookingNo = bookingNo;
  return button;
}

function buildCard(row) {
  const article = make("article", `pickup-card status-${String(row.status || "BOOKED").toLowerCase()}`);
  article.dataset.operationalStatus = String(row.status || "BOOKED");
  article.dataset.operationalTone = "danger";
  article.dataset.bookingNo = String(row.booking_no || "");

  const head = make("div", "booking-card-head");
  const identity = make("div");
  identity.append(make("strong", "", row.booking_no || "—"), make("span", "", `${row.customer_name || "—"} · ${row.customer_mobile || "—"}`));
  const badge = make("span", "chip", `MISSED PICKUP · ${missedDays(row)}D`);
  badge.dataset.operationalBadge = "1";
  head.append(identity, badge);
  article.append(head);

  article.append(make("p", "booking-summary", row.items_summary || "No items"));
  const metrics = make("div", "pickup-qty");
  const booked = make("span", "", "Booked"); booked.append(make("b", "", Number(row.booked_qty || 0)));
  const given = make("span", "", "Given"); given.append(make("b", "", Number(row.given_qty || 0)));
  const remaining = make("span", "", "Remaining"); remaining.append(make("b", "", Number(row.remaining_qty || 0)));
  metrics.append(booked, given, remaining);
  article.append(metrics);
  article.append(make("small", "", `Pickup ${row.pickup_date || "—"} · Missed by ${missedDays(row)} day${missedDays(row) === 1 ? "" : "s"}`));

  const actions = make("div", "quick-actions");
  const whatsapp = make("a", "wa-button", "WhatsApp"); whatsapp.href = wa(row.customer_mobile); whatsapp.target = "_blank"; whatsapp.rel = "noreferrer";
  const call = make("a", "call-button", "Call"); call.href = tel(row.customer_mobile);
  actions.append(whatsapp, call, actionButton("Give Now", "pickup", row.booking_no, "primary"));
  if (String(row.status || "") === "BOOKED") {
    actions.append(actionButton("Reschedule", "reschedule", row.booking_no));
    actions.append(actionButton("Cancel", "cancel", row.booking_no, "danger-link"));
  }
  article.append(actions);
  return article;
}

function currentPickupSearch() {
  const input = document.querySelector(".pickup-search input");
  return input instanceof HTMLInputElement ? input.value.trim() : "";
}

async function loadMissed(page = pickupPage) {
  const serial = ++pickupSerial;
  pickupLoading = true;
  pickupError = "";
  pickupPage = Math.max(1, Number(page) || 1);
  pickupRenderSignature = "";
  renderMissed();
  const q = new URLSearchParams({ page: String(pickupPage), pageSize: "20", view: "missed" });
  const search = currentPickupSearch();
  if (search) q.set("search", search);
  try {
    const response = await window.fetch(`/api/admin/pickups?${q}`, { credentials: "include" });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || "Unable to load missed pickups.");
    if (serial !== pickupSerial) return;
    pickupRows = Array.isArray(data.pickups) ? data.pickups : [];
    pickupTotal = Math.max(0, Number(data.pagination?.total || 0) || 0);
    pickupPages = Math.max(1, Number(data.pagination?.totalPages || 1) || 1);
    pickupPage = Math.min(pickupPage, pickupPages);
  } catch (error) {
    if (serial !== pickupSerial) return;
    pickupRows = [];
    pickupTotal = 0;
    pickupPages = 1;
    pickupError = error instanceof Error ? error.message : "Unable to load missed pickups.";
  } finally {
    if (serial === pickupSerial) {
      pickupLoading = false;
      pickupRenderSignature = "";
      renderMissed();
    }
  }
}

function renderMissed() {
  if (!pickupMode || pageName() !== "Pickup") return;
  const listCard = document.querySelector(".list-card");
  const toolbar = listCard?.querySelector(".pickup-toolbar");
  if (!(listCard instanceof HTMLElement) || !(toolbar instanceof HTMLElement)) return;
  listCard.dataset.phase14xMissedMode = "1";
  let host = listCard.querySelector(":scope > .phase14x-missed-list");
  if (!(host instanceof HTMLElement)) {
    host = make("div", "phase14x-missed-list");
    toolbar.insertAdjacentElement("afterend", host);
  }
  const signature = JSON.stringify({ pickupLoading, pickupError, pickupPage, pickupPages, pickupTotal, rows: pickupRows.map(row => [row.id, row.status, row.remaining_qty, row.missed_days]) });
  if (pickupRenderSignature === signature && host.childNodes.length) return;
  pickupRenderSignature = signature;
  host.replaceChildren();

  if (pickupLoading) {
    host.append(make("div", "phase14x-missed-state", "Loading missed pickups…"));
    return;
  }
  if (pickupError) {
    host.append(make("div", "phase14x-missed-state error", pickupError));
    return;
  }
  if (!pickupRows.length) {
    host.append(make("div", "phase14x-missed-state", currentPickupSearch() ? "No matching missed pickups found." : "No missed pickups. All past pickups are cleared."));
    return;
  }

  const grid = make("div", "phase14x-missed-grid");
  pickupRows.forEach(row => grid.append(buildCard(row)));
  host.append(grid);
  const pager = make("div", "phase14x-missed-pager");
  const previous = actionButton("Previous", "missed-prev", "", "ghost"); previous.disabled = pickupPage <= 1;
  const next = actionButton("Next", "missed-next", "", "ghost"); next.disabled = pickupPage >= pickupPages;
  pager.append(previous, make("span", "", `Page ${pickupPage} / ${pickupPages} · ${pickupTotal} pending`), next);
  host.append(pager);
}

function clearMissedMode() {
  document.querySelectorAll('[data-phase14x-missed-mode="1"]').forEach(node => node.removeAttribute("data-phase14x-missed-mode"));
  document.querySelectorAll(".phase14x-missed-list").forEach(node => node.remove());
  document.querySelectorAll(".phase14x-missed-tab").forEach(node => node.classList.remove("active"));
}

function syncPickupTab() {
  if (pageName() !== "Pickup") {
    if (pickupMode) { pickupMode = false; clearMissedMode(); }
    return;
  }
  const tabs = document.querySelector(".pickup-tabs");
  if (!(tabs instanceof HTMLElement)) return;
  let button = tabs.querySelector(".phase14x-missed-tab");
  if (!(button instanceof HTMLButtonElement)) {
    button = make("button", "phase14x-missed-tab", "Missed Pickup");
    button.type = "button";
    const today = tabs.querySelector("button");
    if (today?.nextSibling) tabs.insertBefore(button, today.nextSibling); else tabs.append(button);
  }
  button.classList.toggle("active", pickupMode);
  if (pickupMode) renderMissed();
}

function setReactInput(input, value) {
  if (!(input instanceof HTMLInputElement)) return;
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set;
  if (setter) setter.call(input, value); else input.value = value;
  input.dispatchEvent(new Event("input", { bubbles: true }));
  input.dispatchEvent(new Event("change", { bubbles: true }));
}
async function waitFor(find, attempts = 40, delay = 100) {
  for (let i = 0; i < attempts; i += 1) {
    const value = find();
    if (value) return value;
    await sleep(delay);
  }
  return null;
}
function navButton(label) {
  return [...document.querySelectorAll(".tabs button")].find(button => text(button) === label) || null;
}
function buttonByText(root, label) {
  return [...(root?.querySelectorAll("button") || [])].find(button => text(button) === label) || null;
}

async function openMissedQueue() {
  const pickupNav = navButton("Pickup");
  pickupNav?.click();
  const tab = await waitFor(() => document.querySelector(".phase14x-missed-tab"));
  if (tab instanceof HTMLButtonElement) tab.click();
}

async function bridgePickup(bookingNo) {
  pickupMode = false;
  clearMissedMode();
  if (pageName() !== "Pickup") navButton("Pickup")?.click();
  const input = await waitFor(() => document.querySelector(".pickup-search input"));
  if (!(input instanceof HTMLInputElement)) return;
  setReactInput(input, bookingNo);
  await sleep(40);
  const tabs = document.querySelector(".pickup-tabs");
  const all = [...(tabs?.querySelectorAll("button") || [])].find(button => text(button) === "All");
  if (all instanceof HTMLButtonElement) all.click();
  let card = await waitFor(() => [...document.querySelectorAll(".pickup-card")].find(node => text(node).includes(bookingNo)), 30, 100);
  if (!card) {
    const search = buttonByText(document.querySelector(".pickup-search"), "Search");
    search?.click();
    card = await waitFor(() => [...document.querySelectorAll(".pickup-card")].find(node => text(node).includes(bookingNo)), 20, 100);
  }
  const open = card ? [...card.querySelectorAll("button")].find(button => /^(Open Pickup|View)$/.test(text(button))) : null;
  open?.click();
}

async function bridgeBooking(bookingNo, action) {
  pickupMode = false;
  clearMissedMode();
  navButton("Bookings")?.click();
  const input = await waitFor(() => document.querySelector(".booking-filters input"));
  if (!(input instanceof HTMLInputElement)) return;
  setReactInput(input, bookingNo);
  await sleep(40);
  buttonByText(document.querySelector(".booking-filters"), "Search")?.click();
  const card = await waitFor(() => [...document.querySelectorAll(".booking-card")].find(node => text(node).includes(bookingNo)), 30, 100);
  if (!card) return;
  const actionButtonNode = [...card.querySelectorAll("button")].find(button => text(button) === action);
  actionButtonNode?.click();
}

function handleAction(target) {
  const node = target instanceof Element ? target.closest("[data-missed-action]") : null;
  if (!(node instanceof HTMLElement)) return false;
  const action = node.dataset.missedAction || "";
  const bookingNo = node.dataset.bookingNo || "";
  if (action === "open-missed") { openMissedQueue(); return true; }
  if (action === "missed-prev") { if (pickupPage > 1) loadMissed(pickupPage - 1); return true; }
  if (action === "missed-next") { if (pickupPage < pickupPages) loadMissed(pickupPage + 1); return true; }
  if (action === "pickup" && bookingNo) { bridgePickup(bookingNo); return true; }
  if (action === "reschedule" && bookingNo) { bridgeBooking(bookingNo, "Edit"); return true; }
  if (action === "cancel" && bookingNo) { bridgeBooking(bookingNo, "Cancel"); return true; }
  return false;
}

document.addEventListener("click", event => {
  if (handleAction(event.target)) {
    event.preventDefault();
    event.stopImmediatePropagation();
    return;
  }
  const button = event.target instanceof Element ? event.target.closest("button") : null;
  if (!(button instanceof HTMLButtonElement)) return;
  if (button.classList.contains("phase14x-missed-tab")) {
    event.preventDefault();
    event.stopImmediatePropagation();
    pickupMode = true;
    pickupPage = 1;
    pickupRenderSignature = "";
    syncPickupTab();
    loadMissed(1);
    return;
  }
  if (pickupMode && button.closest(".pickup-tabs")) {
    pickupMode = false;
    pickupRenderSignature = "";
    clearMissedMode();
    return;
  }
  if (pickupMode && button.closest(".pickup-search") && text(button) === "Search") {
    event.preventDefault();
    event.stopImmediatePropagation();
    pickupPage = 1;
    loadMissed(1);
  }
}, true);

document.addEventListener("keydown", event => {
  if (event.key === "Enter" && pickupMode && event.target instanceof HTMLInputElement && event.target.closest(".pickup-search")) {
    event.preventDefault();
    event.stopImmediatePropagation();
    pickupPage = 1;
    loadMissed(1);
    return;
  }
  if ((event.key === "Enter" || event.key === " ") && event.target instanceof HTMLElement && event.target.dataset.missedAction === "open-missed") {
    event.preventDefault();
    openMissedQueue();
  }
}, true);

function scan() {
  queued = false;
  syncPickupTab();
}
function schedule() {
  if (queued) return;
  queued = true;
  queueMicrotask(scan);
}
new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true, characterData: true });
schedule();
