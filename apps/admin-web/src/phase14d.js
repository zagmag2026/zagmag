import "./phase14d.css";

const cache = {
  bookingsByNo: new Map(),
  bookingsById: new Map(),
  itemsByCode: new Map(),
  itemsById: new Map(),
  itemsByName: new Map()
};

function norm(value){return String(value||"").trim();}
function rememberCatalogItem(item){
  if(!item||typeof item!=="object")return;
  const normalized={...item};
  const id=norm(item.id||item.item_id); const code=norm(item.item_code); const name=norm(item.item_name);
  if(id)cache.itemsById.set(id,normalized);
  if(code)cache.itemsByCode.set(code,normalized);
  if(name)cache.itemsByName.set(name.toLowerCase(),normalized);
}
function hydrateLine(line){
  if(!line||typeof line!=="object")return line;
  const match=cache.itemsById.get(norm(line.item_id))||cache.itemsByCode.get(norm(line.item_code))||cache.itemsByName.get(norm(line.item_name).toLowerCase());
  return match?{...match,...line,image_url:line.image_url||match.image_url||null}:line;
}
function rememberLine(line){
  if(!line)return;
  const hydrated=hydrateLine(line); rememberCatalogItem(hydrated);
}
function mergeRows(oldRow,row){
  const incoming=Array.isArray(row?.item_lines)?row.item_lines.map(hydrateLine):[];
  const previous=Array.isArray(oldRow?.item_lines)?oldRow.item_lines:[];
  return {...oldRow,...row,item_lines:incoming.length?incoming:previous};
}
function rememberRow(row){
  if(!row)return;
  const bookingNo=norm(row.booking_no); const id=norm(row.id||row.booking_id);
  const old=bookingNo?cache.bookingsByNo.get(bookingNo):(id?cache.bookingsById.get(id):null);
  const merged=mergeRows(old||{},row);
  (merged.item_lines||[]).forEach(rememberLine);
  if(bookingNo)cache.bookingsByNo.set(bookingNo,merged);
  if(id)cache.bookingsById.set(id,merged);
}
function capturePayload(payload){
  if(!payload||typeof payload!=="object")return;
  [payload.bookings,payload.pickups,payload.returns,payload.rows,payload.todayBookings,payload.todayPickups,payload.todayReturns,payload.overdueReturns]
    .filter(Array.isArray).forEach(rows=>rows.forEach(rememberRow));

  if(Array.isArray(payload.items)){
    if(payload.booking?.booking_no){
      const lines=payload.items.map(item=>hydrateLine({...item,pending_qty:Math.max(0,Number(item.given_qty||0)-Number(item.returned_qty||0)),remaining_to_give:Math.max(0,Number(item.booked_qty||0)-Number(item.given_qty||0))}));
      lines.forEach(rememberLine); rememberRow({...payload.booking,item_lines:lines});
    }else{
      payload.items.forEach(rememberCatalogItem);
    }
  }
  scheduleScan();
}

const nativeFetch=globalThis.fetch.bind(globalThis);
globalThis.fetch=async(...args)=>{
  const response=await nativeFetch(...args);
  try{
    const url=typeof args[0]==="string"?args[0]:args[0]?.url||"";
    if(url.includes("/api/admin/")&&response.ok&&(response.headers.get("content-type")||"").includes("application/json")){
      const payload=await response.clone().json();
      capturePayload(payload);
    }
  }catch{}
  return response;
};

