import React, { useEffect, useMemo, useRef, useState } from "react";
import ReactDOM from "react-dom/client";
import "./styles.css";
import "./phase14h.css";
import "./phase14i.css";

type Category = { id:string; name:string; codePrefix:string; itemCount:number };
type PublicField = { fieldId:string; name:string; type:string; value:string };
type PublicItem = {
  id:string; itemCode:string; itemName:string; categoryId:string; categoryName:string;
  primaryImage:string|null; images:string[]; fields:PublicField[];
};
type Shop = { name:string; websiteTitle?:string; logoUrl?:string|null; address?:string|null; whatsappNumber:string|null; callNumber:string|null };
type Pagination = { page:number; pageSize:number; total:number; totalPages:number };
type CatalogResponse = { ok:boolean; catalogEnabled?:boolean; availabilityMode?:"STATUS_ONLY"|"EXACT"; shop:Shop; categories:Category[]; items:PublicItem[]; pagination:Pagination; message?:string };
type AvailabilityStatus = "AVAILABLE" | "LIMITED" | "FEW_LEFT" | "NOT_AVAILABLE";
type AvailabilityInfo = {status:AvailabilityStatus;availableQuantity?:number};
type PublicBrowseState = {search:string;categoryId:string;page:number;pickupDate:string;returnDate:string};
type PublicWhatsAppPreview = {mobile:string;message:string;templateName:string};

const API_BASE = String((import.meta as any).env?.VITE_API_BASE || "").replace(/\/$/, "");
async function publicJson<T=any>(path:string,init:RequestInit={},timeoutMs=15000):Promise<T>{
  const controller=init.signal?null:new AbortController();
  const timer=controller?window.setTimeout(()=>controller.abort(),timeoutMs):null;
  try{
    const response=await fetch(`${API_BASE}${path}`,{...init,signal:init.signal||controller?.signal});
    const data=await response.json().catch(()=>({}));
    if(!response.ok||data?.ok===false)throw new Error(data?.message||"Request failed.");
    return data as T;
  }catch(error){
    if(error instanceof DOMException&&error.name==="AbortError")throw new Error("Request timed out. Please try again.");
    throw error;
  }finally{if(timer!==null)window.clearTimeout(timer)}
}
const today = () => new Intl.DateTimeFormat("en-CA", { year:"numeric", month:"2-digit", day:"2-digit" }).format(new Date());
const digits = (value:string|null) => (value || "").replace(/\D/g, "");
const CLICK_GUARD_MS=700, SUBMIT_GUARD_MS=1000;
const PUBLIC_BROWSE_KEY="zhagmag:public:browse:v1";
const lastClick=new WeakMap<Element,number>(), lastSubmit=new WeakMap<HTMLFormElement,number>();

function nextDate(iso:string){const [y,m,d]=iso.split("-").map(Number);if(!y||!m||!d)return"";const value=new Date(y,m-1,d);value.setDate(value.getDate()+1);return new Intl.DateTimeFormat("en-CA",{year:"numeric",month:"2-digit",day:"2-digit"}).format(value);}
function readBrowseState():PublicBrowseState{
  const base={search:"",categoryId:"",page:1,pickupDate:today(),returnDate:nextDate(today())};
  try{
    const raw=sessionStorage.getItem(PUBLIC_BROWSE_KEY);if(!raw)return base;
    const value=JSON.parse(raw) as Partial<PublicBrowseState>;
    const pickup=typeof value.pickupDate==="string"&&value.pickupDate>=today()?value.pickupDate:base.pickupDate;
    const minimum=nextDate(pickup);
    const ret=typeof value.returnDate==="string"&&value.returnDate>=minimum?value.returnDate:minimum;
    return {search:typeof value.search==="string"?value.search:"",categoryId:typeof value.categoryId==="string"?value.categoryId:"",page:Math.max(1,Number(value.page)||1),pickupDate:pickup,returnDate:ret};
  }catch{return base;}
}

document.addEventListener("click",event=>{
  const target=event.target instanceof Element?event.target.closest("button,[role='button']"):null;
  if(!target||target.hasAttribute("disabled")||target.getAttribute("aria-disabled")==="true")return;
  const now=performance.now(), previous=lastClick.get(target)||0;
  if(now-previous<CLICK_GUARD_MS){event.preventDefault();event.stopImmediatePropagation();return;}
  lastClick.set(target,now);
},true);

