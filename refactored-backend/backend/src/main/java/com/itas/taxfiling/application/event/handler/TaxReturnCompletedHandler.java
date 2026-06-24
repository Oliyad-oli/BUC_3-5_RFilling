package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.usecase.certificate.IssueFilingCertificateUseCase;
import com.itas.taxfiling.domain.event.TaxReturnCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On TaxReturn completion, generate the filing certificate (BUC-FIL-040).
 * Synchronous listener — joins the publisher's transaction so the certificate
 * is committed atomically with the COMPLETED status flip.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaxReturnCompletedHandler {

    private final IssueFilingCertificateUseCase issueCertificate;

    @EventListener
    public void onCompleted(TaxReturnCompletedEvent event) {
        log.info("Issuing filing certificate for taxReturnId={}", event.taxReturnId());
        issueCertificate.execute(event.taxReturnId());
    }
}
