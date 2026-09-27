import core from "./phase14bb-screen9-users.js";
import { getSessionUser } from "./auth.ts";

const LINKED_ACTIONS = new Set([
  "GENERAL_INQUIRY",
  "BOOKING_CONFIRMATION",
  "PICKUP_REMINDER",
  "MISSED_PICKUP_REMINDER",
  "RETURN_REMINDER",
  "OVERDUE_REMINDER",
  "PICKUP_DONE",
  "PART_PICKUP_DONE",
  "RETURN_DONE",
  "PART_RETURN_DONE",
  "THANK_YOU"
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

function text(value, max = 500) {
  return String(value ?? "").trim().slice(0, max);
}

function businessToday() {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: "Asia/Kolkata",
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(new Date());
  const byType = Object.fromEntries(parts.map(part => [part.type, part.value]));
  return `${byType.year}-${byType.month}-${byType.day}`;
}

function fillTemplate(template, values) {
  return String(template || "").replace(/\{([a-z0-9_]+)\}/gi, (_, key) => String(values[key] ?? ""));
}

function resolveAction(explicitContext, booking, today) {
  if (explicitContext && LINKED_ACTIONS.has(explicitContext)) return explicitContext;
  if (!booking) return "GENERAL_INQUIRY";

  const status = String(booking.status || "").toUpperCase();
  const pendingPickup = Number(booking.pending_pickup_qty || 0);
  const pendingReturn = Number(booking.pending_return_qty || 0);

  if (status === "RETURNED") return "RETURN_DONE";
  if (status === "PARTIALLY_RETURNED") return "PART_RETURN_DONE";
  if (pendingReturn > 0 && String(booking.return_date || "") < today) return "OVERDUE_REMINDER";
  if (status === "GIVEN") return "PICKUP_DONE";
  if (status === "PARTIALLY_GIVEN") return "PART_PICKUP_DONE";
  if (pendingPickup > 0 && String(booking.pickup_date || "") < today) return "MISSED_PICKUP_REMINDER";
  if (pendingPickup > 0) return "PICKUP_REMINDER";
  return "BOOKING_CONFIRMATION";
}

function renderMessage(template, values) {
  const languageMode = String(template.language_mode || "ALL").toUpperCase();
  const gu = fillTemplate(template.message_gu || template.message_text || "", values).trim();
  const en = fillTemplate(template.message_en || "", values).trim();

  if (languageMode === "GUJARATI") return { languageMode, message: gu };
  if (languageMode === "ENGLISH") return { languageMode, message: en };
  return { languageMode: "ALL", message: [gu, en].filter(Boolean).join("\n\n") };
}

function parseShopName(raw, fallback) {
  try {
    const settings = raw ? JSON.parse(String(raw)) : {};
    return text(settings.shopNameGu || settings.shopName || fallback || "ઝગમગ ડ્રેસીસ", 160);
  } catch {
    return text(fallback || "ઝગમગ ડ્રેસીસ", 160);
  }
}

async function composeLinkedWhatsApp(request, env, customerId) {
  const url = new URL(request.url);
  const bookingId = text(url.searchParams.get("bookingId"), 100);
  const explicitContext = text(url.searchParams.get("context"), 80).toUpperCase();
  if (explicitContext && !LINKED_ACTIONS.has(explicitContext)) {
    return apiJson({ ok: false, error: "INVALID_CONTEXT", message: "WhatsApp template context is invalid." }, 400);
  }

  const statements = [
    env.DB.prepare(`SELECT id,name,mobile FROM customers WHERE id=? LIMIT 1`).bind(customerId),
    env.DB.prepare(`SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1`),
    env.DB.prepare(`
      SELECT id,template_key,template_name,message_text,message_gu,message_en,language_mode,linked_action,is_active
      FROM whatsapp_templates
      WHERE is_active=1
      ORDER BY template_name COLLATE NOCASE
    `)
  ];

  if (bookingId) {
    statements.push(
      env.DB.prepare(`
        SELECT b.id,b.booking_no,b.pickup_date,b.return_date,b.status,b.confirmation_state,
               COALESCE(SUM(CASE WHEN bi.booked_qty>bi.given_qty THEN bi.booked_qty-bi.given_qty ELSE 0 END),0) AS pending_pickup_qty,
               COALESCE(SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END),0) AS pending_return_qty,
               COALESCE(SUM(bi.booked_qty),0) AS booked_qty,
               COALESCE(GROUP_CONCAT(i.item_name, ', '),'') AS item_name
        FROM bookings b
        LEFT JOIN booking_items bi ON bi.booking_id=b.id
        LEFT JOIN items i ON i.id=bi.item_id
        WHERE b.id=? AND b.customer_id=?
        GROUP BY b.id
      `).bind(bookingId, customerId)
    );
  }

  const results = await env.DB.batch(statements);
  const customer = results[0]?.results?.[0];
  if (!customer) return apiJson({ ok: false, error: "NOT_FOUND", message: "Customer not found." }, 404);

  const settingsRow = results[1]?.results?.[0];
  const templates = results[2]?.results || [];
  const booking = bookingId ? results[3]?.results?.[0] : null;
  if (bookingId && !booking) {
    return apiJson({ ok: false, error: "BOOKING_NOT_FOUND", message: "Booking not found for this customer." }, 404);
  }

  const today = businessToday();
  const linkedAction = resolveAction(explicitContext, booking, today);
  const template = templates.find(row => String(row.linked_action || "").toUpperCase() === linkedAction);
  if (!template) {
    return apiJson({
      ok: false,
      error: "TEMPLATE_MISSING",
      message: "No active WhatsApp template is linked to this action."
    }, 409);
  }

  let overdueDays = 0;
  if (booking?.return_date && String(booking.return_date) < today) {
    overdueDays = Math.max(
      1,
      Math.floor(
        (Date.parse(`${today}T00:00:00Z`) - Date.parse(`${booking.return_date}T00:00:00Z`)) / 86400000
      )
    );
  }

  const pendingQty = Number(
    booking?.pending_return_qty > 0 ? booking.pending_return_qty : booking?.pending_pickup_qty || 0
  );
  const values = {
    customer_name: customer.name || "",
    booking_no: booking?.booking_no || "",
    pickup_date: booking?.pickup_date || "",
    return_date: booking?.return_date || "",
    item_name: booking?.item_name || "",
    qty: Number(booking?.booked_qty || 0),
    pending_qty: pendingQty,
    overdue_days: overdueDays,
    shop_name: parseShopName(settingsRow?.value_json, env.APP_NAME)
  };

  const rendered = renderMessage(template, values);
  if (!rendered.message) {
    return apiJson({ ok: false, error: "TEMPLATE_EMPTY", message: "WhatsApp template message is empty." }, 409);
  }

  return apiJson({
    ok: true,
    mobile: customer.mobile,
    templateKey: template.template_key,
    templateName: template.template_name,
    linkedAction,
    languageMode: rendered.languageMode,
    message: rendered.message
  });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const whatsappMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/whatsapp$/);
    if (whatsappMatch && request.method === "GET") {
      const user = await getSessionUser(env, request);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return composeLinkedWhatsApp(request, env, decodeURIComponent(whatsappMatch[1]));
    }

    return core.fetch(request, env, ctx);
  }
};
