/* Phase 14U refresh presentation — no duplicate list/header injection.
   UI-only. Search/Apply refresh uses the native React loading state and never clones prior results. */
const usText=node=>String(node?.textContent||"").trim().replace(/\s+/g," ");
let usQueued=false;
function usLoading(card){return[...card.querySelectorAll(":scope > .empty")].find(n=>/^(Loading|Refreshing)/i.test(usText(n)))||null}
function usClear(){document.querySelectorAll(".phase14u-stale-copy,.phase14u-refresh-badge").forEach(n=>n.remove())}
function usScan(){
  usQueued=false;
  /* Clean up any legacy nodes left by an older runtime. Do not add replacement UI. */
  usClear();
  document.querySelectorAll(".list-card").forEach(card=>{
    const loading=usLoading(card);
    if(loading instanceof HTMLElement){
      loading.setAttribute("role","status");
      loading.setAttribute("aria-live","polite");
    }
  });
}
function usSchedule(){if(usQueued)return;usQueued=true;requestAnimationFrame(usScan)}
new MutationObserver(usSchedule).observe(document.documentElement,{childList:true,subtree:true});
usSchedule();
