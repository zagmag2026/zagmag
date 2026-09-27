import "./phase14as-fix1.css";

const PREFILL_KEY="zhagmag.booking.prefillCustomer";
const MIN_QUERY=2;
const DEBOUNCE_MS=350;
const customerSelectObservers=new WeakMap();
const lineObservers=new WeakMap();
const itemObservers=new WeakMap();
let customerRoot=null,customerRootObserver=null,bookingLines=null,bookingLinesObserver=null,main=null,mainObserver=null,queued=false;

function digits(value){return String(value||"").replace(/\D/g,"")}
function nativeSelect(select,value){const setter=Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype,"value")?.set;setter?.call(select,value);select.dispatchEvent(new Event("change",{bubbles:true}))}
function schedule(){if(queued)return;queued=true;requestAnimationFrame(enhance)}
function bookingTab(){const active=document.querySelector(".tabs button.active");return active?.nextElementSibling instanceof HTMLButtonElement?active.nextElementSibling:null}
function storeCustomer(name,mobile){try{sessionStorage.setItem(PREFILL_KEY,JSON.stringify({name:String(name||"").trim(),mobile:digits(mobile)}))}catch{}}

function ensureCustomerRoot(){
  const root=document.querySelector(".customer-filters")?.closest(".list-card")||null;
  if(root===customerRoot)return;
  customerRootObserver?.disconnect();customerRoot=root;customerRootObserver=null;
  if(root){customerRootObserver=new MutationObserver(schedule);customerRootObserver.observe(root,{childList:true,subtree:true})}
}

function enhanceCustomerActions(){
  ensureCustomerRoot();
  document.querySelectorAll(".customer-grid .customer-card").forEach(card=>{
    const quick=card.querySelector(".quick-actions");if(!quick)return;
    const existing=quick.querySelector(".phase14as-create-booking");
    const eligible=!card.classList.contains("archived")&&Boolean(card.querySelector(".chip.good"));
    if(!eligible){existing?.remove();return}
    if(existing)return;
    const name=String(card.querySelector(".customer-head strong")?.textContent||"").trim();
    const phone=card.querySelector('.customer-head a[href^="tel:"]');
    const mobile=digits(phone?.getAttribute("href")?.replace(/^tel:/i,""));if(!mobile)return;
    const button=document.createElement("button");button.type="button";button.className="ghost phase14as-create-booking";button.textContent="Create Booking";button.setAttribute("aria-label",`Create booking for ${name}`);
    button.addEventListener("click",()=>{storeCustomer(name,mobile);bookingTab()?.click();requestAnimationFrame(schedule)});
    const history=[...quick.querySelectorAll("button")].find(node=>!node.classList.contains("phase14as-create-booking"));
    history?quick.insertBefore(button,history):quick.append(button);
  })
}

function bookingSearchParts(){
  const form=document.querySelector(".booking-form");if(!(form instanceof HTMLFormElement))return null;
  const picker=form.querySelector(".customer-picker");const input=picker?.querySelector("input");const search=picker?.querySelector("button");const select=picker?.nextElementSibling?.querySelector?.("select");
  return picker instanceof HTMLElement&&input instanceof HTMLInputElement&&search instanceof HTMLButtonElement&&select instanceof HTMLSelectElement?{form,picker,input,search,select}:null
}

function customerText(option){const text=String(option.textContent||"").trim();const parts=text.split(" · ").map(v=>v.trim()).filter(Boolean);return{text,name:parts.length>1?parts.slice(0,-1).join(" · "):text,mobile:parts.length>1?parts.at(-1)||"":""}}
function ensureResults(picker){
  let panel=picker.parentElement?.querySelector(":scope > .phase14as-customer-results");
  if(!(panel instanceof HTMLElement)){panel=document.createElement("div");panel.className="phase14as-customer-results";panel.hidden=true;panel.setAttribute("role","listbox");picker.insertAdjacentElement("afterend",panel)}
  let hint=picker.parentElement?.querySelector(":scope > .phase14as-live-hint");
  if(!(hint instanceof HTMLElement)){hint=document.createElement("small");hint.className="phase14as-live-hint";hint.textContent="Type at least 2 characters · results appear automatically";panel.insertAdjacentElement("afterend",hint)}
  return panel
}
function renderResults(input,select,panel){
  const query=input.value.trim();if(query.length<MIN_QUERY){panel.hidden=true;panel.replaceChildren();return}
  const needle=query.toLocaleLowerCase(),number=digits(query);
  const options=[...select.options].filter(o=>o.value).filter(o=>{const t=customerText(o).text;return t.toLocaleLowerCase().includes(needle)||(number&&digits(t).includes(number))}).slice(0,10);
  panel.replaceChildren();
  if(!options.length){const empty=document.createElement("div");empty.className="phase14as-customer-empty";empty.textContent="No matching customer found.";panel.append(empty);panel.hidden=false;return}
  options.forEach(option=>{const info=customerText(option),row=document.createElement("button"),name=document.createElement("span"),mobile=document.createElement("small");row.type="button";row.className="phase14as-customer-result";row.setAttribute("role","option");name.textContent=info.name||"Customer";mobile.textContent=info.mobile;row.append(name,mobile);row.addEventListener("click",()=>{nativeSelect(select,option.value);panel.hidden=true;panel.replaceChildren()});panel.append(row)});
  panel.hidden=false
}
function enhanceAutocomplete(){
  const parts=bookingSearchParts();if(!parts)return;const{picker,input,search,select}=parts,panel=ensureResults(picker);
  picker.classList.add("phase14as-live-customer");
  if(input.dataset.phase14asFix1!=="1"){
    input.dataset.phase14asFix1="1";input.setAttribute("autocomplete","off");
    input.addEventListener("input",()=>{renderResults(input,select,panel);if(input.dataset.phase14asLive==="1")return;window.clearTimeout(Number(input.dataset.phase14asFix1Timer||0));if(input.value.trim().length<MIN_QUERY)return;const timer=window.setTimeout(()=>{if(!search.disabled)search.click()},DEBOUNCE_MS);input.dataset.phase14asFix1Timer=String(timer)});
    input.addEventListener("focus",()=>renderResults(input,select,panel));
  }
  if(!customerSelectObservers.has(select)){const observer=new MutationObserver(()=>requestAnimationFrame(()=>renderResults(input,select,panel)));observer.observe(select,{childList:true});customerSelectObservers.set(select,observer)}
}

