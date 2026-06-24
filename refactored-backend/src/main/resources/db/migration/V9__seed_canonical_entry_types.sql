-- Production-ready line-item entry-type seed for every canonical tax type.
--
-- Layout: one entry type per (tax_type × schedule_kind) on the catalog (V6).
-- Field names that the rule engine reads are kept in camelCase
-- (grossSalary, grossPaid, paymentType, hasTin, grossRevenue,
-- deductibleExpenses, advancePaidAtCustoms, whtCreditsReceived,
-- quarterlyInstalments, netAmount, exciseRate) to match
-- RuleEngineMockAdapter.calculate() exactly. Cosmetic fields stay snake_case
-- to match the existing VAT seed.
--
-- Idempotent on (code, version) — re-running the migration is a no-op for
-- already-seeded rows.

-- ─── Retire entry types under codes the catalog no longer recognises ────────

UPDATE line_item_entry_types
   SET status = 'RETIRED'
 WHERE code   = 'WHT_EMPLOYEE_PAYROLL'
   AND status = 'ACTIVE';

UPDATE line_item_entry_types
   SET status = 'RETIRED'
 WHERE code   = 'WITHHOLDING_TAX_EMPLOYEE_PAYROLL'
   AND status = 'ACTIVE';

-- ─── PAYE — Schedule A employer monthly return ──────────────────────────────
-- One row per employee. Fields cover gross + allowances + pension splits so
-- ITAS can remit PAYE to the Ministry of Revenue and the pension contributions
-- to POESSA / PSSSA from the same return.

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'PAYE_EMPLOYEE_PAYROLL',
    'PAYE',
    'WITHHOLDING',
    1,
    '{"fields":[
        {"key":"employee_name",       "label":"Employee name",               "type":"STRING",  "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"employee_tin",        "label":"Employee TIN",                "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"national_id",         "label":"National ID (FAN)",           "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"position",            "label":"Position / job title",        "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"grossSalary",         "label":"Gross monthly salary (ETB)",  "type":"DECIMAL", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"taxable_allowances",  "label":"Taxable allowances (ETB)",    "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"exempt_allowances",   "label":"Exempt allowances (ETB)",     "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"pension_employee",    "label":"Employee pension 7% (ETB)",   "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"pension_employer",    "label":"Employer pension 11% (ETB)",  "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"is_pension_exempt",   "label":"Pension exempt (expat / contractor)?", "type":"BOOLEAN", "required":false, "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE', NOW(), 'system', 0
) ON CONFLICT (code, version) DO NOTHING;

-- ─── WITHHOLDING_TAX — supplier domestic WHT monthly return ─────────────────
-- One row per supplier payment. Branches by paymentType in the rule engine.

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'WHT_SUPPLIER_PAYMENT',
    'WITHHOLDING_TAX',
    'WITHHOLDING',
    1,
    '{"fields":[
        {"key":"supplier_name",  "label":"Supplier name",          "type":"STRING",  "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"supplier_tin",   "label":"Supplier TIN",           "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"payment_date",   "label":"Payment date",           "type":"DATE",    "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"invoice_number", "label":"Supplier invoice #",     "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"paymentType",    "label":"Payment type",           "type":"ENUM",    "required":true,
            "allowedValues":["DOMESTIC_GOODS","DOMESTIC_SERVICE","DIVIDEND","INTEREST","ROYALTY"], "validationRegex":""},
        {"key":"grossPaid",      "label":"Gross paid (ETB)",       "type":"DECIMAL", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"hasTin",         "label":"Supplier has valid TIN?","type":"BOOLEAN", "required":true,  "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE', NOW(), 'system', 0
) ON CONFLICT (code, version) DO NOTHING;

-- ─── INCOME_TAX_INDIVIDUAL — Schedules B + D (rental + other income) ────────
-- Annual return. Single entry-type with an income_source_type discriminator so
-- the wizard's "first entry-type wins" lookup works. Rental-specific fields
-- (property_address etc.) are optional and only relevant when source=RENTAL.

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'IIT_INCOME_LINE',
    'INCOME_TAX_INDIVIDUAL',
    'OTHER',
    1,
    '{"fields":[
        {"key":"income_source_type",  "label":"Income source",                      "type":"ENUM",    "required":true,
            "allowedValues":["RENTAL_RESIDENTIAL","RENTAL_COMMERCIAL","DIVIDEND","INTEREST","ROYALTY","CAPITAL_GAIN","GAMING_WINNINGS","OTHER"], "validationRegex":""},
        {"key":"payer_or_property",   "label":"Payer name / property address",      "type":"STRING",  "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"payer_tin",           "label":"Payer TIN",                          "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"period_or_receipt",   "label":"Receipt date / lease period start",  "type":"DATE",    "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"grossAmount",         "label":"Gross amount (ETB)",                 "type":"DECIMAL", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"allowable_expenses",  "label":"Allowable expenses (rental only) (ETB)", "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"whtAlreadyPaid",      "label":"WHT already withheld (ETB)",         "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE', NOW(), 'system', 0
) ON CONFLICT (code, version) DO NOTHING;

