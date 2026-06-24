package com.itas.taxfiling.persistence.jpa.entity;

import com.itas.taxfiling.persistence.converter.JsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Local cache table for tax-type-engine SDK responses (Rule 9). Refreshed on a
 * schedule + on cache miss; stores rule package version + serialised metadata.
 */
@Entity
@Table(name = "rule_package_cache",
    indexes = {
        @Index(name = "ix_rpc_taxtype_effective",
               columnList = "tax_type, effective_on", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
public class RulePackageCacheEntity {

    @Id
    @Column(length = 128)
    private String id;

    @Column(name = "tax_type", nullable = false, length = 32)
    private String taxType;

    @Column(name = "effective_on", nullable = false)
    private LocalDate effectiveOn;

    @Column(name = "version", nullable = false, length = 32)
    private String version;

    @Column(name = "metadata_json", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> metadata;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
