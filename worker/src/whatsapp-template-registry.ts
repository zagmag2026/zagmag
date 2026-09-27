export const WHATSAPP_GLOBAL_LANGUAGE_MODES = ["GUJARATI", "ENGLISH", "BOTH"] as const;

export const WHATSAPP_LINKED_ACTIONS = [
  "GENERAL_INQUIRY",
  "BOOKING_CONFIRMATION",
  "RESERVATION_CONFIRMATION",
  "RESERVATION_CANCELLED",
  "BOOKING_UPDATED",
  "BOOKING_CANCELLED",
  "PICKUP_READY",
  "PICKUP_DUE_TODAY",
  "PICKUP_REMINDER",
  "PENDING_PICKUP_REMINDER",
  "MISSED_PICKUP_REMINDER",
  "PICKUP_DONE",
  "PART_PICKUP_DONE",
  "RETURN_DUE_TODAY",
  "RETURN_REMINDER",
  "PENDING_RETURN_REMINDER",
  "OVERDUE_REMINDER",
  "OVERDUE_FINAL_REMINDER",
  "RETURN_DONE",
  "PART_RETURN_DONE",
  "ITEM_AVAILABILITY_REPLY",
  "BOOKING_COMPLETED",
  "THANK_YOU",
  "RESERVATION_REMINDER",
  "RESERVATION_EXPIRY_REMINDER",
  "BOOKING_DETAILS",
  "RETURN_DATE_TIME_UPDATE",
  "RETURN_THANK_YOU",
  "FEEDBACK_REVIEW",
  "OVERDUE_URGENT_REMINDER",
  "OVERDUE_FOLLOW_UP_REMINDER",
  "CANCELLATION_DETAILS",
  "SHOP_ADDRESS",
  "WORKING_HOURS",
  "HOLIDAY_SHOP_CLOSED",
  "CONTACT_US",
  "BILL_DETAILS",
  "PAYMENT_PENDING",
  "FULL_AMOUNT_RECEIVED",
  "BILL_CANCELLED",
  "CUSTOM_GENERAL_MESSAGE"
] as const;

export const WHATSAPP_STATUS_GROUPS = [
  "RESERVED",
  "CONFIRMED_BOOKED",
  "PART_PICKUP",
  "PICKED_UP",
  "PART_RETURN",
  "RETURNED",
  "OVERDUE",
  "CANCELLED",
  "OTHER"
] as const;

export type WhatsAppGlobalLanguageMode = typeof WHATSAPP_GLOBAL_LANGUAGE_MODES[number];
export type WhatsAppLinkedAction = typeof WHATSAPP_LINKED_ACTIONS[number];
export type WhatsAppStatusGroup = typeof WHATSAPP_STATUS_GROUPS[number];

export const WHATSAPP_STATUS_GROUP_LABELS: Record<WhatsAppStatusGroup, string> = {
  RESERVED: "Reserved",
  CONFIRMED_BOOKED: "Confirmed / Booked",
  PART_PICKUP: "Part Pickup",
  PICKED_UP: "Picked Up",
  PART_RETURN: "Part Return",
  RETURNED: "Returned",
  OVERDUE: "Overdue",
  CANCELLED: "Cancelled",
  OTHER: "Other"
};

export const WHATSAPP_TEMPLATES_BY_STATUS_GROUP: Record<
  WhatsAppStatusGroup,
  ReadonlyArray<{ action: WhatsAppLinkedAction; label: string }>
