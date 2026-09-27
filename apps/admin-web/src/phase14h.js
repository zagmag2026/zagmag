import "./phase14ar-i18n-branding.css";
import "./phase14ar-auth-bootstrap-guard.js";
import "./phase14g.js";
import "./phase14e.js";
import "./phase14h.css";
import "./phase14j.js";
import "./phase14as-booking-flow.js";
import "./phase14as-fix1.js";
import "./phase14as-fix2.js";

// Phase14AR language runtimes are deliberately deferred until React has left the
// startup splash. The loader watches only top-level #root view replacement, never
// signed-in page mutations/navigation.
let phase14arMode="";
let phase14arQueued=false;
async function loadPhase14arForReadyView(){
  phase14arQueued=false;
  const hasShell=Boolean(document.querySelector(".shell"));
  const hasAuth=Boolean(document.querySelector(".auth-page"));
  if(hasShell&&phase14arMode!=="admin"){
    if(phase14arMode==="auth")document.querySelector('.phase14ar-language-button[data-phase14ar-auth-owned="1"]')?.remove();
    phase14arMode="admin";
    await import("./phase14ar-i18n-branding.js");
    window.dispatchEvent(new Event("phase14ar-admin-ready"));
    return;
  }
  if(hasAuth&&phase14arMode!=="auth"){
    phase14arMode="auth";
    await import("./phase14ar-auth-i18n.js");
    window.dispatchEvent(new Event("phase14ar-auth-ready"));
  }
}
function schedulePhase14arReadyView(){
  if(phase14arQueued)return;
  phase14arQueued=true;
  queueMicrotask(()=>{void loadPhase14arForReadyView()});
}
const phase14arRoot=document.getElementById("root");
if(phase14arRoot)new MutationObserver(schedulePhase14arReadyView).observe(phase14arRoot,{childList:true});
schedulePhase14arReadyView();

const CLICK_GUARD_MS=700,SUBMIT_GUARD_MS=1000;
const lastClick=new WeakMap(),lastSubmit=new WeakMap();
// Auth is React-owned and already disables its submit button while busy. Keep global
// capture guards away from the login/setup forms so mobile/synthetic submit events
// cannot be swallowed before React handles them.
document.addEventListener("click",event=>{const target=event.target instanceof Element?event.target.closest("button,[role='button']"):null;if(!target||target.closest(".auth-page")||target.hasAttribute("disabled")||target.getAttribute("aria-disabled")==="true")return;const now=performance.now(),previous=lastClick.get(target)||0;if(now-previous<CLICK_GUARD_MS){event.preventDefault();event.stopImmediatePropagation();return}lastClick.set(target,now)},true);
document.addEventListener("submit",event=>{const form=event.target;if(!(form instanceof HTMLFormElement)||form.closest(".auth-page"))return;const now=performance.now(),previous=lastSubmit.get(form)||0;if(now-previous<SUBMIT_GUARD_MS){event.preventDefault();event.stopImmediatePropagation();return}lastSubmit.set(form,now)},true);
