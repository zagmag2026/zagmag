/* Phase 14X Fix10 — every missed-pickup card shows every booked item individually.
   Observes existing responses only; zero additional API calls. */
const rowsByBooking = new Map();
let queued = false;

function normalize(value) { return String(value || "").replace(/\s+/g, " ").trim(); }
function requestUrl(input) { return typeof input === "string" ? input : String(input?.url || ""); }
function remember(rows) {
  if (!Array.isArray(rows)) return;
  for (const row of rows) {
    const no = normalize(row?.booking_no);
    if (!no || !Array.isArray(row?.item_lines)) continue;
    rowsByBooking.set(no, row.item_lines);
  }
  schedule();
}

const priorFetch = window.fetch.bind(window);
window.fetch = async (...args) => {
  const response = await priorFetch(...args);
  const url = requestUrl(args[0]);
  if (response.ok && (/\/api\/admin\/dashboard(?:\?|$)/.test(url) || /\/api\/admin\/pickups\?.*\bview=missed\b/.test(url))) {
    response.clone().json().then(data => {
      remember(data?.missedPickups);
      remember(data?.pickups);
    }).catch(() => {});
  }
  return response;
};

function bookingNo(card) {
  return normalize(card?.textContent).match(/BK-[A-Z0-9-]+/i)?.[0] || "";
}
function initials(line) {
  return normalize(line?.item_name || line?.item_code || "?").slice(0, 2).toUpperCase();
}
function thumb(line) {
  const box = document.createElement("div");
  box.className = "phase14d-item-thumb";
  const url = normalize(line?.image_url);
  const fallback = () => {
    box.classList.add("is-placeholder");
    box.replaceChildren();
    const mark = document.createElement("span"); mark.textContent = initials(line);
    const label = document.createElement("small"); label.textContent = "No image";
    box.append(mark, label);
  };
  if (!url) { fallback(); return box; }
  const img = document.createElement("img");
  img.src = url; img.alt = normalize(line?.item_name || line?.item_code || "Item"); img.loading = "lazy"; img.decoding = "async"; img.onerror = fallback;
  box.append(img);
  return box;
}
function itemCard(line) {
  const card = document.createElement("div"); card.className = "phase14d-item-card";
  const copy = document.createElement("div"); copy.className = "phase14d-item-copy";
  const strong = document.createElement("strong"); strong.textContent = line?.item_code ? `${line.item_code} · ${line.item_name || ""}` : (line?.item_name || "Item");
  const category = document.createElement("span"); category.textContent = normalize(line?.category_name);
  const small = document.createElement("small"); small.textContent = `Booked ${Number(line?.booked_qty || 0)} · Given ${Number(line?.given_qty || 0)} · Remaining ${Number(line?.remaining_to_give || 0)}`;
  copy.append(strong, category, small); card.append(thumb(line), copy); return card;
}
function decorate(card) {
  if (!(card instanceof HTMLElement) || card.dataset.phase14xMissedItems === "1") return;
  if (card.querySelector(":scope > .phase14d-item-grid")) {
    card.dataset.phase14xMissedItems = "1";
    card.dataset.phase14dItems = "1";
    return;
  }
  const no = bookingNo(card); const lines = rowsByBooking.get(no);
  if (!lines?.length) return;
  const grid = document.createElement("div"); grid.className = "phase14d-item-grid"; grid.dataset.phase14dVisual = "1";
  lines.forEach(line => grid.append(itemCard(line)));
  const head = card.querySelector(".booking-card-head,.dashboard-row-head");
  if (head) head.insertAdjacentElement("afterend", grid); else card.prepend(grid);
  const summary = card.querySelector(":scope > p"); if (summary instanceof HTMLElement) summary.style.display = "none";
  card.dataset.phase14xMissedItems = "1";
  card.dataset.phase14dItems = "1";
}
function scan() {
  queued = false;
  document.querySelectorAll(".phase14x-missed-panel .dashboard-row,.phase14x-missed-list .pickup-card").forEach(decorate);
}
function schedule() {
  if (queued) return;
  queued = true;
  queueMicrotask(scan);
}
new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
schedule();
