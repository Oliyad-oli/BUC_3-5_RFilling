package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.usecase.officer.CreateOfficerReviewItemUseCase;
import com.itas.taxfiling.domain.event.TaxReturnFraudFlaggedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Routes risk-engine HIGH outcomes (BUC-FIL-022) into the in-house review queue
 * (BUC-FIL-050). Runs after commit so the review item is created in its own
 * transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudFlaggedHandler {

    private final CreateOfficerReviewItemUseCase createReviewItem;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFraudFlagged(TaxReturnFraudFlaggedEvent event) {
        log.info("Routing fraud-flagged taxReturnId={} (risk={}) into officer queue",
            event.taxReturnId(), event.outcome().level());
        createReviewItem.execute(event.taxReturnId(), event.outcome());
    }
}
