import "./phase14j.css";
import "./phase14k.js";
// Phase 14X Fix8 — visual cleanup only. Icon ownership belongs exclusively to phase14x-icon-runtime.js.

const PLACEHOLDERS = new Map([
  ["Choli", "Category name"],
  ["CH", "Code prefix"],
  ["Colour", "Field name"],
  ["Free Size", "Default value"],
  ["Red\nBlue\nGreen", "One option per line"],
  ["CH-001", "Item code"],
  ["Red Mirror Choli", "Item name"],
  ["https://.../photo-1.jpg\nhttps://.../photo-2.jpg", "Photo URL"],
  ["9876543210", "Mobile number"],
  ["Search code or item name", "Search items"],
  ["Search customer or mobile", "Search customers"],
  ["Search customer / mobile", "Search customers"],
  ["Filter item list", "Search items"],
  ["Booking, customer, mobile or item", "Search records"],
  ["Code or name", "Search items"],
  ["Name or mobile", "Search customers"],
  ["Booking / item / customer", "Search report"],
  ["Record, user or changed value", "Search audit log"],
  ["Search name, email or mobile", "Search users"],
  ["return_reminder", "Template key"],
  ["https://...", "Logo URL"]
]);

function normalize(value) {
  return String(value || "").replace(/\s+/g, " ").trim();
}

const TECHNICAL_COPY = [
  "One bundled refresh for stock and today’s customer activity.",
  "Works only before the first user exists and requires the Cloudflare setup secret.",
  "First URL is the main photo. Up to 8 http/https photos. R2/free storage can be connected later."
];

function shouldHideCopy(text) {
  const lower = text.toLowerCase();
  return TECHNICAL_COPY.includes(text) ||
    text === "No Pricing" ||
    text.includes("Pricing public website") ||
    lower.includes("secure upload") ||
    lower.includes("images are optional");
}

function clean(root = document) {
  root.querySelectorAll?.(".page-head .eyebrow,.list-head .eyebrow,.settings-title .eyebrow,.auth-card form>.eyebrow,.placeholder>.eyebrow,.topbar .sub")
    .forEach((node) => node.classList.add("ui-clean-hidden"));

  root.querySelectorAll?.("input[placeholder],textarea[placeholder]").forEach((field) => {
    const current = field.getAttribute("placeholder") || "";
    const replacement = PLACEHOLDERS.get(current);
    if (replacement && current !== replacement) field.setAttribute("placeholder", replacement);
  });

  root.querySelectorAll?.("p,small,.settings-note,.pill").forEach((node) => {
    const value = normalize(node.textContent);
    if (shouldHideCopy(value)) node.classList.add("ui-clean-hidden");
  });
}

let queued = false;
function scheduleClean() {
  if (queued) return;
  queued = true;
  queueMicrotask(() => {
    queued = false;
    clean(document);
  });
}

new MutationObserver(scheduleClean).observe(document.documentElement, { childList: true, subtree: true });
clean(document);