document.addEventListener("submit",event=>{
  const form=event.target;
  if(!(form instanceof HTMLFormElement))return;
  const now=performance.now(), previous=lastSubmit.get(form)||0;
  if(now-previous<SUBMIT_GUARD_MS){event.preventDefault();event.stopImmediatePropagation();return;}
  lastSubmit.set(form,now);
},true);

function availabilityLabel(info?:AvailabilityInfo,mode:"STATUS_ONLY"|"EXACT"="STATUS_ONLY"){
  if(!info) return "Check Availability";
  if(info.status === "NOT_AVAILABLE") return "Not Available";
  if(mode==="EXACT" && typeof info.availableQuantity==="number") return `Available: ${info.availableQuantity}`;
  if(info.status === "FEW_LEFT") return "Few Left";
  if(info.status === "LIMITED") return "Limited";
  return "Available";
}

function itemImages(item:PublicItem){
  const values=[item.primaryImage,...item.images].filter((value):value is string=>Boolean(value&&value.trim()));
  return [...new Set(values)];
}

function CardImageCarousel({item,onOpen}:{item:PublicItem;onOpen:()=>void}){
  const images=useMemo(()=>itemImages(item),[item]);
  const [index,setIndex]=useState(0);
  const pauseUntil=useRef(0);
  useEffect(()=>{setIndex(0);},[item.id]);
  useEffect(()=>{
    if(images.length<2)return;
    const reduced=window.matchMedia("(prefers-reduced-motion: reduce)");
    const timer=window.setInterval(()=>{if(!document.hidden&&!reduced.matches&&Date.now()>=pauseUntil.current)setIndex(current=>(current+1)%images.length);},4800);
    return ()=>window.clearInterval(timer);
  },[images.length]);
  return <button className="image-button public-card-carousel" onPointerDown={()=>{pauseUntil.current=Date.now()+12000}} onClick={onOpen} aria-label={`${item.itemName} details`}>
    {images.length?<img className="public-card-slide-image" src={images[index]} alt={item.itemName} loading="lazy" onLoad={e=>e.currentTarget.parentElement?.classList.remove("image-error")} onError={e=>{e.currentTarget.style.display="none";e.currentTarget.parentElement?.classList.add("image-error")}} />:<div className="photo-fallback">{item.itemName.slice(0,2).toUpperCase()}</div>}
    {images.length>1?<><span className="public-card-slide-count">{index+1}/{images.length}</span><span className="public-card-slide-dots" aria-hidden="true">{images.map((_,dot)=><span key={dot} className={`public-card-slide-dot ${dot===index?"active":""}`}/>)}</span></>:null}
  </button>;
}

function DetailImageCarousel({item}:{item:PublicItem}){
  const images=useMemo(()=>itemImages(item),[item]);
  const [index,setIndex]=useState(0);
  const touchStart=useRef<number|null>(null);
  const pauseUntil=useRef(0);
  useEffect(()=>{setIndex(0);},[item.id]);
  useEffect(()=>{
    if(images.length<2)return;
    const reduced=window.matchMedia("(prefers-reduced-motion: reduce)");
    const timer=window.setInterval(()=>{if(!document.hidden&&!reduced.matches&&Date.now()>=pauseUntil.current)setIndex(current=>(current+1)%images.length);},4800);
    return ()=>window.clearInterval(timer);
  },[images.length]);
  function move(delta:number){pauseUntil.current=Date.now()+12000;if(images.length>1)setIndex(current=>(current+delta+images.length)%images.length);}
  if(!images.length)return <div className="gallery public-detail-carousel"><div className="photo-fallback large">{item.itemName.slice(0,2).toUpperCase()}</div></div>;
  return <div className="gallery public-detail-carousel"
    onPointerDown={()=>{pauseUntil.current=Date.now()+12000}}
    onTouchStart={event=>{touchStart.current=event.touches[0]?.clientX??null;}}
    onTouchEnd={event=>{const start=touchStart.current;if(start===null)return;const end=event.changedTouches[0]?.clientX??start;const delta=end-start;if(Math.abs(delta)>=40)move(delta>0?-1:1);touchStart.current=null;}}>
    <img className="public-detail-active" src={images[index]} alt={`${item.itemName} ${index+1}`} loading="lazy" onLoad={e=>e.currentTarget.parentElement?.classList.remove("image-error")} onError={e=>{e.currentTarget.style.display="none";e.currentTarget.parentElement?.classList.add("image-error")}} />
    {images.length>1?<><button type="button" className="public-detail-arrow previous" onClick={()=>move(-1)} aria-label="Previous image">‹</button><button type="button" className="public-detail-arrow next" onClick={()=>move(1)} aria-label="Next image">›</button><span className="public-detail-count">{index+1}/{images.length}</span><span className="public-detail-dots">{images.map((_,dot)=><button type="button" key={dot} className={`public-detail-dot ${dot===index?"active":""}`} onClick={()=>{pauseUntil.current=Date.now()+12000;setIndex(dot)}} aria-label={`Show image ${dot+1}`}/>)}</span></>:null}
  </div>;
}

