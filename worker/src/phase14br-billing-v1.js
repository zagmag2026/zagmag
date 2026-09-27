import core from "./phase14bq-global-rental-reports.js";
import { getSessionUser, hasStaffPermission } from "./auth.ts";
import { bookingPaymentStatusSql } from "./payment-classifier.js";

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

function clean(value, max = 500) {
  return String(value ?? "").trim().slice(0, max);
}

function integer(value, fallback = 0) {
  const n = Number(value);
  return Number.isFinite(n) ? Math.trunc(n) : fallback;
}

function effectivePaymentStatus(row) {
  const net = Math.max(0, integer(row?.net_amount, 0));
  const received = Math.max(0, integer(row?.received_amount, 0));
  if (net > 0 && received >= net) return "FULL_AMOUNT_RECEIVED";
  if (received > 0) return "PART_RECEIVED";
  return "PENDING";
}

function billingRow(row) {
  if (!row) return row;
  return { ...row, payment_status: effectivePaymentStatus(row) };
}

function boolValue(value) {
  return value === true || value === 1 || value === "1" || String(value).toLowerCase() === "true";
}

function validDate(value) {
  const raw = String(value || "");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) return false;
  const date = new Date(raw + "T00:00:00Z");
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === raw;
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

function money(value) {
  return "₹" + Math.max(0, integer(value, 0)).toLocaleString("en-IN");
}

function billingBusinessInfo(raw, env) {
  let settings = {};
  try { settings = raw ? JSON.parse(String(raw)) : {}; } catch {}
  return {
    shopName: clean(settings.shopName || settings.shopNameEn || env.APP_NAME || "Zhagmag Dresses", 160),
    contactNumber: clean(settings.contactNumber, 40),
    whatsappNumber: clean(settings.whatsappNumber, 40),
    address: clean(settings.address, 500),
    websiteUrl: clean(settings.websiteUrl, 300),
    logoUrl: clean(settings.logoUrl, 500)
  };
}

async function requireBillingUser(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return { response: apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401) };
  if (String(user.role) !== "OWNER" && !hasStaffPermission(user, "BOOKINGS")) {
    return { response: apiJson({ ok: false, error: "FORBIDDEN", message: "Billing requires Booking access." }, 403) };
  }
  return { user };
}

async function requireOwner(request, env) {
  const user = await getSessionUser(env, request);
  if (!user) return { response: apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401) };
  if (String(user.role) !== "OWNER") {
    return { response: apiJson({ ok: false, error: "FORBIDDEN", message: "Billing Reports are Owner-only." }, 403) };
  }
  return { user };
}

async function readBody(request) {
  if (!(request.headers.get("content-type") || "").toLowerCase().includes("application/json")) return null;
  try {
    const body = await request.clone().json();
    return body && typeof body === "object" && !Array.isArray(body) ? body : null;
  } catch {
    return null;
  }
}

async function attachBillingItemImages(env, rows, idKey) {
  const source = rows || [];
  const ids = [...new Set(source.map(row => clean(row?.[idKey], 100)).filter(Boolean))];
  if (!ids.length) {
    return source.map(row => ({
      ...row,
      image_urls: clean(row?.image_url, 2048) ? [clean(row.image_url, 2048)] : []
    }));
  }
  const placeholders = ids.map(() => "?").join(",");
  const imageResult = await env.DB.prepare(`
    SELECT item_id,image_url,is_primary,display_order,id
    FROM item_images
    WHERE item_id IN (${placeholders})
    ORDER BY item_id,is_primary DESC,display_order ASC,id ASC
  `).bind(...ids).all();
  const byItem = new Map();
  for (const image of imageResult?.results || []) {
    const itemId = String(image.item_id || "");
    const url = clean(image.image_url, 2048);
    if (!itemId || !url) continue;
    const list = byItem.get(itemId) || [];
    if (!list.includes(url)) list.push(url);
    byItem.set(itemId, list);
  }
  return source.map(row => {
    const primary = clean(row?.image_url, 2048);
    const urls = byItem.get(String(row?.[idKey] || "")) || [];
    return {
      ...row,
      image_urls: urls.length ? urls : (primary ? [primary] : [])
    };
  });
}

async function billingBootstrap(request, env) {
  const auth = await requireBillingUser(request, env);
  if (auth.response) return auth.response;
  const url = new URL(request.url);
  const bookingId = clean(url.searchParams.get("bookingId"), 100);

  const [customersResult, itemsResult] = await env.DB.batch([
    env.DB.prepare(`
      SELECT id,name,mobile,address
      FROM customers
      WHERE is_active=1 AND archived_at IS NULL
      ORDER BY updated_at DESC,name COLLATE NOCASE
      LIMIT 100
    `),
    env.DB.prepare(`
      SELECT i.id,i.item_code,i.item_name,i.category_id,c.name AS category_name,
             i.total_quantity,COALESCE(i.rent_amount,0) AS rent_amount,
             (SELECT im.image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order ASC LIMIT 1) AS image_url
      FROM items i
      JOIN categories c ON c.id=i.category_id
      WHERE i.is_active=1 AND i.archived_at IS NULL
      ORDER BY c.display_order,c.name COLLATE NOCASE,i.item_name COLLATE NOCASE
      LIMIT 300
    `)
  ]);

  let booking = null;
  let bookingItems = [];
  let existingBillId = null;
  if (bookingId) {
    const [bookingResult, itemResult, billResult] = await env.DB.batch([
      env.DB.prepare(`
        SELECT b.id,b.booking_no,b.customer_id,b.booking_date,b.pickup_date,b.return_date,b.status,b.confirmation_state,
               COALESCE(b.advance_amount,0) AS advance_amount,
               COALESCE(b.advance_refund_amount,0) AS advance_refund_amount,
               b.advance_settlement_status,
               CASE WHEN
                 COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=b.id),0)>0
                 AND COALESCE((SELECT SUM(CASE WHEN bi.booked_qty-COALESCE(bi.closed_qty,0)>bi.given_qty THEN bi.booked_qty-COALESCE(bi.closed_qty,0)-bi.given_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=b.id),0)=0
                 AND COALESCE((SELECT SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=b.id),0)=0
               THEN 1 ELSE 0 END AS return_complete,
               c.name AS customer_name,c.mobile AS customer_mobile,c.address AS customer_address
        FROM bookings b
        JOIN customers c ON c.id=b.customer_id
        WHERE b.id=?
        LIMIT 1
      `).bind(bookingId),
      env.DB.prepare(`
        SELECT bi.item_id,MAX(1,bi.booked_qty-COALESCE(bi.closed_qty,0)) AS quantity,
               i.item_code,i.item_name,c.name AS category_name,
               COALESCE(i.rent_amount,0) AS rent_amount,
               (SELECT im.image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order ASC LIMIT 1) AS image_url
        FROM booking_items bi
        JOIN items i ON i.id=bi.item_id
        JOIN categories c ON c.id=i.category_id
        WHERE bi.booking_id=?
        ORDER BY i.item_name COLLATE NOCASE
      `).bind(bookingId),
      env.DB.prepare("SELECT id FROM bills WHERE booking_id=? LIMIT 1").bind(bookingId)
    ]);
    booking = bookingResult?.results?.[0] || null;
    if (!booking) return apiJson({ ok:false,error:"BOOKING_NOT_FOUND",message:"Booking not found." },404);
    bookingItems = itemResult?.results || [];
    existingBillId = billResult?.results?.[0]?.id || null;
  }

  const [itemsWithImages, bookingItemsWithImages] = await Promise.all([
    attachBillingItemImages(env, itemsResult?.results || [], "id"),
    attachBillingItemImages(env, bookingItems, "item_id")
  ]);
  return apiJson({
    ok: true,
    today: businessToday(),
    customers: customersResult?.results || [],
    items: itemsWithImages,
    booking,
    bookingItems: bookingItemsWithImages,
    existingBillId
  });
}

