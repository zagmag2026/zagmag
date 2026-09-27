import core from "./phase14be-operational-lifecycle.js";
import { getSessionUser, hasStaffPermission } from "./auth.ts";
import {
  WHATSAPP_LINKED_ACTIONS,
  WHATSAPP_STATUS_GROUP_LABELS,
  WHATSAPP_TEMPLATES_BY_STATUS_GROUP,
  normalizeWhatsAppGlobalLanguage,
  normalizeWhatsAppLinkedAction
} from "./whatsapp-template-registry.ts";

const RETURN_ACTIONS = new Set([
  "RETURN_DUE_TODAY",
  "RETURN_REMINDER",
  "PENDING_RETURN_REMINDER",
  "OVERDUE_REMINDER",
  "OVERDUE_FINAL_REMINDER",
  "RETURN_DONE",
  "PART_RETURN_DONE",
  "BOOKING_COMPLETED",
  "RETURN_DATE_TIME_UPDATE",
  "RETURN_THANK_YOU",
  "OVERDUE_URGENT_REMINDER",
  "OVERDUE_FOLLOW_UP_REMINDER"
]);

function apiJson(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
      "referrer-policy": "no-referrer"
    }
  });
}

function text(value, max = 1000) {
  return String(value ?? "").trim().slice(0, max);
}

function businessToday() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(new Date());
  const byType = Object.fromEntries(parts.map(part => [part.type, part.value]));
  return `${byType.year}-${byType.month}-${byType.day}`;
}

function formatBusinessTime(value) {
  const raw = text(value, 80);
  if (!raw) return "";
  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    hour: "numeric",
    minute: "2-digit",
    hour12: true
  }).format(date);
}

function parseSettings(raw) {
  let value = {};
  try { value = raw ? JSON.parse(String(raw)) : {}; } catch {}
  return {
    shopName: text(value.shopName || value.shopNameGu || value.shopNameEn || "ઝગમગ ડ્રેસીસ", 160),
    websiteTitle: text(value.websiteTitle, 160),
    websiteUrl: text(value.websiteUrl, 1000),
    contactNumber: text(value.contactNumber, 30),
    whatsappNumber: text(value.whatsappNumber, 30),
    showWhatsApp: value.showWhatsApp !== false,
    address: text(value.address, 500),
    availabilityMode: String(value.availabilityMode || "EXACT").toUpperCase() === "STATUS_ONLY" ? "STATUS_ONLY" : "EXACT",
    whatsappTemplateLanguage: normalizeWhatsAppGlobalLanguage(value.whatsappTemplateLanguage)
  };
}

function placeholderPattern() {
  return /(?:\{\{([a-z0-9_]+)\}\}|\{([a-z0-9_]+)\})/gi;
}

function fillTemplate(template, values) {
  return String(template || "").replace(placeholderPattern(), (full, doubleKey, singleKey) => {
    const key = String(doubleKey || singleKey || "").toLowerCase();
    return Object.prototype.hasOwnProperty.call(values, key)
      ? String(values[key] ?? "")
      : full;
  });
}

function unresolvedPlaceholders(message) {
  const values = new Set();
  for (const match of String(message || "").matchAll(placeholderPattern())) {
    const key = String(match[1] || match[2] || "").toLowerCase();
    if (key) values.add(key);
  }
  return [...values];
}

function referencedPlaceholders(message) {
  const values = new Set();
  for (const match of String(message || "").matchAll(placeholderPattern())) {
    const key = String(match[1] || match[2] || "").toLowerCase();
    if (key) values.add(key);
  }
  return [...values];
}

function renderMessage(template, values, globalLanguage) {
  const rawGu = template.message_gu || template.message_text || "";
  const rawEn = template.message_en || "";
  const referenced = referencedPlaceholders(`${rawGu}\n${rawEn}`);
  const missingValues = referenced.filter(key =>
    Object.prototype.hasOwnProperty.call(values, key) && String(values[key] ?? "").trim() === ""
  );
  if (missingValues.length) {
    return {
      error: `Template placeholder value is unavailable: ${missingValues.map(key => `{{${key}}}`).join(", ")}.`
    };
  }
  const gu = fillTemplate(rawGu, values).trim();
  const en = fillTemplate(rawEn, values).trim();
  const unresolved = unresolvedPlaceholders(`${gu}\n${en}`);
  if (unresolved.length) {
    return {
      error: `Template contains unresolved placeholder(s): ${unresolved.map(key => `{{${key}}}`).join(", ")}.`
    };
  }
  if (!gu || !en) return { error: "Gujarati and English template content are both required." };
  const languageMode = normalizeWhatsAppGlobalLanguage(globalLanguage);
  if (languageMode === "GUJARATI") return { languageMode, message: gu };
  if (languageMode === "ENGLISH") return { languageMode, message: en };
  return { languageMode: "BOTH", message: `${gu}\n\n${en}` };
}

