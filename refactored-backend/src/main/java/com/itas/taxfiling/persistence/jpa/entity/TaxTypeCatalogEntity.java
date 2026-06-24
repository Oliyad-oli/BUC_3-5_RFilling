package com.itas.taxfiling.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Canonical Ethiopian tax-type catalog. Source of truth until tax-type-engine
 * ships. Same {@code code} and {@code ledger_abbr} values across all three
 * services (registration / filing / payment); only {@code metadata_json}
 * differs per service.
 */
@Entity
@Table(name = "tax_type_catalog",
    indexes = {
        @Index(name = "ix_tax_type_catalog_active", columnList = "active, sort_order")
    })
@Getter @Setter @NoArgsConstructor
public class TaxTypeCatalogEntity {

    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String code;

    @Column(name = "ledger_abbr", length = 8, nullable = false)
    private String ledgerAbbr;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(name = "legal_basis", length = 128)
    private String legalBasis;

    @Column(length = 16, nullable = false)
    private String frequency;

    @Column(name = "due_offset_days", nullable = false)
    private Integer dueOffsetDays;

    @Column(name = "rate_kind", length = 16, nullable = false)
    private String rateKind;

    @Column(name = "standard_rate", precision = 8, scale = 4)
    private BigDecimal standardRate;

    @Column(name = "ledger_enabled", nullable = false)
    private boolean ledgerEnabled;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
