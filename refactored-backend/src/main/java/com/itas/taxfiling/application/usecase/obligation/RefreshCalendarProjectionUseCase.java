package com.itas.taxfiling.application.usecase.obligation;

import com.itas.taxfiling.application.port.CalendarPeriodRepositoryPort;
import com.itas.taxfiling.application.port.CalendarPeriodRepositoryPort.CalendarPeriod;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Refreshes the local {@code calendar_period} projection from the tax-type-engine
 * SDK (Rule 9 cache pattern). Runs weekly + on {@code CalendarPublishedEvent}.
 *
 * <p>Covers a rolling window: 12 months back + 36 months forward, every active
 * tax type. The forward horizon must exceed what the dashboard shows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshCalendarProjectionUseCase {

    private static final int BACK_MONTHS = 12;
    private static final int FORWARD_MONTHS = 36;

    private final TaxTypeEnginePort taxTypeEngine;
    private final CalendarPeriodRepositoryPort calendar;

    @Transactional
    public Result execute() {
        LocalDate from = LocalDate.now().minusMonths(BACK_MONTHS);
        int totalUpserted = 0;
        // listAvailableTaxTypes() now reads from tax_type_catalog (V6 seed),
        // so each code is already the canonical long form (VAT, PAYE,
        // WITHHOLDING_TAX, INCOME_TAX_INDIVIDUAL, INCOME_TAX_BUSINESS, ...).
        // No more short/long alias-soup — the catalog is the single source.
        List<TaxTypeEnginePort.TaxTypeSummary> taxTypes = taxTypeEngine.listAvailableTaxTypes();
        for (var t : taxTypes) {
            if (!t.active()) continue;
            TaxTypeCode code = new TaxTypeCode(t.code());
            var calendarRows = taxTypeEngine.getCalendar(code, from, BACK_MONTHS + FORWARD_MONTHS);
            var domainRows = calendarRows.stream()
                .map(cp -> new CalendarPeriod(
                    cp.taxType().value(), cp.periodLabel(),
                    cp.startsOn(), cp.endsOn(), cp.dueOn(), cp.frequency()))
                .toList();
            calendar.upsertBatch(domainRows);
            totalUpserted += domainRows.size();
        }
        log.info("Calendar projection refreshed: {} rows across {} tax types",
            totalUpserted, taxTypes.size());
        return new Result(taxTypes.size(), totalUpserted);
    }

    public record Result(int taxTypes, int rowsUpserted) {}
}
