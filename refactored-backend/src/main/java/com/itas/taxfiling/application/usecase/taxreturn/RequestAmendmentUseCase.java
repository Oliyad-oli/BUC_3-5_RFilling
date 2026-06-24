package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AmendmentReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Opens an amendment cycle on a COMPLETED or MANUAL_REVIEW return (BUC-FIL-030).
 * Reason = TAXPAYER_INITIATED for portal flows, OFFICER_REQUESTED when an
 * officer triggers from BUC-FIL-051.
 */
@Service
@RequiredArgsConstructor
public class RequestAmendmentUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public TaxReturn execute(UUID taxReturnId, AmendmentReason reason, String requestedByActorId) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        t.requestAmendment(reason, requestedByActorId);
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
