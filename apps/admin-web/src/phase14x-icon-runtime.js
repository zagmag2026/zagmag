/* Phase 14X Fix8 — single-owner, idempotent Admin Material icon runtime. */
import "./phase14x-global-icons.css";

const text = (node) => String(node?.textContent || "").replace(/\s+/g, " ").trim();
const nav = {
  Dashboard: "home",
  Categories: "category",
  Items: "checkroom",
  Customers: "person",
  Bookings: "calendar_month",
  Pickup: "outbox",
  Returns: "assignment_return",
  Reports: "description",
  Users: "group",
  Settings: "settings",
  "Basic Settings": "settings",
  "Public Website": "language",
  "WhatsApp Templates": "chat",
  "Audit Log": "fact_check"
};
const kpi = {
  Categories: "category",
  Items: "checkroom",
  "Total Quantity": "inventory_2",
  "Available Now": "check_circle",
  "Booked Pending": "event_note",
  "Missed Pickups": "event_busy",
  "Given Out": "outbox",
  "Overdue Returns": "warning"
};
const rules = [
  [/^Logout$/i, "logout"],
  [/^Search(?:\s|$)/i, "search"],
  [/^(Apply|Filter)(?:\s|$)/i, "filter_alt"],
  [/^(Refresh|Refreshing|Loading)/i, "refresh"],
  [/^(Save|Saving)/i, "save"],
  [/^Update(?:\s|$)/i, "check_circle"],
  [/^(Edit|Correct)/i, "edit"],
  [/^(Delete|Remove)/i, "delete"],
  [/^(Cancel|Close)/i, "close"],
  [/^Archive/i, "archive"],
  [/^(Restore|Reset)/i, "restart_alt"],
  [/^History/i, "history"],
  [/^(Fields|Close Fields)/i, "tune"],
  [/^(View|Open|Details)/i, "visibility"],
  [/^Previous$/i, "chevron_left"],
  [/^Next$/i, "chevron_right"],
  [/^Give All/i, "outbox"],
  [/^Return All/i, "assignment_return"],
  [/Reset Password/i, "key"],
  [/^Print/i, "print"],
  [/^Download/i, "download"],
  [/^(Create|Add|New)/i, "add_circle"],
  [/^Back/i, "arrow_back"],
  [/^Sign in$/i, "login"]
];

const LEGACY_PREFIX = /^\s*(?:\+|×|✎|↻|↺|›|‹|↑|↓|↩|⌕|✓|⚿|☎|◉|⇥)\s+/;

function setDataset(element, key, value) {
  if (element.dataset[key] === value) return;
  element.dataset[key] = value;
}

function deleteDataset(element, key) {
  if (!(key in element.dataset)) return;
  delete element.dataset[key];
}

function stripLegacyPrefix(element) {
  for (const node of element.childNodes) {
    if (node.nodeType !== Node.TEXT_NODE) continue;
    const before = node.textContent || "";
    const after = before.replace(LEGACY_PREFIX, "");
    if (after !== before) {
      node.textContent = after;
      if (element.dataset.phase14arRendered) setDataset(element, "phase14arRendered", text(element));
      return true;
    }
    return false;
  }
  return false;
}

function stableLabel(element) {
  const phaseSource = String(element.dataset.phase14arSource || "").replace(LEGACY_PREFIX, "").replace(/\s+/g, " ").trim();
  const current = text(element).replace(LEGACY_PREFIX, "").trim();
  if (phaseSource) {
    setDataset(element, "uiLabelKey", phaseSource);
    return phaseSource;
  }
  const existing = String(element.dataset.uiLabelKey || "").trim();
  if (existing) return existing;
  if (current) setDataset(element, "uiLabelKey", current);
  return current;
}

function iconFor(element, label) {
  const title = String(element.getAttribute?.("title") || "");
  if (element.classList.contains("icon-btn")) {
    if (/move up/i.test(title)) return "arrow_upward";
    if (/move down/i.test(title)) return "arrow_downward";
  }
  if (element.closest(".tabs") || element.closest(".settings-tabs")) return nav[label] || "";
  if (element.classList.contains("wa-button") || /^WhatsApp/i.test(label)) return "chat";
  if (element.classList.contains("call-button") || /^Call/i.test(label)) return "call";
  for (const [rule, icon] of rules) if (rule.test(label)) return icon;
  return "";
}

function decorateAction(element) {
  stripLegacyPrefix(element);
  const icon = iconFor(element, stableLabel(element));
  if (!icon) {
    deleteDataset(element, "uiIcon");
    deleteDataset(element, "uiGlyph");
    deleteDataset(element, "uiIconOnly");
    return;
  }
  setDataset(element, "uiIcon", "1");
  setDataset(element, "uiGlyph", icon);
  if (element.classList.contains("icon-btn")) setDataset(element, "uiIconOnly", "1");
  else deleteDataset(element, "uiIconOnly");
}

function decorateKpi(card) {
  const label = stableLabel(card.querySelector("span"));
  const icon = kpi[label] || "";
  if (icon) setDataset(card, "phase14xGlyph", icon);
  else deleteDataset(card, "phase14xGlyph");
}

function scan() {
  document.body?.classList.add("ui-material-icons-v2");
  document.querySelectorAll("button,a").forEach(decorateAction);
  document.querySelectorAll(".stats .stat").forEach(decorateKpi);
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