function resolveAction(explicitContext, booking, today) {
  const explicit = normalizeWhatsAppLinkedAction(explicitContext);
  if (explicit) return explicit;
  if (!booking) return "GENERAL_INQUIRY";

  const status = String(booking.status || "").toUpperCase();
  const confirmation = String(booking.confirmation_state || "BOOKED").toUpperCase();
  const pendingPickup = Number(booking.pending_pickup_qty || 0);
  const pendingReturn = Number(booking.pending_return_qty || 0);

  if (status === "CANCELLED") {
    return confirmation === "RESERVED" ? "RESERVATION_CANCELLED" : "BOOKING_CANCELLED";
  }
  if (status === "RETURNED" && pendingPickup <= 0 && pendingReturn <= 0) return "RETURN_DONE";
  if (status === "PARTIALLY_RETURNED") return "PART_RETURN_DONE";
  if (pendingReturn > 0 && String(booking.return_date || "") < today) return "OVERDUE_REMINDER";
  if (status === "GIVEN") return "PICKUP_DONE";
  if (status === "PARTIALLY_GIVEN") return "PART_PICKUP_DONE";
  if (pendingPickup > 0 && String(booking.pickup_date || "") < today) return "MISSED_PICKUP_REMINDER";
  if (pendingPickup > 0) return "PICKUP_REMINDER";
  if (confirmation === "RESERVED") return "RESERVATION_CONFIRMATION";
  return "BOOKING_CONFIRMATION";
}

function resolveStatusGroup(booking, today) {
  if (!booking) return "OTHER";
  const status = String(booking.status || "").toUpperCase();
  const confirmation = String(booking.confirmation_state || "BOOKED").toUpperCase();
  const pendingPickup = Number(booking.pending_pickup_qty || 0);
  const pendingReturn = Number(booking.pending_return_qty || 0);
  const givenQty = Number(booking.given_qty || 0);

  if (status === "CANCELLED") return "CANCELLED";
  if (confirmation === "RESERVED" && givenQty <= 0) return "RESERVED";
  if (pendingReturn > 0 && String(booking.return_date || "") < today) return "OVERDUE";
  if (status === "RETURNED" && pendingPickup <= 0 && pendingReturn <= 0) return "RETURNED";
  if (status === "PARTIALLY_RETURNED") return "PART_RETURN";
  if (status === "GIVEN") return "PICKED_UP";
  if (status === "PARTIALLY_GIVEN") return "PART_PICKUP";
  return "CONFIRMED_BOOKED";
}

function applicableTemplateSpecs(statusGroup, booking, today, itemId = "") {
  let specs = [...(WHATSAPP_TEMPLATES_BY_STATUS_GROUP[statusGroup] || [])];
  if (!booking) {
    if (itemId) {
      specs = [
        { action: "ITEM_AVAILABILITY_REPLY", label: "Item Availability Reply" },
        ...specs
      ];
    }
    return specs;
  }

  const status = String(booking.status || "").toUpperCase();
  const confirmation = String(booking.confirmation_state || "BOOKED").toUpperCase();
  const pickupDate = String(booking.pickup_date || "");
  const returnDate = String(booking.return_date || "");
  const pendingPickup = Number(booking.pending_pickup_qty || 0);
  const pendingReturn = Number(booking.pending_return_qty || 0);

  if (status === "CANCELLED" && confirmation === "RESERVED") {
    return [
      { action: "RESERVATION_CANCELLED", label: "Reservation Cancelled" },
      { action: "CANCELLATION_DETAILS", label: "Cancellation Details" }
    ];
  }

  if (pickupDate !== today) {
    specs = specs.filter(spec => spec.action !== "PICKUP_DUE_TODAY");
  }
  if (returnDate !== today) {
    specs = specs.filter(spec => spec.action !== "RETURN_DUE_TODAY");
  }

  if (pendingPickup > 0 && pickupDate && pickupDate < today && confirmation !== "RESERVED") {
    specs = specs.filter(spec => !["PICKUP_DUE_TODAY", "PICKUP_REMINDER"].includes(spec.action));
    specs.unshift({ action: "MISSED_PICKUP_REMINDER", label: "Missed Pickup Reminder" });
  }

  if (statusGroup === "CONFIRMED_BOOKED") {
    specs.push({ action: "BOOKING_UPDATED", label: "Booking Updated" });
  }
  if (statusGroup === "RETURNED" && pendingPickup <= 0 && pendingReturn <= 0) {
    specs.push({ action: "BOOKING_COMPLETED", label: "Booking Completed" });
  }

  const seen = new Set();
  return specs.filter(spec => {
    if (seen.has(spec.action)) return false;
    seen.add(spec.action);
    return true;
  });
}

