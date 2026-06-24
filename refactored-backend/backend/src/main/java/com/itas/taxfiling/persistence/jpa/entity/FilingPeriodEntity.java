package com.itas.taxfiling.persistence.jpa.entity;

import jakarta.persistence.Column;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "filing_period",
    indexes = {
        @Index(name = "ix_filing_period_oblig_label",
               columnList = "taxpayer_obligation_id, period_label", unique = true),
        @Index(name = "ix_filing_period_status",  columnList = "status"),
        @Index(name = "ix_filing_period_due_date", columnList = "due_date"),
        @Index(name = "ix_filing_period_tin_status", columnList = "tin, status")
    })
@Getter @Setter @NoArgsConstructor
public class FilingPeriodEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "taxpayer_obligation_id", nullable = false, updatable = false)
    private UUID taxpayerObligationId;

    @Column(nullable = false, updatable = false, length = 32)
    private String tin;

    @Column(name = "tax_type_code", nullable = false, updatable = false, length = 64)
    private String taxTypeCode;

    @Column(name = "period_label", nullable = false, updatable = false, length = 32)
    private String periodLabel;

    @Column(name = "covers_from", nullable = false, updatable = false)
    private LocalDate coversFrom;

    @Column(name = "covers_to", nullable = false, updatable = false)
    private LocalDate coversTo;

    @Column(name = "due_date", nullable = false, updatable = false)
    private LocalDate dueDate;

    @Column(name = "is_partial", nullable = false, updatable = false)
    private boolean isPartial;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.FilingPeriodStatus status;

    @Column(name = "tax_return_id")
    private UUID taxReturnId;

    @Column(name = "filed_at")
    private Instant filedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When the row was materialized (= persisted for the first time). Lazy
     * model: rows only exist when the period was touched — file-now, filed,
     * or compliance opened a case. For legacy rows this is back-filled to
     * created_at by the V5 migration.
     */
    @Column(name = "materialized_at")
    private Instant materializedAt;

    @Version
    private Long version;
}
