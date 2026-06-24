package com.itas.taxfiling.application.usecase.obligation;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxpayerObligationRepositoryPort;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.model.TaxpayerObligation;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Ensures a {@link TaxpayerObligation} exists for (TIN × tax type). Lazy
 * filing-period model: this no longer pre-generates {@code FilingPeriod} rows.
 * Periods are derived live from the {@code calendar_period} projection at
 * dashboard read time, and materialized only when the taxpayer files
 * (see {@code StartFilingFromPeriodUseCase}).
 *
 * <p>Triggered by the registration-service webhook on tax-type approval and by
 * the internal seed endpoint.
 *
 * <p>Idempotent: returning the existing obligation is a no-op.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateInitialFilingPeriodsUseCase {

    private final TaxpayerObligationRepositoryPort obligations;
    private final TaxTypeEnginePort taxTypeEngine;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public Result execute(String tin, String partyId, TaxTypeCode taxType, LocalDate effectiveFrom) {
        TaxpayerObligation existing = obligations.findByTinAndTaxType(tin, taxType).orElse(null);
        if (existing != null) {
            log.debug("Obligation already exists tin={} taxType={} obligationId={}",
                tin, taxType, existing.getId());
            return new Result(existing.getId(), false);
        }

        var calendar = taxTypeEngine.getCalendar(taxType, effectiveFrom, 1);
        if (calendar.isEmpty()) {
            throw new DomainException("tax-type-engine returned no calendar for " + taxType.value());
        }
        TaxpayerObligation obligation = TaxpayerObligation.create(
            tin, partyId, taxType, calendar.get(0).frequency(), effectiveFrom);
        TaxpayerObligation saved = obligations.save(obligation);
        saved.pullEvents().forEach(eventPublisher::publish);
        log.info("Obligation created tin={} taxType={} obligationId={}", tin, taxType, saved.getId());
        return new Result(saved.getId(), true);
    }

    public record Result(java.util.UUID obligationId, boolean newlyCreated) {}
}
