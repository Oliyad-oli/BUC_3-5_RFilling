package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Internal — records risk-engine result on the aggregate (BUC-FIL-020). */
@Service
@RequiredArgsConstructor
public class RecordRiskOutcomeUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(UUID taxReturnId, RiskOutcome outcome) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        t.recordRiskOutcome(outcome);
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
    }
}