> = {
  RESERVED: [
    { action: "RESERVATION_CONFIRMATION", label: "Reservation Confirmation" },
    { action: "RESERVATION_REMINDER", label: "Reservation Reminder" },
    { action: "RESERVATION_EXPIRY_REMINDER", label: "Reservation Expiry Reminder" }
  ],
  CONFIRMED_BOOKED: [
    { action: "BOOKING_CONFIRMATION", label: "Booking Confirmation" },
    { action: "BOOKING_DETAILS", label: "Booking Details" },
    { action: "PICKUP_REMINDER", label: "Pickup Reminder" },
    { action: "PICKUP_DUE_TODAY", label: "Pickup Today" },
    { action: "PICKUP_READY", label: "Pickup Ready" }
  ],
  PART_PICKUP: [
    { action: "PART_PICKUP_DONE", label: "Partial Pickup Confirmation" },
    { action: "PENDING_PICKUP_REMINDER", label: "Remaining Pickup Reminder" },
    { action: "PICKUP_DUE_TODAY", label: "Remaining Pickup Today" },
    { action: "PICKUP_READY", label: "Remaining Pickup Ready" }
  ],
  PICKED_UP: [
    { action: "PICKUP_DONE", label: "Pickup Confirmation" },
    { action: "RETURN_REMINDER", label: "Return Reminder" },
    { action: "RETURN_DUE_TODAY", label: "Return Today" },
    { action: "RETURN_DATE_TIME_UPDATE", label: "Return Date/Time Update" }
  ],
  PART_RETURN: [
    { action: "PART_RETURN_DONE", label: "Partial Return Confirmation" },
    { action: "RETURN_REMINDER", label: "Remaining Return Reminder" },
    { action: "RETURN_DUE_TODAY", label: "Remaining Return Today" },
    { action: "PENDING_RETURN_REMINDER", label: "Pending Return Reminder" }
  ],
  RETURNED: [
    { action: "RETURN_DONE", label: "Return Confirmation" },
    { action: "RETURN_THANK_YOU", label: "Thank You" },
    { action: "FEEDBACK_REVIEW", label: "Feedback / Review" }
  ],
  OVERDUE: [
    { action: "OVERDUE_REMINDER", label: "Overdue Reminder" },
    { action: "OVERDUE_URGENT_REMINDER", label: "Urgent Reminder" },
    { action: "OVERDUE_FOLLOW_UP_REMINDER", label: "Follow-up Reminder" },
    { action: "OVERDUE_FINAL_REMINDER", label: "Final Reminder" }
  ],
  CANCELLED: [
    { action: "BOOKING_CANCELLED", label: "Cancellation Confirmation" },
    { action: "CANCELLATION_DETAILS", label: "Cancellation Details" }
  ],
  OTHER: [
    { action: "SHOP_ADDRESS", label: "Shop Address" },
    { action: "WORKING_HOURS", label: "Working Hours" },
    { action: "GENERAL_INQUIRY", label: "General Information" },
    { action: "THANK_YOU", label: "Thank You" },
    { action: "HOLIDAY_SHOP_CLOSED", label: "Holiday / Shop Closed" },
    { action: "CONTACT_US", label: "Contact Us" },
    { action: "CUSTOM_GENERAL_MESSAGE", label: "Custom General Message" }
  ]
};

export const WHATSAPP_PLACEHOLDER_SOURCES = {
  shop_name: "Saved Settings · Shop Name",
  business_name: "Saved Settings · Shop Name",
  shop_phone: "Saved Settings · Contact / Call Number",
  shop_whatsapp: "Saved Settings · WhatsApp Number",
  shop_address: "Saved Settings · Address",
  website_title: "Saved Settings · Website Title",
  website_url: "Saved Settings · Website URL",
  customer_name: "Customer · Name",
  customer_mobile: "Customer · Mobile Number",
  customer_address: "Customer · Address",
  booking_no: "Booking · Booking Number",
  booking_id: "Booking · Booking Number",
  booking_status: "Booking · Current lifecycle / confirmation status",
  status: "Booking · Current lifecycle / confirmation status",
  booking_date: "Booking · Booking Date",
  booking_notes: "Booking · Notes",
  booked_by: "Booking · Created By",
  pickup_date: "Booking · Pickup Date",
  return_date: "Booking · Return Date",
  pickup_time: "Latest Pickup event · actual time",
  return_time: "Latest Return event · actual time",
  item_name: "Booking/Item context · readable item summary",
  items: "Booking/Item context · readable item summary",
  item_code: "Booking/Item context · item code list",
  category_name: "Booking/Item context · category list",
  qty: "Booking/Item context · total booked/item quantity",
  availability_status: "Availability · current requested-date status",
  available_qty: "Availability · exact requested-date quantity (Exact Quantity mode only)",
  pending_qty: "Action context · pending pickup or return quantity",
  picked_qty: "Booking · total picked quantity",
  returned_qty: "Booking · total returned quantity",
  overdue_days: "Booking · days after return date",
  related_items: "Related Items · unique readable related-item list",
  today_date: "Business date · Asia/Kolkata",
  staff_name: "Signed-in Admin user · Name",
  bill_no: "Bill · Final Bill Number",
  bill_date: "Bill · Bill Date",
  total_rent: "Bill · Total Rent",
  discount_amount: "Bill · Discount Amount",
  net_amount: "Bill · Net Amount",
  advance_amount: "Bill · Advance Amount",
  balance_amount: "Bill · Balance Amount",
  payment_status: "Bill · Payment Status"
} as const;

export const WHATSAPP_PLACEHOLDERS = Object.keys(WHATSAPP_PLACEHOLDER_SOURCES) as Array<keyof typeof WHATSAPP_PLACEHOLDER_SOURCES>;

const SHOP = ["shop_name", "business_name", "shop_phone", "shop_whatsapp", "shop_address", "website_title", "website_url", "today_date", "staff_name"] as const;
const CUSTOMER = ["customer_name", "customer_mobile", "customer_address"] as const;
const BOOKING = ["booking_no", "booking_id", "booking_status", "status", "booking_date", "booking_notes", "booked_by", "pickup_date", "return_date"] as const;
const ITEMS = ["item_name", "items", "item_code", "category_name", "qty", "related_items"] as const;
const AVAILABILITY = ["availability_status", "available_qty"] as const;
const MOVEMENT = ["pending_qty", "picked_qty", "returned_qty"] as const;
const BILLING = ["bill_no", "bill_date", "total_rent", "discount_amount", "net_amount", "advance_amount", "balance_amount", "payment_status"] as const;

