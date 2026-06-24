-- V1__init_schema.sql
-- BUC-003 (Return Filing) & BUC-005 (Return Processing) Schema

-- Tax Returns
CREATE TABLE IF NOT EXISTS tax_returns (
    id              VARCHAR(36)  PRIMARY KEY,
    tin             VARCHAR(20)  NOT NULL,
    tax_type        VARCHAR(30)  NOT NULL,
    period_start_date DATE       NOT NULL,
    period_end_date   DATE       NOT NULL,
    status          VARCHAR(40)  NOT NULL,
    current_iteration_id VARCHAR(36),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    created_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_tax_returns_tin ON tax_returns(tin);
CREATE INDEX IF NOT EXISTS idx_tax_returns_status ON tax_returns(status);

-- Schedules
CREATE TABLE IF NOT EXISTS schedules (
    id              VARCHAR(36)  PRIMARY KEY,
    tax_return_id   VARCHAR(36)  NOT NULL REFERENCES tax_returns(id) ON DELETE CASCADE,
    code            VARCHAR(20)  NOT NULL,
    name            VARCHAR(200)
);
CREATE INDEX IF NOT EXISTS idx_schedules_return ON schedules(tax_return_id);

-- Line Items
CREATE TABLE IF NOT EXISTS line_items (
    id              VARCHAR(36)  PRIMARY KEY,
    schedule_id     VARCHAR(36)  REFERENCES schedules(id) ON DELETE CASCADE,
    iteration_id    VARCHAR(36),
    line_code       VARCHAR(30)  NOT NULL,
    description     VARCHAR(500),
    amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'ETB',
    source          VARCHAR(30)  NOT NULL,
    reference_id    VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_line_items_schedule ON line_items(schedule_id);
CREATE INDEX IF NOT EXISTS idx_line_items_iteration ON line_items(iteration_id);

-- Calculation Iterations
CREATE TABLE IF NOT EXISTS calculation_iterations (
    id                  VARCHAR(36) PRIMARY KEY,
    tax_return_id       VARCHAR(36) NOT NULL REFERENCES tax_returns(id) ON DELETE CASCADE,
    iteration_number    INT         NOT NULL,
    gross_tax_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    gross_tax_currency  VARCHAR(3)  NOT NULL DEFAULT 'ETB',
    input_credit_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    input_credit_currency VARCHAR(3) NOT NULL DEFAULT 'ETB',
    net_tax_amount      NUMERIC(19,4) NOT NULL DEFAULT 0,
    net_tax_currency    VARCHAR(3)  NOT NULL DEFAULT 'ETB',
    calculated_at       TIMESTAMP   NOT NULL,
    accepted            BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_calc_iter_return ON calculation_iterations(tax_return_id);

-- Amendments
CREATE TABLE IF NOT EXISTS amendments (
    id              VARCHAR(36)  PRIMARY KEY,
    tax_return_id   VARCHAR(36)  NOT NULL REFERENCES tax_returns(id) ON DELETE CASCADE,
    reason          VARCHAR(500),
    requested_at    TIMESTAMP    NOT NULL,
    accepted_at     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_amendments_return ON amendments(tax_return_id);

-- Officer Review Items (BUC-005)
CREATE TABLE IF NOT EXISTS officer_review_items (
    id               VARCHAR(36)  PRIMARY KEY,
    return_id        VARCHAR(36)  NOT NULL,
    kind             VARCHAR(40)  NOT NULL,
    priority         VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assigned_officer VARCHAR(100),
    decision         VARCHAR(40),
    decision_notes   TEXT,
    evidence_payload TEXT,
    created_at       TIMESTAMP    NOT NULL,
    assigned_at      TIMESTAMP,
    decided_at       TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_review_items_status ON officer_review_items(status);
CREATE INDEX IF NOT EXISTS idx_review_items_officer ON officer_review_items(assigned_officer);
CREATE INDEX IF NOT EXISTS idx_review_items_return ON officer_review_items(return_id);

-- Filing Periods
CREATE TABLE IF NOT EXISTS filing_periods (
    id                VARCHAR(36) PRIMARY KEY,
    tin               VARCHAR(20) NOT NULL,
    tax_type          VARCHAR(30) NOT NULL,
    period_start_date DATE        NOT NULL,
    period_end_date   DATE        NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'FUTURE',
    due_date          DATE        NOT NULL,
    filed_date        DATE,
    return_id         VARCHAR(36),
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_filing_periods_tin ON filing_periods(tin);
CREATE INDEX IF NOT EXISTS idx_filing_periods_tin_status ON filing_periods(tin, status);

-- Outbox Entries
CREATE TABLE IF NOT EXISTS outbox_entries (
    id              VARCHAR(36)  PRIMARY KEY,
    aggregate_type  VARCHAR(60),
    aggregate_id    VARCHAR(36),
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP    NOT NULL,
    sent_at         TIMESTAMP,
    retry_count     INT          NOT NULL DEFAULT 0,
    error_message   TEXT
);
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_entries(status);

-- Idempotency Store
CREATE TABLE IF NOT EXISTS idempotency_store (
    idempotency_key  VARCHAR(255) PRIMARY KEY,
    response_payload TEXT,
    created_at       TIMESTAMP    NOT NULL
);
