package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.usecase.taxreturn.CarryForwardLineItemsUseCase;
import com.itas.taxfiling.domain.event.TaxReturnPeriodOpenedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * BUC-FIL-005 — listens to TaxReturnPeriodOpenedEvent and runs carry-forward
 * for the new return. After-commit so the prior-period read sees the committed
 * new return.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarryForwardHandler {

    private final CarryForwardLineItemsUseCase carryForward;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPeriodOpened(TaxReturnPeriodOpenedEvent event) {
        log.debug("Carry-forward triggered for taxReturnId={}", event.taxReturnId());
        carryForward.execute(event.taxReturnId());
    }
}
