/* Phase 14X Fix9 — semantic pastel operational status cards. UI-only; zero API calls. */
import "./phase14x-operational-status.css";

const STATUS_SET = new Set([
  "BOOKED",
  "PARTIALLY_GIVEN",
  "GIVEN",
  "PARTIALLY_RETURNED",
  "RETURNED",
  "CANCELLED",
  "RESCHEDULED",
  "OVERDUE"
]);

function normalize(value) {
  return String(value || "").replace(/\s+/g, " ").trim();
}

function businessToday() {
  try {
    return new Intl.DateTimeFormat("en-CA", {
      timeZone: "Asia/Kolkata",
      year: "numeric",
      month: "2-digit",
      day: "2-digit"
    }).format(new Date());
  } catch {
    return new Date().toISOString().slice(0, 10);
  }
}

function dayDiff(fromIso, toIso) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(fromIso || "") || !/^\d{4}-\d{2}-\d{2}$/.test(toIso || "")) return 0;
  const from = Date.parse(`${fromIso}T00:00:00Z`);
  const to = Date.parse(`${toIso}T00:00:00Z`);
  if (!Number.isFinite(from) || !Number.isFinite(to)) return 0;
  return Math.max(0, Math.round((to - from) / 86400000));
}

function moduleName() {
  return normalize(document.querySelector(".page-head h1")?.textContent);
}

function directLabel(span) {
  if (!(span instanceof HTMLElement)) return "";
  return normalize([...span.childNodes]
    .filter(node => node.nodeType === Node.TEXT_NODE)
    .map(node => node.textContent || "")
    .join(" "));
}

function statusFromClass(card) {
  for (const className of card.classList) {
    if (!className.startsWith("status-")) continue;
    const status = className.slice(7).replace(/-/g, "_").toUpperCase();
    if (STATUS_SET.has(status)) return status;
  }
  return "";
}

function statusFromReport(card) {
  for (const span of card.querySelectorAll(".report-metrics span")) {
    const label = directLabel(span);
    if (!/^Status$/i.test(label)) continue;
    const status = normalize(span.querySelector("b")?.textContent).replace(/\s+/g, "_").toUpperCase();
    if (STATUS_SET.has(status)) return status;
  }
  return "";
}

function statusFromBadge(card) {
  const badge = card.querySelector(".chip");
  const value = normalize(badge?.textContent).toUpperCase();
  if (/^OVERDUE\b/.test(value)) return "OVERDUE";
  if (/^MISSED PICKUP\b/.test(value)) return "BOOKED";
  if (value === "TODAY PICKUP" || value === "UPCOMING") return "BOOKED";
  if (value === "PICKUP COMPLETED") return "GIVEN";
  const status = value.replace(/\s+/g, "_");
  return STATUS_SET.has(status) ? status : "";
}

function readStatus(card) {
  if (card.dataset.operationalStatus) return card.dataset.operationalStatus;
  const status = statusFromClass(card) || statusFromReport(card) || statusFromBadge(card);
  if (status) card.dataset.operationalStatus = status;
  return status;
}

function isoAfterLabel(text, label) {
  const escaped = label.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = normalize(text).match(new RegExp(`${escaped}\\s+(\\d{4}-\\d{2}-\\d{2})`, "i"));
  return match?.[1] || "";
}

function pickupDate(card) {
  return isoAfterLabel(card.textContent, "Pickup");
}

function dueDate(card) {
  return isoAfterLabel(card.textContent, "Due") || isoAfterLabel(card.textContent, "Return");
}

function overdueDays(card) {
  const text = normalize(card.textContent);
  const match = text.match(/OVERDUE\s+(\d+)D/i) || text.match(/Overdue Days\s*(\d+)/i);
  return match ? Number(match[1]) || 0 : 0;
}

function isOverdueCard(card) {
  return card.classList.contains("is-overdue") ||
    card.classList.contains("danger-report") ||
    readStatus(card) === "OVERDUE" ||
    overdueDays(card) > 0;
}

function toneFor(card, status, today) {
  if (isOverdueCard(card)) return "danger";

  const pickup = pickupDate(card);
  if ((status === "BOOKED" || status === "PARTIALLY_GIVEN") && pickup && pickup < today) return "danger";

  if (status === "CANCELLED") return "neutral";
  if (status === "RESCHEDULED") return "accent";
  if (status === "PARTIALLY_GIVEN" || status === "PARTIALLY_RETURNED") return "warning";
  if (status === "GIVEN" || status === "RETURNED") return "success";
  if (status === "BOOKED") return "info";

  const module = moduleName();
  if (module === "Returns") {
    const due = dueDate(card);
    if (due && due < today) return "danger";
    if (due) return "info";
  }
  return "";
}

function badgeForCard(card) {
  if (card.classList.contains("report-row")) {
    for (const span of card.querySelectorAll(".report-metrics span")) {
      const label = directLabel(span);
      if (/^(Status|Overdue Days)$/i.test(label)) return span;
    }
    return null;
  }
  return card.querySelector(".chip");
}

function badgeText(card, status, today) {
  if (isOverdueCard(card)) {
    const days = overdueDays(card);
    return days > 0 ? `OVERDUE ${days}D` : "OVERDUE";
  }

  const pickup = pickupDate(card);
  if ((status === "BOOKED" || status === "PARTIALLY_GIVEN") && pickup) {
    if (pickup < today) {
      const days = dayDiff(pickup, today);
      return days > 0 ? `MISSED PICKUP · ${days}D` : "MISSED PICKUP";
    }
    if (status === "BOOKED" && pickup === today) return "TODAY PICKUP";
    if (status === "BOOKED" && card.classList.contains("pickup-card") && !card.classList.contains("return-card")) return "UPCOMING";
  }

  if (status === "GIVEN" && card.classList.contains("pickup-card") && !card.classList.contains("return-card")) {
    return "PICKUP COMPLETED";
  }

  if (status === "RESCHEDULED") return "RESCHEDULED";
  return status ? status.replaceAll("_", " ") : "";
}

function setValue(element, key, value) {
  if (!value) {
    if (key in element.dataset) delete element.dataset[key];
    return;
  }
  if (element.dataset[key] !== value) element.dataset[key] = value;
}

function decorate(card, today) {
  if (!(card instanceof HTMLElement)) return;
  const status = readStatus(card);
  if (!status && !isOverdueCard(card)) return;

  const tone = toneFor(card, status, today);
  setValue(card, "operationalTone", tone);

  const badge = badgeForCard(card);
  if (!(badge instanceof HTMLElement)) return;
  if (tone) badge.dataset.operationalBadge = "1";

  const next = badgeText(card, status, today);
  if (!next) return;

  if (card.classList.contains("report-row")) {
    const value = badge.querySelector("b");
    if (value && normalize(value.textContent) !== next) value.textContent = next;
  } else if (normalize(badge.textContent) !== next) {
    badge.textContent = next;
  }
}

function scan() {
  const today = businessToday();
  document.querySelectorAll(
    ".booking-card,.pickup-card,.dashboard-row,.history-list article,.report-row"
  ).forEach(card => decorate(card, today));
}

let queued = false;
function schedule() {
  if (queued) return;
  queued = true;
  queueMicrotask(() => {
    queued = false;
    scan();
  });
}

new MutationObserver(schedule).observe(document.documentElement, {
  childList: true,
  subtree: true,
  characterData: true
});
scan();
