package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Removes a LineItem from a Schedule (BUC-FIL-006). Only allowed in DRAFT/AMENDMENT_DRAFT. */
@Service
@RequiredArgsConstructor
public class DeleteLineItemUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(UUID taxReturnId, UUID scheduleId, UUID lineItemId) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        if (t.getStatus() != TaxReturnStatus.DRAFT && t.getStatus() != TaxReturnStatus.AMENDMENT_DRAFT) {
            throw new com.itas.taxfiling.domain.exception.DomainException(
                "cannot delete line items in status " + t.getStatus());
        }
        Schedule schedule = t.getSchedules().stream()
            .filter(s -> s.getId().equals(scheduleId)).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("schedule not found: " + scheduleId));
        schedule.removeLineItem(lineItemId);
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
    }
}
