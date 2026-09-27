import "./phase14as-booking-flow.css";

const CUSTOMER_PREFILL_KEY = "zhagmag.booking.prefillCustomer";
const CUSTOMER_MIN_QUERY = 2;
const CUSTOMER_DEBOUNCE_MS = 350;

const customerSearchState = new WeakMap();
let mainObserver = null;
let observedMain = null;
let customerGridObserver = null;
let observedCustomerGrid = null;
let bookingLinesObserver = null;
let observedBookingLines = null;
let enhanceQueued = false;

function digits(value) {
  return String(value || "").replace(/\D/g, "");
}

function setNativeInput(input, value) {
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")?.set;
  setter?.call(input, value);
  input.dispatchEvent(new Event("input", { bubbles: true }));
}

function setNativeSelect(select, value) {
  const setter = Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, "value")?.set;
  setter?.call(select, value);
  select.dispatchEvent(new Event("change", { bubbles: true }));
}

function readPendingCustomer() {
  try {
    const parsed = JSON.parse(sessionStorage.getItem(CUSTOMER_PREFILL_KEY) || "null");
    if (!parsed || typeof parsed !== "object") return null;
    const mobile = digits(parsed.mobile);
    const name = String(parsed.name || "").trim();
    return mobile ? { mobile, name } : null;
  } catch {
    return null;
  }
}

function writePendingCustomer(name, mobile) {
  try {
    sessionStorage.setItem(CUSTOMER_PREFILL_KEY, JSON.stringify({ name: String(name || "").trim(), mobile: digits(mobile) }));
  } catch {}
}

function clearPendingCustomer() {
  try { sessionStorage.removeItem(CUSTOMER_PREFILL_KEY); } catch {}
}

function bookingTabFromCustomerPage() {
  const active = document.querySelector(".tabs button.active");
  return active?.nextElementSibling instanceof HTMLButtonElement ? active.nextElementSibling : null;
}

function enhanceCustomerCards() {
  const grid = document.querySelector(".customer-grid");
  if (grid !== observedCustomerGrid) {
    customerGridObserver?.disconnect();
    observedCustomerGrid = grid;
    customerGridObserver = null;
    if (grid) {
      customerGridObserver = new MutationObserver(scheduleEnhance);
      customerGridObserver.observe(grid, { childList: true });
    }
  }
  if (!grid) return;

  grid.querySelectorAll(".customer-card").forEach((card) => {
    if (card.dataset.phase14asBooking === "1") return;
    card.dataset.phase14asBooking = "1";
    if (card.classList.contains("archived") || !card.querySelector(".chip.good")) return;

    const quick = card.querySelector(".quick-actions");
    const phoneLink = card.querySelector('.customer-head a[href^="tel:"]');
    const nameNode = card.querySelector(".customer-head strong");
    if (!quick || !phoneLink || !nameNode) return;

    const mobile = digits(phoneLink.getAttribute("href")?.replace(/^tel:/i, ""));
    const name = String(nameNode.textContent || "").trim();
    if (!mobile) return;

    const button = document.createElement("button");
    button.type = "button";
    button.className = "ghost phase14as-create-booking";
    button.textContent = "Create Booking";
    button.addEventListener("click", () => {
      writePendingCustomer(name, mobile);
      const bookingTab = bookingTabFromCustomerPage();
      bookingTab?.click();
      requestAnimationFrame(scheduleEnhance);
    });

    const historyButton = [...quick.querySelectorAll("button")].find((node) => !node.classList.contains("phase14as-create-booking"));
    if (historyButton) quick.insertBefore(button, historyButton);
    else quick.append(button);
  });
}

function customerSearchParts(form) {
  const picker = form.querySelector(".customer-picker");
  const input = picker?.querySelector("input");
  const button = picker?.querySelector("button");
  const customerSelect = picker?.nextElementSibling?.querySelector?.("select");
  return {
    picker,
    input: input instanceof HTMLInputElement ? input : null,
    button: button instanceof HTMLButtonElement ? button : null,
    customerSelect: customerSelect instanceof HTMLSelectElement ? customerSelect : null
  };
}

