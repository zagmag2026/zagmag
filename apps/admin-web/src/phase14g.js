import "./phase14d.css";

const rowsByBooking = new Map();
const catalogByName = new Map();
const catalogByCode = new Map();

const norm = (value) => String(value || "").trim();
const nativeFetch = globalThis.fetch.bind(globalThis);

function rememberCatalog(item) {
  if (!item || typeof item !== "object") return;
  const name = norm(item.item_name).toLowerCase();
  const code = norm(item.item_code);
  if (name) catalogByName.set(name, item);
  if (code) catalogByCode.set(code, item);
}
function hydrate(line) {
  if (!line || typeof line !== "object") return line;
  const match = catalogByCode.get(norm(line.item_code)) || catalogByName.get(norm(line.item_name).toLowerCase());
  return match ? { ...match, ...line, image_url: line.image_url || match.image_url || null } : line;
}
function rememberRow(row) {
  if (!row || typeof row !== "object") return;
  const bookingNo = norm(row.booking_no);
  if (!bookingNo) return;
  const old = rowsByBooking.get(bookingNo) || {};
  const incoming = Array.isArray(row.item_lines) ? row.item_lines.map(hydrate) : [];
  incoming.forEach(rememberCatalog);
  rowsByBooking.set(bookingNo, { ...old, ...row, item_lines: incoming.length ? incoming : (old.item_lines || []) });
}
function capture(payload) {
  if (!payload || typeof payload !== "object") return;
  [payload.bookings, payload.pickups, payload.returns, payload.rows, payload.todayBookings, payload.todayPickups, payload.todayReturns, payload.overdueReturns]
    .filter(Array.isArray).forEach((rows) => rows.forEach(rememberRow));
  if (Array.isArray(payload.items)) {
    payload.items.forEach(rememberCatalog);
    if (payload.booking?.booking_no) {
      const lines = payload.items.map((item) => hydrate({
        ...item,
        pending_qty: Math.max(0, Number(item.given_qty || 0) - Number(item.returned_qty || 0)),
        remaining_to_give: Math.max(0, Number(item.booked_qty || 0) - Number(item.given_qty || 0))
      }));
      rememberRow({ ...payload.booking, item_lines: lines });
    }
  }
  schedule();
}

globalThis.fetch = async (...args) => {
  const response = await nativeFetch(...args);
  try {
    const url = typeof args[0] === "string" ? args[0] : args[0]?.url || "";
    if (url.includes("/api/admin/") && response.ok && (response.headers.get("content-type") || "").includes("application/json")) {
      response.clone().json().then(capture).catch(() => {});
    }
  } catch {}
  return response;
};

