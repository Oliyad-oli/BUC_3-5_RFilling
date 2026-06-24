package com.itas.taxfiling.application.usecase.obligation;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.FilingPeriodRepositoryPort;
import com.itas.taxfiling.domain.model.FilingPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal — called by the TaxReturnCompletedHandler to flip the linked
 * FilingPeriod to FILED. No-op when the tax return isn't linked to a period
 * (legacy/test data). Idempotent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkFilingPeriodFiledUseCase {

    private final FilingPeriodRepositoryPort periods;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(UUID taxReturnId, Instant filedAt) {
        Optional<FilingPeriod> opt = periods.findByTaxReturnId(taxReturnId);
        if (opt.isEmpty()) {
            log.debug("No filing period linked to tax return {}, skipping FILED flip", taxReturnId);
            return;
        }
        FilingPeriod p = opt.get();
        p.markFiled(taxReturnId, filedAt);
        FilingPeriod saved = periods.save(p);
        saved.pullEvents().forEach(eventPublisher::publish);
    }
}
