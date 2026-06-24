package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.usecase.obligation.MarkFilingPeriodFiledUseCase;
import com.itas.taxfiling.domain.event.TaxReturnCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges TaxReturn → FilingPeriod. When a return reaches COMPLETED, flip the
 * linked FilingPeriod to FILED so the dashboard stops showing it as
 * outstanding. Synchronous so the period flip joins the publisher's transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaxReturnCompletedToFilingPeriodHandler {

    private final MarkFilingPeriodFiledUseCase markFiled;

    @EventListener
    public void onCompleted(TaxReturnCompletedEvent event) {
        log.debug("Marking filing period FILED for taxReturnId={}", event.taxReturnId());
        markFiled.execute(event.taxReturnId(), event.occurredAt());
    }
}
