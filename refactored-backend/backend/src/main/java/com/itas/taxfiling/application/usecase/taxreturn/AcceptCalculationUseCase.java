package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Accepts the latest calculation iteration (BUC-FIL-013) and emits the
 * PostingToLedgerEvent so the LedgerPostingOutboxHandler can enqueue the
 * outbox row that the dispatcher will turn into a ledger-engine call.
 */
@Service
@RequiredArgsConstructor
public class AcceptCalculationUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public TaxReturn execute(UUID taxReturnId, UUID iterationId) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        t.acceptCalculation(iterationId);
        t.enqueueLedgerPosting();
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