function scheduleCustomerSearch(input, button) {
  let state = customerSearchState.get(input);
  if (!state) {
    state = { timer: 0, lastExecuted: "", lastInput: "" };
    customerSearchState.set(input, state);
  }
  window.clearTimeout(state.timer);
  const query = input.value.trim();
  const normalized = query.toLocaleLowerCase();
  state.lastInput = normalized;
  if (query.length < CUSTOMER_MIN_QUERY) return;

  state.timer = window.setTimeout(() => {
    if (!input.isConnected || input.value.trim().toLocaleLowerCase() !== normalized) return;
    if (state.lastExecuted === normalized || button.disabled) return;
    state.lastExecuted = normalized;
    button.click();
  }, CUSTOMER_DEBOUNCE_MS);
}

function attachLiveCustomerSearch(form) {
  const { picker, input, button, customerSelect } = customerSearchParts(form);
  if (!picker || !input || !button || !customerSelect) return;

  picker.classList.add("phase14as-live-customer");
  if (!picker.querySelector(".phase14as-live-hint")) {
    const hint = document.createElement("small");
    hint.className = "phase14as-live-hint";
    hint.textContent = "Type at least 2 characters · live search after a short pause";
    picker.insertAdjacentElement("afterend", hint);
  }

  if (input.dataset.phase14asLive !== "1") {
    input.dataset.phase14asLive = "1";
    input.addEventListener("input", () => scheduleCustomerSearch(input, button));
  }

  const pending = readPendingCustomer();
  if (!pending || form.dataset.phase14asPrefill === pending.mobile) return;
  form.dataset.phase14asPrefill = pending.mobile;

  const selectPending = () => {
    const match = [...customerSelect.options].find((option) => digits(option.textContent).includes(pending.mobile));
    if (!match) return false;
    setNativeSelect(customerSelect, match.value);
    clearPendingCustomer();
    return true;
  };

  if (selectPending()) return;

  const optionObserver = new MutationObserver(() => {
    if (selectPending()) optionObserver.disconnect();
  });
  optionObserver.observe(customerSelect, { childList: true });
  window.setTimeout(() => optionObserver.disconnect(), 10000);

  setNativeInput(input, pending.mobile);
  scheduleCustomerSearch(input, button);
}

function categoryFromOption(option) {
  if (!(option instanceof HTMLOptionElement) || !option.value) return "";
  const parts = String(option.textContent || "").split(" · ").map((value) => value.trim()).filter(Boolean);
  return parts.length >= 3 ? parts.slice(2).join(" · ") : "";
}

function itemSelectForLine(line) {
  const labels = [...line.querySelectorAll(":scope > label")].filter((label) => !label.classList.contains("phase14as-category-field"));
  for (const label of labels) {
    const select = label.querySelector("select");
    if (select instanceof HTMLSelectElement) return { label, select };
  }
  return { label: null, select: null };
}

function categoriesForItemSelect(itemSelect) {
  const seen = new Set();
  const list = [];
  [...itemSelect.options].forEach((option) => {
    const category = categoryFromOption(option);
    if (!category || seen.has(category)) return;
    seen.add(category);
    list.push(category);
  });
  return list;
}

function applyCategoryFilter(line, categorySelect, itemSelect) {
  const category = categorySelect.value;
  itemSelect.dataset.phase14asCategory = category;
  itemSelect.disabled = !category;

  [...itemSelect.options].forEach((option) => {
    if (!option.value) {
      option.hidden = false;
      option.disabled = false;
      const placeholder = category ? "Select item" : "Select category first";
      if (option.textContent !== placeholder) option.textContent = placeholder;
      return;
    }
    const matches = !category || categoryFromOption(option) === category;
    option.hidden = !matches;
    option.disabled = !matches;
  });

  if (itemSelect.value) {
    const selected = itemSelect.selectedOptions[0];
    if (category && selected && categoryFromOption(selected) !== category) setNativeSelect(itemSelect, "");
  }
  line.classList.add("phase14as-category-ready");
}