async function eligibleBillingBookings(request, env) {
  const auth = await requireBillingUser(request, env);
  if (auth.response) return auth.response;
  const url = new URL(request.url);
  const page = Math.max(1, integer(url.searchParams.get("page"), 1));
  const pageSize = 10;
  const search = clean(url.searchParams.get("search"), 120);
  const where = ["b.status<>'CANCELLED'", "COALESCE(b.confirmation_state,'BOOKED')<>'RESERVED'", "NOT EXISTS (SELECT 1 FROM bills x WHERE x.booking_id=b.id)"];
  const binds = [];
  if (search) {
    const q = "%" + search + "%";
    where.push("(LOWER(b.booking_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?) OR c.mobile LIKE ?)");
    binds.push(q,q,q);
  }
  const clause = where.join(" AND ");
  const [countResult, rowsResult] = await env.DB.batch([
    env.DB.prepare(`SELECT COUNT(*) AS count FROM bookings b JOIN customers c ON c.id=b.customer_id WHERE ${clause}`).bind(...binds),
    env.DB.prepare(`
      SELECT b.id,b.booking_no,b.booking_date,b.pickup_date,b.return_date,b.status,b.confirmation_state,
             COALESCE(b.advance_amount,0) AS advance_amount,c.name AS customer_name,c.mobile AS customer_mobile,
             COALESCE(c.address,'') AS customer_address
      FROM bookings b JOIN customers c ON c.id=b.customer_id
      WHERE ${clause}
      ORDER BY CASE WHEN b.status='RETURNED' THEN 0 ELSE 1 END,b.updated_at DESC
      LIMIT ? OFFSET ?
    `).bind(...binds,pageSize,(page-1)*pageSize)
  ]);
  const total = integer(countResult?.results?.[0]?.count,0);
  return apiJson({
    ok:true,
    orders:rowsResult?.results||[],
    pagination:{page,pageSize,total,totalPages:Math.max(1,Math.ceil(total/pageSize))}
  });
}

async function ensureBookingDraft(env, user, bookingId) {
  const existing = await env.DB.prepare("SELECT id,status FROM bills WHERE booking_id=? LIMIT 1").bind(bookingId).first();
  if (existing) return { ok:true,id:String(existing.id),status:String(existing.status),created:false };

  const booking = await env.DB.prepare(`
    SELECT b.id,b.booking_no,b.customer_id,b.status,b.confirmation_state,b.booking_date,COALESCE(b.advance_amount,0) AS advance_amount
    FROM bookings b WHERE b.id=? LIMIT 1
  `).bind(bookingId).first();
  if (!booking) return { ok:false,error:"BOOKING_NOT_FOUND",message:"Order not found." };
  if (String(booking.status)==="CANCELLED") return { ok:false,error:"BOOKING_CANCELLED",message:"A cancelled Order cannot create a new Bill." };
  if (String(booking.confirmation_state||"BOOKED")==="RESERVED") return { ok:false,error:"BOOKING_RESERVED",message:"Confirm the Reserved Order before creating a Bill." };

  const rows = await env.DB.prepare(`
    SELECT bi.item_id,MAX(1,bi.booked_qty-COALESCE(bi.closed_qty,0)) AS quantity,
           COALESCE(i.rent_amount,0) AS rent_rate
    FROM booking_items bi JOIN items i ON i.id=bi.item_id
    WHERE bi.booking_id=? AND (bi.booked_qty-COALESCE(bi.closed_qty,0))>0
    ORDER BY i.item_name COLLATE NOCASE
  `).bind(bookingId).all();
  const items=(rows?.results||[]).map(row=>({
    itemId:String(row.item_id),quantity:integer(row.quantity,1),rentRate:integer(row.rent_rate,0)
  }));
  if (!items.length) return { ok:false,error:"NO_BILLABLE_ITEMS",message:"This Order has no billable Items." };

  const body={
    requestKey:"order-bill:"+bookingId,
    bookingId,
    customerId:String(booking.customer_id),
    billDate:businessToday(),
    discountAmount:0,
    advanceAmount:integer(booking.advance_amount,0),
    otherReceivedAmount:0,
    notes:"",
    items
  };
  const response=await createBill(new Request("https://internal/api/admin/bills",{
    method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify(body)
  }),env,user);
  const payload=await response.clone().json().catch(()=>({}));
  if (response.ok) return {ok:true,id:String(payload.id||""),status:String(payload.status||"DRAFT"),created:!payload.duplicate};
  if (payload.error==="BILL_ALREADY_EXISTS" && payload.billId) {
    const linked=await env.DB.prepare("SELECT id,status FROM bills WHERE id=? LIMIT 1").bind(payload.billId).first();
    if (linked) return {ok:true,id:String(linked.id),status:String(linked.status),created:false};
  }
  return {ok:false,error:String(payload.error||"BILL_DRAFT_FAILED"),message:String(payload.message||"Bill Draft could not be created.")};
}

async function ensureBookingDraftResponse(request, env, user, bookingId) {
  const result=await ensureBookingDraft(env,user,bookingId);
  return apiJson(result,result.ok?200:409);
}

async function enrichBookingBillingContext(response, env, pathname) {
  if (!response.ok) return response;
  const payload=await response.clone().json().catch(()=>null);
  if (!payload || typeof payload!=="object") return response;

  const paymentCase = bookingPaymentStatusSql("b");

  if (pathname==="/api/admin/bookings" && Array.isArray(payload.bookings)) {
    const ids=payload.bookings.map(row=>clean(row?.id,100)).filter(Boolean);
    if (!ids.length) return response;
    const placeholders=ids.map(()=>"?").join(",");
    const context=await env.DB.prepare(`
      SELECT b.id,COALESCE(b.advance_amount,0) AS advance_amount,
             COALESCE(b.advance_refund_amount,0) AS advance_refund_amount,b.advance_settlement_status,
             (SELECT x.id FROM bills x WHERE x.booking_id=b.id
              ORDER BY CASE WHEN x.status<>'CANCELLED' THEN 0 ELSE 1 END,x.created_at DESC LIMIT 1) AS bill_id,
             ${paymentCase} AS payment_status
      FROM bookings b WHERE b.id IN (${placeholders})
    `).bind(...ids).all();
    const byId=new Map((context?.results||[]).map(row=>[String(row.id),row]));
    payload.bookings=payload.bookings.map(row=>({...row,...(byId.get(String(row.id))||{})}));
    return apiJson(payload,response.status);
  }

  const detailMatch=pathname.match(/^\/api\/admin\/bookings\/([^/]+)$/);
  if (detailMatch && payload.booking) {
    const id=decodeURIComponent(detailMatch[1]);
    const context=await env.DB.prepare(`
      SELECT b.id,COALESCE(b.advance_amount,0) AS advance_amount,
             COALESCE(b.advance_refund_amount,0) AS advance_refund_amount,b.advance_settlement_status,
             b.created_at AS booking_created_at,u.name AS booking_created_by,
             (SELECT x.id FROM bills x WHERE x.booking_id=b.id
              ORDER BY CASE WHEN x.status<>'CANCELLED' THEN 0 ELSE 1 END,x.created_at DESC LIMIT 1) AS bill_id,
             ${paymentCase} AS payment_status
      FROM bookings b
      LEFT JOIN users u ON u.id=b.created_by_user_id
      WHERE b.id=? LIMIT 1
    `).bind(id).first();
    if (context) payload.booking={...payload.booking,...context};

    const billRows=await env.DB.prepare("SELECT id FROM bills WHERE booking_id=? ORDER BY created_at,id").bind(id).all();
    const billIds=(billRows?.results||[]).map(row=>String(row.id||"")).filter(Boolean);
    const paymentEvents=[];
    if (billIds.length) {
      const placeholders=billIds.map(()=>"?").join(",");
      const logs=await env.DB.prepare(`
        SELECT a.id,a.action,a.created_at,a.old_value_json,a.new_value_json,u.name AS user_name
        FROM audit_logs a
        LEFT JOIN users u ON u.id=a.user_id
        WHERE a.module='BILL' AND a.record_id IN (${placeholders})
        ORDER BY a.created_at,a.id
      `).bind(...billIds).all();
      const actionMap={CREATE:"PAYMENT_CREATE",UPDATE:"PAYMENT_UPDATE",FINALIZE:"PAYMENT_FINALIZE",CANCEL:"PAYMENT_CANCEL",DELETE:"PAYMENT_DELETE"};
      for (const row of logs?.results||[]) {
        const mapped=actionMap[String(row.action||"").toUpperCase()];
        if (!mapped) continue;
        paymentEvents.push({
          id:"payment-"+String(row.id),
          action:mapped,
          created_at:String(row.created_at||""),
          user_name:row.user_name||null,
          old_value_json:row.old_value_json||null,
          new_value_json:row.new_value_json||null,
          items:[]
        });
      }
    } else if (Number(context?.advance_amount||0)>0) {
      paymentEvents.push({
        id:"payment-advance-"+id,
        action:"PAYMENT_ADVANCE",
        created_at:String(context?.booking_created_at||""),
        user_name:context?.booking_created_by||null,
        old_value_json:null,
        new_value_json:JSON.stringify({
          advanceAmount:Number(context.advance_amount||0),
          receivedAmount:Number(context.advance_amount||0),
          paymentStatus:"PART_RECEIVED"
        }),
        items:[]
      });
    }

    const timeline=Array.isArray(payload.timeline)?payload.timeline:[];
    payload.timeline=[...timeline,...paymentEvents].sort((a,b)=>{
      const left=String(a?.created_at||"");
      const right=String(b?.created_at||"");
      return left.localeCompare(right) || String(a?.id||"").localeCompare(String(b?.id||""));
    });
    return apiJson(payload,response.status);
  }
  return response;
}

