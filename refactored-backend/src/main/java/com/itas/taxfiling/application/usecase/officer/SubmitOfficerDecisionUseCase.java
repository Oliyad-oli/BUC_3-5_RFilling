package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.OfficerClearTaxReturnUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.OfficerConfirmFraudUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RequestAmendmentUseCase;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.domain.valueobject.AmendmentReason;
import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Officer's final decision on a fraud-flagged TaxReturn (BUC-FIL-051):
 *   - CLEAR             → TaxReturn → COMPLETED.
 *   - REQUEST_AMENDMENT → TaxReturn → AMENDMENT_DRAFT (taxpayer must fix).
 *   - CONFIRM_FRAUD     → TaxReturn → FRAUD_CONFIRMED; case-management opens
 *                          a case via the FraudConfirmedHandler (Rule 13 Flow B).
 */
@Service
@RequiredArgsConstructor
public class SubmitOfficerDecisionUseCase {

    private final OfficerReviewItemRepositoryPort items;
    private final OfficerClearTaxReturnUseCase clearTaxReturn;
    private final RequestAmendmentUseCase requestAmendment;
    private final OfficerConfirmFraudUseCase confirmFraud;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public OfficerReviewItem execute(UUID reviewItemId, OfficerReviewDecision decision,
                                     String officerActorId, String narrative) {
        OfficerReviewItem item = items.findById(reviewItemId)
            .orElseThrow(() -> new ResourceNotFoundException("review item not found: " + reviewItemId));
        item.decide(decision, officerActorId, narrative);
        OfficerReviewItem saved = items.save(item);
        saved.pullEvents().forEach(eventPublisher::publish);

        switch (decision) {
            case CLEAR -> clearTaxReturn.execute(item.getTaxReturnId());
            case REQUEST_AMENDMENT -> requestAmendment.execute(
                item.getTaxReturnId(), AmendmentReason.OFFICER_REQUESTED, officerActorId);
            case CONFIRM_FRAUD -> confirmFraud.execute(item.getTaxReturnId(), officerActorId, narrative);
        }
        return saved;
    }
}
