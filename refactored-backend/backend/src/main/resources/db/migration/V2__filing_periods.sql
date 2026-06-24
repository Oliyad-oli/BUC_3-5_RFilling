-- ============================================================================
-- Layer-2 filing-period model. Per-taxpayer expansion of the global tax
-- calendar published by tax-type-engine. See:
--   /Users/biniam/Downloads/quarterly_filing_period_logic.md
-- ============================================================================

-- One row per (TIN × tax type). Anchored to the registration's effective_from.
CREATE TABLE taxpayer_obligation (
    id                    UUID PRIMARY KEY,
    tin                   VARCHAR(32) NOT NULL,
    party_id              VARCHAR(64) NOT NULL,
    tax_type_code         VARCHAR(64) NOT NULL,
    frequency             VARCHAR(16) NOT NULL,   -- MONTHLY | QUARTERLY | ANNUAL | DAILY
    effective_from        DATE        NOT NULL,
    effective_to          DATE,                   -- NULL while active
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    version               BIGINT      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ix_obligation_tin_taxtype
    ON taxpayer_obligation (tin, tax_type_code);
CREATE INDEX ix_obligation_tin
    ON taxpayer_obligation (tin);

-- One row per period each obligation must file. First period uses
-- registration date as covers_from (is_partial=true).
CREATE TABLE filing_period (
    id                       UUID PRIMARY KEY,
    taxpayer_obligation_id   UUID        NOT NULL,
    tin                      VARCHAR(32) NOT NULL,
    tax_type_code            VARCHAR(64) NOT NULL,
    period_label             VARCHAR(32) NOT NULL,
    covers_from              DATE        NOT NULL,
    covers_to                DATE        NOT NULL,
    due_date                 DATE        NOT NULL,
    is_partial               BOOLEAN     NOT NULL DEFAULT FALSE,
    status                   VARCHAR(16) NOT NULL,   -- FUTURE | OPEN | DUE | OVERDUE | FILED
    tax_return_id            UUID,                   -- set when a TaxReturn is started/filed
    filed_at                 TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    version                  BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT fk_filing_period_obligation
        FOREIGN KEY (taxpayer_obligation_id)
        REFERENCES taxpayer_obligation(id)
);
CREATE UNIQUE INDEX ix_filing_period_oblig_label
    ON filing_period (taxpayer_obligation_id, period_label);
CREATE INDEX ix_filing_period_status
    ON filing_period (status);
CREATE INDEX ix_filing_period_due_date
    ON filing_period (due_date);
CREATE INDEX ix_filing_period_tin_status
    ON filing_period (tin, status);
CREATE INDEX ix_filing_period_tax_return
    ON filing_period (tax_return_id) WHERE tax_return_id IS NOT NULL;
