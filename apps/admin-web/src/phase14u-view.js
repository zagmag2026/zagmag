/* Phase 14U previous-view memory. It never calls fetch directly. */
const uText=node=>String(node?.textContent||"").trim().replace(/\s+/g," ");
const uModules=new Set(["Items","Customers","Bookings","Pickup","Returns","Reports","Users"]);
let uSaveTimer=0,uQueued=false;
function uModule(){return uText(document.querySelector(".page-head h1"))||"Dashboard"}
function uRoot(m=uModule()){
  if(m==="Items")return document.querySelector(".item-filters");
  if(m==="Customers")return document.querySelector(".customer-filters");
  if(m==="Bookings")return document.querySelector(".booking-filters");
  if(m==="Pickup"||m==="Returns")return document.querySelector(".pickup-toolbar");
  if(m==="Reports")return document.querySelector(".report-filters");
  if(m==="Users")return document.querySelector(".users-toolbar");
  return null;
}
function uKey(m=uModule()){return`zhagmag:phase14u:view:${m}`}
function uPage(){const m=uText(document.querySelector(".pager span")).match(/Page\s+(\d+)/i);return m?Number(m[1]):1}
function uActive(m=uModule()){if(m==="Pickup"||m==="Returns")return uText(document.querySelector(".pickup-tabs button.active"));if(m==="Reports")return uText(document.querySelector(".report-tabs button.active"));return""}
function uCapture(){const m=uModule(),root=uRoot(m);if(!uModules.has(m)||!root)return null;const controls=[...root.querySelectorAll("input,select")].map((c,i)=>({i,tag:c.tagName,type:c instanceof HTMLInputElement?c.type:"",placeholder:c.getAttribute("placeholder")||"",value:c instanceof HTMLInputElement&&c.type==="checkbox"?String(c.checked):String(c.value||"")}));return{v:1,module:m,controls,active:uActive(m),page:uPage(),savedAt:Date.now()}}
function uStore(){const state=uCapture();if(!state)return;try{sessionStorage.setItem(uKey(state.module),JSON.stringify(state))}catch{}}
/* Keep view memory silent. Search/filter actions must never add a second page-header row or restore control. */
function uRender(){uQueued=false;document.querySelector(".phase14u-restore-view")?.remove();document.querySelectorAll(".phase14t-restore-query").forEach(node=>node.remove())}
function uSchedule(){if(uQueued)return;uQueued=true;requestAnimationFrame(uRender)}
function uLaterStore(){clearTimeout(uSaveTimer);uSaveTimer=setTimeout(()=>{uStore();uSchedule()},120)}
document.addEventListener("input",e=>{if(e.isTrusted&&uRoot()?.contains(e.target))uLaterStore()},true);
document.addEventListener("change",e=>{if(e.isTrusted&&uRoot()?.contains(e.target))uLaterStore()},true);
document.addEventListener("click",e=>{if(!e.isTrusted)return;const x=e.target instanceof Element?e.target:null;if(x?.closest(".pager,.pickup-tabs,.report-tabs"))uLaterStore()},true);
new MutationObserver(uSchedule).observe(document.documentElement,{childList:true,subtree:true});uSchedule();
