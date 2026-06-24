package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Read-only catalog of canonical tax types. Source of truth seeded by Flyway
 * (V6 in filing-service) until the tax-type-engine SDK is wired in — then this
 * becomes a read-through cache of the engine.
 */
public interface TaxTypeCatalogRepositoryPort {

    List<TaxTypeCatalogEntry> findAllActive();

    Optional<TaxTypeCatalogEntry> findByCode(TaxTypeCode code);

    /**
     * Canonical catalog entry. Fields mirror the {@code tax_type_catalog}
     * columns; {@code schedules} + {@code ledgerFilingTemplateRef} +
     * {@code ledgerPenaltyTemplateRef} are extracted from {@code metadata_json}
     * by the adapter.
     */
    record TaxTypeCatalogEntry(
        String code,
        String ledgerAbbr,
        String name,
        String legalBasis,
        PeriodFrequency frequency,
        int dueOffsetDays,
        String rateKind,
        BigDecimal standardRate,
        boolean ledgerEnabled,
        boolean active,
        int sortOrder,
        List<ScheduleSpec> schedules,
        String ledgerFilingTemplateRef,
        String ledgerPenaltyTemplateRef
    ) {
        public record ScheduleSpec(
            ScheduleKind kind,
            String label,
            boolean supportsCarryForward
        ) {}
    }
}
