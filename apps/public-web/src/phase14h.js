const CLICK_GUARD_MS=700,SUBMIT_GUARD_MS=1000;
const lastClick=new WeakMap(),lastSubmit=new WeakMap();
document.addEventListener("click",event=>{const target=event.target instanceof Element?event.target.closest("button,[role='button']"):null;if(!target||target.hasAttribute("disabled")||target.getAttribute("aria-disabled")==="true")return;const now=performance.now(),previous=lastClick.get(target)||0;if(now-previous<CLICK_GUARD_MS){event.preventDefault();event.stopImmediatePropagation();return}lastClick.set(target,now)},true);
document.addEventListener("submit",event=>{const form=event.target;if(!(form instanceof HTMLFormElement))return;const now=performance.now(),previous=lastSubmit.get(form)||0;if(now-previous<SUBMIT_GUARD_MS){event.preventDefault();event.stopImmediatePropagation();return}lastSubmit.set(form,now)},true);
function cleanPublicCopy(){
  const hero=document.querySelector(".hero");
  const eyebrow=hero?.querySelector(".eyebrow");if(eyebrow&&eyebrow.textContent!=="ઝગમગ કલેક્શન")eyebrow.textContent="ઝગમગ કલેક્શન";
  const heroP=hero?.querySelector("p");if(heroP&&/Pricing website|Item photos/.test(heroP.textContent||""))heroP.textContent="ડ્રેસના ફોટા અને વિગતો જુઓ, તારીખ પ્રમાણે ઉપલબ્ધતા તપાસો અને પસંદગી માટે સીધો સંપર્ક કરો.";
  const heroSmall=document.querySelector(".hero-card small");if(heroSmall&&/Admin|Future categories/.test(heroSmall.textContent||""))heroSmall.textContent="નવી પસંદગીઓ સમયાંતરે ઉમેરાતી રહેશે.";
  const availP=document.querySelector(".availability-section>div:first-child p");if(availP&&/Selected date range|System Available/.test(availP.textContent||""))availP.textContent="પસંદ કરેલી તારીખ માટે ઉપલબ્ધતા જુઓ.";
  document.querySelectorAll(".availability-box .btn").forEach(btn=>{if(btn.textContent&&/Current Items Check/.test(btn.textContent))btn.textContent="Availability તપાસો"});
  document.querySelectorAll(".empty-state").forEach(node=>{if(/filter માટે public items/.test(node.textContent||""))node.textContent="આ પસંદગી માટે કોઈ item મળ્યું નથી."});
  document.querySelectorAll("footer span").forEach(node=>{if(/Public catalog · Pricing વગર/.test(node.textContent||""))node.textContent="તમારી પસંદગી માટે સંપર્ક કરો";if(/Settingsમાં configure/.test(node.textContent||""))node.remove()});
  document.querySelectorAll(".detail-info p").forEach(node=>{if(/Additional public details/.test(node.textContent||""))node.textContent="વધુ વિગતો ઉપલબ્ધ નથી."});
}
function recordBootFailure(error){
  console.error("Phase14H public UI enhancer failed",error);
  if(Array.isArray(window.__zhagmagBootErrors))window.__zhagmagBootErrors.push(error instanceof Error?(error.stack||error.message):String(error));
}
new MutationObserver(cleanPublicCopy).observe(document.documentElement,{childList:true,subtree:true,characterData:true});
Promise.all([import("./phase14c.js"),import("./phase14h.css")]).then(cleanPublicCopy).catch(recordBootFailure);
cleanPublicCopy();
