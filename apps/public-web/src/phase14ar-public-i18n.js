import "./phase14ar-public-i18n.css";

const LANGUAGE_KEY="zhagmag:ui-language:v1";
const BRANDING_KEY="zhagmag:branding:v1";
const DEFAULT_BRANDING={shopNameGu:"ઝગમગ ડ્રેસીસ",shopNameEn:"Zhagmag Dresses",websiteTitleGu:"તમારી પસંદગીનો ડ્રેસ સરળતાથી શોધો",websiteTitleEn:"Find the dress you love with ease"};
const EN_GU={
"Traditional Rental Collection":"પરંપરાગત ડ્રેસ ભાડે કલેક્શન","Categories":"શ્રેણીઓ","Items":"વસ્તુઓ","Availability":"ઉપલબ્ધતા","Contact":"સંપર્ક","WhatsApp":"વોટ્સએપ","Call":"કૉલ",
"Zhagmag Collection":"ઝગમગ કલેક્શન","Find the dress you love with ease":"તમારી પસંદગીનો ડ્રેસ સરળતાથી શોધો","View dress photos and details, check date-wise availability, and contact us directly for your selection.":"ડ્રેસના ફોટા અને વિગતો જુઓ, તારીખ પ્રમાણે ઉપલબ્ધતા તપાસો અને પસંદગી માટે સીધો સંપર્ક કરો.","View Collection":"કલેક્શન જુઓ","Check Availability":"ઉપલબ્ધતા તપાસો","Live Catalog":"લાઇવ કેટલોગ","New selections will be added from time to time.":"નવી પસંદગીઓ સમયાંતરે ઉમેરાતી રહેશે.",
"Browse":"બ્રાઉઝ","All":"બધું","Active":"સક્રિય","Date-wise check":"તારીખવાર તપાસ","Check availability for your selected dates.":"પસંદ કરેલી તારીખ માટે ઉપલબ્ધતા જુઓ.","Pickup Date":"પિકઅપ તારીખ","Return Date":"પરત તારીખ","Checking…":"ચકાસી રહ્યું છે…","Item name or code":"વસ્તુનું નામ અથવા કોડ","Search":"શોધો","Clear":"સાફ કરો","Refreshing collection…":"કલેક્શન રિફ્રેશ થઈ રહ્યું છે…","Catalog loading…":"કેટલોગ લોડ થઈ રહ્યો છે…","Public Catalog is currently unavailable.":"જાહેર કેટલોગ હાલમાં બંધ છે.","Please contact us by WhatsApp or Call.":"કૃપા કરીને વોટ્સએપ અથવા કૉલ દ્વારા સંપર્ક કરો.","No items found for this selection.":"આ પસંદગી માટે કોઈ વસ્તુ મળી નથી.","Details":"વિગતો","Item Details":"વસ્તુની વિગતો","No additional details are available.":"વધુ વિગતો ઉપલબ્ધ નથી.","WhatsApp Inquiry":"વોટ્સએપ પૂછપરછ","Contact us for your selection":"તમારી પસંદગી માટે સંપર્ક કરો",
"Not Available":"ઉપલબ્ધ નથી","Few Left":"થોડું બાકી","Limited":"મર્યાદિત","Available":"ઉપલબ્ધ","Previous":"પહેલાનું","Next":"આગળનું","Close":"બંધ કરો","Cancel":"રદ કરો","Retry":"ફરી પ્રયાસ કરો","Loading…":"લોડ થઈ રહ્યું છે…","Message Preview":"મેસેજ પૂર્વદર્શન","Open WhatsApp":"વોટ્સએપ ખોલો","Previous image":"પહેલો ફોટો","Next image":"આગળનો ફોટો"
};
const GU_EN=Object.fromEntries(Object.entries(EN_GU).map(([en,gu])=>[gu,en]));
const LEGACY={
"ઝગમગ કલેક્શન":{GU:"ઝગમગ કલેક્શન",EN:"Zhagmag Collection"},
"તમારી પસંદગીનો ડ્રેસ સરળતાથી શોધો":{GU:"તમારી પસંદગીનો ડ્રેસ સરળતાથી શોધો",EN:"Find the dress you love with ease"},
"ડ્રેસના ફોટા અને વિગતો જુઓ, તારીખ પ્રમાણે ઉપલબ્ધતા તપાસો અને પસંદગી માટે સીધો સંપર્ક કરો.":{GU:"ડ્રેસના ફોટા અને વિગતો જુઓ, તારીખ પ્રમાણે ઉપલબ્ધતા તપાસો અને પસંદગી માટે સીધો સંપર્ક કરો.",EN:"View dress photos and details, check date-wise availability, and contact us directly for your selection."},
"Collection જુઓ":{GU:"કલેક્શન જુઓ",EN:"View Collection"},
"Availability તપાસો":{GU:"ઉપલબ્ધતા તપાસો",EN:"Check Availability"},
"નવી પસંદગીઓ સમયાંતરે ઉમેરાતી રહેશે.":{GU:"નવી પસંદગીઓ સમયાંતરે ઉમેરાતી રહેશે.",EN:"New selections will be added from time to time."},
"પસંદ કરેલી તારીખ માટે ઉપલબ્ધતા જુઓ.":{GU:"પસંદ કરેલી તારીખ માટે ઉપલબ્ધતા જુઓ.",EN:"Check availability for your selected dates."},
"Public Catalog હાલમાં બંધ છે.":{GU:"જાહેર કેટલોગ હાલમાં બંધ છે.",EN:"Public Catalog is currently unavailable."},
"કૃપા કરીને WhatsApp અથવા Call દ્વારા સંપર્ક કરો.":{GU:"કૃપા કરીને વોટ્સએપ અથવા કૉલ દ્વારા સંપર્ક કરો.",EN:"Please contact us by WhatsApp or Call."},
"આ પસંદગી માટે કોઈ item મળ્યું નથી.":{GU:"આ પસંદગી માટે કોઈ વસ્તુ મળી નથી.",EN:"No items found for this selection."},
"વધુ વિગતો ઉપલબ્ધ નથી.":{GU:"વધુ વિગતો ઉપલબ્ધ નથી.",EN:"No additional details are available."},
"તમારી પસંદગી માટે સંપર્ક કરો":{GU:"તમારી પસંદગી માટે સંપર્ક કરો",EN:"Contact us for your selection"}
};
let branding=loadBranding(),language=readLanguage(),defaultLanguage="GU",queued=false;
const OBSERVER_OPTIONS={childList:true,subtree:true,characterData:true,attributes:true,attributeFilter:["class"]};
let observer=null;

