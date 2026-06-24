package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Layer-1 calendar projection (Rule 9 cache pattern). Backed by the
 * {@code calendar_period} table — refreshed from tax-type-engine on schedule.
 */
public interface CalendarPeriodRepositoryPort {

    Optional<CalendarPeriod> findByTaxTypeAndLabel(TaxTypeCode taxType, String periodLabel);

    List<CalendarPeriod> findByTaxTypeInRange(TaxTypeCode taxType, LocalDate from, LocalDate to);

    /** Idempotent on (tax_type, period_label) — upserts the rows. */
    void upsertBatch(List<CalendarPeriod> periods);

    record CalendarPeriod(
        String taxTypeCode,
        String periodLabel,
        LocalDate startsOn,
        LocalDate endsOn,
        LocalDate dueOn,
        PeriodFrequency frequency
    ) {}
}
