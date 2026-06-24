-- Filing service initial schema (refined design).
-- Aggregates: TaxReturn, LineItemEntryType, OfficerReviewItem, FilingCertificate, OutboxEntry.
-- Cache + projection tables: rule_package_cache, taxpayer_projection, subledger_projection.
-- Plus idempotency_keys for the IdempotencyFilter.

-- ============================================================================
-- TaxReturn — schedule-based polymorphic line-item model (Rule 5).
-- Schedules + iterations + amendments are stored as JSONB on this row so the
-- aggregate boundary is preserved end-to-end.
-- ============================================================================
CREATE TABLE tax_returns (
    id                              UUID PRIMARY KEY,
    tin                             VARCHAR(32)   NOT NULL,
    party_id                        VARCHAR(64)   NOT NULL,
    tax_type                        VARCHAR(32)   NOT NULL,
    period_start                    DATE          NOT NULL,
    period_end                      DATE          NOT NULL,
    period_label                    VARCHAR(32)   NOT NULL,
    period_frequency                VARCHAR(16)   NOT NULL,
    filing_method                   VARCHAR(16)   NOT NULL,
    rule_package_version            VARCHAR(32)   NOT NULL,
    status                          VARCHAR(32)   NOT NULL,
    schedules_json                  JSONB         NOT NULL DEFAULT '{}'::jsonb,
    iterations_json                 JSONB         NOT NULL DEFAULT '{}'::jsonb,
    open_amendment_json             JSONB,
    historical_amendments_json      JSONB         NOT NULL DEFAULT '{}'::jsonb,
    principal_ledger_entry_id       UUID,
    principal_ledger_entry_at       TIMESTAMP WITH TIME ZONE,
    last_risk_level                 VARCHAR(16),
    last_risk_score                 NUMERIC(10,4),
    last_risk_indicators_json       JSONB,
    last_risk_justification         VARCHAR(4096),
    last_rule_passed                BOOLEAN,
    last_rule_findings_json         JSONB,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    version                         BIGINT        NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ix_tax_returns_tin_taxtype_period
    ON tax_returns (tin, tax_type, period_label);
CREATE INDEX ix_tax_returns_status ON tax_returns (status);

-- ============================================================================
-- LineItemEntryType — admin-config catalog (Rule 11, BUC-FIL-CONFIG-01).
-- Immutable-rows pattern: every edit creates a new (code, version) row.
-- ============================================================================
CREATE TABLE line_item_entry_types (
    id                       UUID PRIMARY KEY,
    code                     VARCHAR(64)   NOT NULL,
    tax_type                 VARCHAR(32)   NOT NULL,
    schedule_kind            VARCHAR(32)   NOT NULL,
    version                  INTEGER       NOT NULL,
    fields_json              JSONB         NOT NULL,
    status                   VARCHAR(16)   NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_actor_id      VARCHAR(128)  NOT NULL,
    revision                 BIGINT        NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ix_lit_code_version ON line_item_entry_types (code, version);
CREATE INDEX ix_lit_taxtype_kind_status
    ON line_item_entry_types (tax_type, schedule_kind, status);

-- ============================================================================
-- OfficerReviewItem — in-house fraud-flagged review queue (BUC-FIL-050/051).
-- ============================================================================
CREATE TABLE officer_review_items (
    id                          UUID PRIMARY KEY,
    tax_return_id               UUID          NOT NULL,
    kind                        VARCHAR(32)   NOT NULL,
    priority                    VARCHAR(16)   NOT NULL,
    risk_justification          VARCHAR(4096) NOT NULL,
    risk_indicators_json        JSONB,
    assigned_officer_actor_id   VARCHAR(128),
    decision                    VARCHAR(32),
    decision_narrative          VARCHAR(2048),
    decided_at                  TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    version                     BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX ix_ori_status_priority ON officer_review_items (decision, priority);
CREATE INDEX ix_ori_assigned ON officer_review_items (assigned_officer_actor_id);

-- ============================================================================
-- FilingCertificate — dms reference + audit metadata (BUC-FIL-040).
-- ============================================================================
CREATE TABLE filing_certificates (
    id                  UUID PRIMARY KEY,
    tax_return_id       UUID          NOT NULL,
    dms_document_id     UUID          NOT NULL,
    certificate_number  VARCHAR(64)   NOT NULL,
    issued_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ix_fc_tax_return_id    ON filing_certificates (tax_return_id);
CREATE UNIQUE INDEX ix_fc_certificate_number ON filing_certificates (certificate_number);

-- ============================================================================
-- OutboxEntry — at-least-once side-effect dispatcher (ledger posts, fan-out).
-- ============================================================================
CREATE TABLE outbox_entries (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(64)   NOT NULL,
    aggregate_id    UUID          NOT NULL,
    topic           VARCHAR(64)   NOT NULL,
    payload         TEXT          NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    priority        VARCHAR(16)   NOT NULL,
    attempts        INTEGER       NOT NULL DEFAULT 0,
    last_error      VARCHAR(4096),
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at         TIMESTAMP WITH TIME ZONE,
    version         BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX ix_outbox_status_next_attempt ON outbox_entries (status, next_attempt_at);
CREATE INDEX ix_outbox_aggregate           ON outbox_entries (aggregate_type, aggregate_id);

-- ============================================================================
-- Idempotency keys — backs IdempotencyFilter; TTL-driven cleanup.
-- ============================================================================
CREATE TABLE idempotency_keys (
    id              VARCHAR(256) PRIMARY KEY,
    key_value       VARCHAR(128) NOT NULL,
    endpoint        VARCHAR(256) NOT NULL,
    request_hash    VARCHAR(128) NOT NULL,
    status_code     INTEGER      NOT NULL,
    response_body   TEXT         NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX ix_idem_key_endpoint ON idempotency_keys (key_value, endpoint);
CREATE INDEX        ix_idem_expires_at  ON idempotency_keys (expires_at);

-- ============================================================================
-- Rule package cache — local cache of tax-type-engine SDK responses (Rule 9).
-- ============================================================================
CREATE TABLE rule_package_cache (
    id              VARCHAR(128) PRIMARY KEY,
    tax_type        VARCHAR(32)  NOT NULL,
    effective_on    DATE         NOT NULL,
    version         VARCHAR(32)  NOT NULL,
    metadata_json   JSONB        NOT NULL,
    fetched_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX ix_rpc_taxtype_effective ON rule_package_cache (tax_type, effective_on);

-- ============================================================================
-- Taxpayer projection — read-model fed from registration-service events (Rule 10).
-- ============================================================================
CREATE TABLE taxpayer_projection (
    id          UUID PRIMARY KEY,
    tin         VARCHAR(32)  NOT NULL,
    party_id    VARCHAR(64)  NOT NULL,
    legal_name  VARCHAR(256) NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX ix_tp_tin ON taxpayer_projection (tin);

-- ============================================================================
-- Subledger projection — (TIN × tax type) → 4 subledger UUIDs (Rule 10 + Rule 3).
-- Keep PRINCIPAL / PENALTY / INTEREST / REFUND in sync with ledger-engine.
-- Late-filing penalty posts to PENALTY (same subledger as late-payment penalty).
-- ============================================================================
CREATE TABLE subledger_projection (
    id                       UUID PRIMARY KEY,
    tin                      VARCHAR(32) NOT NULL,
    tax_type                 VARCHAR(32) NOT NULL,
    principal_subledger_id   UUID        NOT NULL,
    penalty_subledger_id     UUID        NOT NULL,
    interest_subledger_id    UUID        NOT NULL,
    refund_subledger_id      UUID        NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX ix_sp_tin_taxtype ON subledger_projection (tin, tax_type);