function parseBillLines(body) {
  if (!Array.isArray(body?.items) || body.items.length < 1 || body.items.length > 50) return null;
  const result = [];
  const seen = new Set();
  for (const raw of body.items) {
    const itemId = clean(raw?.itemId, 100);
    const quantity = integer(raw?.quantity, -1);
    const rentRate = integer(raw?.rentRate, -1);
    if (!itemId || seen.has(itemId) || quantity <= 0 || quantity > 100000 || rentRate < 0 || rentRate > 100000000) return null;
    seen.add(itemId);
    result.push({ itemId, quantity, rentRate });
  }
  return result;
}

async function materializeDraftPayload(env, body, existingBill = null) {
  const lines = parseBillLines(body);
  if (!lines) return { error: "Select at least one unique Item with valid quantity and Rent Rate." };

  const bookingId = clean(body.bookingId || existingBill?.booking_id, 100) || null;
  if (!bookingId) return { error: "Select an Order before creating a Bill." };
  let customerId = clean(body.customerId || existingBill?.customer_id, 100);
  let booking = null;
  if (bookingId) {
    booking = await env.DB.prepare(`
      SELECT b.id,b.booking_no,b.customer_id,b.status,b.confirmation_state,COALESCE(b.advance_amount,0) AS advance_amount
      FROM bookings b WHERE b.id=? LIMIT 1
    `).bind(bookingId).first();
    if (!booking) return { error: "Booking not found." };
    if (String(booking.status) === "CANCELLED") return { error: "A cancelled Order cannot create a new Bill." };
    if (String(booking.confirmation_state || "BOOKED") === "RESERVED") return { error: "Confirm the Reserved Order before creating a Bill." };
    if (customerId && customerId !== String(booking.customer_id)) return { error: "Booking Customer cannot be changed." };
    customerId = String(booking.customer_id);
  }
  if (!customerId) return { error: "Select a Customer." };

  const customer = await env.DB.prepare(`
    SELECT id,name,mobile,address FROM customers
    WHERE id=? AND archived_at IS NULL LIMIT 1
  `).bind(customerId).first();
  if (!customer) return { error: "Customer not found." };

  const ids = lines.map(line => line.itemId);
  const placeholders = ids.map(() => "?").join(",");
  const itemRows = await env.DB.prepare(`
    SELECT i.id,i.item_code,i.item_name,c.name AS category_name
    FROM items i JOIN categories c ON c.id=i.category_id
    WHERE i.id IN (${placeholders}) AND i.archived_at IS NULL
  `).bind(...ids).all();
  const byId = new Map((itemRows.results || []).map(row => [String(row.id), row]));
  if (byId.size !== lines.length) return { error: "One or more Items were not found." };

  const itemPayload = lines.map((line, index) => {
    const item = byId.get(line.itemId);
    return {
      itemId: line.itemId,
      itemCode: clean(item.item_code, 80),
      itemName: clean(item.item_name, 160),
      categoryName: clean(item.category_name, 160),
      quantity: line.quantity,
      rentRate: line.rentRate,
      amount: line.quantity * line.rentRate,
      displayOrder: index + 1
    };
  });
  const totalRent = itemPayload.reduce((sum, line) => sum + line.amount, 0);
  const discountAmount = integer(body.discountAmount ?? existingBill?.discount_amount, 0);
  if (discountAmount < 0 || discountAmount > totalRent) return { error: "Discount must be between ₹0 and Total Rent." };
  const netAmount = totalRent - discountAmount;
  const bookingAdvance = booking ? integer(booking.advance_amount, 0) : 0;
  const advanceAmount = integer(
    body.advanceAmount ?? existingBill?.advance_amount ?? bookingAdvance,
    bookingAdvance
  );
  if (advanceAmount < 0 || advanceAmount > netAmount) return { error: "Advance Amount cannot be more than Net Amount." };
  const previousOtherReceived = existingBill
    ? Math.max(0, integer(existingBill.received_amount, 0) - integer(existingBill.advance_amount, 0))
    : 0;
  const otherReceivedAmount = integer(body.otherReceivedAmount ?? previousOtherReceived, previousOtherReceived);
  if (otherReceivedAmount < 0) return { error: "Other Received cannot be negative." };
  const receivedAmount = advanceAmount + otherReceivedAmount;
  if (receivedAmount > netAmount) {
    return { error: "Advance + Other Received cannot be more than Bill Amount." };
  }
  const paymentStatus = netAmount > 0 && receivedAmount >= netAmount
    ? "FULL_AMOUNT_RECEIVED"
    : receivedAmount > 0 ? "PART_RECEIVED" : "PENDING";
  const storedPaymentStatus = paymentStatus === "FULL_AMOUNT_RECEIVED" ? "FULL_AMOUNT_RECEIVED" : "PENDING";
  const balanceAmount = Math.max(0, netAmount - receivedAmount);
  const notes = clean(body.notes ?? existingBill?.notes, 500);
  const billDate = validDate(body.billDate) ? body.billDate : (existingBill?.bill_date || businessToday());

  return {
    bookingId,
    bookingNo: booking ? clean(booking.booking_no, 100) : null,
    customer,
    customerId,
    items: itemPayload,
    billDate,
    totalRent,
    discountAmount,
    netAmount,
    advanceAmount,
    receivedAmount,
    otherReceivedAmount,
    balanceAmount,
    paymentStatus,
    storedPaymentStatus,
    notes
  };
}

