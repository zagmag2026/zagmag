/* Phase 14X Fix10 UI3 Performance Fix2 — compact mobile queues + Customer/Category ERP polish.
   UI-only. Idempotent DOM decoration; no fetch/API calls and no self-triggering observer loop. */
import "./phase14x-ui3.css";

const normalize = value => String(value || "").replace(/\s+/g, " ").trim();
let scheduledFrame = 0;

function pageTitle() {
  return normalize(document.querySelector(".page-head h1")?.textContent);
}

function addClasses(node, ...classes) {
  if (!(node instanceof Element)) return;
  const missing = classes.filter(name => name && !node.classList.contains(name));
  if (missing.length) node.classList.add(...missing);
}

function setTextIfChanged(node, value) {
  if (!(node instanceof Node)) return;
  const next = String(value ?? "");
  if (node.textContent !== next) node.textContent = next;
}

function setHrefIfChanged(anchor, value) {
  if (!(anchor instanceof HTMLAnchorElement)) return;
  const next = String(value || "");
  if (anchor.getAttribute("href") !== next) anchor.setAttribute("href", next);
}

function centerActiveTab(tabs, active, smooth = true) {
  if (!(tabs instanceof HTMLElement) || !(active instanceof HTMLElement)) return;
  const max = Math.max(0, tabs.scrollWidth - tabs.clientWidth);
  const target = Math.max(0, Math.min(max, active.offsetLeft - (tabs.clientWidth - active.offsetWidth) / 2));
  if (Math.abs(tabs.scrollLeft - target) < 2) return;
  tabs.scrollTo({ left: target, behavior: smooth ? "smooth" : "auto" });
}

function decorateQueueTabs() {
  document.querySelectorAll(".pickup-toolbar").forEach(toolbar => {
    if (!(toolbar instanceof HTMLElement)) return;
    const tabs = toolbar.querySelector(":scope > .pickup-tabs");
    const search = toolbar.querySelector(":scope > .pickup-search");
    if (!(tabs instanceof HTMLElement)) return;

    if (toolbar.dataset.phase14xUi3Queue !== "1") {
      toolbar.dataset.phase14xUi3Queue = "1";
      addClasses(toolbar, "phase14x-ui3-queue-toolbar");
      addClasses(tabs, "phase14x-ui3-scroll-tabs");
      if (search instanceof HTMLElement) addClasses(search, "phase14x-ui3-queue-search");
    }

    const active = tabs.querySelector(":scope > button.active");
    const key = normalize(active?.textContent);
    if (active instanceof HTMLElement && key && tabs.dataset.phase14xUi3Active !== key) {
      tabs.dataset.phase14xUi3Active = key;
      requestAnimationFrame(() => centerActiveTab(tabs, active, false));
    }
  });
}

function splitCustomerContact(value) {
  const text = normalize(value);
  const match = text.match(/^(.*?)\s*[·•]\s*([+\d][\d\s()+-]{6,})$/);
  if (!match) return { name: text, mobile: "" };
  return { name: normalize(match[1]), mobile: normalize(match[2]) };
}

function isBookingMetric(span) {
  if (!(span instanceof HTMLElement)) return false;
  if (span.classList.contains("phase14x-ui3-booking-metric")) return true;
  const direct = normalize([...span.childNodes]
    .filter(node => node.nodeType === Node.TEXT_NODE)
    .map(node => node.textContent || "")
    .join(" "));
  const nested = normalize(span.querySelector(":scope > em")?.textContent);
  return /^Booking$/i.test(direct) || /^Booking$/i.test(nested);
}

function ensureBookingMetric(card, bookingNo) {
  const metrics = card.querySelector(":scope > .dashboard-meta");
  if (!(metrics instanceof HTMLElement) || !bookingNo) return;

  /* Fix2: retain exactly one Booking metric. This also repairs cards already polluted by
     the previous runtime, where every observer pass inserted another generated chip. */
  const generated = [...metrics.querySelectorAll(":scope > .phase14x-ui3-booking-metric")];
  const legacy = [...metrics.querySelectorAll(":scope > span")]
    .filter(span => isBookingMetric(span) && !generated.includes(span));
  const candidates = [...generated, ...legacy];

  let metric = candidates[0];
  candidates.slice(1).forEach(node => node.remove());

  if (!(metric instanceof HTMLElement)) {
    metric = document.createElement("span");
    metric.className = "phase14x-ui3-booking-metric";
    metrics.prepend(metric);
  } else {
    addClasses(metric, "phase14x-ui3-booking-metric");
    if (metrics.firstElementChild !== metric) metrics.prepend(metric);
  }

  let label = metric.querySelector(":scope > em");
  let value = metric.querySelector(":scope > b");
  const directText = normalize([...metric.childNodes]
    .filter(node => node.nodeType === Node.TEXT_NODE)
    .map(node => node.textContent || "")
    .join(" "));

  if (!(label instanceof HTMLElement) || !(value instanceof HTMLElement) || directText) {
    label = document.createElement("em");
    value = document.createElement("b");
    metric.replaceChildren(label, value);
  }
  setTextIfChanged(label, "Booking");
  setTextIfChanged(value, bookingNo);
}

