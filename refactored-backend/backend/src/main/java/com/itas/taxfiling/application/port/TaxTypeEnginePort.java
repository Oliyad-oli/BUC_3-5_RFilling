package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.LocalDate;
import java.util.List;

/**
 * Tax-type-engine SDK boundary (Rule 9). Local cache tables back this port —
 * the adapter consults the cache first, falls back to remote SDK fetch, and
 * refreshes the cache on a schedule.
 */
public interface TaxTypeEnginePort {

    RulePackageVersion currentRulePackage(TaxTypeCode taxType, LocalDate effectiveOn);

    List<EntryFieldDefinition> defaultFieldsFor(TaxTypeCode taxType, ScheduleKind kind,
                                                RulePackageVersion rulePackage);

    List<TaxTypeSummary> listAvailableTaxTypes();

    UpcomingPeriod nextPeriod(TaxTypeCode taxType, LocalDate after);

    /**
     * Layer-1 global calendar lookup — returns every period for {@code taxType}
     * starting at or after {@code from}, up to roughly {@code horizonMonths}
     * months ahead. The Ministry of Revenue publishes this; we project + cache.
     * The mock returns a deterministic generated list aligned to the tax type's
     * frequency.
     */
    List<CalendarPeriod> getCalendar(TaxTypeCode taxType, LocalDate from, int horizonMonths);

    /** Schedules to pre-populate when opening a new TaxReturn (BUC-FIL-001). */
    List<ScheduleSpec> schedulesFor(TaxTypeCode taxType, RulePackageVersion rulePackage);

    record TaxTypeSummary(
        String code,
        String label,
        PeriodFrequency frequency,
        boolean active
    ) {}

    record UpcomingPeriod(
        TaxTypeCode taxType,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate dueDate,
        PeriodFrequency frequency
    ) {}

    record ScheduleSpec(
        ScheduleKind kind,
        String label,
        boolean supportsCarryForward
    ) {}

    /** One global-calendar row for a tax type — the Ministry of Revenue's published period. */
    record CalendarPeriod(
        TaxTypeCode taxType,
        PeriodFrequency frequency,
        String periodLabel,        // 'APR-2026', 'Q1-2025/26', 'FY-2025/26'
        LocalDate startsOn,
        LocalDate endsOn,
        LocalDate dueOn
    ) {}
}