function merge(...groups: ReadonlyArray<readonly string[]>): string[] {
  return [...new Set(groups.flatMap(group => [...group]))];
}

export const WHATSAPP_PLACEHOLDERS_BY_ACTION: Record<WhatsAppLinkedAction, string[]> = {
  GENERAL_INQUIRY: merge(SHOP, CUSTOMER, ITEMS, AVAILABILITY, ["pickup_date", "return_date"]),
  BOOKING_CONFIRMATION: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  RESERVATION_CONFIRMATION: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  RESERVATION_CANCELLED: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  BOOKING_UPDATED: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  BOOKING_CANCELLED: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  PICKUP_READY: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  PICKUP_DUE_TODAY: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  PICKUP_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  PENDING_PICKUP_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  MISSED_PICKUP_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["overdue_days"]),
  PICKUP_DONE: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["pickup_time"]),
  PART_PICKUP_DONE: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["pickup_time"]),
  RETURN_DUE_TODAY: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  RETURN_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  PENDING_RETURN_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  OVERDUE_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["overdue_days"]),
  OVERDUE_FINAL_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["overdue_days"]),
  RETURN_DONE: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["return_time"]),
  PART_RETURN_DONE: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["return_time"]),
  ITEM_AVAILABILITY_REPLY: merge(SHOP, CUSTOMER, ITEMS, AVAILABILITY),
  BOOKING_COMPLETED: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["pickup_time", "return_time"]),
  THANK_YOU: merge(SHOP, CUSTOMER, ["booking_no", "booking_id"]),
  RESERVATION_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  RESERVATION_EXPIRY_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  BOOKING_DETAILS: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  RETURN_DATE_TIME_UPDATE: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT),
  RETURN_THANK_YOU: merge(SHOP, CUSTOMER, BOOKING),
  FEEDBACK_REVIEW: merge(SHOP, CUSTOMER, BOOKING),
  OVERDUE_URGENT_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["overdue_days"]),
  OVERDUE_FOLLOW_UP_REMINDER: merge(SHOP, CUSTOMER, BOOKING, ITEMS, MOVEMENT, ["overdue_days"]),
  CANCELLATION_DETAILS: merge(SHOP, CUSTOMER, BOOKING, ITEMS),
  SHOP_ADDRESS: merge(SHOP, CUSTOMER),
  WORKING_HOURS: merge(SHOP, CUSTOMER),
  HOLIDAY_SHOP_CLOSED: merge(SHOP, CUSTOMER),
  CONTACT_US: merge(SHOP, CUSTOMER),
  BILL_DETAILS: merge(SHOP, CUSTOMER, BOOKING, BILLING),
  PAYMENT_PENDING: merge(SHOP, CUSTOMER, BOOKING, BILLING),
  FULL_AMOUNT_RECEIVED: merge(SHOP, CUSTOMER, BOOKING, BILLING),
  BILL_CANCELLED: merge(SHOP, CUSTOMER, BOOKING, BILLING),
  CUSTOM_GENERAL_MESSAGE: merge(SHOP, CUSTOMER)
};

export function normalizeWhatsAppGlobalLanguage(value: unknown): WhatsAppGlobalLanguageMode {
  const normalized = String(value || "").trim().toUpperCase();
  if (normalized === "ALL") return "BOTH";
  return (WHATSAPP_GLOBAL_LANGUAGE_MODES as readonly string[]).includes(normalized)
    ? normalized as WhatsAppGlobalLanguageMode
    : "BOTH";
}

export function normalizeWhatsAppLinkedAction(value: unknown): WhatsAppLinkedAction | null {
  const normalized = String(value || "").trim().toUpperCase();
  return (WHATSAPP_LINKED_ACTIONS as readonly string[]).includes(normalized)
    ? normalized as WhatsAppLinkedAction
    : null;
}

export function extractTemplatePlaceholders(message: string): string[] {
  const values = new Set<string>();
  for (const match of String(message || "").matchAll(/(?:\{\{([a-z0-9_]+)\}\}|\{([a-z0-9_]+)\})/gi)) {
    values.add(String(match[1] || match[2] || "").toLowerCase());
  }
  return [...values].filter(Boolean);
}

export function validateTemplatePlaceholders(
  linkedAction: WhatsAppLinkedAction,
  messages: string[]
): string | null {
  const allowed = new Set(WHATSAPP_PLACEHOLDERS_BY_ACTION[linkedAction] || []);
  const known = new Set(WHATSAPP_PLACEHOLDERS as string[]);
  for (const placeholder of messages.flatMap(extractTemplatePlaceholders)) {
    if (!known.has(placeholder)) return `Unknown WhatsApp placeholder: {{${placeholder}}}.`;
    if (!allowed.has(placeholder)) {
      return `Placeholder {{${placeholder}}} is not supported for ${linkedAction.replaceAll("_", " ")}.`;
    }
  }
  return null;
}