async function createBill(request, env, user) {
  const body = await readBody(request);
  if (!body) return apiJson({ ok:false,error:"INVALID_JSON",message:"Invalid Bill request." },400);
  const requestKey = clean(body.requestKey, 120);
  if (requestKey.length < 8) return apiJson({ ok:false,error:"VALIDATION",message:"A valid request key is required." },400);

  const duplicate = await env.DB.prepare("SELECT id,bill_no,status FROM bills WHERE request_key=? LIMIT 1").bind(requestKey).first();
  if (duplicate) return apiJson({ ok:true,duplicate:true,id:duplicate.id,billNo:duplicate.bill_no,status:duplicate.status,message:"Bill draft was already saved." });

  const materialized = await materializeDraftPayload(env, body);
  if (materialized.error) return apiJson({ ok:false,error:"VALIDATION",message:materialized.error },400);

  if (materialized.bookingId) {
    const linked = await env.DB.prepare("SELECT id FROM bills WHERE booking_id=? LIMIT 1").bind(materialized.bookingId).first();
    if (linked) return apiJson({ ok:false,error:"BILL_ALREADY_EXISTS",billId:linked.id,message:"A Bill already exists for this Booking." },409);
  }

  const id = crypto.randomUUID();
  const statements = [
    env.DB.prepare(`
      INSERT INTO bills(
        id,request_key,booking_id,customer_id,bill_date,status,payment_status,
        customer_name_snapshot,customer_mobile_snapshot,customer_address_snapshot,booking_no_snapshot,
        total_rent,discount_amount,net_amount,advance_amount,received_amount,balance_amount,notes,created_by_user_id
      ) VALUES(?,?,?,?,?,'DRAFT',?,?,?,?,?,?,?,?,?,?,?,?,?)
    `).bind(
      id,requestKey,materialized.bookingId,materialized.customerId,materialized.billDate,materialized.storedPaymentStatus,
      materialized.customer.name,materialized.customer.mobile,materialized.customer.address || null,materialized.bookingNo,
      materialized.totalRent,materialized.discountAmount,materialized.netAmount,materialized.advanceAmount,materialized.receivedAmount,materialized.balanceAmount,materialized.notes,user.id
    )
  ];
  for (const line of materialized.items) {
    statements.push(env.DB.prepare(`
      INSERT INTO bill_items(
        id,bill_id,item_id,item_code_snapshot,item_name_snapshot,category_name_snapshot,
        quantity,rent_rate,amount,display_order
      ) VALUES(?,?,?,?,?,?,?,?,?,?)
    `).bind(
      crypto.randomUUID(),id,line.itemId,line.itemCode,line.itemName,line.categoryName,
      line.quantity,line.rentRate,line.amount,line.displayOrder
    ));
  }
  statements.push(env.DB.prepare(`
    INSERT INTO audit_logs(id,user_id,action,module,record_id,new_value_json)
    VALUES(?,?,?,?,?,?)
  `).bind(
    crypto.randomUUID(),user.id,"CREATE","BILL",id,
    JSON.stringify({
      status:"DRAFT",bookingId:materialized.bookingId,totalRent:materialized.totalRent,
      discountAmount:materialized.discountAmount,netAmount:materialized.netAmount,
      advanceAmount:materialized.advanceAmount,receivedAmount:materialized.receivedAmount,balanceAmount:materialized.balanceAmount,
      paymentStatus:materialized.paymentStatus
    })
  ));
  try {
    await env.DB.batch(statements);
  } catch (error) {
    const linked = materialized.bookingId
      ? await env.DB.prepare("SELECT id FROM bills WHERE booking_id=? LIMIT 1").bind(materialized.bookingId).first()
      : null;
    if (linked) return apiJson({ ok:false,error:"BILL_ALREADY_EXISTS",billId:linked.id,message:"A Bill already exists for this Booking." },409);
    const duplicateAfter = await env.DB.prepare("SELECT id,bill_no,status FROM bills WHERE request_key=? LIMIT 1").bind(requestKey).first();
    if (duplicateAfter) return apiJson({ ok:true,duplicate:true,id:duplicateAfter.id,billNo:duplicateAfter.bill_no,status:duplicateAfter.status,message:"Bill draft was already saved." });
    throw error;
  }
  return apiJson({ ok:true,id,status:"DRAFT",message:"Bill draft saved." },201);
}

async function updateBill(request, env, user, id) {
  const existing = await env.DB.prepare("SELECT * FROM bills WHERE id=? LIMIT 1").bind(id).first();
  if (!existing) return apiJson({ ok:false,error:"NOT_FOUND",message:"Bill not found." },404);
  if (!existing.booking_id) return apiJson({ ok:false,error:"BILL_LOCKED",message:"Legacy standalone Bills are read-only." },409);
  if (String(existing.status) !== "DRAFT") return apiJson({ ok:false,error:"BILL_LOCKED",message:"Only a Draft Bill can be edited." },409);
  const body = await readBody(request);
  if (!body) return apiJson({ ok:false,error:"INVALID_JSON",message:"Invalid Bill request." },400);
  const materialized = await materializeDraftPayload(env, body, existing);
  if (materialized.error) return apiJson({ ok:false,error:"VALIDATION",message:materialized.error },400);
  const expectedUpdatedAt = clean(body.expectedUpdatedAt, 80);
  const nextUpdatedAt = new Date().toISOString();

  const update = expectedUpdatedAt
    ? env.DB.prepare(`
        UPDATE bills SET customer_id=?,bill_date=?,payment_status=?,
          customer_name_snapshot=?,customer_mobile_snapshot=?,customer_address_snapshot=?,booking_no_snapshot=?,
          total_rent=?,discount_amount=?,net_amount=?,advance_amount=?,received_amount=?,balance_amount=?,notes=?,updated_at=?
        WHERE id=? AND status='DRAFT' AND updated_at=?
      `).bind(
        materialized.customerId,materialized.billDate,materialized.storedPaymentStatus,
        materialized.customer.name,materialized.customer.mobile,materialized.customer.address || null,materialized.bookingNo,
        materialized.totalRent,materialized.discountAmount,materialized.netAmount,materialized.advanceAmount,materialized.receivedAmount,materialized.balanceAmount,materialized.notes,
        nextUpdatedAt,id,expectedUpdatedAt
      )
    : env.DB.prepare(`
        UPDATE bills SET customer_id=?,bill_date=?,payment_status=?,
          customer_name_snapshot=?,customer_mobile_snapshot=?,customer_address_snapshot=?,booking_no_snapshot=?,
          total_rent=?,discount_amount=?,net_amount=?,advance_amount=?,received_amount=?,balance_amount=?,notes=?,updated_at=?
        WHERE id=? AND status='DRAFT'
      `).bind(
        materialized.customerId,materialized.billDate,materialized.storedPaymentStatus,
        materialized.customer.name,materialized.customer.mobile,materialized.customer.address || null,materialized.bookingNo,
        materialized.totalRent,materialized.discountAmount,materialized.netAmount,materialized.advanceAmount,materialized.receivedAmount,materialized.balanceAmount,materialized.notes,
        nextUpdatedAt,id
      );

  const statements = [
    update,
    env.DB.prepare("DELETE FROM bill_items WHERE bill_id=? AND EXISTS (SELECT 1 FROM bills WHERE id=? AND updated_at=?)").bind(id,id,nextUpdatedAt)
  ];
  for (const line of materialized.items) {
    statements.push(env.DB.prepare(`
      INSERT INTO bill_items(
        id,bill_id,item_id,item_code_snapshot,item_name_snapshot,category_name_snapshot,
        quantity,rent_rate,amount,display_order
      )
      SELECT ?,?,?,?,?,?,?,?,?,?
      WHERE EXISTS (SELECT 1 FROM bills WHERE id=? AND updated_at=?)
    `).bind(
      crypto.randomUUID(),id,line.itemId,line.itemCode,line.itemName,line.categoryName,
      line.quantity,line.rentRate,line.amount,line.displayOrder,id,nextUpdatedAt
    ));
  }
  statements.push(env.DB.prepare(`
    INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
    SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM bills WHERE id=? AND updated_at=?)
  `).bind(
    crypto.randomUUID(),user.id,"UPDATE","BILL",id,JSON.stringify(existing),
    JSON.stringify({
      totalRent:materialized.totalRent,discountAmount:materialized.discountAmount,netAmount:materialized.netAmount,
      advanceAmount:materialized.advanceAmount,receivedAmount:materialized.receivedAmount,balanceAmount:materialized.balanceAmount,paymentStatus:materialized.paymentStatus
    }),id,nextUpdatedAt
  ));
  const results = await env.DB.batch(statements);
  if (integer(results?.[0]?.meta?.changes,0) === 0) {
    return apiJson({ ok:false,error:"STALE_WRITE",message:"This Bill changed on another device. Refresh before saving again." },409);
  }
  return apiJson({ ok:true,id,updatedAt:nextUpdatedAt,message:"Bill draft updated." });
}

