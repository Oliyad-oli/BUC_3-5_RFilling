-- Canonical Ethiopian tax-type catalog (filing-service copy).
-- Source of truth until tax-type-engine ships. Same 7 rows, same `code`
-- values, same `ledger_abbr` values across registration-/filing-/payment-service —
-- only the metadata_json payload differs per service.
--
-- Naming convention:
--   `code`         — canonical long-form code stored everywhere (matches the
--                    real ledger engine's accepted taxType list).
--   `ledger_abbr`  — 3-letter abbreviation used only in subledger account names
--                    (SL-{abbr}-{component}-{tin}). The ledger engine derives
--                    it internally; we cache it for cross-reference.
--
-- Coverage:
--   v4.0 ledger engine accepts: VAT, PAYE, WITHHOLDING_TAX, INCOME_TAX_INDIVIDUAL.
--   INCOME_TAX_BUSINESS / TURNOVER_TAX / EXCISE_TAX are filed-only until the
--   ledger team enables them (ledger_enabled = false → outbox skip).

CREATE TABLE IF NOT EXISTS tax_type_catalog (
    code            VARCHAR(64)  PRIMARY KEY,
    ledger_abbr     VARCHAR(8)   NOT NULL,
    name            VARCHAR(128) NOT NULL,
    legal_basis     VARCHAR(128),
    frequency       VARCHAR(16)  NOT NULL,
    due_offset_days INTEGER      NOT NULL,
    rate_kind       VARCHAR(16)  NOT NULL,
    standard_rate   NUMERIC(8,4),
    ledger_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order      INTEGER      NOT NULL,
    metadata_json   JSONB
);

CREATE INDEX IF NOT EXISTS ix_tax_type_catalog_active ON tax_type_catalog (active, sort_order);

INSERT INTO tax_type_catalog (
    code, ledger_abbr, name, legal_basis, frequency, due_offset_days,
    rate_kind, standard_rate, ledger_enabled, active, sort_order, metadata_json
) VALUES
(
    'VAT', 'VAT', 'Value-Added Tax', 'Proc. 285/2002 + 1395/2025',
    'MONTHLY', 30, 'FIXED', 0.15, TRUE, TRUE, 10,
    jsonb_build_object(
        'schedules', jsonb_build_array(
            jsonb_build_object('kind','SALES','label','VAT Sales','supportsCarryForward', FALSE),
            jsonb_build_object('kind','PURCHASES','label','VAT Purchases','supportsCarryForward', FALSE)
        ),
        'ledgerFilingTemplateRef', 'TPL-VAT-RETURN-FILED-001',
        'ledgerPenaltyTemplateRef', 'TPL-VAT-PENALTY-ASSESSED-001'
    )
),
(
    'PAYE', 'PAYE', 'Pay-As-You-Earn (Schedule A — employment)',
    'Proc. 979/2016 + 1395/2025',
    'MONTHLY', 30, 'BRACKETED', NULL, TRUE, TRUE, 20,
    jsonb_build_object(
        'schedules', jsonb_build_array(
            jsonb_build_object('kind','WITHHOLDING','label','Employee Withholding (PAYE)','supportsCarryForward', FALSE)
        ),
        'ledgerFilingTemplateRef', 'TPL-PAYE-RETURN-FILED-001',
        'ledgerPenaltyTemplateRef', NULL
    )
),
(
    'WITHHOLDING_TAX', 'WHT', 'Domestic Withholding Tax (supplier)',
    'Proc. 979/2016 + 1395/2025',
    'MONTHLY', 30, 'FIXED', 0.03, TRUE, TRUE, 30,
    jsonb_build_object(
        'schedules', jsonb_build_array(
            jsonb_build_object('kind','WITHHOLDING','label','Supplier Withholding','supportsCarryForward', FALSE)
        ),
        'ledgerFilingTemplateRef', 'TPL-WHT-RETURN-FILED-001',
        'ledgerPenaltyTemplateRef', NULL
    )
),
(
    'INCOME_TAX_INDIVIDUAL', 'IIT',
    'Personal Income Tax — Schedules B + D (rental + other)',
    'Proc. 979/2016',
    'ANNUAL', 120, 'BRACKETED', NULL, TRUE, TRUE, 40,
    jsonb_build_object(
        'schedules', jsonb_build_array(
            jsonb_build_object('kind','OTHER','label','Rental Income (Schedule B)','supportsCarryForward', FALSE),
            jsonb_build_object('kind','OTHER','label','Other Income (Schedule D)','supportsCarryForward', FALSE)
        ),
        'ledgerFilingTemplateRef', 'TPL-INC-RETURN-FILED-001',
        'ledgerPenaltyTemplateRef', NULL
    )
),
(
    'INCOME_TAX_BUSINESS', 'CIT',
    'Corporate Income Tax — Schedule C',
    'Proc. 979/2016 + 1395/2025',
    'ANNUAL', 120, 'FIXED', 0.30, FALSE, TRUE, 50,
    jsonb_build_object(
        'schedules', jsonb_build_array(
            jsonb_build_object('kind','SALES','label','Revenue','supportsCarryForward', FALSE),
            jsonb_build_object('kind','PURCHASES','label','Expenses','supportsCarryForward', FALSE)
        ),
        'ledgerFilingTemplateRef', NULL,
        'ledgerPenaltyTemplateRef', NULL
    )
),
(
    'TURNOVER_TAX', 'TOT', 'Turnover Tax (sub-VAT-threshold)',
    'Proc. 308/2002',
    'QUARTERLY', 30, 'TIERED', NULL, FALSE, TRUE, 60,
    jsonb_build_object(
        'schedules', jsonb_build_array(
            jsonb_build_object('kind','SALES','label','Turnover Receipts','supportsCarryForward', FALSE)
        ),
        'ledgerFilingTemplateRef', NULL,
        'ledgerPenaltyTemplateRef', NULL
    )
),
(
    'EXCISE_TAX', 'EXC', 'Excise Tax',
    'Proc. 1186/2020 + 1395/2025',
    'MONTHLY', 30, 'AD_VALOREM', NULL, FALSE, TRUE, 70,
    jsonb_build_object(
        'schedules', jsonb_build_array(
            jsonb_build_object('kind','SALES','label','Excisable Sales','supportsCarryForward', FALSE)
        ),
        'ledgerFilingTemplateRef', NULL,
        'ledgerPenaltyTemplateRef', NULL
    )
)
ON CONFLICT (code) DO NOTHING;

-- Backfill the legacy filing_template_mapping rows from the catalog so the
-- outbox lookups keep working until that table is retired. Idempotent —
-- skips rows that already exist.
INSERT INTO filing_template_mapping (
    tax_type_code, filing_template_ref, late_filing_penalty_template_ref,
    created_by, created_at, updated_by, updated_at
)
SELECT
    code,
    metadata_json->>'ledgerFilingTemplateRef',
    metadata_json->>'ledgerPenaltyTemplateRef',
    'tax_type_catalog_seed', NOW(),
    'tax_type_catalog_seed', NOW()
FROM tax_type_catalog
WHERE metadata_json->>'ledgerFilingTemplateRef' IS NOT NULL
ON CONFLICT (tax_type_code) DO NOTHING;
