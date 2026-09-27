import core from "./phase14aw-screen4-bookings.js";
import { getSessionUser } from "./auth.ts";

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

function boolValue(value) {
  return value === true || value === 1 || value === "1" || String(value).toLowerCase() === "true";
}

function parseOptions(raw) {
  try {
    const parsed = raw ? JSON.parse(String(raw)) : [];
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
}

async function itemManagementBootstrap(env) {
  const today = businessToday();
  const [categoriesResult, fieldsResult, itemsResult, relationsResult, imagesResult] = await env.DB.batch([
    env.DB.prepare(`
      WITH field_counts AS (
        SELECT category_id,COUNT(*) AS field_count
        FROM category_fields GROUP BY category_id
      ), item_counts AS (
        SELECT category_id,
               SUM(CASE WHEN archived_at IS NULL THEN 1 ELSE 0 END) AS item_count,
               COUNT(*) AS linked_item_count
        FROM items GROUP BY category_id
      )
      SELECT c.id,c.name,c.code_prefix,c.is_active,c.public_visible,c.display_order,
             COALESCE(fc.field_count,0) AS field_count,
             COALESCE(ic.item_count,0) AS item_count,
             COALESCE(ic.linked_item_count,0) AS linked_item_count
      FROM categories c
      LEFT JOIN field_counts fc ON fc.category_id=c.id
      LEFT JOIN item_counts ic ON ic.category_id=c.id
      ORDER BY c.display_order ASC,c.name COLLATE NOCASE ASC
    `),
    env.DB.prepare(`
      WITH value_counts AS (
        SELECT category_field_id,COUNT(DISTINCT item_id) AS value_count
        FROM item_field_values GROUP BY category_field_id
      )
      SELECT f.id,f.category_id,f.field_name,f.field_type,f.is_required,f.default_value,f.options_json,
             f.public_visible,f.is_active,f.display_order,COALESCE(vc.value_count,0) AS value_count
      FROM category_fields f
      LEFT JOIN value_counts vc ON vc.category_field_id=f.id
      ORDER BY category_id,display_order ASC,field_name COLLATE NOCASE ASC
    `),
    env.DB.prepare(`
      WITH stock AS (
        SELECT bi.item_id,
               SUM(CASE
                 WHEN b.status NOT IN ('CANCELLED','RETURNED') AND b.pickup_date<=? AND b.return_date>=?
                 THEN MAX(0,bi.booked_qty-bi.closed_qty-bi.given_qty) ELSE 0 END
               ) AS booked_qty,
               SUM(CASE
                 WHEN b.status NOT IN ('CANCELLED','RETURNED')
                 THEN MAX(0,bi.given_qty-bi.returned_qty) ELSE 0 END
               ) AS given_qty
        FROM booking_items bi
        JOIN bookings b ON b.id=bi.booking_id
        GROUP BY bi.item_id
      )
      SELECT i.id,i.item_code,i.item_name,i.category_id,c.name AS category_name,i.total_quantity,
             COALESCE(i.rent_amount,0) AS rent_amount,
             i.is_active,i.public_visible,i.archived_at,i.updated_at,
             COALESCE(s.booked_qty,0) AS booked_qty,
             COALESCE(s.given_qty,0) AS given_qty,
             (
               SELECT im.image_url FROM item_images im
               WHERE im.item_id=i.id
               ORDER BY im.is_primary DESC,im.display_order ASC
               LIMIT 1
             ) AS primary_image_url
      FROM items i
      JOIN categories c ON c.id=i.category_id
      LEFT JOIN stock s ON s.item_id=i.id
      WHERE i.archived_at IS NULL
      ORDER BY c.display_order ASC,c.name COLLATE NOCASE ASC,i.item_name COLLATE NOCASE ASC
    `).bind(today, today),
    env.DB.prepare(`
      SELECT source_item_id,related_item_id,display_order
      FROM item_related_items
      ORDER BY source_item_id,display_order ASC,related_item_id ASC
    `),
    env.DB.prepare(`
      SELECT id,item_id,image_url,is_primary,display_order
      FROM item_images
      ORDER BY item_id,is_primary DESC,display_order ASC,id ASC
    `)
  ]);

  const fieldsByCategory = new Map();
  for (const row of fieldsResult.results || []) {
    const categoryId = String(row.category_id);
    const list = fieldsByCategory.get(categoryId) || [];
    list.push({
      ...row,
      is_required: boolValue(row.is_required),
      public_visible: boolValue(row.public_visible),
      is_active: boolValue(row.is_active),
      options: parseOptions(row.options_json)
    });
    fieldsByCategory.set(categoryId, list);
  }

  const categories = (categoriesResult.results || []).map(row => ({
    ...row,
    is_active: boolValue(row.is_active),
    public_visible: boolValue(row.public_visible),
    field_count: Number(row.field_count || 0),
    item_count: Number(row.item_count || 0),
    fields: fieldsByCategory.get(String(row.id)) || []
  }));

  const imagesByItem = new Map();
  for (const image of imagesResult.results || []) {
    const itemId = String(image.item_id || "");
    if (!itemId) continue;
    const list = imagesByItem.get(itemId) || [];
    list.push({
      ...image,
      is_primary: boolValue(image.is_primary)
    });
    imagesByItem.set(itemId, list);
  }

  const items = (itemsResult.results || []).map(row => {
    const total = Number(row.total_quantity || 0);
    const booked = Number(row.booked_qty || 0);
    const given = Number(row.given_qty || 0);
    return {
      ...row,
      total_quantity: total,
      booked_qty: booked,
      given_qty: given,
      available_qty: Math.max(0, total - booked - given),
      is_active: boolValue(row.is_active),
      public_visible: boolValue(row.public_visible),
      field_values: [],
      images: imagesByItem.get(String(row.id)) || []
    };
  });

  return {
    ok: true,
    categories,
    items,
    relations: relationsResult.results || []
  };
}

function itemPositiveInt(value, fallback, min, max) {
  const parsed = Number.parseInt(String(value || ""), 10);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, parsed));
}

