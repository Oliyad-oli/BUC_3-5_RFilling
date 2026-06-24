package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Internal — called by the OutboxDispatcher after ledger-engine accepted the
 * principal posting. Updates the aggregate to POSTED_TO_LEDGER and emits
 * PostedToLedgerEvent which kicks off the parallel risk + rule arms.
 */
@Service
@RequiredArgsConstructor
public class RecordLedgerPostedUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(UUID taxReturnId, LedgerEntryReference reference) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        t.recordLedgerPosted(reference);
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
    }
}
