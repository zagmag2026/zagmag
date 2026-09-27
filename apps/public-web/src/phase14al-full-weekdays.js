/* Phase 14AL — full weekday labels for Public Website.
   Uses display preferences already bundled into the existing public catalog response.
   No standalone settings request and no extra API call. */

const WEEKDAYS = {
  GU: ["રવિવાર", "સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર"],
  EN: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
};

let prefs = { defaultLanguage: "GU", dateFormat: "DD-MM-YYYY" };
let queued = false;

function normalizePrefs(value) {
  return {
    defaultLanguage: value?.defaultLanguage === "EN" ? "EN" : "GU",
    dateFormat: value?.dateFormat === "YYYY-MM-DD" ? "YYYY-MM-DD" : "DD-MM-YYYY"
  };
}

function dateLabel(value) {
  const match = String(value || "").match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return "";
  const [, year, month, day] = match;
  const index = new Date(Date.UTC(Number(year), Number(month) - 1, Number(day))).getUTCDay();
  const weekday = WEEKDAYS[prefs.defaultLanguage][index];
  const date = prefs.dateFormat === "YYYY-MM-DD" ? `${year}-${month}-${day}` : `${day}-${month}-${year}`;
  return `${weekday} · ${date}`;
}

function decorate(input) {
  if (!(input instanceof HTMLInputElement) || input.type !== "date") return;
  const label = input.closest(".availability-box label");
  if (!label) return;
  let note = label.querySelector(":scope > .phase14al-full-weekday");
  if (!(note instanceof HTMLElement)) {
    note = document.createElement("small");
    note.className = "phase14al-full-weekday";
    input.insertAdjacentElement("afterend", note);
  }
  const text = dateLabel(input.value);
  if (note.textContent !== text) note.textContent = text;
  note.hidden = !text;
}

function scan() {
  queued = false;
  document.querySelectorAll('.availability-box input[type="date"]').forEach(decorate);
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
    const url = typeof input === "string" ? input : String(input?.url || "");
    if (response.ok && /\/api\/public\/catalog(?:\?|$)/.test(url) && (response.headers.get("content-type") || "").includes("application/json")) {
      const data = await response.clone().json();
      if (data?.shop) {
        const next = normalizePrefs(data.shop);
        if (next.defaultLanguage !== prefs.defaultLanguage || next.dateFormat !== prefs.dateFormat) {
          prefs = next;
          schedule();
        }
      }
    }
  } catch {}
  return response;
};

document.addEventListener("input", event => {
  if (event.target instanceof HTMLInputElement && event.target.type === "date") schedule();
}, true);
document.addEventListener("change", event => {
  if (event.target instanceof HTMLInputElement && event.target.type === "date") schedule();
}, true);
new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
schedule();