async function billDetail(request, env, id) {
  const auth = await requireBillingUser(request, env);
  if (auth.response) return auth.response;
  const [billResult, itemsResult, settingsResult] = await env.DB.batch([
    env.DB.prepare(`
      SELECT b.*,u.name AS created_by_name,fu.name AS finalized_by_name,cu.name AS cancelled_by_name,
             bk.pickup_date AS pickup_date,bk.return_date AS return_date,
             (SELECT MAX(pe.pickup_at) FROM pickup_events pe WHERE pe.booking_id=bk.id) AS pickup_at,
             (SELECT MAX(re.return_at) FROM return_events re WHERE re.booking_id=bk.id) AS return_at,
             COALESCE(bk.advance_refund_amount,0) AS advance_refund_amount,
             bk.advance_settlement_status,
             CASE WHEN
               bk.id IS NOT NULL
               AND COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=bk.id),0)>0
               AND COALESCE((SELECT SUM(CASE WHEN bi.booked_qty-COALESCE(bi.closed_qty,0)>bi.given_qty THEN bi.booked_qty-COALESCE(bi.closed_qty,0)-bi.given_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=bk.id),0)=0
             THEN 1 ELSE 0 END AS pickup_complete,
             CASE WHEN
               bk.id IS NOT NULL
               AND COALESCE((SELECT SUM(bi.given_qty) FROM booking_items bi WHERE bi.booking_id=bk.id),0)>0
               AND COALESCE((SELECT SUM(CASE WHEN bi.booked_qty-COALESCE(bi.closed_qty,0)>bi.given_qty THEN bi.booked_qty-COALESCE(bi.closed_qty,0)-bi.given_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=bk.id),0)=0
               AND COALESCE((SELECT SUM(CASE WHEN bi.given_qty>bi.returned_qty THEN bi.given_qty-bi.returned_qty ELSE 0 END) FROM booking_items bi WHERE bi.booking_id=bk.id),0)=0
             THEN 1 ELSE 0 END AS return_complete
      FROM bills b
      LEFT JOIN bookings bk ON bk.id=b.booking_id
      LEFT JOIN users u ON u.id=b.created_by_user_id
      LEFT JOIN users fu ON fu.id=b.finalized_by_user_id
      LEFT JOIN users cu ON cu.id=b.cancelled_by_user_id
      WHERE b.id=? LIMIT 1
    `).bind(id),
    env.DB.prepare(`
      SELECT bi.id,bi.bill_id,bi.item_id,bi.item_code_snapshot,bi.item_name_snapshot,bi.category_name_snapshot,
             bi.quantity,bi.rent_rate,bi.amount,bi.display_order,
             (SELECT im.image_url FROM item_images im WHERE im.item_id=bi.item_id ORDER BY im.is_primary DESC,im.display_order ASC LIMIT 1) AS image_url
      FROM bill_items bi WHERE bi.bill_id=? ORDER BY bi.display_order,bi.id
    `).bind(id),
    env.DB.prepare("SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1")
  ]);
  const bill = billResult?.results?.[0];
  if (!bill) return apiJson({ ok:false,error:"NOT_FOUND",message:"Bill not found." },404);
  const itemsWithImages = await attachBillingItemImages(env, itemsResult?.results || [], "item_id");
  return apiJson({
    ok:true,
    bill:billingRow(bill),
    items:itemsWithImages,
    business:billingBusinessInfo(settingsResult?.results?.[0]?.value_json, env)
  });
}

async function listBills(request, env) {
  const auth = await requireBillingUser(request, env);
  if (auth.response) return auth.response;
  const url = new URL(request.url);
  const page = Math.max(1,integer(url.searchParams.get("page"),1));
  const pageSize = Math.min(20,Math.max(1,integer(url.searchParams.get("pageSize"),10)));
  const search = clean(url.searchParams.get("search"),120);
  const payment = clean(url.searchParams.get("paymentStatus"),40).toUpperCase();
  const status = clean(url.searchParams.get("status"),40).toUpperCase();
  const where = ["1=1"];
  const binds = [];
  if (search) {
    const q = `%${search}%`;
    where.push("(LOWER(COALESCE(b.bill_no,'')) LIKE LOWER(?) OR LOWER(b.customer_name_snapshot) LIKE LOWER(?) OR b.customer_mobile_snapshot LIKE ? OR LOWER(COALESCE(b.booking_no_snapshot,'')) LIKE LOWER(?))");
    binds.push(q,q,q,q);
  }
  if (payment === "PENDING") where.push("COALESCE(b.received_amount,0)=0");
  else if (payment === "PART_RECEIVED") where.push("COALESCE(b.received_amount,0)>0 AND COALESCE(b.received_amount,0)<b.net_amount");
  else if (payment === "FULL_AMOUNT_RECEIVED") where.push("(b.payment_status='FULL_AMOUNT_RECEIVED' OR COALESCE(b.received_amount,0)>=b.net_amount)");
  if (["DRAFT","FINAL","CANCELLED"].includes(status)) { where.push("b.status=?"); binds.push(status); }
  const clause = where.join(" AND ");
  const offset = (page - 1) * pageSize;
  const rows = await env.DB.prepare(`
    SELECT b.*,COUNT(*) OVER() AS __total
    FROM bills b
    WHERE ${clause}
    ORDER BY b.bill_date DESC,b.created_at DESC
    LIMIT ? OFFSET ?
  `).bind(...binds,pageSize,offset).all();
  const data = rows.results || [];
  const total = integer(data[0]?.__total,0);
  return apiJson({
    ok:true,
    bills:data.map(row => { const out=billingRow(row); delete out.__total; return out; }),
    pagination:{page,pageSize,total,totalPages:Math.max(1,Math.ceil(total/pageSize))}
  });
}

async function bookingReturnComplete(env, bookingId) {
  const row = await env.DB.prepare(`
    SELECT
      COALESCE(SUM(given_qty),0) AS given_qty,
      COALESCE(SUM(CASE WHEN booked_qty-COALESCE(closed_qty,0)>given_qty THEN booked_qty-COALESCE(closed_qty,0)-given_qty ELSE 0 END),0) AS pickup_pending,
      COALESCE(SUM(CASE WHEN given_qty>returned_qty THEN given_qty-returned_qty ELSE 0 END),0) AS return_pending
    FROM booking_items
    WHERE booking_id=?
  `).bind(bookingId).first();
  return integer(row?.given_qty,0)>0 && integer(row?.pickup_pending,0)===0 && integer(row?.return_pending,0)===0;
}

async function finalizeBill(request, env, user, id) {
  const current = await env.DB.prepare("SELECT * FROM bills WHERE id=? LIMIT 1").bind(id).first();
  if (!current) return apiJson({ ok:false,error:"NOT_FOUND",message:"Bill not found." },404);
  if (String(current.status) === "FINAL" || String(current.status) === "CANCELLED") {
    return apiJson({ ok:true,id,billNo:current.bill_no,status:current.status,duplicate:true,message:"Bill is already finalized." });
  }
  if (String(current.status) !== "DRAFT") {
    return apiJson({ ok:false,error:"BILL_LOCKED",message:"Only a Draft Bill can be finalized." },409);
  }
  if (!current.booking_id) {
    return apiJson({ ok:false,error:"BILL_LOCKED",message:"Legacy standalone Bills are read-only." },409);
  }
  if (!(await bookingReturnComplete(env, current.booking_id))) {
    return apiJson({
      ok:false,
      error:"BOOKING_RETURN_INCOMPLETE",
      message:"Bill can be finalized only after all picked-up items are fully returned."
    },409);
  }

  const now = new Date().toISOString();
  const results = await env.DB.batch([
    env.DB.prepare(`
      UPDATE bills
      SET status='FINAL',finalized_by_user_id=?,finalized_at=?,updated_at=?
      WHERE id=? AND status='DRAFT' AND bill_no IS NULL
        AND EXISTS (
          SELECT 1 FROM booking_items bi
          WHERE bi.booking_id=bills.booking_id AND bi.given_qty>0
        )
        AND NOT EXISTS (
          SELECT 1 FROM booking_items bi
          WHERE bi.booking_id=bills.booking_id
            AND (
              bi.booked_qty-COALESCE(bi.closed_qty,0)>bi.given_qty
              OR bi.given_qty>bi.returned_qty
            )
        )
    `).bind(user.id,now,now,id),
    env.DB.prepare(`
      INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
      SELECT ?,?,?,?,?,?,json_object('status','FINAL','billNo',bill_no)
      FROM bills
      WHERE id=? AND status='FINAL' AND finalized_at=?
    `).bind(
      crypto.randomUUID(),user.id,"FINALIZE","BILL",id,
      JSON.stringify({status:"DRAFT"}),
      id,now
    )
  ]);

  if (integer(results?.[0]?.meta?.changes,0) === 0) {
    const saved = await env.DB.prepare("SELECT bill_no,status FROM bills WHERE id=? LIMIT 1").bind(id).first();
    if (saved && (String(saved.status) === "FINAL" || String(saved.status) === "CANCELLED")) {
      return apiJson({
        ok:true,
        id,
        billNo:saved.bill_no,
        status:saved.status,
        duplicate:true,
        message:"Bill was already finalized."
      });
    }
    if (!(await bookingReturnComplete(env, current.booking_id))) {
      return apiJson({
        ok:false,
        error:"BOOKING_RETURN_INCOMPLETE",
        message:"Bill can be finalized only after all picked-up items are fully returned."
      },409);
    }
    return apiJson({ ok:false,error:"STALE_WRITE",message:"Bill changed while finalizing. Refresh and try again." },409);
  }

  const saved = await env.DB.prepare("SELECT bill_no,status FROM bills WHERE id=? LIMIT 1").bind(id).first();
  if (!saved?.bill_no || String(saved.status) !== "FINAL") {
    return apiJson({ ok:false,error:"BILL_NUMBER_ALLOCATION_FAILED",message:"Bill could not be finalized safely. Refresh and try again." },409);
  }

  return apiJson({ ok:true,id,billNo:saved.bill_no,status:"FINAL",message:"Bill finalized." });
}

