-- Seed admin-configurable line-item entry types for VAT (BUC-FIL-CONFIG-01).
--
-- Without these rows the wizard's Sales/Purchases steps render an empty
-- skeleton because the controller returns []. The catalog is intentionally
-- admin-editable in production (immutable-row pattern); the seed below is the
-- "ship with sensible defaults" baseline.
--
-- Idempotent — uses ON CONFLICT to skip if a row with the same (code, version)
-- already exists.

-- The JPA adapter wraps the fields list in a {"fields": [...]} object so the
-- entity's Map<String,Object> column round-trips correctly. Match that shape.

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'VAT_SALES_INVOICE',
    'VAT',
    'SALES',
    1,
    '{"fields":[
        {"key":"customer_name",   "label":"Customer name",   "type":"STRING", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"customer_tin",    "label":"Customer TIN",    "type":"STRING", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"invoice_number",  "label":"Invoice #",       "type":"STRING", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"invoice_date",    "label":"Invoice date",    "type":"DATE", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"description",     "label":"Description",     "type":"STRING", "required":false, "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE',
    NOW(),
    'system',
    0
) ON CONFLICT (code, version) DO NOTHING;

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'VAT_PURCHASE_INVOICE',
    'VAT',
    'PURCHASES',
    1,
    '{"fields":[
        {"key":"supplier_name",   "label":"Supplier name",   "type":"STRING", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"supplier_tin",    "label":"Supplier TIN",    "type":"STRING", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"invoice_number",  "label":"Invoice #",       "type":"STRING", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"invoice_date",    "label":"Invoice date",    "type":"DATE", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"description",     "label":"Description",     "type":"STRING", "required":false, "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE',
    NOW(),
    'system',
    0
) ON CONFLICT (code, version) DO NOTHING;
