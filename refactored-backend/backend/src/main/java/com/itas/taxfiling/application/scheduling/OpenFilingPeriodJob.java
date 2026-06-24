package com.itas.taxfiling.application.scheduling;

import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.application.port.RegistrationProjectionPort.TaxpayerSnapshot;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.TaxTypeSummary;
import com.itas.taxfiling.application.usecase.taxreturn.OpenFilingPeriodUseCase;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * BUC-FIL-001 — scheduler that opens filing periods at the start of each
 * monthly cycle. Iterates all active taxpayers × all active tax types and
 * calls OpenFilingPeriodUseCase per tuple.
 *
 * Cron is configurable; default fires at 00:05 on the 1st of every month.
 * The use case itself is idempotent so re-running the job is safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenFilingPeriodJob {

    private final RegistrationProjectionPort registration;
    private final TaxTypeEnginePort taxTypeEngine;
    private final OpenFilingPeriodUseCase openFilingPeriod;

    @Scheduled(cron = "${itas.scheduler.open-filing-period-cron:0 5 0 1 * *}")
    public void run() {
        LocalDate today = LocalDate.now();
        int opened = 0;
        int skipped = 0;
        for (TaxpayerSnapshot taxpayer : registration.listActive()) {
            for (TaxTypeSummary taxType : taxTypeEngine.listAvailableTaxTypes()) {
                if (!taxType.active()) continue;
                try {
                    var nextPeriod = taxTypeEngine.nextPeriod(new TaxTypeCode(taxType.code()), today);
                    Period period = new Period(
                        nextPeriod.periodStart(), nextPeriod.periodEnd(), nextPeriod.frequency());
                    openFilingPeriod.execute(taxpayer.tin(), new TaxTypeCode(taxType.code()), period);
                    opened++;
                } catch (Exception ex) {
                    log.warn("OpenFilingPeriodJob: skipped tin={} taxType={} reason={}",
                        taxpayer.tin(), taxType.code(), ex.getMessage());
                    skipped++;
                }
            }
        }
        log.info("OpenFilingPeriodJob complete: opened={} skipped={}", opened, skipped);
    }
}
