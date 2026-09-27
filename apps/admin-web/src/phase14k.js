import "./phase14k.css";
import "./phase14l.css";
import "./phase14m.css";
import "./phase14n.css";
import "./phase14p.js";
import "./phase14q.js";
import "./phase14r.js";
import "./phase14s.js";
import "./phase14t.js";
import "./phase14u.js";
import "./phase14w.js";
import "./phase14x.js";
import "./phase14x-category-form.css";
import "./phase14x-category-drawer.js";
import "./phase14x-icon-runtime.js";
import "./phase14x-booking-item-state.js";
import "./phase14x-operational-status.js";
import "./phase14x-missed-pickup.js";
import "./phase14x-missed-items.js";
import "./phase14x-missed-pickup-polish.js";
import "./phase14x-ui3.js";
import "./phase14y-uniform.css";
import "./phase14z-missed-parity.css";
import "./phase14aa-ui-stability.css";
import "./phase14aa-form-action-parity.css";
import "./phase14ab-customer-actions.css";
import "./phase14ac-visual-regression-fix.css";
import "./phase14ad-customer-canonical.css";
import "./phase14af-form-canonical.js";
import "./phase14ag-secondary-form-canonical.js";
import "./phase14ah-operational-settings.css";
import "./phase14ai-detail-single-owner.css";
import "./phase14aj-business-time.js";
import "./phase14ak-partial-return.js";
import "./phase14am-search-flow.css";
import "./phase14an-master-list-parity.css";
import "./phase14ao-erp-neutral.css";
import "./phase14ap-erp-hierarchy.css";

function labelText(input){
  const label=input.closest("label");
  if(!label)return "";
  return [...label.childNodes].filter(node=>node.nodeType===Node.TEXT_NODE).map(node=>node.textContent||"").join(" ").trim().toLowerCase();
}
function nextDateValue(iso){const [year,month,day]=String(iso||"").split("-").map(Number);if(!year||!month||!day)return "";const value=new Date(Date.UTC(year,month-1,day));value.setUTCDate(value.getUTCDate()+1);const y=value.getUTCFullYear(),m=String(value.getUTCMonth()+1).padStart(2,"0"),d=String(value.getUTCDate()).padStart(2,"0");return `${y}-${m}-${d}`;}
function setReactValue(input,value){if(!value||input.value===value)return;const setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value")?.set;setter?.call(input,value);input.dispatchEvent(new Event("input",{bubbles:true}));input.dispatchEvent(new Event("change",{bubbles:true}));}
function findReturnDate(pickup){const form=pickup.closest("form");if(!form)return null;return [...form.querySelectorAll('input[type="date"]')].find(input=>labelText(input).startsWith("return date"))||null;}
function findPickupDate(form){return [...form.querySelectorAll('input[type="date"]')].find(input=>labelText(input).startsWith("pickup date"))||null;}
function enforcePair(pickup){if(!pickup.value)return true;const returnDate=findReturnDate(pickup);if(!returnDate)return true;const minimum=nextDateValue(pickup.value);if(!minimum)return true;returnDate.min=minimum;if(!returnDate.value||returnDate.value<minimum){setReactValue(returnDate,minimum);return false;}return true;}
function scan(){document.querySelectorAll('input[type="date"]').forEach(input=>{if(labelText(input).startsWith("pickup date"))enforcePair(input);});}
document.addEventListener("change",event=>{const input=event.target;if(!(input instanceof HTMLInputElement)||input.type!=="date")return;const label=labelText(input);if(label.startsWith("pickup date")){enforcePair(input);return;}if(label.startsWith("return date")){const form=input.closest("form"),pickup=form?findPickupDate(form):null;if(pickup?.value){const minimum=nextDateValue(pickup.value);input.min=minimum;if(input.value&&input.value<minimum)setReactValue(input,minimum);}}},true);
document.addEventListener("submit",event=>{const form=event.target;if(!(form instanceof HTMLFormElement))return;const pickup=findPickupDate(form);if(!pickup?.value)return;const returnDate=findReturnDate(pickup);if(!returnDate)return;const minimum=nextDateValue(pickup.value);if(!minimum)return;returnDate.min=minimum;if(!returnDate.value||returnDate.value<minimum){event.preventDefault();event.stopImmediatePropagation();setReactValue(returnDate,minimum);returnDate.focus();}},true);
let queued=false;function schedule(){if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;scan();});}new MutationObserver(schedule).observe(document.documentElement,{childList:true,subtree:true});schedule();