function availabilityStatusLabel(available) {
  if (available <= 0) return "Not Available";
  if (available <= 2) return "Few Left";
  if (available <= 5) return "Limited";
  return "Available";
}

function friendlyBookingStatus(booking) {
  if (!booking) return "";
  const confirmation = String(booking.confirmation_state || "BOOKED").toUpperCase();
  const status = String(booking.status || "BOOKED").toUpperCase();
  if (confirmation === "RESERVED" && Number(booking.given_qty || 0) === 0) return "Reserved";
  const labels = {
    BOOKED: "Booked",
    PARTIALLY_GIVEN: "Part Picked Up",
    GIVEN: "Full Picked Up",
    PARTIALLY_RETURNED: "Part Return",
    RETURNED: "Full Returned",
    CANCELLED: "Cancelled"
  };
  return labels[status] || status.replaceAll("_", " ");
}

function readableItemSummary(rows) {
  if (!rows.length) return "";
  return rows.map(row => {
    const name = text(row.item_name, 160);
    const qty = Number(row.booked_qty || 0);
    return rows.length > 1 ? `• ${name} ×${qty}` : `${name} ×${qty}`;
  }).join(rows.length > 1 ? "\n" : "");
}

function valuesForAction(baseValues, action, booking) {
  const pendingPickup = Number(booking?.pending_pickup_qty || 0);
  const pendingReturn = Number(booking?.pending_return_qty || 0);
  return {
    ...baseValues,
    pending_qty: RETURN_ACTIONS.has(action) ? pendingReturn : pendingPickup
  };
}

