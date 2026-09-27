-- Phase 14BG Settings / WhatsApp expansion.
-- Language selection is now global in site_settings; per-template language_mode remains
-- compatibility storage only and is normalized to ALL. Every template stores both languages.

UPDATE whatsapp_templates
SET language_mode='ALL'
WHERE language_mode IS NULL OR language_mode<>'ALL';

INSERT OR IGNORE INTO whatsapp_templates
  (id,template_key,template_name,message_text,is_active,message_gu,message_en,language_mode,linked_action)
VALUES
  (
    'tpl_reservation_confirmation',
    'reservation_confirmation',
    'Reservation Confirmation',
    'નમસ્તે {customer_name}, Booking {booking_no} માટે તમારું Reservation નોંધાઈ ગયું છે. Pickup: {pickup_date}, Return: {return_date}. Items: {item_name}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} માટે તમારું Reservation નોંધાઈ ગયું છે. Pickup: {pickup_date}, Return: {return_date}. Items: {item_name}. — {shop_name}',
    'Hello {customer_name}, your reservation for booking {booking_no} is confirmed. Pickup: {pickup_date}, Return: {return_date}. Items: {item_name}. — {shop_name}',
    'ALL',
    'RESERVATION_CONFIRMATION'
  ),
  (
    'tpl_reservation_cancelled',
    'reservation_cancelled',
    'Reservation Cancelled',
    'નમસ્તે {customer_name}, Booking {booking_no} માટેનું Reservation cancel કરવામાં આવ્યું છે. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} માટેનું Reservation cancel કરવામાં આવ્યું છે. — {shop_name}',
    'Hello {customer_name}, the reservation for booking {booking_no} has been cancelled. — {shop_name}',
    'ALL',
    'RESERVATION_CANCELLED'
  ),
  (
    'tpl_booking_updated',
    'booking_updated',
    'Booking Updated',
    'નમસ્તે {customer_name}, Booking {booking_no} update થયું છે. Pickup: {pickup_date}, Return: {return_date}. Items: {item_name}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} update થયું છે. Pickup: {pickup_date}, Return: {return_date}. Items: {item_name}. — {shop_name}',
    'Hello {customer_name}, booking {booking_no} has been updated. Pickup: {pickup_date}, Return: {return_date}. Items: {item_name}. — {shop_name}',
    'ALL',
    'BOOKING_UPDATED'
  ),
  (
    'tpl_booking_cancelled',
    'booking_cancelled',
    'Booking Cancelled',
    'નમસ્તે {customer_name}, Booking {booking_no} cancel કરવામાં આવ્યું છે. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} cancel કરવામાં આવ્યું છે. — {shop_name}',
    'Hello {customer_name}, booking {booking_no} has been cancelled. — {shop_name}',
    'ALL',
    'BOOKING_CANCELLED'
  ),
  (
    'tpl_pickup_ready',
    'pickup_ready',
    'Pickup Ready',
    'નમસ્તે {customer_name}, Booking {booking_no} ના items pickup માટે તૈયાર છે. Pickup તારીખ: {pickup_date}. Items: {item_name}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} ના items pickup માટે તૈયાર છે. Pickup તારીખ: {pickup_date}. Items: {item_name}. — {shop_name}',
    'Hello {customer_name}, the items for booking {booking_no} are ready for pickup. Pickup date: {pickup_date}. Items: {item_name}. — {shop_name}',
    'ALL',
    'PICKUP_READY'
  ),
  (
    'tpl_pickup_due_today',
    'pickup_due_today',
    'Pickup Due Today',
    'નમસ્તે {customer_name}, Booking {booking_no} નો Pickup આજે {today_date} છે. બાકી Pickup જથ્થો: {pending_qty}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} નો Pickup આજે {today_date} છે. બાકી Pickup જથ્થો: {pending_qty}. — {shop_name}',
    'Hello {customer_name}, pickup for booking {booking_no} is due today ({today_date}). Pending pickup quantity: {pending_qty}. — {shop_name}',
    'ALL',
    'PICKUP_DUE_TODAY'
  ),
  (
    'tpl_return_due_today',
    'return_due_today',
    'Return Due Today',
    'નમસ્તે {customer_name}, Booking {booking_no} નું Return આજે {today_date} છે. બાકી Return જથ્થો: {pending_qty}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} નું Return આજે {today_date} છે. બાકી Return જથ્થો: {pending_qty}. — {shop_name}',
    'Hello {customer_name}, return for booking {booking_no} is due today ({today_date}). Pending return quantity: {pending_qty}. — {shop_name}',
    'ALL',
    'RETURN_DUE_TODAY'
  ),
  (
    'tpl_pending_pickup_reminder',
    'pending_pickup_reminder',
    'Pending Pickup Reminder',
    'નમસ્તે {customer_name}, Booking {booking_no} માં {pending_qty} quantity pickup માટે બાકી છે. Pickup તારીખ: {pickup_date}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} માં {pending_qty} quantity pickup માટે બાકી છે. Pickup તારીખ: {pickup_date}. — {shop_name}',
    'Hello {customer_name}, booking {booking_no} still has {pending_qty} quantity pending for pickup. Pickup date: {pickup_date}. — {shop_name}',
    'ALL',
    'PENDING_PICKUP_REMINDER'
  ),
  (
    'tpl_pending_return_reminder',
    'pending_return_reminder',
    'Pending Return Reminder',
    'નમસ્તે {customer_name}, Booking {booking_no} માં {pending_qty} quantity return માટે બાકી છે. Return તારીખ: {return_date}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} માં {pending_qty} quantity return માટે બાકી છે. Return તારીખ: {return_date}. — {shop_name}',
    'Hello {customer_name}, booking {booking_no} still has {pending_qty} quantity pending for return. Return date: {return_date}. — {shop_name}',
    'ALL',
    'PENDING_RETURN_REMINDER'
  ),
  (
    'tpl_overdue_final_reminder',
    'overdue_final_reminder',
    'Overdue Follow-up / Final Reminder',
    'નમસ્તે {customer_name}, Booking {booking_no} નું Return {overdue_days} દિવસથી overdue છે. બાકી જથ્થો: {pending_qty}. કૃપા કરીને આજે સંપર્ક કરો. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} નું Return {overdue_days} દિવસથી overdue છે. બાકી જથ્થો: {pending_qty}. કૃપા કરીને આજે સંપર્ક કરો. — {shop_name}',
    'Hello {customer_name}, return for booking {booking_no} is overdue by {overdue_days} day(s). Pending quantity: {pending_qty}. Please contact us today. — {shop_name}',
    'ALL',
    'OVERDUE_FINAL_REMINDER'
  ),
  (
    'tpl_item_availability_reply',
    'item_availability_reply',
    'Item Availability Reply',
    'નમસ્તે {customer_name}, {item_name} વિશેની માહિતી: Item Code {item_code}, Category {category_name}. સંબંધિત items: {related_items}. વધુ માહિતી માટે {shop_whatsapp} પર સંપર્ક કરો. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, {item_name} વિશેની માહિતી: Item Code {item_code}, Category {category_name}. સંબંધિત items: {related_items}. વધુ માહિતી માટે {shop_whatsapp} પર સંપર્ક કરો. — {shop_name}',
    'Hello {customer_name}, here is the item information for {item_name}. Item code: {item_code}. Category: {category_name}. Related items: {related_items}. Contact us on {shop_whatsapp} for availability. — {shop_name}',
    'ALL',
    'ITEM_AVAILABILITY_REPLY'
  ),
  (
    'tpl_booking_completed',
    'booking_completed',
    'Booking Completed',
    'નમસ્તે {customer_name}, Booking {booking_no} પૂર્ણ થયું છે. Picked: {picked_qty}, Returned: {returned_qty}. આપનો આભાર. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} પૂર્ણ થયું છે. Picked: {picked_qty}, Returned: {returned_qty}. આપનો આભાર. — {shop_name}',
    'Hello {customer_name}, booking {booking_no} is completed. Picked: {picked_qty}, Returned: {returned_qty}. Thank you. — {shop_name}',
    'ALL',
    'BOOKING_COMPLETED'
  );

CREATE INDEX IF NOT EXISTS ix_whatsapp_templates_action_name
  ON whatsapp_templates(linked_action,template_name);
