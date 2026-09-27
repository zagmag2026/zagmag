import "./phase14ar-public-i18n.js";
import "./phase14t-public.js";
import "./phase14al-full-weekdays.js";

const BUSINESS_TZ="Asia/Kolkata";
const PUBLIC_BROWSE_KEY="zhagmag:public:browse:v1";
function labelText(input){const label=input.closest("label");if(!label)return "";return [...label.childNodes].filter((n)=>n.nodeType===Node.TEXT_NODE).map((n)=>n.textContent||"").join(" ").trim().toLowerCase();}
function setReactValue(input,value){if(!value||input.value===value)return;const setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value")?.set;setter?.call(input,value);input.dispatchEvent(new Event("input",{bubbles:true}));input.dispatchEvent(new Event("change",{bubbles:true}));}
function businessDate(value=new Date()){try{return new Intl.DateTimeFormat("en-CA",{timeZone:BUSINESS_TZ,year:"numeric",month:"2-digit",day:"2-digit"}).format(value);}catch{return value.toISOString().slice(0,10);}}
function nextDateValue(iso){const [year,month,day]=String(iso||"").split("-").map(Number);if(!year||!month||!day)return "";const value=new Date(Date.UTC(year,month-1,day));value.setUTCDate(value.getUTCDate()+1);const y=value.getUTCFullYear(),m=String(value.getUTCMonth()+1).padStart(2,"0"),d=String(value.getUTCDate()).padStart(2,"0");return `${y}-${m}-${d}`;}
function storedBrowse(){try{const value=JSON.parse(sessionStorage.getItem(PUBLIC_BROWSE_KEY)||"null");return value&&typeof value==="object"?value:null;}catch{return null;}}
function pair(scope){const dates=scope?[...scope.querySelectorAll('input[type="date"]')]:[];return {pickup:dates.find(input=>labelText(input).startsWith("pickup date"))||null,returnDate:dates.find(input=>labelText(input).startsWith("return date"))||null};}
function enforce(scope,withDefaults=false){const {pickup,returnDate}=pair(scope);if(!pickup||!returnDate)return true;if(withDefaults&&scope.dataset.phase14ajBusinessDefaults!=="1"){scope.dataset.phase14ajBusinessDefaults="1";const saved=storedBrowse(),today=businessDate();const savedPickup=typeof saved?.pickupDate==="string"&&saved.pickupDate>=today?saved.pickupDate:"";const targetPickup=savedPickup||today;if(pickup.value!==targetPickup)setReactValue(pickup,targetPickup);const minimum=nextDateValue(targetPickup);const savedReturn=typeof saved?.returnDate==="string"&&saved.returnDate>=minimum?saved.returnDate:"";const targetReturn=savedReturn||minimum;if(returnDate.value!==targetReturn)setReactValue(returnDate,targetReturn);}if(!pickup.value)return true;const minimum=nextDateValue(pickup.value);if(!minimum)return true;returnDate.min=minimum;if(!returnDate.value||returnDate.value<minimum){setReactValue(returnDate,minimum);return false;}return true;}
document.addEventListener("change",event=>{const input=event.target;if(!(input instanceof HTMLInputElement)||input.type!=="date")return;const scope=input.closest(".availability-box");if(!scope)return;const label=labelText(input);if(label.startsWith("pickup date")){enforce(scope,false);return;}if(label.startsWith("return date")){const {pickup}=pair(scope);if(pickup?.value){const minimum=nextDateValue(pickup.value);input.min=minimum;if(input.value&&input.value<minimum)setReactValue(input,minimum);}}},true);
// Final UI-side gate: an invalid same-day/earlier check never reaches React/API.
document.addEventListener("click",event=>{const target=event.target instanceof Element?event.target.closest(".availability-box button"):null;if(!target)return;const scope=target.closest(".availability-box");if(!scope)return;const {pickup,returnDate}=pair(scope);if(!pickup?.value||!returnDate)return;const minimum=nextDateValue(pickup.value);if(!minimum)return;returnDate.min=minimum;if(!returnDate.value||returnDate.value<minimum){event.preventDefault();event.stopImmediatePropagation();setReactValue(returnDate,minimum);returnDate.focus();}},true);
let queued=false;function scheduleDefaults(){if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;document.querySelectorAll(".availability-box").forEach(scope=>enforce(scope,true));});}
new MutationObserver(scheduleDefaults).observe(document.documentElement,{childList:true,subtree:true});
scheduleDefaults();
void import("./main.tsx")
  .then(()=>import("./phase14y-public-uniform.css"))
  .then(()=>import("./phase14aa-public-stability.css"))
  .then(()=>import("./phase14ah-public-contact.js"));
