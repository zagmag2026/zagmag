/* Phase 14AK — Return queue Partially Returned tab.
   Reuses the existing Return list request path; no extra polling/API call. */

let partialReturnMode = false;
let forwardingNativeClick = false;
let queued = false;

function isReturnQueue(section) {
  return section instanceof HTMLElement && [...section.querySelectorAll(".eyebrow")]
    .some(node => (node.textContent || "").trim() === "Return Queue");
}

function currentReturnSection() {
  return [...document.querySelectorAll("section.list-card")].find(isReturnQueue) || null;
}

function syncTab() {
  const section = currentReturnSection();
  if (!section) return;
  const tabs = section.querySelector(".pickup-tabs");
  if (!tabs) return;
  const nativeButtons = [...tabs.querySelectorAll("button:not(.phase14ak-partial-return-tab)")];
  const pending = nativeButtons.find(button => (button.textContent || "").trim() === "Pending Return");
  if (!pending) return;

  let partial = tabs.querySelector(".phase14ak-partial-return-tab");
  if (!partial) {
    partial = document.createElement("button");
    partial.type = "button";
    partial.className = "phase14ak-partial-return-tab";
    partial.textContent = "Partially Returned";
    partial.setAttribute("aria-label", "Show partially returned bookings");
    partial.addEventListener("click", event => {
      event.preventDefault();
      partialReturnMode = true;
      forwardingNativeClick = true;
      try { pending.click(); } finally { forwardingNativeClick = false; }
      schedule();
    });
    pending.insertAdjacentElement("afterend", partial);
  }

  if (partialReturnMode) {
    nativeButtons.forEach(button => button.classList.remove("active"));
    partial.classList.add("active");
  } else {
    partial.classList.remove("active");
  }
}

function schedule() {
  if (queued) return;
  queued = true;
  requestAnimationFrame(() => {
    queued = false;
    syncTab();
  });
}

document.addEventListener("click", event => {
  const target = event.target;
  if (!(target instanceof Element)) return;
  const button = target.closest("button");
  if (!button) return;

  if (button.closest("nav.tabs")) {
    partialReturnMode = false;
    schedule();
    return;
  }

  const tabs = button.closest(".pickup-tabs");
  const section = button.closest("section.list-card");
  if (!tabs || !isReturnQueue(section) || button.classList.contains("phase14ak-partial-return-tab")) return;
  if (!forwardingNativeClick) {
    partialReturnMode = false;
    schedule();
  }
}, true);

const priorFetch = globalThis.fetch.bind(globalThis);
globalThis.fetch = async (input, init = {}) => {
  if (partialReturnMode && typeof input === "string") {
    try {
      const method = String(init.method || "GET").toUpperCase();
      const url = new URL(input, location.origin);
      if (method === "GET" && url.pathname === "/api/admin/returns" && url.searchParams.get("view") === "pending") {
        url.searchParams.set("view", "partial");
        return priorFetch(url.toString(), init);
      }
    } catch {}
  }
  return priorFetch(input, init);
};

new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
schedule();
