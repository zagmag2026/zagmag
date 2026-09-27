-- Billing final UI: persist Draft/Final invoice notes.
-- Production remains untouched until an explicit production deploy is authorized.

ALTER TABLE bills ADD COLUMN notes TEXT;
