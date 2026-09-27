/* Phase 14AR auth-safe bilingual layer.
   This module is dynamically loaded only after React has rendered .auth-page.
   It never participates in splash/auth bootstrap and never calls an API. */
const LANGUAGE_KEY="zhagmag:ui-language:v1";
const BRANDING_KEY="zhagmag:branding:v1";
const DEFAULT_BRANDING={shopNameGu:"ઝગમગ ડ્રેસીસ",shopNameEn:"Zhagmag Dresses"};
const AUTH_COPY={
  "Rental Management System":"ડ્રેસ ભાડે વ્યવસ્થાપન સિસ્ટમ",
  "Admin / Staff":"એડમિન / સ્ટાફ",
  "Sign in":"લૉગ ઇન",
  "Secure access for Owner, Admin and Staff.":"માલિક, એડમિન અને સ્ટાફ માટે સુરક્ષિત પ્રવેશ.",
  "Email or Mobile":"ઈમેઇલ અથવા મોબાઇલ",
  "Password":"પાસવર્ડ",
  "Please wait…":"કૃપા કરીને રાહ જુઓ…",
  "Initial Owner Setup":"પ્રારંભિક માલિક સેટઅપ",
  "First-time setup":"પ્રથમ વખત સેટઅપ",
  "Create Owner":"માલિક બનાવો",
  "Works only before the first user exists and requires the Cloudflare setup secret.":"પ્રથમ વપરાશકર્તા બન્યા પહેલાં જ કાર્ય કરે છે અને Cloudflare setup secret જરૂરી છે.",
  "Owner Name":"માલિકનું નામ",
  "Email":"ઈમેઇલ",
  "Mobile":"મોબાઇલ",
  "Setup Token":"સેટઅપ ટોકન",
  "Creating…":"બનાવી રહ્યા છીએ…",
  "Back to Sign in":"લૉગ ઇન પર પાછા જાઓ",
  "Invalid email/mobile or password.":"ઈમેઇલ/મોબાઇલ અથવા પાસવર્ડ ખોટો છે.",
  "Too many sign-in attempts. Try again later.":"ઘણા લૉગ ઇન પ્રયાસ થયા. થોડા સમય પછી ફરી પ્રયત્ન કરો.",
  "Request timed out. Your entered data is still on screen; try again.":"વિનંતિનો સમય પૂરો થયો. દાખલ કરેલી માહિતી સ્ક્રીન પર છે; ફરી પ્રયત્ન કરો.",
  "Login failed.":"લૉગ ઇન નિષ્ફળ થયું.",
  "Owner created. Please sign in.":"માલિક બનાવાયો. હવે લૉગ ઇન કરો.",
  "Setup failed.":"સેટઅપ નિષ્ફળ થયું."
};
const GU_EN=Object.fromEntries(Object.entries(AUTH_COPY).map(([en,gu])=>[gu,en]));
let queued=false;
let observer=null;
const OBSERVER_OPTIONS={childList:true,subtree:true,characterData:true};

function normalize(value){return String(value||"").replace(/\s+/g," ").trim()}
function currentLanguage(){
  try{const value=localStorage.getItem(LANGUAGE_KEY);if(value==="EN"||value==="GU")return value}catch{}
  return document.documentElement.lang==="en"?"EN":"GU";
}
function persistLanguage(value){try{localStorage.setItem(LANGUAGE_KEY,value)}catch{}}
function branding(){
  try{return {...DEFAULT_BRANDING,...JSON.parse(localStorage.getItem(BRANDING_KEY)||"{}")} }catch{return {...DEFAULT_BRANDING}}
}
function directText(node){return normalize([...node.childNodes].filter(child=>child.nodeType===Node.TEXT_NODE).map(child=>child.textContent||"").join(" "))}
function trackedSource(node,current){
  const rendered=normalize(node.dataset.phase14arAuthRendered||"");
  let source=normalize(node.dataset.phase14arAuthSource||"");
  if(!source||(rendered&&normalize(current)!==rendered)){source=normalize(current);node.dataset.phase14arAuthSource=source}
  return source;
}
function setDirectText(node,value){
  const nodes=[...node.childNodes].filter(child=>child.nodeType===Node.TEXT_NODE);
  const target=nodes.find(child=>normalize(child.textContent))||nodes[0];
  if(!target)return false;
  const before=target.textContent||"";
  const lead=(before.match(/^\s*/)||[""])[0],trail=(before.match(/\s*$/)||[""])[0];
  const next=`${lead}${value}${trail}`;
  if(target.textContent===next)return false;
  target.textContent=next;return true;
}
function translate(source,target){
  const clean=normalize(source);if(!clean)return clean;
  return target==="GU"?(AUTH_COPY[clean]||clean):(GU_EN[clean]||clean);
}
function applyNode(node,target){
  if(!(node instanceof HTMLElement))return;
  const current=directText(node);if(!current)return;
  const source=trackedSource(node,current),value=translate(source,target);
  setDirectText(node,value);node.dataset.phase14arAuthRendered=value;
}
function applyBrand(target){
  const strong=document.querySelector(".auth-brand strong");if(!strong)return;
  const value=target==="EN"?branding().shopNameEn:branding().shopNameGu;
  if(strong.textContent!==value)strong.textContent=value;
}
function ensureLanguageButton(target){
  if(!document.querySelector(".auth-page")||!document.body)return null;
  let button=document.querySelector(".phase14ar-language-button");
  if(!button){
    button=document.createElement("button");
    button.type="button";
    button.className="phase14ar-language-button";
    button.dataset.phase14arExternal="1";
    button.dataset.phase14arAuthOwned="1";
    button.addEventListener("click",()=>{
      const next=currentLanguage()==="GU"?"EN":"GU";
      persistLanguage(next);
      document.documentElement.lang=next==="EN"?"en":"gu";
      schedule();
    });
    document.body.append(button);
  }
  if(button.dataset.phase14arAuthListener!=="1"){
    button.dataset.phase14arAuthListener="1";
    button.addEventListener("click",()=>queueMicrotask(schedule));
  }
  const text=target==="GU"?"🌐 GU":"🌐 EN";
  const title=target==="GU"?"Switch to English":"ગુજરાતીમાં બદલો";
  if(button.textContent!==text)button.textContent=text;
  if(button.getAttribute("title")!==title)button.setAttribute("title",title);
  if(button.getAttribute("aria-label")!==title)button.setAttribute("aria-label",title);
  return button;
}
function scan(){
  queued=false;observer?.disconnect();
  try{
    const page=document.querySelector(".auth-page");if(!page)return;
    const target=currentLanguage();
    ensureLanguageButton(target);
    applyBrand(target);
    page.querySelectorAll(".auth-brand>div>span:last-child,.auth-card .eyebrow,.auth-card h1,.auth-card p.muted,.auth-card label,.auth-card button,.auth-card .message").forEach(node=>applyNode(node,target));
    page.dataset.phase14arAuthReady=target;
  }finally{observer?.observe(document.documentElement,OBSERVER_OPTIONS)}
}
function schedule(){if(queued)return;queued=true;queueMicrotask(scan)}
observer=new MutationObserver(schedule);observer.observe(document.documentElement,OBSERVER_OPTIONS);
window.addEventListener("storage",event=>{if(event.key===LANGUAGE_KEY||event.key===BRANDING_KEY)schedule()});
window.addEventListener("phase14ar-auth-ready",schedule);
schedule();
