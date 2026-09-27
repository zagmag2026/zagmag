/* Phase 14AJ/14AL — business date/time display hardening.
   Storage remains UTC. Visible timestamps use Asia/Kolkata and the configured
   language/date format. Weekday names are always full. No polling and no
   standalone settings API call. */

const BUSINESS_TZ = "Asia/Kolkata";
const IST_OFFSET_MINUTES = 330;
const PREF_KEY = "zhagmag:display-prefs:v1";
const DEFAULT_PREFS = { defaultLanguage: "GU", dateFormat: "DD-MM-YYYY" };
const WEEKDAY_NAMES = {
  GU: ["રવિવાર", "સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર"],
  EN: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
};
const EN_WEEKDAY_INDEX = new Map([
  ["Sunday", 0], ["Monday", 1], ["Tuesday", 2], ["Wednesday", 3],
  ["Thursday", 4], ["Friday", 5], ["Saturday", 6]
]);
const rawText = new WeakMap();
let queued = false;

function normalizePrefs(value) {
  return {
    defaultLanguage: value?.defaultLanguage === "EN" ? "EN" : "GU",
    dateFormat: value?.dateFormat === "YYYY-MM-DD" ? "YYYY-MM-DD" : "DD-MM-YYYY"
  };
}

function readPrefs() {
  try { return normalizePrefs(JSON.parse(localStorage.getItem(PREF_KEY) || "{}")); }
  catch { return { ...DEFAULT_PREFS }; }
}

let prefs = readPrefs();

function savePrefs(value) {
  const next = normalizePrefs({ ...prefs, ...value });
  const changed = next.defaultLanguage !== prefs.defaultLanguage || next.dateFormat !== prefs.dateFormat;
  prefs = next;
  try { localStorage.setItem(PREF_KEY, JSON.stringify(next)); } catch {}
  if (changed) schedule();
}

function dateText(year, month, day) {
  return prefs.dateFormat === "YYYY-MM-DD"
    ? `${year}-${month}-${day}`
    : `${day}-${month}-${year}`;
}

function weekday(date) {
  let index = -1;
  try {
    const englishName = new Intl.DateTimeFormat("en-US", {
      timeZone: BUSINESS_TZ,
      weekday: "long"
    }).format(date);
    index = EN_WEEKDAY_INDEX.get(englishName) ?? -1;
  } catch {}
  if (index < 0) index = new Date(date.getTime() + IST_OFFSET_MINUTES * 60 * 1000).getUTCDay();
  return WEEKDAY_NAMES[prefs.defaultLanguage][index];
}

function businessParts(date) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: BUSINESS_TZ,
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(date);
  const map = Object.fromEntries(parts.map(part => [part.type, part.value]));
  return { year: map.year, month: map.month, day: map.day };
}

function parseUtcTimestamp(value) {
  const text = String(value || "").trim();
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(text)) return new Date(`${text.replace(" ", "T")}Z`);
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?$/.test(text)) return new Date(`${text}Z`);
  const date = new Date(text);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatDateOnly(value) {
  const match = String(value || "").match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return value;
  const [, year, month, day] = match;
  const date = new Date(`${year}-${month}-${day}T00:00:00+05:30`);
  if (Number.isNaN(date.getTime())) return value;
  return `${weekday(date)} · ${dateText(year, month, day)}`;
}

function formatDateTime(value) {
  const date = parseUtcTimestamp(value);
  if (!date) return value;
  const { year, month, day } = businessParts(date);
  const time = new Intl.DateTimeFormat("en-IN", {
    timeZone: BUSINESS_TZ,
    hour: "2-digit",
    minute: "2-digit",
    hour12: true
  }).format(date).toUpperCase();
  return `${weekday(date)}, ${dateText(year, month, day)} · ${time}`;
}

const DATE_OR_TIME = /\b\d{4}-\d{2}-\d{2}(?:[ T]\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?(?:Z|[+-]\d{2}:\d{2})?)?\b/g;

function hasRawDate(value) {
  DATE_OR_TIME.lastIndex = 0;
  const matched = DATE_OR_TIME.test(String(value || ""));
  DATE_OR_TIME.lastIndex = 0;
  return matched;
}

function formatText(value) {
  DATE_OR_TIME.lastIndex = 0;
  return String(value || "").replace(DATE_OR_TIME, token => /[ T]\d{2}:\d{2}:\d{2}/.test(token) ? formatDateTime(token) : formatDateOnly(token));
}

function excluded(node) {
  const parent = node.parentElement;
  if (!parent) return true;
  return Boolean(parent.closest("input,textarea,select,option,pre,code,script,style,[contenteditable='true']"));
}

function scan() {
  queued = false;
  const root = document.querySelector("main") || document.body;
  if (!root) return;
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  let node;
  while ((node = walker.nextNode())) {
    if (excluded(node)) continue;
    const current = node.nodeValue || "";
    let entry = rawText.get(node);
    if (!entry) {
      if (!hasRawDate(current)) continue;
      entry = { source: current, rendered: current };
      rawText.set(node, entry);
    } else if (current !== entry.rendered) {
      if (!hasRawDate(current)) { rawText.delete(node); continue; }
      entry.source = current;
    }
    const formatted = formatText(entry.source);
    entry.rendered = formatted;
    if (current !== formatted) node.nodeValue = formatted;
  }
}

function schedule() {
  if (queued) return;
  queued = true;
  requestAnimationFrame(scan);
}

const priorFetch = globalThis.fetch.bind(globalThis);
globalThis.fetch = async (input, init = {}) => {
  const response = await priorFetch(input, init);
  try {
    const url = typeof input === "string" ? input : input?.url || "";
    const method = String(init.method || (typeof input !== "string" ? input?.method : "GET") || "GET").toUpperCase();
    if (response.ok && (response.headers.get("content-type") || "").includes("application/json")) {
      if (/\/api\/auth\/(?:me|login)(?:\?|$)/.test(url)) {
        const data = await response.clone().json();
        if (data?.displayPreferences) savePrefs(data.displayPreferences);
      } else if (/\/api\/admin\/settings\/bootstrap(?:\?|$)/.test(url)) {
        const data = await response.clone().json();
        if (data?.settings) savePrefs(data.settings);
      } else if (method === "PUT" && /\/api\/admin\/settings(?:\?|$)/.test(url) && typeof init.body === "string") {
        const body = JSON.parse(init.body);
        savePrefs(body);
      }
    }
  } catch {}
  return response;
};

new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true, characterData: true });
window.addEventListener("zhagmag-display-prefs-changed", schedule);
schedule();
