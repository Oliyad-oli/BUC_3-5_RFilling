-- Per-tax-type ledger template mapping for filing-service.
--
-- Each row says "when filing-service posts to ledger for tax type X,
-- use template Y". The two columns map to the two journals filing-service
-- can post:
--   filing_template_ref  → on accept of a return (BUC-FIL-013, principal post)
--   late_filing_penalty_template_ref → one-off late-filing fee at the same
--                                      moment if the return was filed late
--
-- All rows are admin-managed via the back-office (BUC-FIL-CONFIG-LDG-01).
-- The ledger adapter looks up the template by tax_type_code on every post;
-- if the row is missing, the call fails with a typed engine error instead
-- of guessing a default.

CREATE TABLE filing_template_mapping (
    tax_type_code                       VARCHAR(32)              PRIMARY KEY,
    filing_template_ref                 VARCHAR(64)              NOT NULL,
    late_filing_penalty_template_ref    VARCHAR(64),
    created_by                          VARCHAR(64)              NOT NULL,
    created_at                          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by                          VARCHAR(64)              NOT NULL,
    updated_at                          TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO filing_template_mapping (
    tax_type_code,
    filing_template_ref,
    late_filing_penalty_template_ref,
    created_by, created_at, updated_by, updated_at
) VALUES
    ('VAT',            'TPL-VAT-RETURN-FILED-001',  'TPL-VAT-PENALTY-ASSESSED-001',  'system', now(), 'system', now()),
    ('INCOME_TAX',     'TPL-INC-RETURN-FILED-001',  NULL,                            'system', now(), 'system', now()),
    ('WITHHOLDING_TAX','TPL-WHT-RETURN-FILED-001',  NULL,                            'system', now(), 'system', now()),
    ('PAYE',           'TPL-PAYE-RETURN-FILED-001', NULL,                            'system', now(), 'system', now());