function matchMissedDashboardCard(card) {
  if (!(card instanceof HTMLElement)) return;
  addClasses(card, "phase14x-ui3-missed-card");

  const head = card.querySelector(":scope > .phase14x-missed-identity,:scope > .booking-card-head,:scope > .dashboard-row-head");
  if (!(head instanceof HTMLElement)) return;
  addClasses(head, "dashboard-row-head", "phase14x-ui3-missed-head");

  if (card.dataset.phase14xUi3MissedMatched === "1") {
    ensureBookingMetric(card, normalize(card.dataset.phase14xUi3BookingNo || card.dataset.bookingNo));
    return;
  }

  const identity = head.querySelector(":scope > div");
  if (!(identity instanceof HTMLElement)) return;
  const strong = identity.querySelector(":scope > strong");
  const contactNode = identity.querySelector(":scope > span,:scope > a");
  if (!(strong instanceof HTMLElement) || !(contactNode instanceof HTMLElement)) return;

  const existingBooking = normalize(card.dataset.bookingNo || card.dataset.phase14xUi3BookingNo || strong.textContent);
  const bookingNo = /^BK-/i.test(existingBooking) ? existingBooking : normalize(card.dataset.phase14xUi3BookingNo);
  const parsed = splitCustomerContact(contactNode.textContent);

  if (parsed.name && bookingNo && /^BK-/i.test(normalize(strong.textContent))) {
    card.dataset.phase14xUi3BookingNo = bookingNo;
    setTextIfChanged(strong, parsed.name);
  }

  if (parsed.mobile) {
    let mobile = identity.querySelector(":scope > a.phase14x-ui3-mobile");
    if (!(mobile instanceof HTMLAnchorElement)) {
      mobile = document.createElement("a");
      mobile.className = "phase14x-ui3-mobile";
      contactNode.replaceWith(mobile);
    }
    setTextIfChanged(mobile, parsed.mobile);
    setHrefIfChanged(mobile, `tel:${parsed.mobile.replace(/\s+/g, "")}`);
  }

  ensureBookingMetric(card, card.dataset.phase14xUi3BookingNo || bookingNo);
  card.dataset.phase14xUi3MissedMatched = "1";
}

function polishMissedCards() {
  document.querySelectorAll(".phase14x-missed-panel .dashboard-row").forEach(matchMissedDashboardCard);
  document.querySelectorAll(".phase14x-missed-list .pickup-card").forEach(card => {
    if (card instanceof HTMLElement) addClasses(card, "phase14x-ui3-missed-pickup-card");
  });
}

function decorateCustomerPage() {
  const list = document.querySelector(".list-card");
  if (!(list instanceof HTMLElement)) return;
  addClasses(list, "phase14x-ui3-customer-list");

  const head = list.querySelector(":scope > .list-head");
  if (head instanceof HTMLElement) addClasses(head, "phase14x-ui3-customer-head");

  const grid = list.querySelector(":scope > .customer-grid");
  if (grid instanceof HTMLElement) addClasses(grid, "phase14x-ui3-customer-grid");

  list.querySelectorAll(".customer-card").forEach(card => {
    if (!(card instanceof HTMLElement) || card.dataset.phase14xUi3Customer === "1") return;
    card.dataset.phase14xUi3Customer = "1";
    addClasses(card, "phase14x-ui3-customer-card");
    addClasses(card.querySelector(".customer-head"), "phase14x-ui3-customer-card-head");
    addClasses(card.querySelector(".customer-stats"), "phase14x-ui3-customer-stats");
    addClasses(card.querySelector(".quick-actions"), "phase14x-ui3-customer-quick");
    addClasses(card.querySelector(".row-actions"), "phase14x-ui3-customer-more");
  });
}