function categoryOf(option){if(!(option instanceof HTMLOptionElement)||!option.value)return"";const parts=String(option.textContent||"").split(" · ").map(v=>v.trim()).filter(Boolean);return parts.length>=3?parts.slice(2).join(" · "):""}
function itemSelect(line){for(const label of line.querySelectorAll(":scope > label")){if(label.classList.contains("phase14as-category-field"))continue;const select=label.querySelector("select");if(select instanceof HTMLSelectElement)return{label,select}}return null}
function applyFilter(categorySelect,select){const category=categorySelect.value;select.dataset.phase14asCategory=category;select.disabled=!category;[...select.options].forEach(option=>{if(!option.value){option.hidden=false;option.disabled=false;return}const match=categoryOf(option)===category;option.hidden=!match;option.disabled=!match});if(select.value&&categoryOf(select.selectedOptions[0])!==category)nativeSelect(select,"")}
function hydrateLine(line){
  if(!lineObservers.has(line)){const o=new MutationObserver(()=>requestAnimationFrame(()=>line.isConnected&&hydrateLine(line)));o.observe(line,{childList:true});lineObservers.set(line,o)}
  const found=itemSelect(line);if(!found)return;const{label,select}=found;label.classList.add("phase14as-item-field");
  let categoryLabel=line.querySelector(":scope > .phase14as-category-field"),categorySelect=categoryLabel?.querySelector("select");
  if(!(categoryLabel instanceof HTMLLabelElement)||!(categorySelect instanceof HTMLSelectElement)){categoryLabel=document.createElement("label");categoryLabel.className="phase14as-category-field";categoryLabel.append(document.createTextNode("Category"));categorySelect=document.createElement("select");categorySelect.setAttribute("aria-label","Category");categoryLabel.append(categorySelect);line.insertBefore(categoryLabel,label)}
  if(!itemObservers.has(select)){const o=new MutationObserver(()=>requestAnimationFrame(()=>line.isConnected&&hydrateLine(line)));o.observe(select,{childList:true});itemObservers.set(select,o)}
  const categories=[],seen=new Set();[...select.options].forEach(option=>{const c=categoryOf(option);if(c&&!seen.has(c)){seen.add(c);categories.push(c)}});
  const current=categoryOf(select.selectedOptions[0])||select.dataset.phase14asCategory||categorySelect.value||"",signature=categories.join("\u0001");
  if(categorySelect.dataset.phase14asFix1Options!==signature){categorySelect.replaceChildren();const blank=document.createElement("option");blank.value="";blank.textContent=categories.length?"Select category":"Loading categories…";categorySelect.append(blank);categories.forEach(c=>{const o=document.createElement("option");o.value=c;o.textContent=c;categorySelect.append(o)});categorySelect.dataset.phase14asFix1Options=signature}
  categorySelect.disabled=!categories.length;categorySelect.value=categories.includes(current)?current:"";
  if(categorySelect.dataset.phase14asFix1Bound!=="1"){categorySelect.dataset.phase14asFix1Bound="1";categorySelect.addEventListener("change",()=>applyFilter(categorySelect,select));select.addEventListener("change",()=>{const c=categoryOf(select.selectedOptions[0]);if(c)categorySelect.value=c;applyFilter(categorySelect,select)})}
  applyFilter(categorySelect,select);line.classList.add("phase14as-category-ready")
}
function enhanceCategories(){
  const lines=document.querySelector(".booking-lines");
  if(lines!==bookingLines){bookingLinesObserver?.disconnect();bookingLines=lines;bookingLinesObserver=null;if(lines){bookingLinesObserver=new MutationObserver(schedule);bookingLinesObserver.observe(lines,{childList:true})}}
  lines?.querySelectorAll(".booking-line").forEach(hydrateLine)
}

function ensureMain(){const next=document.querySelector(".shell > main");if(next===main)return;mainObserver?.disconnect();main=next;mainObserver=null;if(main){mainObserver=new MutationObserver(schedule);mainObserver.observe(main,{childList:true})}}
function enhance(){queued=false;ensureMain();enhanceCustomerActions();enhanceAutocomplete();enhanceCategories()}
document.addEventListener("click",event=>{const target=event.target instanceof Element?event.target:null;if(target?.closest(".tabs")){requestAnimationFrame(schedule);return}const panel=document.querySelector(".phase14as-customer-results:not([hidden])");if(panel&&!target?.closest(".phase14as-customer-results,.customer-picker"))panel.hidden=true},false);
window.addEventListener("phase14ar-admin-ready",schedule);
requestAnimationFrame(schedule);
