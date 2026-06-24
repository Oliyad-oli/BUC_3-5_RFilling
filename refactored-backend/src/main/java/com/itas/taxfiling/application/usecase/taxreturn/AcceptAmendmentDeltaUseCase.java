package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AmendmentDelta;
import com.itas.taxfiling.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Accepts the amendment delta (BUC-FIL-032). Computes delta = newNet -
 * originalNet from the iterations and references the original ledger entry id.
 * Aggregate emits AmendmentAcceptedEvent which the outbox handler turns into a
 * ledger.postAdjustment call (Rule 8 — PRINCIPAL only).
 */
@Service
@RequiredArgsConstructor
public class AcceptAmendmentDeltaUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public TaxReturn execute(UUID taxReturnId, UUID amendmentIterationId) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));

        CalculationIteration newIter = t.getIterations().stream()
            .filter(i -> i.getId().equals(amendmentIterationId))
            .findFirst()
            .orElseThrow(() -> new DomainException("iteration not found: " + amendmentIterationId));
        if (newIter.getOutcome() == null) {
            throw new DomainException("amendment iteration not yet completed");
        }
        Money newNet = newIter.getOutcome().netTax();
        Money originalNet = t.getIterations().stream()
            .filter(CalculationIteration::isAccepted)
            .reduce((a, b) -> a)
            .orElseThrow(() -> new DomainException("no accepted iteration on original return"))
            .getOutcome().netTax();
        Money delta = newNet.subtract(originalNet);
        UUID originalEntryId = t.getPrincipalLedgerEntry()
            .orElseThrow(() -> new DomainException("original ledger entry not found"))
            .entryId();

        t.acceptAmendmentDelta(new AmendmentDelta(delta, originalEntryId));
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
