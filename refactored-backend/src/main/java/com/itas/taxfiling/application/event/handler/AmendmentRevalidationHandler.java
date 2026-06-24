package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.usecase.taxreturn.StartAmendmentRevalidationUseCase;
import com.itas.taxfiling.domain.event.AmendmentDeltaPostedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * BUC-FIL-033 — after the amendment delta posts to ledger, the TaxReturn
 * re-enters UNDER_VALIDATION and risk + rule run again on the amended return.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmendmentRevalidationHandler {

    private final StartAmendmentRevalidationUseCase startRevalidation;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAmendmentDeltaPosted(AmendmentDeltaPostedEvent event) {
        log.info("Re-validating amendment for taxReturnId={}", event.taxReturnId());
        startRevalidation.execute(event.taxReturnId());
    }
}
