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
 * Internal — transitions UNDER_VALIDATION and emits the fan-out event
 * (Rule 7, BUC-FIL-020/021).
 */
@Service
@RequiredArgsConstructor
public class StartPostLedgerValidationUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(UUID taxReturnId) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        t.startPostLedgerValidation();
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
    }
}