function App() {
  const [initialBrowse]=useState(readBrowseState);
  const [shop,setShop]=useState<Shop>({name:"ઝગમગ ડ્રેસીસ",websiteTitle:"ઝગમગ ડ્રેસીસ",whatsappNumber:null,callNumber:null});
  const [catalogEnabled,setCatalogEnabled]=useState(true);
  const [availabilityMode,setAvailabilityMode]=useState<"STATUS_ONLY"|"EXACT">("EXACT");
  const [categories,setCategories]=useState<Category[]>([]);
  const [items,setItems]=useState<PublicItem[]>([]);
  const [pagination,setPagination]=useState<Pagination>({page:initialBrowse.page,pageSize:12,total:0,totalPages:1});
  const [searchInput,setSearchInput]=useState(initialBrowse.search);
  const [search,setSearch]=useState(initialBrowse.search);
  const [categoryId,setCategoryId]=useState(initialBrowse.categoryId);
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState("");
  const [pickupDate,setPickupDate]=useState(initialBrowse.pickupDate);
  const [returnDate,setReturnDate]=useState(initialBrowse.returnDate);
  const [availability,setAvailability]=useState<Record<string,AvailabilityInfo>>({});
  const [availabilityLoading,setAvailabilityLoading]=useState(false);
  const [selected,setSelected]=useState<PublicItem|null>(null);
  const [whatsappBusy,setWhatsappBusy]=useState(false);
  const [whatsappError,setWhatsappError]=useState("");
  const [whatsappPreview,setWhatsappPreview]=useState<PublicWhatsAppPreview|null>(null);
  const [logoFailed,setLogoFailed]=useState(false);
  const catalogSerial=useRef(0);const availabilitySerial=useRef(0);const whatsappSerial=useRef(0);
  useEffect(()=>setLogoFailed(false),[shop.logoUrl]);

  async function loadCatalog(page=1,nextSearch=search,nextCategory=categoryId){
    const serial=++catalogSerial.current;availabilitySerial.current++;
    setLoading(true); setError("");
    try{
      const qs=new URLSearchParams({page:String(page),pageSize:"12"});
      if(nextSearch)qs.set("search",nextSearch);
      if(nextCategory)qs.set("categoryId",nextCategory);
      const data=await publicJson<CatalogResponse>(`/api/public/catalog?${qs.toString()}`);
      if(serial!==catalogSerial.current)return;
      setShop(data.shop); setCatalogEnabled(data.catalogEnabled!==false); setAvailabilityMode(data.availabilityMode==="EXACT"?"EXACT":"STATUS_ONLY"); setCategories(data.categories); setItems(data.items); setPagination(data.pagination);
      setAvailability({});
    }catch(err){ if(serial===catalogSerial.current)setError(err instanceof Error?err.message:"Unable to load catalog."); }
    finally{ if(serial===catalogSerial.current)setLoading(false); }
  }

  useEffect(()=>{ void loadCatalog(initialBrowse.page,initialBrowse.search,initialBrowse.categoryId); },[]);
  useEffect(()=>{
    try{sessionStorage.setItem(PUBLIC_BROWSE_KEY,JSON.stringify({search,categoryId,page:pagination.page,pickupDate,returnDate} satisfies PublicBrowseState));}catch{}
  },[search,categoryId,pagination.page,pickupDate,returnDate]);

  const selectedCategoryName=useMemo(()=>categories.find(c=>c.id===categoryId)?.name || "All Items",[categories,categoryId]);
  const whatsappDigits=digits(shop.whatsappNumber);
  const callDigits=digits(shop.callNumber);
  const initialLoading=loading&&!items.length;
  const refreshing=loading&&items.length>0;

  function submitSearch(e:React.FormEvent){
    e.preventDefault(); const value=searchInput.trim(); setSearch(value); void loadCatalog(1,value,categoryId);
  }
  function chooseCategory(id:string){ setCategoryId(id); void loadCatalog(1,search,id); }

  async function checkAvailability(){
    if(!pickupDate || !returnDate || pickupDate>returnDate || !items.length){
      setError("Pickup અને Return date સાચી રીતે પસંદ કરો."); return;
    }
    const serial=++availabilitySerial.current;setAvailabilityLoading(true); setError("");
    try{
      const data=await publicJson<{ok:boolean;message?:string;availabilityMode?:"STATUS_ONLY"|"EXACT";availability?:Array<{itemId:string;status:AvailabilityStatus;availableQuantity?:number}>}>("/api/public/availability",{
        method:"POST",headers:{"content-type":"application/json"},
        body:JSON.stringify({pickupDate,returnDate,itemIds:items.map(i=>i.id)})
      });
      if(serial!==availabilitySerial.current)return;
      const map:Record<string,AvailabilityInfo>={};
      for(const row of data.availability || [])map[row.itemId]={status:row.status,availableQuantity:row.availableQuantity};
      if(data.availabilityMode)setAvailabilityMode(data.availabilityMode);
      setAvailability(map);
      document.getElementById("items")?.scrollIntoView({behavior:"smooth",block:"start"});
    }catch(err){if(serial===availabilitySerial.current)setError(err instanceof Error?err.message:"Availability check failed.");}
    finally{if(serial===availabilitySerial.current)setAvailabilityLoading(false);}
  }

  async function prepareWhatsAppInquiry(item?:PublicItem){
    if(whatsappBusy||!whatsappDigits)return;
    const serial=++whatsappSerial.current;setWhatsappBusy(true);
    setWhatsappError("");
    setWhatsappPreview(null);
    try{
      const q=new URLSearchParams();
      if(item?.id)q.set("itemId",item.id);
      if(pickupDate)q.set("pickupDate",pickupDate);
      if(returnDate)q.set("returnDate",returnDate);
      const data=await publicJson<any>(`/api/public/whatsapp-inquiry?${q.toString()}`,{headers:{"accept":"application/json"}});
      if(serial!==whatsappSerial.current)return;
      if(!data.mobile||!data.message)throw new Error("WhatsApp inquiry is unavailable right now.");
      setWhatsappPreview({
        mobile:String(data.mobile),
        message:String(data.message),
        templateName:String(data.templateName||"General Inquiry")
      });
    }catch(err){
      if(serial===whatsappSerial.current)setWhatsappError(err instanceof Error?err.message:"WhatsApp inquiry is unavailable right now.");
    }finally{
      if(serial===whatsappSerial.current)setWhatsappBusy(false);
    }
  }

  function openPreparedWhatsApp(){
    const preview=whatsappPreview;
    if(!preview)return;
    const mobile=digits(preview.mobile);
    if(!mobile){setWhatsappError("WhatsApp inquiry is unavailable right now.");setWhatsappPreview(null);return;}
    window.open(`https://wa.me/${mobile}?text=${encodeURIComponent(preview.message)}`,"_blank","noopener,noreferrer");
    setWhatsappPreview(null);
  }  useEffect(()=>{
    if(!selected&&!whatsappPreview)return;
    const timer=window.setTimeout(()=>{const dialogs=[...document.querySelectorAll<HTMLElement>('[role="dialog"]')];const dialog=dialogs[dialogs.length-1];(dialog?.querySelector<HTMLElement>('button,a[href],input,select,textarea,[tabindex]:not([tabindex="-1"])')||dialog)?.focus()},0);
    const onKey=(event:KeyboardEvent)=>{const dialogs=[...document.querySelectorAll<HTMLElement>('[role="dialog"]')];const dialog=dialogs[dialogs.length-1];if(!dialog)return;if(event.key==="Escape"){event.preventDefault();if(whatsappPreview)setWhatsappPreview(null);else setSelected(null);return}if(event.key!=="Tab")return;const nodes=[...dialog.querySelectorAll<HTMLElement>('button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])')];if(!nodes.length)return;const first=nodes[0],last=nodes[nodes.length-1];if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus()}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus()}};
    document.addEventListener("keydown",onKey);return()=>{window.clearTimeout(timer);document.removeEventListener("keydown",onKey)};
  },[selected,whatsappPreview]);



  return (
    <div className="app">
      <header className="topbar">
        <a className="brand-wrap" href="#top" aria-label={shop.name}>
          {shop.logoUrl&&!logoFailed?<img className="brand-logo" src={shop.logoUrl} alt="" onError={()=>setLogoFailed(true)}/>:<div className="brand-mark">ઝ</div>}
          <div><div className="brand">{shop.name}</div><div className="tagline">Traditional Rental Collection</div></div>
        </a>
        <nav><a href="#categories">Categories</a><a href="#items">Items</a><a href="#availability">Availability</a><a href="#contact">Contact</a></nav>
        <div className="quick-contact">
          {whatsappDigits ? <button type="button" className="mini-btn" onClick={()=>void prepareWhatsAppInquiry()} disabled={whatsappBusy}>{whatsappBusy?"Loading…":"WhatsApp"}</button> : null}
          {callDigits ? <a className="mini-btn" href={`tel:+${callDigits}`}>Call</a> : null}
        </div>
      </header>

      {whatsappError?<div className="notice error" role="alert">{whatsappError}</div>:null}
      <main id="top">
        <section className="hero">
          <div>
            <span className="eyebrow">Zhagmag Collection</span>
            <h1>{shop.websiteTitle || "Find the dress you love with ease"}</h1>
            <p>View dress photos and details, check date-wise availability, and contact us directly for your selection.</p>
            <div className="hero-actions"><a className="btn primary" href="#items">View Collection</a><a className="btn secondary" href="#availability">Check Availability</a></div>
          </div>
          <div className="hero-card"><div className="status-dot"></div><strong>Live Catalog</strong><span>{pagination.total} public item{pagination.total===1?"":"s"}</span><small>New selections will be added from time to time.</small></div>
        </section>

        <section id="categories" className="section compact-section">
          <div className="section-head"><div><span className="eyebrow">Browse</span><h2>Categories</h2></div><span className="pill">{categories.length} Active</span></div>
          <div className="category-row">
            <button className={`category-chip ${categoryId===""?"active":""}`} onClick={()=>chooseCategory("")}>All <span>{categories.reduce((s,c)=>s+c.itemCount,0)}</span></button>
            {categories.map(c=><button key={c.id} className={`category-chip ${categoryId===c.id?"active":""}`} onClick={()=>chooseCategory(c.id)}>{c.name}<span>{c.itemCount}</span></button>)}
          </div>
        </section>

        <section id="availability" className="section availability-section">
          <div><span className="eyebrow">Date-wise check</span><h2>Check Availability</h2><p>Check availability for your selected dates.</p></div>
          <div className="availability-box">
            <label>Pickup Date<input min={today()} type="date" value={pickupDate} onChange={e=>{const value=e.target.value;setPickupDate(value);const minimum=nextDate(value);if(returnDate<minimum)setReturnDate(minimum);setAvailability({});}} /></label>
            <label>Return Date<input min={nextDate(pickupDate)||today()} type="date" value={returnDate} onChange={e=>{setReturnDate(e.target.value);setAvailability({});}} /></label>
            <button className="btn primary" onClick={()=>void checkAvailability()} disabled={availabilityLoading || !items.length}>{availabilityLoading?"Checking…":"Check Availability"}</button>
          </div>
        </section>

        <section id="items" className="section">
          <div className="section-head item-head"><div><span className="eyebrow">{selectedCategoryName}</span><h2>Items</h2></div>
            <form className="search-form" onSubmit={submitSearch}><input placeholder="Item name or code" value={searchInput} onChange={e=>setSearchInput(e.target.value)} /><button className="btn secondary" type="submit">Search</button>{search?<button type="button" className="text-btn" onClick={()=>{setSearch("");setSearchInput("");void loadCatalog(1,"",categoryId);}}>Clear</button>:null}</form>
          </div>

          {error?<div className="notice error" role="alert">{error}{initialLoading?<button type="button" className="mini-btn" onClick={()=>void loadCatalog(initialBrowse.page,search,categoryId)}>Retry</button>:null}</div>:null}
          {refreshing?<div className="phase14u-refreshing" role="status" aria-live="polite">Refreshing collection…</div>:null}
          {!loading&&!catalogEnabled?<div className="empty-state"><strong>Public Catalog is currently unavailable.</strong><br/>Please contact us by WhatsApp or Call.</div>:null}
          {initialLoading?<div className="empty-state">Catalog loading…</div>:null}
          {!loading && !items.length?<div className="empty-state">No items found for this selection.</div>:null}
          {items.length ? <div className={`item-grid ${refreshing?"phase14u-stale-grid":""}`} aria-busy={refreshing}>
            {items.map(item=>{
              const info=availability[item.id];
              const status=info?.status;
              return <article className="item-card" key={item.id}>
                <CardImageCarousel item={item} onOpen={()=>setSelected(item)} />
                <div className="item-body">
                  <div className="item-title-row"><div><span className="item-code">{item.itemCode}</span><h3>{item.itemName}</h3></div><span className="category-tag">{item.categoryName}</span></div>
                  {item.fields.length?<div className="field-list">{item.fields.slice(0,3).map(f=><span key={f.fieldId}><b>{f.name}:</b> {f.value}</span>)}</div>:null}
                  <div className={`availability-badge ${status?status.toLowerCase():"unchecked"}`}>{availabilityLabel(info,availabilityMode)}</div>
                  <div className="card-actions"><button className="btn secondary small" onClick={()=>setSelected(item)}>Details</button>{whatsappDigits?<button type="button" className="btn primary small" onClick={()=>void prepareWhatsAppInquiry(item)} disabled={whatsappBusy}>{whatsappBusy?"Loading…":"WhatsApp"}</button>:null}{callDigits?<a className="icon-action" href={`tel:+${callDigits}`} aria-label="Call">Call</a>:null}</div>
                </div>
              </article>
            })}
          </div>:null}

          {pagination.totalPages>1?<div className="pager"><button disabled={pagination.page<=1||loading} onClick={()=>void loadCatalog(pagination.page-1)}>Previous</button><span>Page {pagination.page} / {pagination.totalPages}</span><button disabled={pagination.page>=pagination.totalPages||loading} onClick={()=>void loadCatalog(pagination.page+1)}>Next</button></div>:null}
        </section>
      </main>

      <footer id="contact"><div><strong>{shop.name}</strong><span>{shop.address||"Contact us for your selection"}</span></div><div className="footer-actions">{whatsappDigits?<button type="button" className="btn primary small" data-contact-kind="whatsapp" onClick={()=>void prepareWhatsAppInquiry()} disabled={whatsappBusy}>{whatsappBusy?"Loading…":"WhatsApp"}</button>:null}{callDigits?<a className="btn secondary small" href={`tel:+${callDigits}`}>Call</a>:null}</div></footer>

      {selected?<div className="modal-backdrop" role="presentation" onMouseDown={e=>{if(e.target===e.currentTarget)setSelected(null)}}><section className="detail-modal" role="dialog" aria-modal="true" aria-label={`${selected.itemName} details`}>
        <div className="modal-head"><div><span className="item-code">{selected.itemCode}</span><h2>{selected.itemName}</h2><span className="category-tag">{selected.categoryName}</span></div><button className="close-btn" onClick={()=>setSelected(null)} aria-label="Close">×</button></div>
        <div className="detail-grid"><DetailImageCarousel item={selected}/>
          <div className="detail-info"><h3>Item Details</h3>{selected.fields.length?<dl>{selected.fields.map(f=><React.Fragment key={f.fieldId}><dt>{f.name}</dt><dd>{f.value}</dd></React.Fragment>)}</dl>:<p>No additional details are available.</p>}<div className={`availability-badge ${availability[selected.id]?.status?availability[selected.id].status.toLowerCase():"unchecked"}`}>{availabilityLabel(availability[selected.id],availabilityMode)}</div><div className="modal-actions">{whatsappDigits?<button type="button" className="btn primary" onClick={()=>void prepareWhatsAppInquiry(selected)} disabled={whatsappBusy}>{whatsappBusy?"Loading…":"WhatsApp Inquiry"}</button>:null}{callDigits?<a className="btn secondary" href={`tel:+${callDigits}`}>Call</a>:null}</div></div>
        </div>
      </section></div>:null}

      {whatsappPreview?<div className="modal-backdrop" role="presentation" onMouseDown={e=>{if(e.target===e.currentTarget)setWhatsappPreview(null)}}><section className="detail-modal whatsapp-preview-modal" role="dialog" aria-modal="true" aria-label="WhatsApp message preview">
        <div className="modal-head"><div><span className="item-code">WhatsApp</span><h2>Message Preview</h2><span className="category-tag">{whatsappPreview.templateName}</span></div><button className="close-btn" onClick={()=>setWhatsappPreview(null)} aria-label="Close">×</button></div>
        <div className="detail-info"><p style={{whiteSpace:"pre-wrap"}}>{whatsappPreview.message}</p><div className="modal-actions"><button type="button" className="btn secondary" onClick={()=>setWhatsappPreview(null)}>Cancel</button><button type="button" className="btn primary" onClick={openPreparedWhatsApp}>Open WhatsApp</button></div></div>
      </section></div>:null}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")!).render(<App />);
