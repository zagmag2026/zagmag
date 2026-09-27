-- Screen 3 Customers: bilingual WhatsApp templates used by dynamic customer actions.
ALTER TABLE whatsapp_templates ADD COLUMN message_gu TEXT;
ALTER TABLE whatsapp_templates ADD COLUMN message_en TEXT;

UPDATE whatsapp_templates
SET message_gu = COALESCE(NULLIF(message_gu,''), message_text)
WHERE message_gu IS NULL OR message_gu='';

UPDATE whatsapp_templates SET message_en='Hello {customer_name}, your booking {booking_no} is confirmed. Pickup: {pickup_date}. Return: {return_date}. — {shop_name}' WHERE template_key='booking_confirmation';
UPDATE whatsapp_templates SET message_en='Hello {customer_name}, this is a pickup reminder for booking {booking_no}. Pickup: {pickup_date}. Pending quantity: {pending_qty}. — {shop_name}' WHERE template_key='pickup_reminder';
UPDATE whatsapp_templates SET message_en='Hello {customer_name}, this is a return reminder for booking {booking_no}. Return: {return_date}. Pending quantity: {pending_qty}. — {shop_name}' WHERE template_key='return_reminder';
UPDATE whatsapp_templates SET message_en='Hello {customer_name}, booking {booking_no} is overdue by {overdue_days} day(s). Return date: {return_date}. Pending quantity: {pending_qty}. — {shop_name}' WHERE template_key='overdue_reminder';
UPDATE whatsapp_templates SET message_en='Hello {customer_name}, thank you for contacting {shop_name}.' WHERE template_key='general_inquiry';

INSERT OR IGNORE INTO whatsapp_templates (id,template_key,template_name,message_text,is_active,message_gu,message_en)
VALUES (
  'tpl_missed_pickup_reminder',
  'missed_pickup_reminder',
  'Missed Pickup Reminder',
  'નમસ્તે {customer_name}, Booking {booking_no} માટે Pickup તારીખ {pickup_date} પસાર થઈ ગઈ છે. બાકી જથ્થો: {pending_qty}. — {shop_name}',
  1,
  'નમસ્તે {customer_name}, Booking {booking_no} માટે Pickup તારીખ {pickup_date} પસાર થઈ ગઈ છે. બાકી જથ્થો: {pending_qty}. — {shop_name}',
  'Hello {customer_name}, the pickup date for booking {booking_no} has passed. Pickup date: {pickup_date}. Pending quantity: {pending_qty}. — {shop_name}'
);
