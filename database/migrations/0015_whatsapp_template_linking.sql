-- Screen 10 Settings: central WhatsApp template language + linked-action rules.
ALTER TABLE whatsapp_templates ADD COLUMN language_mode TEXT NOT NULL DEFAULT 'ALL';
ALTER TABLE whatsapp_templates ADD COLUMN linked_action TEXT;

UPDATE whatsapp_templates
SET language_mode='ALL'
WHERE language_mode IS NULL OR language_mode='';

UPDATE whatsapp_templates SET linked_action='BOOKING_CONFIRMATION' WHERE template_key='booking_confirmation';
UPDATE whatsapp_templates SET linked_action='PICKUP_REMINDER' WHERE template_key='pickup_reminder';
UPDATE whatsapp_templates SET linked_action='MISSED_PICKUP_REMINDER' WHERE template_key='missed_pickup_reminder';
UPDATE whatsapp_templates SET linked_action='RETURN_REMINDER' WHERE template_key='return_reminder';
UPDATE whatsapp_templates SET linked_action='OVERDUE_REMINDER' WHERE template_key='overdue_reminder';
UPDATE whatsapp_templates SET linked_action='GENERAL_INQUIRY' WHERE template_key='general_inquiry';

INSERT OR IGNORE INTO whatsapp_templates
  (id,template_key,template_name,message_text,is_active,message_gu,message_en,language_mode,linked_action)
VALUES
  (
    'tpl_pickup_done',
    'pickup_done',
    'Pickup Done',
    'નમસ્તે {customer_name}, Booking {booking_no} નો Pickup પૂર્ણ થયો છે. Pickup તારીખ: {pickup_date}. આપનો આભાર. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} નો Pickup પૂર્ણ થયો છે. Pickup તારીખ: {pickup_date}. આપનો આભાર. — {shop_name}',
    'Hello {customer_name}, pickup for booking {booking_no} is complete. Pickup date: {pickup_date}. Thank you. — {shop_name}',
    'ALL',
    'PICKUP_DONE'
  ),
  (
    'tpl_part_pickup_done',
    'part_pickup_done',
    'Part Pickup Done',
    'નમસ્તે {customer_name}, Booking {booking_no} નો Partial Pickup પૂર્ણ થયો છે. બાકી Pickup જથ્થો: {pending_qty}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} નો Partial Pickup પૂર્ણ થયો છે. બાકી Pickup જથ્થો: {pending_qty}. — {shop_name}',
    'Hello {customer_name}, a partial pickup for booking {booking_no} is complete. Pending pickup quantity: {pending_qty}. — {shop_name}',
    'ALL',
    'PART_PICKUP_DONE'
  ),
  (
    'tpl_return_done',
    'return_done',
    'Return Done',
    'નમસ્તે {customer_name}, Booking {booking_no} નું Return પૂર્ણ થયું છે. Return તારીખ: {return_date}. આપનો આભાર. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} નું Return પૂર્ણ થયું છે. Return તારીખ: {return_date}. આપનો આભાર. — {shop_name}',
    'Hello {customer_name}, return for booking {booking_no} is complete. Return date: {return_date}. Thank you. — {shop_name}',
    'ALL',
    'RETURN_DONE'
  ),
  (
    'tpl_part_return_done',
    'part_return_done',
    'Part Return Done',
    'નમસ્તે {customer_name}, Booking {booking_no} નું Partial Return પૂર્ણ થયું છે. બાકી Return જથ્થો: {pending_qty}. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, Booking {booking_no} નું Partial Return પૂર્ણ થયું છે. બાકી Return જથ્થો: {pending_qty}. — {shop_name}',
    'Hello {customer_name}, a partial return for booking {booking_no} is complete. Pending return quantity: {pending_qty}. — {shop_name}',
    'ALL',
    'PART_RETURN_DONE'
  ),
  (
    'tpl_thank_you',
    'thank_you',
    'Thank You',
    'નમસ્તે {customer_name}, ઝગમગ ડ્રેસીસ પસંદ કરવા બદલ આપનો ખૂબ આભાર. ફરી સેવા આપવાની તક મળશે તેવી આશા. — {shop_name}',
    1,
    'નમસ્તે {customer_name}, ઝગમગ ડ્રેસીસ પસંદ કરવા બદલ આપનો ખૂબ આભાર. ફરી સેવા આપવાની તક મળશે તેવી આશા. — {shop_name}',
    'Hello {customer_name}, thank you for choosing {shop_name}. We look forward to serving you again.',
    'ALL',
    'THANK_YOU'
  );

UPDATE whatsapp_templates SET linked_action='PICKUP_DONE', language_mode=COALESCE(NULLIF(language_mode,''),'ALL') WHERE template_key='pickup_done';
UPDATE whatsapp_templates SET linked_action='PART_PICKUP_DONE', language_mode=COALESCE(NULLIF(language_mode,''),'ALL') WHERE template_key='part_pickup_done';
UPDATE whatsapp_templates SET linked_action='RETURN_DONE', language_mode=COALESCE(NULLIF(language_mode,''),'ALL') WHERE template_key='return_done';
UPDATE whatsapp_templates SET linked_action='PART_RETURN_DONE', language_mode=COALESCE(NULLIF(language_mode,''),'ALL') WHERE template_key='part_return_done';
UPDATE whatsapp_templates SET linked_action='THANK_YOU', language_mode=COALESCE(NULLIF(language_mode,''),'ALL') WHERE template_key='thank_you';

-- Allow draft/inactive alternatives but only one active template per linked action.
CREATE UNIQUE INDEX IF NOT EXISTS ux_whatsapp_templates_active_link
  ON whatsapp_templates(linked_action)
  WHERE is_active=1 AND linked_action IS NOT NULL AND linked_action<>'';

CREATE INDEX IF NOT EXISTS ix_whatsapp_templates_linked_action
  ON whatsapp_templates(linked_action,is_active);
