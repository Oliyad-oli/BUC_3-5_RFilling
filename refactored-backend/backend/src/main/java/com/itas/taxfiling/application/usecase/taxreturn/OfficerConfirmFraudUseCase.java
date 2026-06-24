package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Internal — used by SubmitOfficerDecision to mark a return as fraud-confirmed. */
@Service
@RequiredArgsConstructor
public class OfficerConfirmFraudUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(UUID taxReturnId, String officerActorId, String narrative) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        t.officerConfirmFraud(officerActorId, narrative);
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
    }
}
