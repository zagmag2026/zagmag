import core from "./index.ts";

const JSON_TYPE="application/json";
function uniqueBookingIds(rows,key="id"){return [...new Set((rows||[]).map((row)=>String(row?.[key]||"").trim()).filter(Boolean))];}
async function loadVisualItems(env,bookingIds){
  if(!bookingIds.length)return new Map();
  const placeholders=bookingIds.map(()=>"?").join(",");
  const result=await env.DB.prepare(`
    SELECT bi.booking_id,i.id AS item_id,i.item_code,i.item_name,c.name AS category_name,
      bi.booked_qty,bi.given_qty,bi.returned_qty,
      MAX(0,bi.given_qty-bi.returned_qty) AS pending_qty,
      MAX(0,bi.booked_qty-bi.given_qty) AS remaining_to_give,
      (SELECT image_url FROM item_images im WHERE im.item_id=i.id ORDER BY im.is_primary DESC,im.display_order LIMIT 1) AS image_url
    FROM booking_items bi
    JOIN items i ON i.id=bi.item_id
    JOIN categories c ON c.id=i.category_id
    WHERE bi.booking_id IN (${placeholders})
    ORDER BY bi.booking_id,i.item_name COLLATE NOCASE
  `).bind(...bookingIds).all();
  const grouped=new Map();
  for(const row of result.results||[]){
    const key=String(row.booking_id);const list=grouped.get(key)||[];
    list.push({item_id:String(row.item_id||""),item_code:String(row.item_code||""),item_name:String(row.item_name||""),category_name:String(row.category_name||""),image_url:row.image_url?String(row.image_url):null,booked_qty:Number(row.booked_qty||0),given_qty:Number(row.given_qty||0),returned_qty:Number(row.returned_qty||0),pending_qty:Number(row.pending_qty||0),remaining_to_give:Number(row.remaining_to_give||0)});
    grouped.set(key,list);
  }
  return grouped;
}
function attachRows(rows,grouped,idKey="id"){for(const row of rows||[]){row.item_lines=grouped.get(String(row?.[idKey]||""))||[];}}
async function enrichAdminPayload(url,payload,env){
  if(!payload||payload.ok===false)return payload;const path=url.pathname;
  if(path==="/api/admin/dashboard"){
    const groups=[payload.todayBookings,payload.todayPickups,payload.todayReturns,payload.overdueReturns];
    const ids=[...new Set(groups.flatMap((rows)=>uniqueBookingIds(rows,"id")))];const visual=await loadVisualItems(env,ids);groups.forEach((rows)=>attachRows(rows,visual,"id"));return payload;
  }
  if(path==="/api/admin/bookings"||path==="/api/admin/bookings/bootstrap"){
    const ids=uniqueBookingIds(payload.bookings,"id");attachRows(payload.bookings,await loadVisualItems(env,ids),"id");return payload;
  }
  if(path==="/api/admin/pickups"||path==="/api/admin/pickups/bootstrap"){
    const ids=uniqueBookingIds(payload.pickups,"id");attachRows(payload.pickups,await loadVisualItems(env,ids),"id");return payload;
  }
  if(path==="/api/admin/returns"){
    const ids=uniqueBookingIds(payload.returns,"id");attachRows(payload.returns,await loadVisualItems(env,ids),"id");return payload;
  }
  if(/^\/api\/admin\/customers\/[^/]+\/history$/.test(path)){
    const ids=uniqueBookingIds(payload.bookings,"id");attachRows(payload.bookings,await loadVisualItems(env,ids),"id");return payload;
  }
  if(path==="/api/admin/reports"){
    const type=url.searchParams.get("type")||"date-bookings";
    if(type==="date-bookings"){
      const ids=uniqueBookingIds(payload.rows,"id");attachRows(payload.rows,await loadVisualItems(env,ids),"id");
    }else if(type==="current-given"||type==="overdue"){
      const ids=uniqueBookingIds(payload.rows,"booking_id");const visual=await loadVisualItems(env,ids);
      for(const row of payload.rows||[]){const lines=visual.get(String(row.booking_id))||[];const exact=lines.find((line)=>line.item_id===String(row.item_id||""));row.image_url=exact?.image_url||null;row.category_name=row.category_name||exact?.category_name||"";row.item_lines=exact?[exact]:[];}
    }
    return payload;
  }
  return payload;
}
export default{
  async fetch(request,env){
    const response=await core.fetch(request,env);
    if(request.method!=="GET"||!response.ok)return response;
    const contentType=response.headers.get("content-type")||"";if(!contentType.includes(JSON_TYPE))return response;
    const url=new URL(request.url);if(!url.pathname.startsWith("/api/admin/"))return response;
    let payload;
    try{payload=await response.clone().json();payload=await enrichAdminPayload(url,payload,env);}catch(error){console.error("Phase14D response enrichment skipped",error);return response;}
    const headers=new Headers(response.headers);headers.set("content-type","application/json; charset=utf-8");
    return new Response(JSON.stringify(payload),{status:response.status,statusText:response.statusText,headers});
  }
};