function bookingNo(node) { return node?.textContent?.match(/BK-[A-Z0-9-]+/i)?.[0] || ""; }
function initials(line) { return norm(line?.item_name || line?.item_code || "?").slice(0, 2).toUpperCase(); }
function modeFor(node) {
  if (node.closest(".return-card") || node.closest(".overdue-panel")) return "return";
  if (node.closest(".pickup-card")) return "pickup";
  const title = node.closest(".dashboard-panel")?.querySelector(".panel-head h2")?.textContent || "";
  if (/Pickup/i.test(title)) return "pickup";
  if (/Return|Overdue/i.test(title)) return "return";
  if (node.closest(".history-list")) return "history";
  return "booking";
}
function parseSummary(text) {
  return norm(text).split(/\s*,\s*/).filter(Boolean).map((part, index) => {
    const match = part.match(/^(.*?)\s*[×x]\s*(\d+)$/i);
    const name = norm(match?.[1] || part);
    const qty = Number(match?.[2] || 0);
    const catalog = catalogByName.get(name.toLowerCase()) || catalogByCode.get(name) || {};
    return hydrate({ ...catalog, item_id: catalog.id || `summary-${index}`, item_name: catalog.item_name || name, booked_qty: qty, image_url: catalog.image_url || null });
  });
}
function meta(line, mode) {
  if (mode === "pickup") return `Booked ${line.booked_qty || 0} · Given ${line.given_qty || 0} · Remaining ${line.remaining_to_give ?? Math.max(0, Number(line.booked_qty || 0) - Number(line.given_qty || 0))}`;
  if (mode === "return") return `Given ${line.given_qty || 0} · Returned ${line.returned_qty || 0} · Pending ${line.pending_qty ?? Math.max(0, Number(line.given_qty || 0) - Number(line.returned_qty || 0))}`;
  if (mode === "history") return `Booked ${line.booked_qty || 0} · Given ${line.given_qty || 0} · Returned ${line.returned_qty || 0}`;
  return `Booked ${line.booked_qty || 0}`;
}
function placeholder(box, line) {
  box.classList.add("is-placeholder");
  box.replaceChildren();
  const mark = document.createElement("span"); mark.textContent = initials(line);
  const text = document.createElement("small"); text.textContent = "No image";
  box.append(mark, text);
}
function itemCard(raw, mode) {
  const line = hydrate(raw) || raw;
  const card = document.createElement("div"); card.className = "phase14d-item-card";
  const thumb = document.createElement("div"); thumb.className = "phase14d-item-thumb";
  const url = norm(line.image_url);
  if (url) {
    const img = document.createElement("img"); img.src = url; img.alt = line.item_name || line.item_code || "Item"; img.loading = "lazy"; img.onerror = () => placeholder(thumb, line); thumb.append(img);
  } else placeholder(thumb, line);
  const copy = document.createElement("div"); copy.className = "phase14d-item-copy";
  const title = document.createElement("strong"); title.textContent = line.item_code ? `${line.item_code} · ${line.item_name || ""}` : (line.item_name || "Item");
  const category = document.createElement("span"); category.textContent = line.category_name || "";
  const qty = document.createElement("small"); qty.textContent = meta(line, mode);
  copy.append(title, category, qty); card.append(thumb, copy); return card;
}
function summaryElement(node) {
  if (node.matches(".booking-card")) return node.querySelector(".booking-summary");
  return node.querySelector(":scope > p");
}
function anchor(node) {
  if (node.matches(".booking-card")) return node.querySelector(".booking-dates") || node.querySelector(".booking-card-head");
  if (node.matches(".pickup-card")) return node.querySelector(".booking-card-head");
  if (node.matches(".dashboard-row")) return node.querySelector(".dashboard-row-head");
  return node.firstElementChild;
}
function render(node) {
  const no = bookingNo(node); const row = no ? rowsByBooking.get(no) : null; const summary = summaryElement(node);
  let lines = Array.isArray(row?.item_lines) && row.item_lines.length ? row.item_lines.map(hydrate) : parseSummary(summary?.textContent || "");
  const mode = modeFor(node);
  if (mode === "return" && lines.some((line) => Number(line.given_qty || 0) > 0)) lines = lines.filter((line) => Number(line.given_qty || 0) > 0);
  if (!lines.length) return;
  const fingerprint = lines.map((line) => `${line.item_id || line.item_code || line.item_name}|${line.image_url || ""}|${line.booked_qty || 0}|${line.given_qty || 0}|${line.returned_qty || 0}`).join(";");
  const old = node.querySelector(":scope > .phase14g-direct-items");
  if (old?.dataset.fingerprint === fingerprint) return;
  old?.remove();
  const grid = document.createElement("div"); grid.className = "phase14d-item-grid phase14g-direct-items"; grid.dataset.fingerprint = fingerprint;
  lines.forEach((line) => grid.append(itemCard(line, mode)));
  const target = anchor(node); if (target) target.insertAdjacentElement("afterend", grid); else node.prepend(grid);
  if (summary) summary.style.display = "none";
  node.dataset.phase14dItems = "1";
}
function scan() {
  document.querySelectorAll(".dashboard-row,.booking-card,.pickup-card,.history-list > article").forEach(render);
}
let queued = false;
function schedule() { if (queued) return; queued = true; requestAnimationFrame(() => { queued = false; scan(); }); }
new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
setInterval(schedule, 1000);
schedule();
