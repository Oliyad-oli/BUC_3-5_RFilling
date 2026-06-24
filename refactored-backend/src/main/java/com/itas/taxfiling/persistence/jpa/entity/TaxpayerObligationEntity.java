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
@Table(name = "taxpayer_obligation",
    indexes = {
        @Index(name = "ix_obligation_tin_taxtype", columnList = "tin, tax_type_code", unique = true),
        @Index(name = "ix_obligation_tin", columnList = "tin")
    })
@Getter @Setter @NoArgsConstructor
public class TaxpayerObligationEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 32)
    private String tin;

    @Column(name = "party_id", nullable = false, updatable = false, length = 64)
    private String partyId;

    @Column(name = "tax_type_code", nullable = false, updatable = false, length = 64)
    private String taxTypeCode;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.PeriodFrequency frequency;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
