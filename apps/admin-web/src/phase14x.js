import "./phase14x.css";

/* Phase 14X — Admin list-state clarity + actionable dashboard KPIs.
   UI-only. No fetch/API/D1 calls are created here. */
const xText = node => String(node?.textContent || "").trim().replace(/\s+/g, " ");
const xLoading = value => /^(Loading|Refreshing|Please wait)/i.test(String(value || "").trim());
const xNoData = value => /^(No\b|Nothing\b)/i.test(String(value || "").trim());
let xQueued = false;

const X_KPIS = {
  "Categories": { glyph: "▦", target: "Categories", hint: "Open categories" },
  "Items": { glyph: "▣", target: "Items", hint: "Open items" },
  "Total Quantity": { glyph: "▤", target: "Items", hint: "Open inventory" },
  "Available Now": { glyph: "✓", target: "Items", hint: "View item availability" },
  "Booked Pending": { glyph: "◫", target: "Bookings", hint: "Open bookings" },
  "Given Out": { glyph: "↑", target: "Returns", hint: "Open return queue" },
  "Overdue Returns": { glyph: "!", target: "Returns", hint: "Open returns" },
};

function xModule() {
  return xText(document.querySelector(".page-head h1")) || "";
}
function xClickNav(target) {
  const button = [...document.querySelectorAll(".tabs button")].find(node => xText(node) === target);
  if (button instanceof HTMLButtonElement && !button.disabled) button.click();
}
function xEnhanceKpis() {
  document.querySelectorAll(".stats .stat").forEach(card => {
    if (!(card instanceof HTMLElement)) return;
    const label = xText(card.querySelector("span"));
    const config = X_KPIS[label];
    if (!config) return;
    card.classList.add("phase14x-kpi");
    card.dataset.phase14xGlyph = config.glyph;
    card.dataset.phase14xHint = config.hint;
    card.setAttribute("role", "button");
    card.setAttribute("tabindex", "0");
    card.setAttribute("aria-label", `${label}: ${config.hint}`);
    if (card.dataset.phase14xBound === "1") return;
    card.dataset.phase14xBound = "1";
    const open = () => xClickNav(config.target);
    card.addEventListener("click", open);
    card.addEventListener("keydown", event => {
      if (event.key !== "Enter" && event.key !== " ") return;
      event.preventDefault();
      open();
    });
  });
}

function xFilterHost(surface) {
  return surface.querySelector(".item-filters,.customer-filters,.booking-filters,.pickup-toolbar,.report-filters,.users-toolbar,.audit-filters");
}
function xHasActiveFilter(surface) {
  const host = xFilterHost(surface);
  if (!host) return false;
  for (const input of host.querySelectorAll("input")) {
    if (!(input instanceof HTMLInputElement)) continue;
    if ((input.type === "text" || input.type === "search" || input.type === "date") && input.value.trim()) return true;
    if ((input.type === "checkbox" || input.type === "radio") && input.checked) return true;
  }
  for (const select of host.querySelectorAll("select")) {
    if (select instanceof HTMLSelectElement && select.value) return true;
  }
  return false;
}
function xActiveQueueLabel(surface) {
  return xText(surface.querySelector(".pickup-tabs button.active"));
}
function xEmptyCopy(module, surface, filtered) {
  if (module === "Returns") {
    const tab = xActiveQueueLabel(surface);
    if (filtered) return ["No matching return records found.", "Change the search or filter and try again."];
    if (tab === "Today Due") return ["No returns due today.", "There are no return records due for today."];
    if (tab === "Pending Return") return ["No pending returns.", "Nothing is currently waiting to be returned."];
    if (tab === "Overdue") return ["No overdue returns.", "There are no overdue return records."];
    if (tab === "Returned") return ["No returned records.", "No completed returns were found in this view."];
    return ["No return records found.", "Records will appear here when available."];
  }
  if (module === "Pickup") {
    const tab = xActiveQueueLabel(surface);
    if (filtered) return ["No matching pickup records found.", "Change the search or filter and try again."];
    if (tab === "Today") return ["No pickups due today.", "There are no pickups scheduled for today."];
    if (tab === "Upcoming") return ["No upcoming pickups.", "No upcoming pickup records were found."];
    if (tab === "Partially Given") return ["No partial pickups.", "No bookings are partially given right now."];
    if (tab === "Completed") return ["No completed pickups.", "No completed pickup records were found."];
    return ["No pickup records found.", "Records will appear here when available."];
  }
  const noun = ({
    Categories: "categories",
    Items: "items",
    Customers: "customers",
    Bookings: "bookings",
    Reports: "report records",
    Users: "users",
    Settings: "records",
  })[module] || "records";
  if (filtered) return [`No matching ${noun} found.`, "Change the search or filter and try again."];
  return [`No ${noun} found.`, "Records will appear here when available."];
}
function xDashboardCopy(surface) {
  const title = xText(surface.querySelector(".panel-head h2"));
  const map = {
    "Today Bookings": ["No bookings today.", "No bookings are scheduled for today."],
    "Today Pickups": ["No pickups today.", "No pickups are scheduled for today."],
    "Today Returns": ["No returns today.", "No returns are due today."],
    "Overdue Returns": ["No overdue returns.", "There are no overdue returns."],
  };
  return map[title] || ["No records found.", "Records will appear here when available."];
}
function xSetEmptyPresentation(empty, title, hint) {
  if (!(empty instanceof HTMLElement)) return;
  empty.classList.remove("phase14s-loading");
  empty.classList.add("phase14x-empty-state");
  empty.dataset.phase14xHint = hint;
  if (xNoData(xText(empty)) && xText(empty) !== title) empty.textContent = title;
}
function xSyncSurface(surface) {
  if (!(surface instanceof HTMLElement)) return;
  const empty = surface.querySelector(":scope > .empty") || (surface.classList.contains("report-shell") ? surface.querySelector(".empty") : null);
  const pager = surface.querySelector(":scope > .pager") || surface.querySelector(".pager");
  if (!(empty instanceof HTMLElement)) {
    if (pager instanceof HTMLElement && pager.hidden) pager.hidden = false;
    return;
  }

  const current = xText(empty);
  const loading = xLoading(current);
  empty.classList.toggle("phase14s-loading", loading);
  if (loading) {
    empty.classList.remove("phase14x-empty-state");
    delete empty.dataset.phase14xHint;
    if (pager instanceof HTMLElement) pager.hidden = true;
    return;
  }

  const settledEmpty = xNoData(current);
  if (settledEmpty) {
    surface.querySelectorAll(".phase14u-stale-copy,.phase14u-refresh-badge").forEach(node => node.remove());
    const copy = surface.classList.contains("dashboard-panel")
      ? xDashboardCopy(surface)
      : xEmptyCopy(xModule(), surface, xHasActiveFilter(surface));
    xSetEmptyPresentation(empty, copy[0], copy[1]);
    if (pager instanceof HTMLElement) pager.hidden = true;
  } else {
    empty.classList.remove("phase14x-empty-state", "phase14s-loading");
    delete empty.dataset.phase14xHint;
    if (pager instanceof HTMLElement) pager.hidden = false;
  }
}
function xScan() {
  xQueued = false;
  xEnhanceKpis();
  document.querySelectorAll(".list-card,.dashboard-panel").forEach(xSyncSurface);
}
function xSchedule() {
  if (xQueued) return;
  xQueued = true;
  requestAnimationFrame(xScan);
}

new MutationObserver(xSchedule).observe(document.documentElement, { childList: true, subtree: true, characterData: true });
xSchedule();
