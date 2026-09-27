import React from "react";

// Phase 14AQ native Dashboard render helpers. The main Dashboard consumes the existing
// bundled /api/admin/dashboard payload; this module never fetches data itself.
export type NativeMissedDashboardEntry = {
  id:string; booking_no:string; pickup_date:string; return_date:string; status:string;
  customer_name:string; customer_mobile:string; items_summary:string;
  booked_qty:number; given_qty:number; remaining_qty:number; missed_days:number;
};

export function missedKpiClass(label:string){return label==="Missed Pickups"?"phase14x-missed-kpi":"";}
export function missedPanelClass(kind:string){return kind==="missed"?"phase14x-missed-panel":"";}
export function missedPanelSubtitle(kind:string,fallback:string){return kind==="missed"?"Past pickup date · quantity still pending":fallback;}
export function missedBadge(kind:string,row:{status:string;missed_days?:number;overdue_days?:number}){
  if(kind==="missed")return `MISSED PICKUP · ${Math.max(1,Number(row.missed_days||0))}D`;
  if(kind==="overdue")return `OVERDUE ${Number(row.overdue_days||0)}D`;
  return String(row.status||"").replaceAll("_"," ");
}

export function NativeMissedActions({row}:{row:NativeMissedDashboardEntry}){
  return <>
    <button type="button" className="ghost phase14x-missed-open" data-missed-action="pickup" data-booking-no={row.booking_no}>Give Now</button>
    {row.status==="BOOKED"&&<button type="button" className="ghost" data-missed-action="reschedule" data-booking-no={row.booking_no}>Reschedule</button>}
    {row.status==="BOOKED"&&<button type="button" className="danger-link" data-missed-action="cancel" data-booking-no={row.booking_no}>Cancel</button>}
  </>;
}
