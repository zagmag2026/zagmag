/* Phase 14T operational sticky Save footer.
   Phase 14AI hardening: the React source button is always hidden while the
   generated footer owns Save, preventing transient duplicate Save actions. */
const O = node => String(node?.textContent || "").trim().replace(/\s+/g, " ");
let oq = false;

const oKind = () => O(document.querySelector(".page-head h1")) === "Pickup"
  ? "pickup"
  : O(document.querySelector(".page-head h1")) === "Returns"
    ? "returns"
    : "";

function oSource(panel, kind) {
  const re = kind === "pickup" ? /^Save Pickup$/i : /^Save Return$/i;
  return [...panel.querySelectorAll("button")].find(button =>
    re.test(O(button)) && !button.closest(".correction-card") && !button.classList.contains("phase14t-op-save")
  ) || null;
}

function oHideSource(button) {
  if (!(button instanceof HTMLButtonElement)) return;
  button.classList.add("phase14t-source-save");
  button.hidden = true;
  button.setAttribute("aria-hidden", "true");
  button.tabIndex = -1;
}

function oRestoreSources(panel) {
  panel?.querySelectorAll(".phase14t-source-save").forEach(node => {
    node.classList.remove("phase14t-source-save");
    if (node instanceof HTMLButtonElement) {
      node.hidden = false;
      node.removeAttribute("aria-hidden");
      node.removeAttribute("tabindex");
    }
  });
}

function oScan() {
  oq = false;
  const kind = oKind();
  const panel = kind ? document.querySelector(".history-panel.pickup-panel") : null;
  if (!panel) return;

  let footer = panel.querySelector(":scope > .phase14t-op-footer");
  const source = oSource(panel, kind);

  if (!source || panel.querySelector(".correction-card")) {
    footer?.remove();
    panel.classList.remove("phase14t-op-panel");
    oRestoreSources(panel);
    return;
  }

  if (!footer) {
    footer = document.createElement("div");
    footer.className = "phase14t-op-footer";

    const summary = document.createElement("span");
    summary.className = "phase14t-op-summary";
    summary.setAttribute("aria-live", "polite");

    const save = document.createElement("button");
    save.type = "button";
    save.className = "primary phase14t-op-save";
    save.addEventListener("click", () => {
      const currentPanel = save.closest(".history-panel.pickup-panel");
      const currentKind = oKind();
      const currentSource = currentPanel && currentKind ? oSource(currentPanel, currentKind) : null;
      if (currentSource && !currentSource.disabled) currentSource.click();
    });

    footer.append(summary, save);
    panel.appendChild(footer);
  }

  panel.classList.add("phase14t-op-panel");
  oHideSource(source);

  const rowSelector = kind === "pickup"
    ? ".pickup-line:not(.pickup-line-head)"
    : ".return-line:not(.return-line-head)";
  const inputs = [...panel.querySelectorAll(`${rowSelector} input[type="number"]`)];
  const selected = inputs.reduce((total, input) => total + Math.max(0, Number(input.value) || 0), 0);
  const pending = inputs.reduce((total, input) => total + Math.max(0, Number(input.max) || 0), 0);

  const summary = footer.querySelector(".phase14t-op-summary");
  const save = footer.querySelector(".phase14t-op-save");
  const summaryText = `Selected ${selected} · Pending ${pending}`;
  const buttonText = O(source) || (kind === "pickup" ? "Save Pickup" : "Save Return");

  if (summary && summary.textContent !== summaryText) summary.textContent = summaryText;
  if (save) {
    if (save.textContent !== buttonText) save.textContent = buttonText;
    save.disabled = source.disabled || selected <= 0;
  }

  const rect = panel.getBoundingClientRect();
  footer.style.left = `${Math.max(0, rect.left)}px`;
  footer.style.width = `${Math.max(0, rect.width)}px`;
  footer.style.bottom = `${Math.max(0, innerHeight - rect.bottom)}px`;
}

function oSchedule() {
  if (oq) return;
  oq = true;
  requestAnimationFrame(oScan);
}

document.addEventListener("input", oSchedule, true);
document.addEventListener("change", oSchedule, true);
addEventListener("resize", oSchedule);
new MutationObserver(oSchedule).observe(document.documentElement, { childList: true, subtree: true });
oSchedule();