async function composeLinkedWhatsApp(request, env, user, customerId) {
  const url = new URL(request.url);
  const bookingId = text(url.searchParams.get("bookingId"), 100);
  const itemId = text(url.searchParams.get("itemId"), 100);
  const requestedPickupDate = text(url.searchParams.get("pickupDate"), 20);
  const requestedReturnDate = text(url.searchParams.get("returnDate"), 20);
  const validItemAvailabilityDates = /^\d{4}-\d{2}-\d{2}$/.test(requestedPickupDate) &&
    /^\d{4}-\d{2}-\d{2}$/.test(requestedReturnDate) && requestedPickupDate <= requestedReturnDate;
  const explicitContext = text(url.searchParams.get("context"), 80).toUpperCase();
  const mode = text(url.searchParams.get("mode"), 30).toLowerCase();

  if (explicitContext && !WHATSAPP_LINKED_ACTIONS.includes(explicitContext)) {
    return apiJson({ ok: false, error: "INVALID_CONTEXT", message: "WhatsApp template context is invalid." }, 400);
  }
  if (mode && mode !== "templates") {
    return apiJson({ ok: false, error: "INVALID_MODE", message: "WhatsApp request mode is invalid." }, 400);
  }

  const statements = [
    env.DB.prepare("SELECT id,name,mobile,address FROM customers WHERE id=? LIMIT 1").bind(customerId),
    env.DB.prepare("SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1"),
    env.DB.prepare(`
      SELECT id,template_key,template_name,message_text,message_gu,message_en,linked_action,is_active
      FROM whatsapp_templates
      WHERE is_active=1
      ORDER BY template_name COLLATE NOCASE
    `)
  ];

  if (bookingId) {
    statements.push(
      env.DB.prepare(`
        SELECT b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,b.confirmation_state,b.notes,
               u.name AS booked_by,
               COALESCE(SUM(bi.booked_qty),0) AS booked_qty,
               COALESCE(SUM(bi.given_qty),0) AS given_qty,
               COALESCE(SUM(bi.returned_qty),0) AS returned_qty,
               COALESCE(SUM(CASE WHEN bi.booked_qty-bi.closed_qty>bi.given_qty THEN bi.booked_qty-bi.closed_qty-bi.given_qty ELSE 0 END),0) AS pending_pickup_qty,
               COALESCE(SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS pending_return_qty
        FROM bookings b
        LEFT JOIN booking_items bi ON bi.booking_id=b.id
        LEFT JOIN users u ON u.id=b.created_by_user_id
        WHERE b.id=? AND b.customer_id=?
        GROUP BY b.id
      `).bind(bookingId, customerId),
      env.DB.prepare(`
        SELECT i.id AS item_id,i.item_name,i.item_code,c.name AS category_name,
               bi.booked_qty,bi.closed_qty,bi.given_qty,bi.returned_qty
        FROM booking_items bi
        JOIN items i ON i.id=bi.item_id
        JOIN categories c ON c.id=i.category_id
        WHERE bi.booking_id=?
        ORDER BY i.item_name COLLATE NOCASE
      `).bind(bookingId),
      env.DB.prepare(`
        SELECT DISTINCT related.item_name
        FROM booking_items bi
        JOIN item_related_items rel ON rel.source_item_id=bi.item_id
        JOIN items related ON related.id=rel.related_item_id
        WHERE bi.booking_id=? AND related.archived_at IS NULL
        ORDER BY related.item_name COLLATE NOCASE
      `).bind(bookingId),
      env.DB.prepare("SELECT MAX(pickup_at) AS event_at FROM pickup_events WHERE booking_id=?").bind(bookingId),
      env.DB.prepare("SELECT MAX(return_at) AS event_at FROM return_events WHERE booking_id=?").bind(bookingId)
    );
  } else if (itemId) {
    statements.push(
      env.DB.prepare(`
        SELECT i.id AS item_id,i.item_name,i.item_code,c.name AS category_name,i.total_quantity AS booked_qty,
               0 AS closed_qty,0 AS given_qty,0 AS returned_qty
        FROM items i JOIN categories c ON c.id=i.category_id
        WHERE i.id=? AND i.archived_at IS NULL
        LIMIT 1
      `).bind(itemId),
      env.DB.prepare(`
        SELECT DISTINCT related.item_name
        FROM item_related_items rel
        JOIN items related ON related.id=rel.related_item_id
        WHERE rel.source_item_id=? AND related.archived_at IS NULL
        ORDER BY related.item_name COLLATE NOCASE
      `).bind(itemId)
    );
    if (validItemAvailabilityDates) {
      statements.push(
        env.DB.prepare(`
          SELECT i.id,i.total_quantity,
            COALESCE(SUM(CASE
              WHEN b.id IS NULL THEN 0
              WHEN b.pickup_date<=? AND b.return_date>=? THEN bi.booked_qty-bi.closed_qty
              WHEN b.return_date<? AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty
              ELSE 0 END),0) AS reserved_qty
          FROM items i
          LEFT JOIN booking_items bi ON bi.item_id=i.id
          LEFT JOIN bookings b ON b.id=bi.booking_id AND b.status NOT IN ('CANCELLED','RETURNED')
          WHERE i.id=?
          GROUP BY i.id
        `).bind(requestedReturnDate,requestedPickupDate,requestedPickupDate,itemId)
      );
    }
  }

  const results = await env.DB.batch(statements);
  const customer = results[0]?.results?.[0];
  if (!customer) return apiJson({ ok: false, error: "NOT_FOUND", message: "Customer not found." }, 404);

  const settings = parseSettings(results[1]?.results?.[0]?.value_json);
  const templates = results[2]?.results || [];
  let booking = null;
  let itemRows = [];
  let relatedRows = [];
  let pickupEventAt = "";
  let returnEventAt = "";
  let itemAvailabilityRow = null;

  if (bookingId) {
    booking = results[3]?.results?.[0] || null;
    if (!booking) {
      return apiJson({ ok: false, error: "BOOKING_NOT_FOUND", message: "Booking not found for this customer." }, 404);
    }
    itemRows = results[4]?.results || [];
    relatedRows = results[5]?.results || [];
    pickupEventAt = results[6]?.results?.[0]?.event_at || "";
    returnEventAt = results[7]?.results?.[0]?.event_at || "";
  } else if (itemId) {
    itemRows = results[3]?.results || [];
    relatedRows = results[4]?.results || [];
    itemAvailabilityRow = validItemAvailabilityDates ? (results[5]?.results?.[0] || null) : null;
  }

  const today = businessToday();
  let overdueDays = 0;
  if (booking?.return_date && String(booking.return_date) < today) {
    overdueDays = Math.max(
      1,
      Math.floor((Date.parse(`${today}T00:00:00Z`) - Date.parse(`${booking.return_date}T00:00:00Z`)) / 86400000)
    );
  }

  const itemCodes = [...new Set(itemRows.map(row => text(row.item_code, 80)).filter(Boolean))];
  const categories = [...new Set(itemRows.map(row => text(row.category_name, 160)).filter(Boolean))];
  const relatedItems = [...new Set(relatedRows.map(row => text(row.item_name, 160)).filter(Boolean))];
  const totalQty = booking
    ? Number(booking.booked_qty || 0)
    : itemRows.reduce((sum, row) => sum + Number(row.booked_qty || 0), 0);
  const readableItems = readableItemSummary(itemRows);
  const friendlyStatus = friendlyBookingStatus(booking);
  const itemAvailableQuantity = itemAvailabilityRow
    ? Math.max(0, Number(itemAvailabilityRow.total_quantity || 0) - Number(itemAvailabilityRow.reserved_qty || 0))
    : null;
  const itemAvailabilityStatus = itemAvailableQuantity === null ? "" : availabilityStatusLabel(itemAvailableQuantity);

  const baseValues = {
    shop_name: settings.shopName,
    business_name: settings.shopName,
    shop_phone: settings.contactNumber,
    shop_whatsapp: settings.whatsappNumber,
    shop_address: settings.address,
    website_title: settings.websiteTitle,
    website_url: settings.websiteUrl,
    customer_name: customer.name || "",
    customer_mobile: customer.mobile || "",
    customer_address: customer.address || "",
    booking_no: booking?.booking_no || "",
    booking_id: booking?.booking_no || "",
    booking_status: friendlyStatus,
    status: friendlyStatus,
    booking_date: booking?.booking_date || "",
    booking_notes: booking?.notes || "",
    booked_by: booking?.booked_by || "",
    pickup_date: booking?.pickup_date || "",
    return_date: booking?.return_date || "",
    pickup_time: formatBusinessTime(pickupEventAt),
    return_time: formatBusinessTime(returnEventAt),
    item_name: readableItems,
    items: readableItems,
    item_code: itemCodes.join(", "),
    category_name: categories.join(", "),
    qty: totalQty,
    availability_status: itemAvailabilityStatus,
    available_qty: settings.availabilityMode === "EXACT" && itemAvailableQuantity !== null ? itemAvailableQuantity : "",
    pending_qty: 0,
    picked_qty: Number(booking?.given_qty || 0),
    returned_qty: Number(booking?.returned_qty || 0),
    overdue_days: overdueDays,
    related_items: relatedItems.join(", "),
    today_date: today,
    staff_name: user?.name || ""
  };

  if (mode === "templates") {
    const statusGroup = resolveStatusGroup(booking, today);
    const specs = applicableTemplateSpecs(statusGroup, booking, today, itemId);
    const renderedTemplates = [];
    const invalidTemplates = [];

    for (const spec of specs) {
      const template = templates.find(row => String(row.linked_action || "").toUpperCase() === spec.action);
      if (!template) continue;
      const rendered = renderMessage(
        template,
        valuesForAction(baseValues, spec.action, booking),
        settings.whatsappTemplateLanguage
      );
      if (rendered.error || !rendered.message) {
        invalidTemplates.push({
          linkedAction: spec.action,
          reason: rendered.error || "Template message is empty."
        });
        continue;
      }
      renderedTemplates.push({
        templateKey: template.template_key,
        templateName: template.template_name || spec.label,
        configuredName: template.template_name,
        linkedAction: spec.action,
        languageMode: rendered.languageMode,
        message: rendered.message
      });
    }

    if (!renderedTemplates.length) {
      return apiJson({
        ok: false,
        error: invalidTemplates.length ? "TEMPLATE_INVALID" : "TEMPLATE_MISSING",
        message: invalidTemplates.length
          ? `No valid active WhatsApp template is available for ${WHATSAPP_STATUS_GROUP_LABELS[statusGroup] || statusGroup}.`
          : `No active WhatsApp template is available for ${WHATSAPP_STATUS_GROUP_LABELS[statusGroup] || statusGroup}.`
      }, 409);
    }

    return apiJson({
      ok: true,
      customerName: customer.name || "",
      mobile: customer.mobile,
      statusGroup,
      statusGroupLabel: WHATSAPP_STATUS_GROUP_LABELS[statusGroup] || statusGroup,
      templates: renderedTemplates
    });
  }

  const linkedAction = resolveAction(explicitContext, booking, today);
  const template = templates.find(row => String(row.linked_action || "").toUpperCase() === linkedAction);
  if (!template) {
    return apiJson({
      ok: false,
      error: "TEMPLATE_MISSING",
      message: "No active WhatsApp template is linked to this action."
    }, 409);
  }

  const rendered = renderMessage(
    template,
    valuesForAction(baseValues, linkedAction, booking),
    settings.whatsappTemplateLanguage
  );
  if (rendered.error) {
    return apiJson({ ok: false, error: "TEMPLATE_UNRESOLVED", message: rendered.error }, 409);
  }
  if (!rendered.message) {
    return apiJson({ ok: false, error: "TEMPLATE_EMPTY", message: "WhatsApp template message is empty." }, 409);
  }

  return apiJson({
    ok: true,
    customerName: customer.name || "",
    mobile: customer.mobile,
    templateKey: template.template_key,
    templateName: template.template_name,
    linkedAction,
    languageMode: rendered.languageMode,
    message: rendered.message
  });
}