function categoryGlyph(name) {
  const value = normalize(name).toLowerCase();
  if (/(choli|dress|lehenga|saree|sari|gown|kurta|sherwani|costume|clothes|apparel)/.test(value)) return "checkroom";
  if (/(jewel|ornament|diamond)/.test(value)) return "diamond";
  if (/(shoe|footwear|sandal)/.test(value)) return "steps";
  if (/(bag|purse)/.test(value)) return "shopping_bag";
  return "category";
}

function decorateCategoryStats() {
  const stats = document.querySelector(".mini-stats");
  if (!(stats instanceof HTMLElement)) return;
  addClasses(stats, "phase14x-ui3-category-stats");
  const icons = new Map([
    ["Total Categories", "category"],
    ["Active", "check_circle"],
    ["Public", "visibility"],
    ["Custom Fields", "tune"]
  ]);
  stats.querySelectorAll(":scope > article").forEach(article => {
    if (!(article instanceof HTMLElement)) return;
    const label = normalize(article.querySelector("span")?.textContent);
    const icon = icons.get(label) || "analytics";
    if (article.dataset.phase14xUi3StatIcon !== icon) article.dataset.phase14xUi3StatIcon = icon;
  });
}

function decorateCategoryCard(main) {
  if (!(main instanceof HTMLElement)) return;
  const copy = main.querySelector(":scope > .category-copy");
  if (!(copy instanceof HTMLElement)) return;
  addClasses(main, "phase14x-ui3-category-main");
  addClasses(copy, "phase14x-ui3-category-copy");

  const name = normalize(copy.querySelector(":scope > strong")?.textContent);
  let icon = main.querySelector(":scope > .phase14x-ui3-category-card-icon");
  if (!(icon instanceof HTMLElement)) {
    icon = document.createElement("span");
    icon.className = "phase14x-ui3-category-card-icon";
    icon.setAttribute("aria-hidden", "true");
    main.prepend(icon);
  }
  const glyph = categoryGlyph(name);
  if (icon.textContent !== glyph) icon.textContent = glyph;
  if (main.dataset.phase14xUi3CategoryName !== name) main.dataset.phase14xUi3CategoryName = name;

  const prefix = main.querySelector(":scope > .prefix") || copy.querySelector(":scope > .prefix");
  if (prefix instanceof HTMLElement) {
    addClasses(prefix, "phase14x-ui3-category-prefix");
    if (prefix.parentElement !== copy) {
      const title = copy.querySelector(":scope > strong");
      if (title) title.insertAdjacentElement("afterend", prefix);
      else copy.prepend(prefix);
    }
  }

  const actions = main.querySelector(":scope > .row-actions");
  if (actions instanceof HTMLElement) addClasses(actions, "phase14x-ui3-category-actions");
}

function decorateCategoryPage() {
  decorateCategoryStats();
  const list = [...document.querySelectorAll(".list-card")].find(node =>
    normalize(node.querySelector(".list-head h2")?.textContent) === "Categories"
  );
  if (list instanceof HTMLElement) addClasses(list, "phase14x-ui3-category-list");
  addClasses(list?.querySelector(".list-head"), "phase14x-ui3-category-toolbar");
  list?.querySelectorAll(".category-main").forEach(decorateCategoryCard);
}

function scan() {
  scheduledFrame = 0;
  const title = pageTitle();
  if (title === "Pickup" || title === "Returns") decorateQueueTabs();
  if (title === "Dashboard" || title === "Pickup") polishMissedCards();
  if (title === "Customers") decorateCustomerPage();
  if (title === "Categories & Custom Fields") decorateCategoryPage();
}

function schedule() {
  if (scheduledFrame) return;
  scheduledFrame = requestAnimationFrame(scan);
}

document.addEventListener("click", event => {
  const button = event.target instanceof Element ? event.target.closest(".pickup-tabs button") : null;
  if (!button) return;
  requestAnimationFrame(() => {
    const tabs = button.closest(".pickup-tabs");
    const active = tabs?.querySelector("button.active");
    if (tabs instanceof HTMLElement && active instanceof HTMLElement) {
      const key = normalize(active.textContent);
      if (tabs.dataset.phase14xUi3Active !== key) tabs.dataset.phase14xUi3Active = key;
      centerActiveTab(tabs, active, true);
    }
  });
}, true);

window.addEventListener("resize", schedule);
new MutationObserver(schedule).observe(document.documentElement, {
  childList: true,
  subtree: true
});
schedule();
