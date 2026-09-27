import "./phase14ar-i18n-branding.css";
import COPY_A from "./phase14ar-admin-copy-a.js";
import COPY_B from "./phase14ar-admin-copy-b.js";

const LANGUAGE_KEY="zhagmag:ui-language:v1";
const BRANDING_KEY="zhagmag:branding:v1";
const DEFAULT_BRANDING={shopNameGu:"ઝગમગ ડ્રેસીસ",shopNameEn:"Zhagmag Dresses",websiteTitleGu:"ઝગમગ ડ્રેસીસ",websiteTitleEn:"Zhagmag Dresses"};
const EN_GU={...COPY_A,...COPY_B};
const GU_EN=Object.fromEntries(Object.entries(EN_GU).map(([en,gu])=>[gu,en]));
const LEGACY={
"Off કરતાં customer catalog ખાલી/unavailable રહેશે.":{GU:"બંધ હોય ત્યારે ગ્રાહક કેટલોગ ઉપલબ્ધ રહેશે નહીં.",EN:"When off, the customer catalog will be unavailable."},
"Category અને Itemનું individual Public Show/Hide તેમના respective Master screensમાંથી control થાય છે. Pricing public website પર હજુ OFF છે.":{GU:"શ્રેણી અને વસ્તુનું જાહેર બતાવો/છુપાવો તેમના માસ્ટર સ્ક્રીનમાંથી નિયંત્રિત થાય છે. જાહેર વેબસાઇટ પર ભાવ હજુ બંધ છે.",EN:"Category and Item public visibility is controlled from their Master screens. Pricing remains off on the public website."}
};
let branding=loadJson(BRANDING_KEY,DEFAULT_BRANDING);let language=readLanguage();let defaultLanguage="GU";let queued=false;let afterReactQueued=false;