-- ─── INCOME_TAX_BUSINESS — Schedule C revenue side ──────────────────────────
-- Annual CIT return. Multiple revenue lines aggregate into `grossRevenue` for
-- the rule engine. MAT (2.5%) is computed off the same revenue total.

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'CIT_REVENUE_LINE',
    'INCOME_TAX_BUSINESS',
    'SALES',
    1,
    '{"fields":[
        {"key":"revenue_category",   "label":"Revenue category",   "type":"ENUM",    "required":true,
            "allowedValues":["SALES_OF_GOODS","SERVICES","RENT_RECEIVED","INTEREST_INCOME","DIVIDENDS_RECEIVED","CAPITAL_GAINS","FOREX_GAINS","OTHER"], "validationRegex":""},
        {"key":"description",        "label":"Description",        "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"grossRevenue",       "label":"Gross revenue (ETB)","type":"DECIMAL", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"advancePaidAtCustoms","label":"Advance income tax paid at customs (ETB)", "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"whtCreditsReceived", "label":"WHT credits from customers (ETB)", "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"quarterlyInstalments","label":"Quarterly instalments paid (ETB)", "type":"DECIMAL", "required":false, "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE', NOW(), 'system', 0
) ON CONFLICT (code, version) DO NOTHING;

-- ─── INCOME_TAX_BUSINESS — Schedule C expense side ──────────────────────────

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'CIT_EXPENSE_LINE',
    'INCOME_TAX_BUSINESS',
    'PURCHASES',
    1,
    '{"fields":[
        {"key":"expense_category",   "label":"Expense category",   "type":"ENUM",    "required":true,
            "allowedValues":["COST_OF_GOODS_SOLD","SALARIES_AND_BENEFITS","RENT","UTILITIES","INTEREST_EXPENSE","DEPRECIATION","REPAIRS_MAINTENANCE","PROFESSIONAL_FEES","FOREX_LOSSES","BAD_DEBTS","OTHER"], "validationRegex":""},
        {"key":"description",        "label":"Description",        "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"deductibleExpenses", "label":"Deductible expense (ETB)", "type":"DECIMAL", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"supplier_tin",       "label":"Counterparty TIN",   "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"is_supported_by_invoice", "label":"Supported by tax invoice?", "type":"BOOLEAN", "required":true, "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE', NOW(), 'system', 0
) ON CONFLICT (code, version) DO NOTHING;

-- ─── TURNOVER_TAX — quarterly receipts summary (sub-VAT-threshold) ──────────
-- One row per activity bucket — TOT is tiered (2% goods, 10% services).

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'TOT_TURNOVER_LINE',
    'TURNOVER_TAX',
    'SALES',
    1,
    '{"fields":[
        {"key":"activity_type",      "label":"Activity type", "type":"ENUM", "required":true,
            "allowedValues":["GOODS","SERVICES"], "validationRegex":""},
        {"key":"description",        "label":"Description",   "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"period_month",       "label":"Month within quarter (1/2/3)", "type":"INTEGER", "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"grossTurnover",      "label":"Gross turnover (ETB)", "type":"DECIMAL", "required":true,  "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE', NOW(), 'system', 0
) ON CONFLICT (code, version) DO NOTHING;

-- ─── EXCISE_TAX — monthly excisable sales declaration ──────────────────────
-- One row per excisable product/HS-code. Field names match the rule engine.

INSERT INTO line_item_entry_types (
    id, code, tax_type, schedule_kind, version, fields_json, status,
    created_at, created_by_actor_id, revision
) VALUES (
    gen_random_uuid(),
    'EXCISE_PRODUCT_SALE',
    'EXCISE_TAX',
    'SALES',
    1,
    '{"fields":[
        {"key":"product_category",   "label":"Excise category",   "type":"ENUM", "required":true,
            "allowedValues":["TOBACCO","ALCOHOL","SUGAR_BEVERAGES","FUEL","VEHICLES","TELECOM_SERVICES","LUXURY_GOODS"], "validationRegex":""},
        {"key":"product_description","label":"Product description","type":"STRING",  "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"hs_code",            "label":"HS code",            "type":"STRING",  "required":false, "allowedValues":[], "validationRegex":""},
        {"key":"quantity",           "label":"Quantity",           "type":"DECIMAL", "required":true,  "allowedValues":[], "validationRegex":""},
        {"key":"unit_of_measure",    "label":"Unit",               "type":"ENUM", "required":true,
            "allowedValues":["PIECE","KG","LITRE","PACK","CARTON"], "validationRegex":""},
        {"key":"netAmount",          "label":"Net ex-factory amount (ETB)", "type":"DECIMAL", "required":true, "allowedValues":[], "validationRegex":""},
        {"key":"exciseRate",         "label":"Excise rate (fraction, e.g. 0.60 for 60%)", "type":"DECIMAL", "required":true, "allowedValues":[], "validationRegex":""}
    ]}'::jsonb,
    'ACTIVE', NOW(), 'system', 0
) ON CONFLICT (code, version) DO NOTHING;