function bookingNoFrom(node){return node?.textContent?.match(/BK-[A-Z0-9-]+/i)?.[0]||"";}
function rowForNode(node){const no=bookingNoFrom(node);return no?cache.bookingsByNo.get(no):null;}
function initials(line){return norm(line?.item_name||line?.item_code||"?").slice(0,2).toUpperCase();}
function modeFor(node){
  if(node.closest(".return-card")||node.closest(".overdue-panel"))return "return";
  if(node.closest(".pickup-card"))return "pickup";
  const panelTitle=node.closest(".dashboard-panel")?.querySelector(".panel-head h2")?.textContent||"";
  if(/Pickup/i.test(panelTitle))return "pickup";
  if(/Return|Overdue/i.test(panelTitle))return "return";
  if(node.closest(".history-list")||node.closest(".report-row"))return "history";
  return "booking";
}
function meta(line,mode){
  if(mode==="pickup")return `Booked ${line.booked_qty||0} · Given ${line.given_qty||0} · Remaining ${line.remaining_to_give??Math.max(0,Number(line.booked_qty||0)-Number(line.given_qty||0))}`;
  if(mode==="return")return `Given ${line.given_qty||0} · Returned ${line.returned_qty||0} · Pending ${line.pending_qty??Math.max(0,Number(line.given_qty||0)-Number(line.returned_qty||0))}`;
  if(mode==="history")return `Booked ${line.booked_qty||0} · Given ${line.given_qty||0} · Returned ${line.returned_qty||0}`;
  return `Booked ${line.booked_qty||0}`;
}
function imageBox(line,detail=false){
  const image=document.createElement("div"); image.className=detail?"phase14d-detail-thumb":"phase14d-item-thumb";
  const url=norm(line?.image_url);
  const showPlaceholder=()=>{image.classList.add("is-placeholder");image.replaceChildren();const mark=document.createElement("span");mark.textContent=initials(line);const label=document.createElement("small");label.textContent="No image";image.append(mark,label);};
  if(url){
    const img=document.createElement("img");img.src=url;img.alt=line?.item_name||line?.item_code||"Item";img.loading="lazy";img.decoding="async";img.onerror=showPlaceholder;image.append(img);
  }else showPlaceholder();
  return image;
}
function visualCard(rawLine,mode){
  const line=hydrateLine(rawLine)||rawLine;
  const card=document.createElement("div");card.className="phase14d-item-card";
  const copy=document.createElement("div");copy.className="phase14d-item-copy";
  const strong=document.createElement("strong");strong.textContent=line.item_code?`${line.item_code} · ${line.item_name||""}`:(line.item_name||"Item");
  const category=document.createElement("span");category.textContent=line.category_name||"";
  const small=document.createElement("small");small.textContent=meta(line,mode);
  copy.append(strong,category,small);card.append(imageBox(line),copy);return card;
}
function fallbackLines(row){
  const text=norm(row?.items_summary);if(!text)return[];
  return text.split(/\s*,\s*/).map((part,index)=>{
    const match=part.match(/^(.*?)\s*[×x]\s*(\d+)$/i);const name=norm(match?.[1]||part);const qty=Math.max(0,Number(match?.[2]||0));
    const catalog=cache.itemsByName.get(name.toLowerCase())||{};
    return hydrateLine({...catalog,item_id:catalog.id||`fallback-${index}`,item_name:catalog.item_name||name,item_code:catalog.item_code||"",category_name:catalog.category_name||"",booked_qty:qty,given_qty:Number(row?.given_qty||0),returned_qty:Number(row?.returned_qty||0),remaining_to_give:Number(row?.remaining_to_give??row?.remaining_qty??0),pending_qty:Number(row?.pending_qty||0),image_url:catalog.image_url||null});
  }).filter(line=>line.item_name);
}
function uniqueLines(lines){
  const seen=new Set();
  return lines.filter((rawLine,index)=>{
    const line=hydrateLine(rawLine)||rawLine||{};
    const key=[
      norm(line.booking_item_id),norm(line.item_id),norm(line.item_code),norm(line.item_name),
      Number(line.booked_qty||0),Number(line.given_qty||0),Number(line.returned_qty||0),
      Number(line.pending_qty||0),Number(line.remaining_to_give||0)
    ].join("|")||`line-${index}`;
    if(seen.has(key))return false;
    seen.add(key);return true;
  });
}
function existingVisualGrid(node){
  const grids=[...node.querySelectorAll(":scope > .phase14d-item-grid[data-phase14d-visual='1']")];
  grids.slice(1).forEach(grid=>grid.remove());
  return grids[0]||null;
}
function renderBookingItems(node,row,anchor,oldSummary){
  if(!node)return;
  if(node.classList?.contains("report-primary"))node.querySelectorAll(":scope > .phase14d-item-card").forEach(card=>card.remove());
  const existing=existingVisualGrid(node);
  if(node.dataset.phase14dItems==="1"&&existing)return;
  if(node.dataset.phase14dItems==="1"&&!existing)delete node.dataset.phase14dItems;
  const mode=modeFor(node);let lines=Array.isArray(row?.item_lines)?row.item_lines.map(hydrateLine):[];
  if(!lines.length)lines=fallbackLines(row);
  lines=uniqueLines(lines);
  if(!lines.length)return;
  node.dataset.phase14dItems="1";
  if(oldSummary)oldSummary.style.display="none";
  const grid=document.createElement("div");grid.className="phase14d-item-grid";grid.dataset.phase14dVisual="1";
  lines.forEach(line=>grid.append(visualCard(line,mode)));
  (anchor||node.firstElementChild||node).insertAdjacentElement("afterend",grid);
}
function decorateCards(){
  document.querySelectorAll(".dashboard-row").forEach(node=>renderBookingItems(node,rowForNode(node),node.querySelector(".dashboard-row-head"),node.querySelector(":scope > p")));
  document.querySelectorAll(".booking-card").forEach(node=>renderBookingItems(node,rowForNode(node),node.querySelector(".booking-card-head"),node.querySelector(".booking-summary")));
  document.querySelectorAll(".pickup-card").forEach(node=>renderBookingItems(node,rowForNode(node),node.querySelector(".booking-card-head"),node.querySelector(":scope > p")));
  document.querySelectorAll(".history-list > article").forEach(node=>renderBookingItems(node,rowForNode(node),node.firstElementChild,node.querySelector(":scope > p")));
  document.querySelectorAll(".report-row").forEach(node=>{
    const row=rowForNode(node);const primary=node.querySelector(".report-primary");
    if(row&&primary)renderBookingItems(primary,row,primary.querySelector("span")||primary.querySelector("strong"),primary.querySelector("p"));
  });
}
function decorateDetailLines(){
  document.querySelectorAll(".detail-items article, .pickup-line:not(.pickup-line-head), .return-line:not(.return-line-head)").forEach(node=>{
    if(node.dataset.phase14dThumb==="1")return;const text=node.textContent||"";const code=[...cache.itemsByCode.keys()].find(value=>text.includes(value));const line=code?cache.itemsByCode.get(code):null;if(!line)return;
    const first=node.firstElementChild;if(!first)return;node.dataset.phase14dThumb="1";first.prepend(imageBox(line,true));first.classList.add("phase14d-detail-identity");
  });
}
function labelText(input){const label=input.closest("label");if(!label)return"";return[...label.childNodes].filter(n=>n.nodeType===Node.TEXT_NODE).map(n=>n.textContent||"").join(" ").trim();}
function setReactValue(input,value){const setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value")?.set;setter?.call(input,value);input.dispatchEvent(new Event("input",{bubbles:true}));input.dispatchEvent(new Event("change",{bubbles:true}));}
function findPairedInput(input,wanted){const scope=input.closest(".form-grid, .report-filters, .audit-filters")||input.parentElement?.parentElement;if(!scope)return null;return[...scope.querySelectorAll('input[type="date"]')].find(candidate=>labelText(candidate).toLowerCase().startsWith(wanted.toLowerCase()))||null;}
document.addEventListener("change",event=>{const input=event.target;if(!(input instanceof HTMLInputElement)||input.type!=="date"||!input.value)return;const label=labelText(input).toLowerCase();if(label.startsWith("pickup date")){const paired=findPairedInput(input,"return date");if(paired){paired.min=input.value;setReactValue(paired,input.value);}}else if(label.startsWith("from date")){const paired=findPairedInput(input,"to date");if(paired){paired.min=input.value;setReactValue(paired,input.value);}}},true);
function enforceDateMins(){document.querySelectorAll('input[type="date"]').forEach(input=>{const label=labelText(input).toLowerCase();if(label.startsWith("pickup date")||label.startsWith("from date")){const paired=findPairedInput(input,label.startsWith("pickup")?"return date":"to date");if(paired&&input.value)paired.min=input.value;}});}
function clearStaleDecorations(){document.querySelectorAll("[data-phase14d-items='1']").forEach(node=>{const no=bookingNoFrom(node);const row=no?cache.bookingsByNo.get(no):null;if(!row)return;const grid=existingVisualGrid(node);if(grid&&grid.children.length===0){grid.remove();delete node.dataset.phase14dItems;}});}
let scanQueued=false;function scheduleScan(){if(scanQueued)return;scanQueued=true;requestAnimationFrame(()=>{scanQueued=false;clearStaleDecorations();decorateCards();decorateDetailLines();enforceDateMins();});}
new MutationObserver(scheduleScan).observe(document.documentElement,{childList:true,subtree:true});
setInterval(scheduleScan,1200);
scheduleScan();
void import("./main.tsx");
