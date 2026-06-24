package com.itas.taxfiling.persistence.jpa.entity;

import com.itas.taxfiling.persistence.converter.JsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for TaxReturn (refined model). Schedules + iterations + amendments
 * persisted as JSONB columns to match the polymorphic line-item model (Rule 5)
 * — each schedule's line items carry a universal-spine + JSONB entry_specific_data
 * and are stored in the same shape they live in the aggregate.
 *
 * The persistence adapter handles serialisation; this entity is dumb storage.
 */
@Entity
@Table(name = "tax_returns",
    indexes = {
        @Index(name = "ix_tax_returns_tin_taxtype_period",
               columnList = "tin, tax_type, period_label", unique = true),
        @Index(name = "ix_tax_returns_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
public class TaxReturnEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String tin;

    @Column(name = "party_id", nullable = false, updatable = false)
    private String partyId;

    @Column(name = "tax_type", nullable = false, updatable = false, length = 32)
    private String taxType;

    @Column(name = "period_start", nullable = false, updatable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false, updatable = false)
    private LocalDate periodEnd;

    @Column(name = "period_label", nullable = false, updatable = false, length = 32)
    private String periodLabel;

    @Column(name = "period_frequency", nullable = false, updatable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.PeriodFrequency periodFrequency;

    @Column(name = "filing_method", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.FilingMethod filingMethod;

    @Column(name = "rule_package_version", nullable = false, length = 32)
    private String rulePackageVersion;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.TaxReturnStatus status;

    @Column(name = "schedules_json", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> schedules;

    @Column(name = "iterations_json", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> iterations;

    @Column(name = "open_amendment_json", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> openAmendment;

    @Column(name = "historical_amendments_json", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> historicalAmendments;

    @Column(name = "principal_ledger_entry_id")
    private UUID principalLedgerEntryId;

    @Column(name = "principal_ledger_entry_at")
    private Instant principalLedgerEntryAt;

    @Column(name = "last_risk_level", length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.RiskLevel lastRiskLevel;

    @Column(name = "last_risk_score", precision = 10, scale = 4)
    private BigDecimal lastRiskScore;

    @Column(name = "last_risk_indicators_json", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> lastRiskIndicators;

    @Column(name = "last_risk_justification", length = 4096)
    private String lastRiskJustification;

    @Column(name = "last_rule_passed")
    private Boolean lastRulePassed;

    @Column(name = "last_rule_findings_json", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> lastRuleFindings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