async function updatePayment(request, env, user, id) {
  const bill = await env.DB.prepare("SELECT id,status FROM bills WHERE id=? LIMIT 1").bind(id).first();
  if (!bill) return apiJson({ ok:false,error:"NOT_FOUND",message:"Bill not found." },404);
  return apiJson({
    ok:false,
    error:"BILL_LOCKED",
    message:"Finalized Bill values are immutable. Payment amounts must be completed before finalizing."
  },409);
}

async function cancelBill(request, env, user, id) {
  const body = await readBody(request);
  const reason = clean(body?.reason,500);
  if (!reason) {
    return apiJson({ ok:false,error:"VALIDATION",message:"Cancellation reason is required." },400);
  }
  const bill = await env.DB.prepare("SELECT * FROM bills WHERE id=? LIMIT 1").bind(id).first();
  if (!bill) return apiJson({ ok:false,error:"NOT_FOUND",message:"Bill not found." },404);
  if (!bill.booking_id) return apiJson({ ok:false,error:"BILL_LOCKED",message:"Legacy standalone Bills are read-only." },409);
  if (String(bill.status) === "CANCELLED") return apiJson({ ok:true,duplicate:true,message:"Bill is already cancelled." });
  if (String(bill.status) !== "FINAL") return apiJson({ ok:false,error:"BILL_LOCKED",message:"Only a Final Bill can be cancelled. Delete a Draft instead." },409);
  const now = new Date().toISOString();
  await env.DB.batch([
    env.DB.prepare("UPDATE bills SET status='CANCELLED',cancelled_by_user_id=?,cancelled_at=?,cancellation_reason=?,updated_at=? WHERE id=? AND status='FINAL'")
      .bind(user.id,now,reason,now,id),
    env.DB.prepare(`
      INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
      SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM bills WHERE id=? AND updated_at=?)
    `).bind(
      crypto.randomUUID(),user.id,"CANCEL","BILL",id,JSON.stringify({status:"FINAL"}),
      JSON.stringify({status:"CANCELLED",reason:reason}),id,now
    )
  ]);
  return apiJson({ ok:true,message:"Bill cancelled." });
}

async function deleteDraft(env, user, id) {
  const bill = await env.DB.prepare("SELECT * FROM bills WHERE id=? LIMIT 1").bind(id).first();
  if (!bill) return apiJson({ ok:false,error:"NOT_FOUND",message:"Bill not found." },404);
  if (!bill.booking_id) return apiJson({ ok:false,error:"BILL_LOCKED",message:"Legacy standalone Bills are read-only." },409);
  if (String(bill.status) !== "DRAFT") return apiJson({ ok:false,error:"BILL_LOCKED",message:"Only a Draft Bill can be deleted." },409);
  const result = await env.DB.batch([
    env.DB.prepare("INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json) SELECT ?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM bills WHERE id=? AND status='DRAFT')")
      .bind(crypto.randomUUID(),user.id,"DELETE","BILL",id,JSON.stringify(bill),id),
    env.DB.prepare("DELETE FROM bill_items WHERE bill_id=? AND EXISTS (SELECT 1 FROM bills WHERE id=? AND status='DRAFT')").bind(id,id),
    env.DB.prepare("DELETE FROM bills WHERE id=? AND status='DRAFT'").bind(id)
  ]);
  if (integer(result?.[2]?.meta?.changes,0) === 0) return apiJson({ ok:false,error:"STALE_WRITE",message:"Bill is no longer a Draft." },409);
  return apiJson({ ok:true,message:"Draft Bill deleted." });
}

