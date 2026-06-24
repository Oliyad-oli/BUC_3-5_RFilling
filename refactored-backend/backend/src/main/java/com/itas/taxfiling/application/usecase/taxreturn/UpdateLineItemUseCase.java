package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.service.LineItemConsistencyChecker;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.ValidationLevel;
import com.itas.taxfiling.domain.valueobject.ValidationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Updates an existing line item's amount and/or entry data (BUC-FIL-006). */
@Service
@RequiredArgsConstructor
public class UpdateLineItemUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final LineItemEntryTypeRepositoryPort entryTypes;
    private final EventPublisherPort eventPublisher;
    private final LineItemConsistencyChecker consistencyChecker = new LineItemConsistencyChecker();

    @Transactional
    public LineItem execute(UUID taxReturnId, UUID scheduleId, UUID lineItemId,
                            Money amount, EntrySpecificData entryData) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        Schedule schedule = t.getSchedules().stream()
            .filter(s -> s.getId().equals(scheduleId)).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("schedule not found: " + scheduleId));
        LineItem li = schedule.findLineItem(lineItemId)
            .orElseThrow(() -> new ResourceNotFoundException("line item not found: " + lineItemId));

        // Mutating the embedded entity directly (allowed because we're in the aggregate's tx).
        if (amount != null) li.replaceAmount(amount);
        if (entryData != null) li.replaceEntryData(entryData);

        LineItemEntryType type = entryTypes.findById(li.getEntryTypeId())
            .orElseThrow(() -> new DomainException("entry type missing: " + li.getEntryTypeId()));
        List<ValidationMessage> findings = consistencyChecker.check(li, type);
        boolean hasLevel1Error = findings.stream()
            .anyMatch(m -> m.level() == ValidationLevel.LEVEL_1_FIELD
                && m.severity() == ValidationMessage.Severity.ERROR);
        if (hasLevel1Error) {
            throw new DomainException("line item failed Level 1 validation: " + findings);
        }
        if (findings.isEmpty()) li.clearFindings();
        else                    t.flagLineItem(scheduleId, li.getId(), findings);

        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
        return li;
    }
}
