/* Phase 14S visible-label and loading feedback cleanup. */
function phase14sPolish(){
  const sub=document.querySelector(".topbar .sub");if(sub&&sub.textContent!=="Admin Panel")sub.textContent="Admin Panel";
  document.querySelectorAll(".empty").forEach(n=>n.classList.toggle("phase14s-loading",/^(Loading|Refreshing|Please wait)/i.test(String(n.textContent||"").trim())))
}
new MutationObserver(phase14sPolish).observe(document.documentElement,{childList:true,subtree:true,characterData:true});phase14sPolish();