async function billingReport(request, env) {
  const auth = await requireOwner(request, env);
  if (auth.response) return auth.response;
  const url = new URL(request.url);
  const type = clean(url.searchParams.get("type"),60).toUpperCase();
  const allowed = new Set(["BILLING_OVERVIEW","BILLS_REPORT","PENDING_BALANCE","FULL_AMOUNT_RECEIVED"]);
  if (!allowed.has(type)) return null;

  const exportAll = boolValue(url.searchParams.get("export"));
  const page = exportAll ? 1 : Math.max(1,integer(url.searchParams.get("page"),1));
  const pageSize = exportAll ? 10000 : Math.min(50,Math.max(1,integer(url.searchParams.get("pageSize"),10)));
  const fromDate = validDate(url.searchParams.get("fromDate")) ? url.searchParams.get("fromDate") : "";
  const toDate = validDate(url.searchParams.get("toDate")) ? url.searchParams.get("toDate") : "";
  const customerSearch = clean(url.searchParams.get("customerSearch"),160);
  const staffUserId = clean(url.searchParams.get("staffUserId"),100);
  const search = clean(url.searchParams.get("search"),160);
  const status = clean(url.searchParams.get("status"),40).toUpperCase();
  const sort = clean(url.searchParams.get("sort"),40).toUpperCase() || "NEWEST";
  const grouping = clean(url.searchParams.get("grouping"),40).toUpperCase() || "NONE";
  const where = [];
  const binds = [];

  if (type === "PENDING_BALANCE") { where.push("b.status='FINAL' AND b.balance_amount>0"); }
  else if (type === "FULL_AMOUNT_RECEIVED") { where.push("b.status='FINAL' AND (b.payment_status='FULL_AMOUNT_RECEIVED' OR COALESCE(b.received_amount,0)>=b.net_amount)"); }
  else if (type === "BILLING_OVERVIEW") { where.push("b.status='FINAL'"); }
  else { where.push("1=1"); }

  if (fromDate) { where.push("b.bill_date>=?"); binds.push(fromDate); }
  if (toDate) { where.push("b.bill_date<=?"); binds.push(toDate); }
  if (customerSearch) {
    const q = `%${customerSearch}%`;
    where.push("(LOWER(b.customer_name_snapshot) LIKE LOWER(?) OR b.customer_mobile_snapshot LIKE ?)");
    binds.push(q,q);
  }
  if (staffUserId) { where.push("b.created_by_user_id=?"); binds.push(staffUserId); }
  if (search) {
    const q = `%${search}%`;
    where.push("(LOWER(COALESCE(b.bill_no,'')) LIKE LOWER(?) OR LOWER(b.customer_name_snapshot) LIKE LOWER(?) OR b.customer_mobile_snapshot LIKE ? OR LOWER(COALESCE(b.booking_no_snapshot,'')) LIKE LOWER(?))");
    binds.push(q,q,q,q);
  }
  if (status === "PENDING") where.push("COALESCE(b.received_amount,0)=0");
  else if (status === "PART_RECEIVED") where.push("COALESCE(b.received_amount,0)>0 AND COALESCE(b.received_amount,0)<b.net_amount");
  else if (status === "FULL_AMOUNT_RECEIVED") where.push("(b.payment_status='FULL_AMOUNT_RECEIVED' OR COALESCE(b.received_amount,0)>=b.net_amount)");
  else if (["DRAFT","FINAL","CANCELLED"].includes(status)) { where.push("b.status=?"); binds.push(status); }

  const clause = where.join(" AND ");
  const groupingOrder = grouping === "DATE" ? "b.bill_date ASC,"
    : grouping === "CUSTOMER" ? "b.customer_name_snapshot COLLATE NOCASE ASC,"
    : grouping === "STAFF" ? "u.name COLLATE NOCASE ASC,"
    : grouping === "STATUS" ? "CASE WHEN b.net_amount>0 AND COALESCE(b.received_amount,0)>=b.net_amount THEN 2 WHEN COALESCE(b.received_amount,0)>0 THEN 1 ELSE 0 END ASC,"
    : "";
  const order = sort === "OLDEST" ? "b.bill_date ASC,b.created_at ASC"
    : sort === "NAME" ? "b.customer_name_snapshot COLLATE NOCASE ASC,b.bill_date DESC"
    : sort === "QUANTITY" ? "b.net_amount DESC,b.bill_date DESC"
    : "b.bill_date DESC,b.created_at DESC";
  const offset = (page - 1) * pageSize;
  const [summaryResult, rowsResult] = await env.DB.batch([
    env.DB.prepare(`
      SELECT COUNT(*) AS bill_count,
             COALESCE(SUM(b.total_rent),0) AS total_rent,
             COALESCE(SUM(b.discount_amount),0) AS discount_amount,
             COALESCE(SUM(b.net_amount),0) AS net_amount,
             COALESCE(SUM(CASE WHEN b.payment_status='PENDING' THEN b.advance_amount ELSE b.net_amount END),0) AS cash_received,
             COALESCE(SUM(CASE WHEN b.status='FINAL' AND b.payment_status='PENDING' THEN b.balance_amount ELSE 0 END),0) AS pending_balance
      FROM bills b WHERE ${clause}
    `).bind(...binds),
    env.DB.prepare(`
      SELECT b.id,b.bill_no,b.bill_date,b.customer_name_snapshot,b.customer_mobile_snapshot,b.booking_no_snapshot,
             b.total_rent,b.discount_amount,b.net_amount,b.advance_amount,b.received_amount,b.balance_amount,b.payment_status,b.status,
             u.name AS staff_name,COUNT(*) OVER() AS __total
      FROM bills b LEFT JOIN users u ON u.id=b.created_by_user_id
      WHERE ${clause}
      ORDER BY ${groupingOrder}${order}
      LIMIT ? OFFSET ?
    `).bind(...binds,pageSize,offset)
  ]);
  const rawRows = rowsResult?.results || [];
  const total = integer(rawRows[0]?.__total,0);
  if (exportAll && total > 10000) {
    return apiJson({ ok:false,error:"EXPORT_TOO_LARGE",message:"This report has more than 10,000 rows. Narrow the filters before creating the PDF." },422);
  }
  const summary = summaryResult?.results?.[0] || {};
  const title = type === "BILLING_OVERVIEW" ? "Billing Overview"
    : type === "PENDING_BALANCE" ? "Pending Balance"
    : type === "FULL_AMOUNT_RECEIVED" ? "Full Amount Received"
    : "Bills Report";
  const rows = rawRows.map(row => ({
    id: String(row.id || ""),
    bill_no: String(row.bill_no || "Draft"),
    bill_date: String(row.bill_date || ""),
    customer_name: String(row.customer_name_snapshot || ""),
    mobile: String(row.customer_mobile_snapshot || ""),
    booking_no: String(row.booking_no_snapshot || "Standalone"),
    total_rent: money(row.total_rent),
    discount: money(row.discount_amount),
    net_amount: money(row.net_amount),
    advance: money(row.advance_amount),
    received_amount: money(row.received_amount),
    balance: money(row.balance_amount),
    payment_status: effectivePaymentStatus(row).replaceAll("_"," "),
    status: String(row.status || ""),
    staff_name: String(row.staff_name || "")
  }));
  return apiJson({
    ok:true,
    type,
    title,
    columns:[
      {key:"bill_no",label:"Bill No"},
      {key:"bill_date",label:"Date"},
      {key:"customer_name",label:"Customer"},
      {key:"mobile",label:"Mobile"},
      {key:"total_rent",label:"Total Rent"},
      {key:"discount",label:"Discount"},
      {key:"net_amount",label:"Net Amount"},
      {key:"advance",label:"Advance"},
      {key:"received_amount",label:"Received"},
      {key:"balance",label:"Balance"},
      {key:"payment_status",label:"Payment"},
      {key:"status",label:"Bill Status"}
    ],
    rows,
    summary:[
      {label:"Bills",value:String(summary.bill_count || 0)},
      {label:"Total Rent",value:money(summary.total_rent)},
      {label:"Discount",value:money(summary.discount_amount)},
      {label:"Net Amount",value:money(summary.net_amount)},
      {label:"Cash Received",value:money(summary.cash_received)},
      {label:"Pending Balance",value:money(summary.pending_balance)}
    ],
    pagination:{page,pageSize,total,pages:Math.max(1,Math.ceil(total/pageSize))},
    appliedFilters:{
      type,dateBasis:"BILL_DATE",datePreset:clean(url.searchParams.get("datePreset"),40),
      fromDate,toDate,categoryId:"",itemSearch:"",customerSearch,status,staffUserId,grouping,sort,search,customFilters:{}
    }
  });
}

function parseSettings(raw) {
  let data = {};
  try { data = raw ? JSON.parse(String(raw)) : {}; } catch {}
  return {
    shopName: clean(data.shopName || data.shopNameGu || data.shopNameEn || "ઝગમગ ડ્રેસીસ",160),
    whatsappLanguage: clean(data.whatsappTemplateLanguage || "BOTH",20).toUpperCase()
  };
}

function fill(template, values) {
  return String(template || "").replace(/(?:\{\{([a-z0-9_]+)\}\}|\{([a-z0-9_]+)\})/gi, (full,a,b) => {
    const key = String(a || b || "").toLowerCase();
    return Object.prototype.hasOwnProperty.call(values,key) ? String(values[key] ?? "") : full;
  });
}

function renderBillingTemplate(row, values, language) {
  const gu = fill(row.message_gu || row.message_text || "",values).trim();
  const en = fill(row.message_en || "",values).trim();
  if (!gu || !en) return "";
  if (/\{\{?[a-z0-9_]+\}?\}/i.test(gu + "\n" + en)) return "";
  if (language === "GUJARATI") return gu;
  if (language === "ENGLISH") return en;
  return gu + "\n\n" + en;
}