function loadBranding(){try{return {...DEFAULT_BRANDING,...JSON.parse(localStorage.getItem(BRANDING_KEY)||"{}")}}catch{return {...DEFAULT_BRANDING}}}
function readLanguage(){try{const v=localStorage.getItem(LANGUAGE_KEY);return v==="GU"||v==="EN"?v:null}catch{return null}}
function saveLanguage(v){try{localStorage.setItem(LANGUAGE_KEY,v)}catch{}}
function saveBranding(){try{localStorage.setItem(BRANDING_KEY,JSON.stringify(branding))}catch{}}
function currentLanguage(){return language||defaultLanguage||"GU"}
function normalize(v){return String(v||"").replace(/\s+/g," ").trim()}
function directText(node){return normalize([...node.childNodes].filter(n=>n.nodeType===Node.TEXT_NODE).map(n=>n.textContent||"").join(" "))}
function setTextIfChanged(node,value){if(node&&node.textContent!==value){node.textContent=value;return true}return false}
function setAttrIfChanged(node,name,value){if(node&&node.getAttribute(name)!==value){node.setAttribute(name,value);return true}return false}
function setDocumentTitleIfChanged(value){if(document.title!==value)document.title=value}
function trackedSource(node,current){const rendered=normalize(node.dataset.phase14arRendered||"");let source=normalize(node.dataset.phase14arSource||"");if(!source||(rendered&&normalize(current)!==rendered)){source=normalize(current);node.dataset.phase14arSource=source;}return source}
function setDirectText(node,value){const nodes=[...node.childNodes].filter(n=>n.nodeType===Node.TEXT_NODE);const target=nodes.find(n=>normalize(n.textContent))||nodes[0];if(!target)return false;const before=target.textContent||"";const lead=(before.match(/^\s*/)||[""])[0],trail=(before.match(/\s*$/)||[""])[0];const next=`${lead}${value}${trail}`;if(target.textContent!==next)target.textContent=next;return true}
function dynamicTranslate(source,target){let m;if(target==="GU"){
  if((m=source.match(/^(\d+) public items?$/)))return `${m[1]} જાહેર વસ્તુ`;
  if((m=source.match(/^(\d+) Active$/)))return `${m[1]} સક્રિય`;
  if((m=source.match(/^Page (\d+) \/ (\d+)$/)))return `પાનું ${m[1]} / ${m[2]}`;
  if((m=source.match(/^Available: (\d+)$/)))return `ઉપલબ્ધ: ${m[1]}`;
  if((m=source.match(/^Show image (\d+)$/)))return `ફોટો ${m[1]} બતાવો`;
}else{
  if((m=source.match(/^(\d+) જાહેર વસ્તુ$/)))return `${m[1]} public item${m[1]==="1"?"":"s"}`;
  if((m=source.match(/^(\d+) સક્રિય$/)))return `${m[1]} Active`;
  if((m=source.match(/^પાનું (\d+) \/ (\d+)$/)))return `Page ${m[1]} / ${m[2]}`;
  if((m=source.match(/^ઉપલબ્ધ: (\d+)$/)))return `Available: ${m[1]}`;
  if((m=source.match(/^ફોટો (\d+) બતાવો$/)))return `Show image ${m[1]}`;
}return source}
function translate(source,target=currentLanguage()){const clean=normalize(source);if(!clean)return clean;if(LEGACY[clean])return LEGACY[clean][target];if(target==="GU")return EN_GU[clean]||dynamicTranslate(clean,target);return GU_EN[clean]||dynamicTranslate(clean,target)}
function mergeBranding(data){
  const source=data?.branding||data?.shop||null;if(!source)return;
  branding={...branding,shopNameGu:source.shopNameGu||source.nameGu||branding.shopNameGu,shopNameEn:source.shopNameEn||source.nameEn||branding.shopNameEn,websiteTitleGu:source.websiteTitleGu||branding.websiteTitleGu,websiteTitleEn:source.websiteTitleEn||branding.websiteTitleEn};
  const pref=data?.displayPreferences||source;if(pref?.defaultLanguage==="GU"||pref?.defaultLanguage==="EN")defaultLanguage=pref.defaultLanguage;
  saveBranding();if(!language)language=defaultLanguage;schedule();
}
function shopName(){return currentLanguage()==="EN"?branding.shopNameEn:branding.shopNameGu}
function websiteTitle(){return currentLanguage()==="EN"?branding.websiteTitleEn:branding.websiteTitleGu}
function applyBranding(){
  const name=shopName();document.querySelectorAll(".brand,footer strong").forEach(node=>{const parent=node.closest("footer");if(node.classList.contains("brand")||parent)setTextIfChanged(node,name)});
  const hero=document.querySelector(".hero h1");if(hero)setTextIfChanged(hero,websiteTitle());
  const brandWrap=document.querySelector(".brand-wrap");if(brandWrap)setAttrIfChanged(brandWrap,"aria-label",name);
  setDocumentTitleIfChanged(name);setAttrIfChanged(document.documentElement,"lang",currentLanguage()==="EN"?"en":"gu");
}
function applyCopy(node){
  const protectedModalHead=node instanceof HTMLElement&&node.closest(".detail-modal .modal-head")&&!node.closest(".whatsapp-preview-modal");
  if(!(node instanceof HTMLElement)||node.classList.contains("phase14ar-public-language")||node.closest(".item-title-row,.field-list,.detail-info dl")||protectedModalHead||node.matches(".item-head .eyebrow"))return;
  const current=directText(node);if(!current)return;
  const existingSource=node.dataset.phase14arSource||current;if(node.classList.contains("category-chip")&&existingSource!=="All"&&existingSource!=="બધું")return;
  const source=trackedSource(node,current),value=translate(source);node.classList.remove("phase14ar-public-copy");node.removeAttribute("data-phase14ar-copy");node.style.removeProperty("--phase14ar-copy-color");setDirectText(node,value);node.dataset.phase14arRendered=value;
}
function applyAttributes(){
  document.querySelectorAll("input[placeholder]").forEach(input=>{const source=input.dataset.phase14arPlaceholder||input.getAttribute("placeholder")||"";if(!source)return;if(!input.dataset.phase14arPlaceholder)input.dataset.phase14arPlaceholder=source;setAttrIfChanged(input,"placeholder",translate(source))});
  document.querySelectorAll("[aria-label]").forEach(node=>{if(node.classList.contains("brand-wrap")||node.classList.contains("phase14ar-public-language"))return;const source=node.dataset.phase14arAria||node.getAttribute("aria-label")||"";if(!source)return;if(!node.dataset.phase14arAria)node.dataset.phase14arAria=source;setAttrIfChanged(node,"aria-label",translate(source))});
}
function ensureLanguageButton(){
  const host=document.querySelector(".quick-contact");if(!host)return;let button=host.querySelector(".phase14ar-public-language");if(!button){button=document.createElement("button");button.type="button";button.className="phase14ar-public-language";button.addEventListener("click",()=>{language=currentLanguage()==="GU"?"EN":"GU";saveLanguage(language);schedule()});host.prepend(button)}
  const text=currentLanguage()==="GU"?"🌐 ગુજરાતી":"🌐 English",title=currentLanguage()==="GU"?"Switch to English":"ગુજરાતીમાં બદલો";setTextIfChanged(button,text);setAttrIfChanged(button,"title",title);
}
const priorFetch=globalThis.fetch.bind(globalThis);globalThis.fetch=async(...args)=>{const response=await priorFetch(...args);try{const url=typeof args[0]==="string"?args[0]:String(args[0]?.url||"");if(/\/api\/public\/(catalog|bootstrap)(?:\?|$)/.test(url)&&(response.headers.get("content-type")||"").includes("application/json")){mergeBranding(await response.clone().json())}}catch{}return response};
function scan(){
  queued=false;
  observer?.disconnect();
  try{
    ensureLanguageButton();applyBranding();applyAttributes();document.querySelectorAll("header a,nav a,button,a.btn,a.mini-btn,h1,h2,h3,.eyebrow,.tagline,.hero p,.hero-card strong,.hero-card span,.hero-card small,.section-head>.pill,.availability-section>div>p,label,.availability-badge,.empty-state,.phase14u-refreshing,.pager span,footer span,.detail-info>p,.detail-info>h3").forEach(applyCopy);
  }finally{
    observer?.observe(document.documentElement,OBSERVER_OPTIONS);
  }
}
function schedule(){if(queued)return;queued=true;queueMicrotask(scan)}
observer=new MutationObserver(schedule);observer.observe(document.documentElement,OBSERVER_OPTIONS);schedule();