async function composePublicWhatsAppInquiry(request, env) {
  const url = new URL(request.url);
  const itemId = text(url.searchParams.get("itemId"), 100);
  const pickupDate = text(url.searchParams.get("pickupDate"), 20);
  const returnDate = text(url.searchParams.get("returnDate"), 20);

  const statements = [
    env.DB.prepare("SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1"),
    env.DB.prepare(`
      SELECT id,template_key,template_name,message_text,message_gu,message_en,linked_action,is_active
      FROM whatsapp_templates
      WHERE is_active=1 AND UPPER(linked_action)='GENERAL_INQUIRY'
      ORDER BY updated_at DESC, template_name COLLATE NOCASE
      LIMIT 1
    `)
  ];
  const validAvailabilityDates = /^\d{4}-\d{2}-\d{2}$/.test(pickupDate) &&
    /^\d{4}-\d{2}-\d{2}$/.test(returnDate) && pickupDate <= returnDate;
  if (itemId) {
    statements.push(
      env.DB.prepare(`
        SELECT i.id AS item_id,i.item_name,i.item_code,c.name AS category_name
        FROM items i
        JOIN categories c ON c.id=i.category_id
        WHERE i.id=? AND i.archived_at IS NULL AND i.is_active=1 AND i.public_visible=1
          AND c.is_active=1 AND c.public_visible=1
        LIMIT 1
      `).bind(itemId),
      env.DB.prepare(`
        SELECT DISTINCT related.item_name
        FROM item_related_items rel
        JOIN items related ON related.id=rel.related_item_id
        WHERE rel.source_item_id=? AND related.archived_at IS NULL
        ORDER BY related.item_name COLLATE NOCASE
      `).bind(itemId)
    );
    if (validAvailabilityDates) {
      statements.push(
        env.DB.prepare(`
          SELECT i.id,i.total_quantity,
            COALESCE(SUM(CASE
              WHEN b.id IS NULL THEN 0
              WHEN b.pickup_date<=? AND b.return_date>=? THEN bi.booked_qty-bi.closed_qty
              WHEN b.return_date<? AND bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty
              ELSE 0 END),0) AS reserved_qty
          FROM items i
          LEFT JOIN booking_items bi ON bi.item_id=i.id
          LEFT JOIN bookings b ON b.id=bi.booking_id AND b.status NOT IN ('CANCELLED','RETURNED')
          WHERE i.id=?
          GROUP BY i.id
        `).bind(returnDate,pickupDate,pickupDate,itemId)
      );
    }
  }

  const results = await env.DB.batch(statements);
  const settings = parseSettings(results[0]?.results?.[0]?.value_json);
  if (!settings.showWhatsApp || !settings.whatsappNumber) {
    return apiJson({
      ok: false,
      error: "WHATSAPP_UNAVAILABLE",
      message: "WhatsApp inquiry is currently unavailable."
    }, 409);
  }

  const template = results[1]?.results?.[0];
  if (!template) {
    return apiJson({
      ok: false,
      error: "TEMPLATE_MISSING",
      message: "The WhatsApp inquiry template is currently unavailable."
    }, 409);
  }

  const item = itemId ? (results[2]?.results?.[0] || null) : null;
  const relatedRows = itemId ? (results[3]?.results || []) : [];
  const availabilityRow = itemId && validAvailabilityDates ? (results[4]?.results?.[0] || null) : null;
  if (itemId && !item) {
    return apiJson({ ok: false, error: "ITEM_NOT_FOUND", message: "This item is not available for inquiry." }, 404);
  }

  const today = businessToday();
  const itemName = item ? text(item.item_name, 160) : "";
  const itemCode = item ? text(item.item_code, 80) : "";
  const categoryName = item ? text(item.category_name, 160) : "";
  const relatedItems = [...new Set(relatedRows.map(row => text(row.item_name, 160)).filter(Boolean))];
  const availableQuantity = availabilityRow
    ? Math.max(0, Number(availabilityRow.total_quantity || 0) - Number(availabilityRow.reserved_qty || 0))
    : null;
  const availabilityStatus = availableQuantity === null ? "" : availabilityStatusLabel(availableQuantity);
  const values = {
    shop_name: settings.shopName,
    business_name: settings.shopName,
    shop_phone: settings.contactNumber,
    shop_whatsapp: settings.whatsappNumber,
    shop_address: settings.address,
    website_title: settings.websiteTitle,
    website_url: settings.websiteUrl,
    customer_name: "",
    customer_mobile: "",
    customer_address: "",
    booking_no: "",
    booking_id: "",
    booking_status: "General Inquiry",
    status: "General Inquiry",
    booking_date: "",
    booking_notes: "",
    booked_by: "",
    pickup_date: /^\d{4}-\d{2}-\d{2}$/.test(pickupDate) ? pickupDate : "",
    return_date: /^\d{4}-\d{2}-\d{2}$/.test(returnDate) ? returnDate : "",
    pickup_time: "",
    return_time: "",
    item_name: itemName,
    items: itemName,
    item_code: itemCode,
    category_name: categoryName,
    qty: item ? 1 : 0,
    availability_status: availabilityStatus,
    available_qty: settings.availabilityMode === "EXACT" && availableQuantity !== null ? availableQuantity : "",
    pending_qty: 0,
    picked_qty: 0,
    returned_qty: 0,
    overdue_days: 0,
    related_items: relatedItems.join(", "),
    today_date: today,
    staff_name: ""
  };

  const rendered = renderMessage(template, values, settings.whatsappTemplateLanguage);
  if (rendered.error || !rendered.message) {
    return apiJson({
      ok: false,
      error: "TEMPLATE_INVALID",
      message: "The WhatsApp inquiry template is currently unavailable."
    }, 409);
  }

  return apiJson({
    ok: true,
    mobile: settings.whatsappNumber,
    templateKey: template.template_key,
    templateName: template.template_name,
    linkedAction: "GENERAL_INQUIRY",
    languageMode: rendered.languageMode,
    message: rendered.message
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (url.pathname === "/api/public/whatsapp-inquiry" && request.method === "GET") {
      return composePublicWhatsAppInquiry(request, env);
    }
    const whatsappMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/whatsapp$/);
    if (whatsappMatch && request.method === "GET") {
      const user = await getSessionUser(env, request);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      const bookingId = text(url.searchParams.get("bookingId"), 100);
      const itemId = text(url.searchParams.get("itemId"), 100);
      const permitted = bookingId
        ? (hasStaffPermission(user, "BOOKINGS") || hasStaffPermission(user, "DASHBOARD"))
        : itemId
          ? hasStaffPermission(user, "ITEMS")
          : hasStaffPermission(user, "CUSTOMERS");
      if (!permitted) {
        return apiJson({ ok: false, error: "FORBIDDEN", message: "This WhatsApp action is not enabled for your Staff account." }, 403);
      }
      return composeLinkedWhatsApp(request, env, user, decodeURIComponent(whatsappMatch[1]));
    }
    return core.fetch(request, env, ctx);
  }
};
