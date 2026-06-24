package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.usecase.officer.OpenFraudCaseUseCase;
import com.itas.taxfiling.domain.event.FraudConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * On officer-confirmed fraud (BUC-FIL-051 outcome=CONFIRM_FRAUD), hand off to
 * case-management (Rule 13 Flow B). filing-service does NOT own the post-
 * confirmation lifecycle — case-management does.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudConfirmedHandler {

    private final OpenFraudCaseUseCase openFraudCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFraudConfirmed(FraudConfirmedEvent event) {
        log.info("Opening case-management case for confirmed fraud, taxReturnId={}",
            event.taxReturnId());
        openFraudCase.execute(event.taxReturnId(), event.officerActorId(), event.narrative());
    }
}
