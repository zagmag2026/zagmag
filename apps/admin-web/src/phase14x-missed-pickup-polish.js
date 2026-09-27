/* Phase 14X Fix10 UI2 — compact missed-pickup ordering + KPI affordance.
   UI-only; observes existing DOM and never creates API/Worker calls. */
import "./phase14x-missed-pickup-polish.css";

const normalize = value => String(value || "").replace(/\s+/g, " ").trim();
let queued = false;

function directLabel(node) {
  if (!(node instanceof HTMLElement)) return "";
  return normalize([...node.childNodes]
    .filter(child => child.nodeType === Node.TEXT_NODE)
    .map(child => child.textContent || "")
    .join(" "));
}

function moveFirst(card, node) {
  if (!(card instanceof HTMLElement) || !(node instanceof HTMLElement)) return;
  if (card.firstElementChild !== node) card.prepend(node);
}

function moveAfter(node, anchor) {
  if (!(node instanceof HTMLElement) || !(anchor instanceof HTMLElement)) return;
  if (anchor.nextElementSibling !== node) anchor.insertAdjacentElement("afterend", node);
}

function removeDuplicateBookingMetric(card) {
  if (!card.classList.contains("dashboard-row")) return;
  card.querySelectorAll(":scope > .dashboard-meta > span").forEach(span => {
    if (/^Booking$/i.test(directLabel(span))) span.remove();
  });
}

function polishCard(card) {
  if (!(card instanceof HTMLElement)) return;
  const dashboard = card.classList.contains("dashboard-row");
  const head = card.querySelector(":scope > .booking-card-head,:scope > .dashboard-row-head");
  if (!(head instanceof HTMLElement)) return;

  head.classList.add("phase14x-missed-identity");
  if (dashboard) head.classList.add("dashboard-row-head");
  moveFirst(card, head);

  const grid = card.querySelector(":scope > .phase14d-item-grid");
  if (grid instanceof HTMLElement) moveAfter(grid, head);

  const metrics = card.querySelector(dashboard ? ":scope > .dashboard-meta" : ":scope > .pickup-qty");
  const metricsAnchor = grid instanceof HTMLElement ? grid : head;
  if (metrics instanceof HTMLElement) moveAfter(metrics, metricsAnchor);

  const note = [...card.children].find(child => child.tagName === "SMALL");
  if (note instanceof HTMLElement && metrics instanceof HTMLElement) moveAfter(note, metrics);

  const actions = card.querySelector(":scope > .dashboard-actions,:scope > .quick-actions");
  const actionAnchor = note instanceof HTMLElement ? note : (metrics instanceof HTMLElement ? metrics : metricsAnchor);
  if (actions instanceof HTMLElement) moveAfter(actions, actionAnchor);

  removeDuplicateBookingMetric(card);
}

function polishKpi() {
  const kpi = document.querySelector(".phase14x-missed-kpi");
  if (!(kpi instanceof HTMLElement)) return;

  /* Reuse the one global KPI system. It owns both the left icon and right chevron. */
  kpi.classList.add("phase14x-kpi");
  kpi.classList.remove("phase14x-missed-kpi-actionable");

  /* Clean up the old UI2 custom chevron if a stale DOM still contains one. */
  kpi.querySelectorAll(":scope > .phase14x-missed-kpi-chevron").forEach(node => node.remove());
}

function scan() {
  queued = false;
  document.querySelectorAll(
    ".phase14x-missed-panel .dashboard-row,.phase14x-missed-list .pickup-card"
  ).forEach(polishCard);
  polishKpi();
}

function schedule() {
  if (queued) return;
  queued = true;
  queueMicrotask(scan);
}

new MutationObserver(schedule).observe(document.documentElement, {
  childList: true,
  subtree: true
});
schedule();