async function itemManagementItems(url, env) {
  const today = businessToday();
  const page = itemPositiveInt(url.searchParams.get("page"), 1, 1, 1000000);
  const pageSize = itemPositiveInt(url.searchParams.get("pageSize"), 10, 10, 50);
  const search = String(url.searchParams.get("search") || "").trim().slice(0, 120);
  const categoryId = String(url.searchParams.get("categoryId") || "").trim().slice(0, 100);
  const offset = (page - 1) * pageSize;

  const filters = [];
  const filterBindings = [];
  if (categoryId) {
    filters.push("i.category_id=?");
    filterBindings.push(categoryId);
  }
  if (search) {
    const like = `%${search}%`;
    filters.push("(i.item_name LIKE ? COLLATE NOCASE OR i.item_code LIKE ? COLLATE NOCASE OR c.name LIKE ? COLLATE NOCASE)");
    filterBindings.push(like, like, like);
  }
  const filterSql = filters.length ? ` AND ${filters.join(" AND ")}` : "";

  const countRow = await env.DB.prepare(`
    SELECT COUNT(*) AS total
    FROM items i
    JOIN categories c ON c.id=i.category_id
    WHERE i.archived_at IS NULL${filterSql}
  `).bind(...filterBindings).first();

  const itemsResult = await env.DB.prepare(`
    WITH stock AS (
      SELECT bi.item_id,
             SUM(CASE
               WHEN b.status NOT IN ('CANCELLED','RETURNED') AND b.pickup_date<=? AND b.return_date>=?
               THEN MAX(0,bi.booked_qty-bi.closed_qty-bi.given_qty) ELSE 0 END
             ) AS booked_qty,
             SUM(CASE
               WHEN b.status NOT IN ('CANCELLED','RETURNED')
               THEN MAX(0,bi.given_qty-bi.returned_qty) ELSE 0 END
             ) AS given_qty
      FROM booking_items bi
      JOIN bookings b ON b.id=bi.booking_id
      GROUP BY bi.item_id
    )
    SELECT i.id,i.item_code,i.item_name,i.category_id,c.name AS category_name,i.total_quantity,
           COALESCE(i.rent_amount,0) AS rent_amount,
           i.is_active,i.public_visible,i.archived_at,i.updated_at,
           COALESCE(s.booked_qty,0) AS booked_qty,
           COALESCE(s.given_qty,0) AS given_qty
    FROM items i
    JOIN categories c ON c.id=i.category_id
    LEFT JOIN stock s ON s.item_id=i.id
    WHERE i.archived_at IS NULL${filterSql}
    ORDER BY c.display_order ASC,c.name COLLATE NOCASE ASC,i.item_name COLLATE NOCASE ASC
    LIMIT ? OFFSET ?
  `).bind(today, today, ...filterBindings, pageSize, offset).all();

  const rows = itemsResult.results || [];
  const ids = rows.map(row => String(row.id)).filter(Boolean);
  let valueRows = [];
  let imageRows = [];
  if (ids.length) {
    const placeholders = ids.map(() => "?").join(",");
    const [valuesResult, imagesResult] = await env.DB.batch([
      env.DB.prepare(`
        SELECT item_id,category_field_id,value_text
        FROM item_field_values
        WHERE item_id IN (${placeholders})
        ORDER BY item_id,category_field_id
      `).bind(...ids),
      env.DB.prepare(`
        SELECT id,item_id,image_url,is_primary,display_order
        FROM item_images
        WHERE item_id IN (${placeholders})
        ORDER BY item_id,is_primary DESC,display_order ASC
      `).bind(...ids)
    ]);
    valueRows = valuesResult.results || [];
    imageRows = imagesResult.results || [];
  }

  const valuesByItem = new Map();
  for (const row of valueRows) {
    const itemId = String(row.item_id);
    const list = valuesByItem.get(itemId) || [];
    list.push(row);
    valuesByItem.set(itemId, list);
  }
  const imagesByItem = new Map();
  for (const row of imageRows) {
    const itemId = String(row.item_id);
    const list = imagesByItem.get(itemId) || [];
    list.push({ ...row, is_primary: boolValue(row.is_primary) });
    imagesByItem.set(itemId, list);
  }

  const items = rows.map(row => {
    const total = Number(row.total_quantity || 0);
    const booked = Number(row.booked_qty || 0);
    const given = Number(row.given_qty || 0);
    return {
      ...row,
      total_quantity: total,
      booked_qty: booked,
      given_qty: given,
      available_qty: Math.max(0, total - booked - given),
      is_active: boolValue(row.is_active),
      public_visible: boolValue(row.public_visible),
      field_values: valuesByItem.get(String(row.id)) || [],
      images: imagesByItem.get(String(row.id)) || []
    };
  });

  const total = Number(countRow?.total || 0);
  return {
    ok: true,
    items,
    page,
    pageSize,
    total,
    totalPages: Math.max(1, Math.ceil(total / pageSize))
  };
}