function loadJson(key,fallback){try{return {...fallback,...JSON.parse(localStorage.getItem(key)||"{}")};;}catch{return {...fallback}}}
function readLanguage(){try{const v=localStorage.getItem(LANGUAGE_KEY);return v==="EN"||v==="GU"?v:null}catch{return null}}
function persistLanguage(value){try{localStorage.setItem(LANGUAGE_KEY,value)}catch{}}
function persistBranding(){try{localStorage.setItem(BRANDING_KEY,JSON.stringify(branding))}catch{}}
function currentLanguage(){return language||defaultLanguage||"GU"}
function normalize(value){return String(value||"").replace(/\s+/g," ").trim()}
function directText(node){return normalize([...node.childNodes].filter(n=>n.nodeType===Node.TEXT_NODE).map(n=>n.textContent||"").join(" "))}
function setTextIfChanged(node,value){if(node&&node.textContent!==value){node.textContent=value;return true}return false}
function setAttrIfChanged(node,name,value){if(node&&node.getAttribute(name)!==value){node.setAttribute(name,value);return true}return false}
function setDocumentTitleIfChanged(value){if(document.title!==value)document.title=value}
function isAuthReactOwned(node){return Boolean(node?.closest?.(".auth-page"))}
function trackedSource(node,current){
  const rendered=normalize(node.dataset.phase14arRendered||"");let source=normalize(node.dataset.phase14arSource||"");
  if(!source||(rendered&&normalize(current)!==rendered)){source=normalize(current);node.dataset.phase14arSource=source;}
  return source;
}
function setDirectText(node,value){
  const nodes=[...node.childNodes].filter(n=>n.nodeType===Node.TEXT_NODE);const target=nodes.find(n=>normalize(n.textContent))||nodes[0];if(!target)return false;
  const before=target.textContent||"";const lead=(before.match(/^\s*/)||[""])[0],trail=(before.match(/\s*$/)||[""])[0];const next=`${lead}${value}${trail}`;
  if(target.textContent===next)return false;target.textContent=next;return true;
}
function dynamicTranslate(source,target){
  let m;
  if(target==="GU"){
    if((m=source.match(/^(\d+) records$/)))return `${m[1]} રેકોર્ડ`;
    if((m=source.match(/^Page (\d+) \/ (\d+)$/)))return `પાનું ${m[1]} / ${m[2]}`;
    if((m=source.match(/^(\d+) users$/)))return `${m[1]} વપરાશકર્તા`;
    if((m=source.match(/^Last login: (.+)$/)))return `છેલ્લું લૉગિન: ${m[1]}`;
    if((m=source.match(/^OVERDUE (\d+)D$/)))return `મોડું ${m[1]} દિવસ`;
    if((m=source.match(/^MISSED PICKUP · (\d+)D$/)))return `ચૂકેલ પિકઅપ · ${m[1]} દિવસ`;
    if((m=source.match(/^Available (\d+) \/ (\d+)$/)))return `ઉપલબ્ધ ${m[1]} / ${m[2]}`;
  }else{
    if((m=source.match(/^(\d+) રેકોર્ડ$/)))return `${m[1]} records`;
    if((m=source.match(/^પાનું (\d+) \/ (\d+)$/)))return `Page ${m[1]} / ${m[2]}`;
    if((m=source.match(/^(\d+) વપરાશકર્તા$/)))return `${m[1]} users`;
    if((m=source.match(/^છેલ્લું લૉગિન: (.+)$/)))return `Last login: ${m[1]}`;
    if((m=source.match(/^મોડું (\d+) દિવસ$/)))return `OVERDUE ${m[1]}D`;
    if((m=source.match(/^ચૂકેલ પિકઅપ · (\d+) દિવસ$/)))return `MISSED PICKUP · ${m[1]}D`;
    if((m=source.match(/^ઉપલબ્ધ (\d+) \/ (\d+)$/)))return `Available ${m[1]} / ${m[2]}`;
  }
  return source;
}
function translate(source,target=currentLanguage()){
  const clean=normalize(source);if(!clean)return clean;
  if(LEGACY[clean])return LEGACY[clean][target];
  const exact=target==="GU"?EN_GU[clean]:GU_EN[clean];
  return exact||dynamicTranslate(clean,target);
}
function isBusinessData(node){
  return Boolean(node.matches(".customer-head>div:nth-child(2)>strong,.customer-head>div:nth-child(2)>a,.item-title>div>strong,.item-title>div>span,.category-copy>strong,.field-row>div>strong,.field-row>div>span,.booking-card-head>div>strong,.booking-card-head>div>span,.dashboard-row-head>div>strong,.dashboard-row-head>div>a,.history-title h2,.history-title>div>span,.report-primary>strong,.report-primary>span,.report-primary>p,.user-title>strong,.user-title>span,.template-card .template-head strong,.template-card .template-head span,.template-card>p,.detail-items strong,.detail-items span,.pickup-line:not(.pickup-line-head) strong,.pickup-line:not(.pickup-line-head) small,.return-line:not(.return-line-head) strong,.return-line:not(.return-line-head) small"));
}
function applyCopy(node){
  if(!(node instanceof HTMLElement)||isAuthReactOwned(node)||node.closest(".phase14ar-branding-grid")||node.classList.contains("phase14ar-language-button")||isBusinessData(node))return;
  const current=directText(node);if(!current)return;const source=trackedSource(node,current);const value=translate(source);
  node.classList.remove("phase14ar-i18n-copy","phase14ar-checkbox-copy");node.removeAttribute("data-phase14ar-copy");node.style.removeProperty("--phase14ar-i18n-color");
  setDirectText(node,value);node.dataset.phase14arRendered=value;
}
function applyOptions(){document.querySelectorAll("option").forEach(option=>{if(isAuthReactOwned(option))return;const current=normalize(option.textContent);if(!current)return;const source=trackedSource(option,current);const value=translate(source);if(option.textContent!==value)option.textContent=value;if(option.label!==value)option.label=value;option.dataset.phase14arRendered=value;});}
function applyAttributes(){
  document.querySelectorAll("input[placeholder],textarea[placeholder]").forEach(field=>{if(isAuthReactOwned(field))return;const source=field.dataset.phase14arPlaceholder||field.getAttribute("placeholder")||"";if(!source)return;if(!field.dataset.phase14arPlaceholder)field.dataset.phase14arPlaceholder=source;setAttrIfChanged(field,"placeholder",translate(source));});
  document.querySelectorAll("[aria-label]").forEach(node=>{if(isAuthReactOwned(node)||node.classList.contains("phase14ar-language-button"))return;const source=node.dataset.phase14arAria||node.getAttribute("aria-label")||"";if(!source)return;if(!node.dataset.phase14arAria)node.dataset.phase14arAria=source;setAttrIfChanged(node,"aria-label",translate(source));});
  document.querySelectorAll("[title]").forEach(node=>{if(isAuthReactOwned(node)||node.classList.contains("phase14ar-language-button"))return;const source=node.dataset.phase14arTitle||node.getAttribute("title")||"";if(!source)return;if(!node.dataset.phase14arTitle)node.dataset.phase14arTitle=source;setAttrIfChanged(node,"title",translate(source));});
}
function mergeBranding(data){
  const source=data?.branding||data?.settings||data?.shop||null;if(!source)return;
  branding={...branding,shopNameGu:source.shopNameGu||source.nameGu||branding.shopNameGu,shopNameEn:source.shopNameEn||source.nameEn||branding.shopNameEn,websiteTitleGu:source.websiteTitleGu||branding.websiteTitleGu,websiteTitleEn:source.websiteTitleEn||branding.websiteTitleEn};
  const pref=data?.displayPreferences||source;if(pref?.defaultLanguage==="EN"||pref?.defaultLanguage==="GU")defaultLanguage=pref.defaultLanguage;
  persistBranding();if(!language)language=defaultLanguage;schedule();
}
function selectedShopName(){return currentLanguage()==="EN"?branding.shopNameEn:branding.shopNameGu}
function selectedWebsiteTitle(){return currentLanguage()==="EN"?branding.websiteTitleEn:branding.websiteTitleGu}
function applyBranding(){
  const shop=selectedShopName(),title=selectedWebsiteTitle();
  document.querySelectorAll(".topbar .brand,.splash").forEach(node=>setTextIfChanged(node,shop));
  const settingsTitle=document.querySelector(".settings-section .settings-title h2");if(settingsTitle&&/ઝગમગ ડ્રેસીસ|Zhagmag Dresses/i.test(settingsTitle.textContent||""))setTextIfChanged(settingsTitle,shop);
  setDocumentTitleIfChanged(currentLanguage()==="EN"?`${title||shop} — Admin`:`${title||shop} — સંચાલન`);setAttrIfChanged(document.documentElement,"lang",currentLanguage()==="EN"?"en":"gu");
}
function ensureLanguageButton(){
  if(!document.body)return;
  let button=document.querySelector(".phase14ar-language-button");
  if(!button){button=document.createElement("button");button.type="button";button.className="phase14ar-language-button";button.dataset.phase14arExternal="1";button.addEventListener("click",()=>{language=currentLanguage()==="GU"?"EN":"GU";persistLanguage(language);schedule()});document.body.append(button)}
  else if(button.parentElement!==document.body)document.body.append(button);
  const text=currentLanguage()==="GU"?"🌐 GU":"🌐 EN",title=currentLanguage()==="GU"?"Switch to English":"ગુજરાતીમાં બદલો";setTextIfChanged(button,text);setAttrIfChanged(button,"title",title);setAttrIfChanged(button,"aria-label",title);
}
function editorValue(key){return branding[key]||DEFAULT_BRANDING[key]||""}
function ensureBrandingEditor(){
  const section=document.querySelector(".settings-section");if(!section)return;
  const labels=[...section.querySelectorAll(".settings-form-grid > label")];const shop=labels.find(label=>/^Shop Name$/i.test(label.dataset.phase14arSource||directText(label)));const title=labels.find(label=>/^Website Title$/i.test(label.dataset.phase14arSource||directText(label)));if(!shop||!title)return;
  if(!shop.classList.contains("phase14ar-branding-source-hidden"))shop.classList.add("phase14ar-branding-source-hidden");if(!title.classList.contains("phase14ar-branding-source-hidden"))title.classList.add("phase14ar-branding-source-hidden");
  let grid=section.querySelector(".phase14ar-branding-grid");if(!grid){grid=document.createElement("div");grid.className="phase14ar-branding-grid";shop.parentElement.insertBefore(grid,shop);grid.innerHTML=`<div class="phase14ar-branding-title"><strong></strong><span>GU · EN</span></div><label data-brand-key="shopNameGu"><span></span><input maxlength="160"></label><label data-brand-key="shopNameEn"><span></span><input maxlength="160"></label><label data-brand-key="websiteTitleGu"><span></span><input maxlength="160"></label><label data-brand-key="websiteTitleEn"><span></span><input maxlength="160"></label>`;grid.querySelectorAll("[data-brand-key]").forEach(label=>{const key=label.dataset.brandKey,input=label.querySelector("input");input.value=editorValue(key);input.addEventListener("input",()=>{branding[key]=input.value})})}
  const gu=currentLanguage()==="GU";setTextIfChanged(grid.querySelector(".phase14ar-branding-title strong"),gu?"દ્વિભાષી બ્રાન્ડિંગ":"Bilingual Branding");
  const labelsText={shopNameGu:gu?"દુકાનનું નામ — ગુજરાતી":"Shop Name — Gujarati",shopNameEn:gu?"દુકાનનું નામ — અંગ્રેજી":"Shop Name — English",websiteTitleGu:gu?"વેબસાઇટ શીર્ષક — ગુજરાતી":"Website Title — Gujarati",websiteTitleEn:gu?"વેબસાઇટ શીર્ષક — અંગ્રેજી":"Website Title — English"};
  grid.querySelectorAll("[data-brand-key]").forEach(label=>{setTextIfChanged(label.querySelector("span"),labelsText[label.dataset.brandKey]);const input=label.querySelector("input");if(document.activeElement!==input&&input.value!==editorValue(label.dataset.brandKey))input.value=editorValue(label.dataset.brandKey)});
}
const priorFetch=globalThis.fetch.bind(globalThis);
globalThis.fetch=async(input,init={})=>{
  const url=typeof input==="string"?input:String(input?.url||"");const method=String(init?.method||(typeof input!=="string"?input?.method:"GET")||"GET").toUpperCase();
  let path="";try{path=new URL(url,window.location.origin).pathname}catch{}
  const adminApi=path.startsWith("/api/admin/");
  if(method==="PUT"&&path==="/api/admin/settings"&&typeof init.body==="string"){
    try{const body=JSON.parse(init.body);const grid=document.querySelector(".phase14ar-branding-grid");if(grid){grid.querySelectorAll("[data-brand-key]").forEach(label=>{branding[label.dataset.brandKey]=label.querySelector("input").value.trim()})}Object.assign(body,branding);const useEnglish=body.defaultLanguage==="EN";body.shopName=useEnglish?branding.shopNameEn:branding.shopNameGu;body.websiteTitle=useEnglish?branding.websiteTitleEn:branding.websiteTitleGu;init={...init,body:JSON.stringify(body)}}catch{}
  }
  const response=await priorFetch(input,init);
  try{
    if((response.headers.get("content-type")||"").includes("application/json")){
      response.clone().json().then(data=>{mergeBranding(data);if(adminApi)scheduleAfterReact()}).catch(()=>{if(adminApi)scheduleAfterReact()});
    }else if(adminApi)scheduleAfterReact();
  }catch{if(adminApi)scheduleAfterReact()}
  return response;
};
function scan(){
  queued=false;
  if(!document.querySelector(".shell"))return;
  ensureLanguageButton();ensureBrandingEditor();applyBranding();applyOptions();applyAttributes();document.querySelectorAll("button,a,h1,h2,h3,strong,span,p,small,label,.empty,.message,.report-count,.settings-note,.placeholder-strip,.pill").forEach(applyCopy);
}
function schedule(){if(queued)return;queued=true;queueMicrotask(scan)}
function scheduleAfterReact(){if(afterReactQueued)return;afterReactQueued=true;window.setTimeout(()=>{afterReactQueued=false;schedule()},0)}
document.addEventListener("click",event=>{const target=event.target instanceof Element?event.target.closest("button,a"):null;if(!target||target.classList.contains("phase14ar-language-button")||!target.closest(".shell"))return;scheduleAfterReact()});
document.addEventListener("change",event=>{const target=event.target instanceof Element?event.target:null;if(!target||!target.closest(".shell")||!(target.matches("select")||target.matches('input[type="checkbox"],input[type="radio"]')))return;scheduleAfterReact()});
window.addEventListener("storage",event=>{if(event.key===LANGUAGE_KEY){language=readLanguage();schedule()}else if(event.key===BRANDING_KEY){branding=loadJson(BRANDING_KEY,DEFAULT_BRANDING);schedule()}});
window.addEventListener("phase14ar-branding",event=>{mergeBranding(event.detail);scheduleAfterReact()});
schedule();window.setTimeout(schedule,150);
