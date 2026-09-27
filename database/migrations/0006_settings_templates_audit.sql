PRAGMA foreign_keys = ON;

-- Phase 12 keeps shop/public controls in the existing settings JSON table.
-- No default site_settings row is inserted here: until Owner/Admin saves Settings,
-- Worker environment values remain valid fallbacks for existing deployments.

INSERT OR IGNORE INTO whatsapp_templates (id, template_key, template_name, message_text, is_active)
VALUES
  ('tpl_booking_confirmation','booking_confirmation','Booking Confirmation','નમસ્તે {customer_name}, તમારી Booking {booking_no} નોંધાઈ ગઈ છે. Pickup: {pickup_date}, Return: {return_date}.',1),
  ('tpl_pickup_reminder','pickup_reminder','Pickup Reminder','નમસ્તે {customer_name}, Booking {booking_no} માટે Pickup તારીખ {pickup_date} છે.',1),
  ('tpl_return_reminder','return_reminder','Return Reminder','નમસ્તે {customer_name}, Booking {booking_no} માટે Return તારીખ {return_date} છે.',1),
  ('tpl_overdue_reminder','overdue_reminder','Overdue Reminder','નમસ્તે {customer_name}, Booking {booking_no} ના item(s) ની Return તારીખ {return_date} પસાર થઈ ગઈ છે. કૃપા કરીને સંપર્ક કરો.',1),
  ('tpl_general_inquiry','general_inquiry','General Inquiry','નમસ્તે, ઝગમગ ડ્રેસીસમાં આપનું સ્વાગત છે. કૃપા કરીને જરૂરી item અને તારીખ જણાવો.',1);

CREATE INDEX IF NOT EXISTS ix_audit_logs_created_at
  ON audit_logs(created_at DESC);

CREATE INDEX IF NOT EXISTS ix_audit_logs_action_created
  ON audit_logs(action, created_at DESC);