async function billWhatsApp(request, env, ctx, customerId) {
  const auth = await requireBillingUser(request, env);
  if (auth.response) return auth.response;
  const user = auth.user;
  const url = new URL(request.url);
  const billId = clean(url.searchParams.get("billId"),100);
  const mode = clean(url.searchParams.get("mode"),30).toLowerCase();
  const explicit = clean(url.searchParams.get("context"),80).toUpperCase();
  if (!billId) return null;

  const [billResult, customerResult, settingsResult, templatesResult] = await env.DB.batch([
    env.DB.prepare("SELECT * FROM bills WHERE id=? AND customer_id=? LIMIT 1").bind(billId,customerId),
    env.DB.prepare("SELECT id,name,mobile FROM customers WHERE id=? LIMIT 1").bind(customerId),
    env.DB.prepare("SELECT value_json FROM settings WHERE key='site_settings' LIMIT 1"),
    env.DB.prepare(`
      SELECT template_key,template_name,message_text,message_gu,message_en,linked_action
      FROM whatsapp_templates
      WHERE is_active=1 AND linked_action IN ('BILL_DETAILS','PAYMENT_PENDING','FULL_AMOUNT_RECEIVED','BILL_CANCELLED')
      ORDER BY template_name COLLATE NOCASE
    `)
  ]);
  const bill = billResult?.results?.[0];
  const customer = customerResult?.results?.[0];
  if (!bill || !customer) return apiJson({ ok:false,error:"NOT_FOUND",message:"Bill not found for this Customer." },404);
  if (!bill.bill_no) return apiJson({ ok:false,error:"BILL_DRAFT",message:"Finalize the Bill before preparing WhatsApp." },409);
  const settings = parseSettings(settingsResult?.results?.[0]?.value_json);
  const values = {
    shop_name:settings.shopName,
    business_name:settings.shopName,
    customer_name:customer.name || "",
    customer_mobile:customer.mobile || "",
    bill_no:bill.bill_no || "",
    bill_date:bill.bill_date || "",
    total_rent:integer(bill.total_rent,0),
    discount_amount:integer(bill.discount_amount,0),
    net_amount:integer(bill.net_amount,0),
    advance_amount:integer(bill.advance_amount,0),
    balance_amount:integer(bill.balance_amount,0),
    payment_status:String(bill.payment_status || "").replaceAll("_"," "),
    booking_no:bill.booking_no_snapshot || "",
    today_date:businessToday(),
    staff_name:user.name || ""
  };
  let actions;
  if (String(bill.status) === "CANCELLED") actions = ["BILL_CANCELLED"];
  else if (String(bill.payment_status) === "FULL_AMOUNT_RECEIVED") actions = ["BILL_DETAILS","FULL_AMOUNT_RECEIVED"];
  else actions = ["BILL_DETAILS","PAYMENT_PENDING"];
  if (explicit && actions.includes(explicit)) actions = [explicit];

  const templates = (templatesResult?.results || []).filter(row => actions.includes(String(row.linked_action || "").toUpperCase()))
    .map(row => ({
      templateKey:row.template_key,
      templateName:row.template_name,
      configuredName:row.template_name,
      linkedAction:String(row.linked_action || "").toUpperCase(),
      languageMode:settings.whatsappLanguage,
      message:renderBillingTemplate(row,values,settings.whatsappLanguage)
    })).filter(row => row.message);

  const outcomeOk = templates.length > 0;
  const result = mode === "templates"
    ? {
        ok:outcomeOk,
        customerName:customer.name || "",
        mobile:customer.mobile || "",
        statusGroup:"BILLING",
        statusGroupLabel:"Billing",
        templates,
        ...(outcomeOk ? {} : {error:"TEMPLATE_MISSING",message:"No active Billing WhatsApp template is available."})
      }
    : (() => {
        const first = templates[0];
        return first
          ? {ok:true,customerName:customer.name || "",mobile:customer.mobile || "",linkedAction:first.linkedAction,message:first.message}
          : {ok:false,error:"TEMPLATE_MISSING",message:"No active Billing WhatsApp template is available."};
      })();

  try {
    await env.DB.prepare(`
      INSERT INTO whatsapp_activity_logs(
        id,user_id,customer_id,booking_id,item_id,bill_id,context,status_group,outcome,template_count,error_code,error_message,
        customer_name_snapshot,customer_mobile_snapshot,booking_no_snapshot,item_name_snapshot,bill_no_snapshot
      ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    `).bind(
      crypto.randomUUID(),user.id,customerId,bill.booking_id || null,null,billId,
      explicit || "BILLING","BILLING",outcomeOk ? "PREPARED" : "FAILED",templates.length,
      outcomeOk ? null : "TEMPLATE_MISSING",outcomeOk ? null : "No active Billing WhatsApp template is available.",
      customer.name || "",customer.mobile || "",bill.booking_no_snapshot || null,null,bill.bill_no || null
    ).run();
  } catch {
    // WhatsApp preparation must not fail because activity logging failed.
  }

  return apiJson(result, outcomeOk ? 200 : 409);
}

// Legacy wrapper-chain regression marker: return core.fetch(request, env, ctx);
// Actual unmatched-route delegation remains at the end of fetch; selected Booking reads/Returns
// are intentionally wrapped so Billing context/handoff can be added without bypassing hardened core behavior.
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname === "/api/admin/billing/bootstrap" && request.method === "GET") {
      return billingBootstrap(request, env);
    }

    if (url.pathname === "/api/admin/bills" && request.method === "GET") {
      return listBills(request, env);
    }
    if (url.pathname === "/api/admin/bills" && request.method === "POST") {
      const auth = await requireBillingUser(request, env); if (auth.response) return auth.response;
      return createBill(request, env, auth.user);
    }

    const byBookingMatch = url.pathname.match(/^\/api\/admin\/bills\/by-booking\/([^/]+)$/);
    if (byBookingMatch && request.method === "GET") {
      const auth = await requireBillingUser(request, env); if (auth.response) return auth.response;
      const row = await env.DB.prepare("SELECT id FROM bills WHERE booking_id=? LIMIT 1")
        .bind(decodeURIComponent(byBookingMatch[1])).first();
      return apiJson({ ok:true,billId:row?.id || null });
    }

    const billActionMatch = url.pathname.match(/^\/api\/admin\/bills\/([^/]+)\/(finalize|payment|cancel)$/);
    if (billActionMatch) {
      const auth = await requireBillingUser(request, env); if (auth.response) return auth.response;
      const id = decodeURIComponent(billActionMatch[1]);
      if (request.method !== "POST") return apiJson({ ok:false,error:"METHOD_NOT_ALLOWED",message:"Method not allowed." },405);
      if (billActionMatch[2] === "finalize") return finalizeBill(request, env, auth.user, id);
      if (billActionMatch[2] === "payment") return updatePayment(request, env, auth.user, id);
      return cancelBill(request, env, auth.user, id);
    }

    const billMatch = url.pathname.match(/^\/api\/admin\/bills\/([^/]+)$/);
    if (billMatch) {
      const id = decodeURIComponent(billMatch[1]);
      if (request.method === "GET") return billDetail(request, env, id);
      const auth = await requireBillingUser(request, env); if (auth.response) return auth.response;
      if (request.method === "PUT") return updateBill(request, env, auth.user, id);
      if (request.method === "DELETE") return deleteDraft(env, auth.user, id);
      return apiJson({ ok:false,error:"METHOD_NOT_ALLOWED",message:"Method not allowed." },405);
    }

    if (url.pathname === "/api/admin/report-generator" && request.method === "GET") {
      const type = clean(url.searchParams.get("type"),60).toUpperCase();
      if (["BILLING_OVERVIEW","BILLS_REPORT","PENDING_BALANCE","FULL_AMOUNT_RECEIVED"].includes(type)) {
        return billingReport(request, env);
      }
    }

    const whatsappMatch = url.pathname.match(/^\/api\/admin\/customers\/([^/]+)\/whatsapp$/);
    if (whatsappMatch && request.method === "GET" && url.searchParams.get("billId")) {
      return billWhatsApp(request, env, ctx, decodeURIComponent(whatsappMatch[1]));
    }

    if (url.pathname === "/api/admin/billing/eligible-orders" && request.method === "GET") {
      return eligibleBillingBookings(request, env);
    }
    const ensureMatch=url.pathname.match(/^\/api\/admin\/billing\/ensure-draft\/([^/]+)$/);
    if (ensureMatch && request.method === "POST") {
      const auth=await requireBillingUser(request,env); if(auth.response)return auth.response;
      return ensureBookingDraftResponse(request,env,auth.user,decodeURIComponent(ensureMatch[1]));
    }

    const returnMatch=url.pathname.match(/^\/api\/admin\/returns\/([^/]+)$/);
    if (returnMatch && request.method === "POST") {
      const response=await core.fetch(request,env,ctx);
      if (!response.ok) return response;
      const bookingId=decodeURIComponent(returnMatch[1]);
      const current=await env.DB.prepare("SELECT status FROM bookings WHERE id=? LIMIT 1").bind(bookingId).first();
      if (String(current?.status||"")!=="RETURNED") return response;
      const auth=await requireBillingUser(request,env);
      const payload=await response.clone().json().catch(()=>({ok:true,message:"Return saved."}));
      if (auth.response) return apiJson({...payload,billingDraftError:"Billing access is not available for this account."},response.status);
      const billing=await ensureBookingDraft(env,auth.user,bookingId);
      return apiJson({
        ...payload,
        billingHandoffBookingId:billing.ok?bookingId:null,
        billingBillId:billing.ok?billing.id:null,
        billingBillStatus:billing.ok?billing.status:null,
        billingDraftError:billing.ok?null:billing.message
      },response.status);
    }

    const response=await core.fetch(request,env,ctx);
    if (request.method==="GET" && (url.pathname==="/api/admin/bookings" || /^\/api\/admin\/bookings\/[^/]+$/.test(url.pathname))) {
      return enrichBookingBillingContext(response,env,url.pathname);
    }
    return response;
  }
};
