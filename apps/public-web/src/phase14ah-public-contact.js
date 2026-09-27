import "./phase14ah-public-contact.css";

/* Phase 14AH — public footer contact icon hardening.
   React owns the links; this layer only decorates them with inline SVG icons.
   No polling, fetches or API calls. */
const SVG_NS = "http://www.w3.org/2000/svg";
const ICON_PATHS = {
  whatsapp: "M20 2H4C2.9 2 2.01 2.9 2.01 4L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2ZM6 6h12v2H6V6Zm0 3h12v2H6V9Zm0 3h8v2H6v-2Z",
  call: "M6.62 10.79a15.46 15.46 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.02-.24c1.12.37 2.33.57 3.57.57A1 1 0 0 1 21 16.5V20a1 1 0 0 1-1 1C10.61 21 3 13.39 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2Z"
};
let queued = false;

function kindFor(control) {
  const explicit = String(control.getAttribute("data-contact-kind") || "");
  if (explicit === "whatsapp" || explicit === "call") return explicit;
  const href = String(control.getAttribute("href") || "");
  if (href.startsWith("https://wa.me/")) return "whatsapp";
  if (href.startsWith("tel:")) return "call";
  return null;
}

function makeIcon(kind) {
  const span = document.createElement("span");
  span.className = `phase14ah-footer-icon ${kind}`;
  span.setAttribute("aria-hidden", "true");

  const svg = document.createElementNS(SVG_NS, "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("focusable", "false");
  const path = document.createElementNS(SVG_NS, "path");
  path.setAttribute("d", ICON_PATHS[kind]);
  svg.appendChild(path);
  span.appendChild(svg);
  return span;
}

function syncFooterIcons() {
  queued = false;
  document.querySelectorAll(".footer-actions a.btn,.footer-actions button.btn").forEach(control => {
    const kind = kindFor(control);
    if (!kind) return;
    control.classList.add("phase14ah-footer-contact");
    const existing = control.querySelector(":scope > .phase14ah-footer-icon");
    if (existing) {
      existing.classList.toggle("whatsapp", kind === "whatsapp");
      existing.classList.toggle("call", kind === "call");
      return;
    }
    control.prepend(makeIcon(kind));
  });
}

function schedule() {
  if (queued) return;
  queued = true;
  requestAnimationFrame(syncFooterIcons);
}

new MutationObserver(schedule).observe(document.getElementById("root") || document.documentElement, {
  childList: true,
  subtree: true
});
schedule();