async function readBody(request) {
  if (!(request.headers.get("content-type") || "").toLowerCase().includes("application/json")) return null;
  try {
    const body = await request.json();
    return body && typeof body === "object" && !Array.isArray(body) ? body : null;
  } catch {
    return null;
  }
}

async function replaceRelatedItems(request, env, user, sourceItemId) {
  const body = await readBody(request);
  if (!body || !Array.isArray(body.relatedItemIds)) {
    return apiJson({ ok: false, error: "VALIDATION", message: "relatedItemIds array is required." }, 400);
  }

  const source = await env.DB.prepare(`
    SELECT id,item_name,is_active,archived_at,total_quantity,updated_at
    FROM items WHERE id=? LIMIT 1
  `).bind(sourceItemId).first();
  if (!source || source.archived_at) {
    return apiJson({ ok: false, error: "NOT_FOUND", message: "Source item not found." }, 404);
  }

  const requested = [...new Set(body.relatedItemIds.map(value => String(value).trim()).filter(Boolean))].slice(0, 300);
  if (requested.includes(sourceItemId)) {
    return apiJson({ ok: false, error: "VALIDATION", message: "An item cannot be related to itself." }, 400);
  }

  const existingRows = await env.DB.prepare(`
    SELECT related_item_id,display_order FROM item_related_items
    WHERE source_item_id=? ORDER BY display_order ASC,related_item_id ASC
  `).bind(sourceItemId).all();
  const existing = (existingRows.results || []).map(row => String(row.related_item_id));
  const newlyAdded = requested.filter(id => !existing.includes(id));

  if (requested.length) {
    const placeholders = requested.map(() => "?").join(",");
    const today = businessToday();
    const targetRows = await env.DB.prepare(`
      SELECT i.id,i.is_active,i.archived_at,i.total_quantity,
             COALESCE((
               SELECT SUM(MAX(0,bi.booked_qty-bi.closed_qty-bi.given_qty))
               FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id
               WHERE bi.item_id=i.id AND b.status NOT IN ('CANCELLED','RETURNED')
                 AND b.pickup_date<=? AND b.return_date>=?
             ),0) AS booked_qty,
             COALESCE((
               SELECT SUM(MAX(0,bi.given_qty-bi.returned_qty))
               FROM booking_items bi JOIN bookings b ON b.id=bi.booking_id
               WHERE bi.item_id=i.id AND b.status NOT IN ('CANCELLED','RETURNED')
             ),0) AS given_qty
      FROM items i WHERE i.id IN (${placeholders})
    `).bind(today, today, ...requested).all();
    const byId = new Map((targetRows.results || []).map(row => [String(row.id), row]));
    if (byId.size !== requested.length) {
      return apiJson({ ok: false, error: "VALIDATION", message: "One or more related items were not found." }, 400);
    }
    for (const id of newlyAdded) {
      const row = byId.get(id);
      const available = row
        ? Math.max(0, Number(row.total_quantity || 0) - Number(row.booked_qty || 0) - Number(row.given_qty || 0))
        : 0;
      if (!row || !boolValue(row.is_active) || row.archived_at || available <= 0) {
        return apiJson({ ok: false, error: "RELATED_ITEM_UNAVAILABLE", message: "Inactive, archived or unavailable items cannot be newly related." }, 409);
      }
    }
  }

  if (existing.length === requested.length && existing.every((id, index) => id === requested[index])) {
    return apiJson({ ok: true, unchanged: true, message: "Related items are already up to date." });
  }

  const expectedUpdatedAt = String(body.expectedUpdatedAt || "").trim();
  const nextUpdatedAt = new Date().toISOString();
  const statements = [
    expectedUpdatedAt
      ? env.DB.prepare(`UPDATE items SET updated_at=? WHERE id=? AND updated_at=?`).bind(nextUpdatedAt, sourceItemId, expectedUpdatedAt)
      : env.DB.prepare(`UPDATE items SET updated_at=? WHERE id=?`).bind(nextUpdatedAt, sourceItemId),
    env.DB.prepare(`DELETE FROM item_related_items
      WHERE source_item_id=? AND EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)`)
      .bind(sourceItemId, sourceItemId, nextUpdatedAt)
  ];
  requested.forEach((relatedItemId, index) => {
    statements.push(
      env.DB.prepare(`
        INSERT INTO item_related_items(source_item_id,related_item_id,display_order,created_by_user_id)
        SELECT ?,?,?,? WHERE EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)
      `).bind(sourceItemId, relatedItemId, index + 1, user.id, sourceItemId, nextUpdatedAt)
    );
  });
  statements.push(
    env.DB.prepare(`
      INSERT INTO audit_logs(id,user_id,action,module,record_id,old_value_json,new_value_json)
      SELECT ?,?,?,?,?,?,? WHERE EXISTS (SELECT 1 FROM items WHERE id=? AND updated_at=?)
    `).bind(
      crypto.randomUUID(),
      user.id,
      "REPLACE",
      "RELATED_ITEM",
      sourceItemId,
      JSON.stringify({ relatedItemIds: existing }),
      JSON.stringify({ relatedItemIds: requested }),
      sourceItemId,
      nextUpdatedAt
    )
  );
  const result = await env.DB.batch(statements);
  if (expectedUpdatedAt && Number(result?.[0]?.meta?.changes || 0) === 0) {
    return apiJson({ ok: false, error: "STALE_WRITE", message: "Related Items changed on another device. Refresh before saving again." }, 409);
  }
  return apiJson({ ok: true, updatedAt: nextUpdatedAt, message: "Related items updated.", sourceItemId, relatedItemIds: requested });
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname === "/api/admin/item-management/bootstrap" && request.method === "GET") {
      const user = await getSessionUser(env, request);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return apiJson(await itemManagementBootstrap(env));
    }

    if (url.pathname === "/api/admin/item-management/items" && request.method === "GET") {
      const user = await getSessionUser(env, request);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      return apiJson(await itemManagementItems(url, env));
    }

    const relatedMatch = url.pathname.match(/^\/api\/admin\/related-items\/([^/]+)$/);
    if (relatedMatch && request.method === "PUT") {
      const user = await getSessionUser(env, request);
      if (!user) return apiJson({ ok: false, error: "UNAUTHENTICATED", message: "Please sign in." }, 401);
      if (String(user.role) !== "OWNER") {
        return apiJson({ ok: false, error: "FORBIDDEN", message: "You do not have permission for this action." }, 403);
      }
      return replaceRelatedItems(request, env, user, decodeURIComponent(relatedMatch[1]));
    }

    return core.fetch(request, env, ctx);
  }
};