function enhanceBookingLine(line) {
  const { label: itemLabel, select: itemSelect } = itemSelectForLine(line);
  if (!itemLabel || !itemSelect) return;
  itemLabel.classList.add("phase14as-item-field");

  let categoryLabel = line.querySelector(":scope > .phase14as-category-field");
  let categorySelect = categoryLabel?.querySelector("select");
  if (!(categoryLabel instanceof HTMLLabelElement) || !(categorySelect instanceof HTMLSelectElement)) {
    categoryLabel = document.createElement("label");
    categoryLabel.className = "phase14as-category-field";
    categoryLabel.append(document.createTextNode("Category"));
    categorySelect = document.createElement("select");
    categorySelect.setAttribute("aria-label", "Category");
    categoryLabel.append(categorySelect);
    line.insertBefore(categoryLabel, itemLabel);
  }

  const categories = categoriesForItemSelect(itemSelect);
  const selectedItemCategory = categoryFromOption(itemSelect.selectedOptions[0]);
  const wantedCategory = selectedItemCategory || itemSelect.dataset.phase14asCategory || categorySelect.value || "";
  const signature = categories.join("\u0001");
  if (categorySelect.dataset.phase14asOptions !== signature) {
    categorySelect.innerHTML = "";
    const blank = document.createElement("option");
    blank.value = "";
    blank.textContent = "Select category";
    categorySelect.append(blank);
    categories.forEach((category) => {
      const option = document.createElement("option");
      option.value = category;
      option.textContent = category;
      categorySelect.append(option);
    });
    categorySelect.dataset.phase14asOptions = signature;
  }
  categorySelect.value = categories.includes(wantedCategory) ? wantedCategory : "";

  if (categorySelect.dataset.phase14asBound !== "1") {
    categorySelect.dataset.phase14asBound = "1";
    categorySelect.addEventListener("change", () => {
      applyCategoryFilter(line, categorySelect, itemSelect);
      requestAnimationFrame(() => line.isConnected && enhanceBookingLine(line));
    });
    itemSelect.addEventListener("change", () => {
      const category = categoryFromOption(itemSelect.selectedOptions[0]);
      if (category) categorySelect.value = category;
      applyCategoryFilter(line, categorySelect, itemSelect);
      requestAnimationFrame(() => line.isConnected && enhanceBookingLine(line));
    });
  }

  applyCategoryFilter(line, categorySelect, itemSelect);
}

function enhanceBookingPage() {
  const form = document.querySelector(".booking-form");
  if (!(form instanceof HTMLFormElement)) {
    bookingLinesObserver?.disconnect();
    bookingLinesObserver = null;
    observedBookingLines = null;
    return;
  }

  attachLiveCustomerSearch(form);
  const lines = form.querySelector(".booking-lines");
  if (!(lines instanceof HTMLElement)) return;

  if (lines !== observedBookingLines) {
    bookingLinesObserver?.disconnect();
    observedBookingLines = lines;
    bookingLinesObserver = new MutationObserver(scheduleEnhance);
    bookingLinesObserver.observe(lines, { childList: true });
  }
  lines.querySelectorAll(".booking-line").forEach(enhanceBookingLine);
}

function enhanceCurrentView() {
  enhanceQueued = false;
  enhanceCustomerCards();
  enhanceBookingPage();
}

function scheduleEnhance() {
  if (enhanceQueued) return;
  enhanceQueued = true;
  requestAnimationFrame(enhanceCurrentView);
}

function ensureMainObserver() {
  const main = document.querySelector(".shell > main");
  if (main === observedMain) return;
  mainObserver?.disconnect();
  observedMain = main;
  mainObserver = null;
  if (!main) return;
  mainObserver = new MutationObserver(scheduleEnhance);
  mainObserver.observe(main, { childList: true });
}

document.addEventListener("click", (event) => {
  const target = event.target instanceof Element ? event.target : null;
  if (!target?.closest(".tabs")) return;
  requestAnimationFrame(() => {
    ensureMainObserver();
    scheduleEnhance();
  });
}, false);

window.addEventListener("phase14ar-admin-ready", () => {
  ensureMainObserver();
  scheduleEnhance();
});

requestAnimationFrame(() => {
  ensureMainObserver();
  scheduleEnhance();
});
