package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.service.LineItemConsistencyChecker;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.ValidationLevel;
import com.itas.taxfiling.domain.valueobject.ValidationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Adds a LineItem to a Schedule (BUC-FIL-004). Runs validation cascade Levels 1
 * + 2 (Rule 12) — Level 1 errors block the save, Level 2 findings flag the line.
 */
@Service
@RequiredArgsConstructor
public class AddLineItemUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final LineItemEntryTypeRepositoryPort entryTypes;
    private final EventPublisherPort eventPublisher;
    private final LineItemConsistencyChecker consistencyChecker = new LineItemConsistencyChecker();

    @Transactional
    public LineItem execute(UUID taxReturnId, UUID scheduleId, UUID entryTypeId,
                            Money amount, LineItemSource source, EntrySpecificData entryData) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        LineItemEntryType type = entryTypes.findById(entryTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("entry type not found: " + entryTypeId));
        if (type.getStatus() == EntryTypeStatus.RETIRED) {
            throw new DomainException("entry type retired: " + type.getCode() + " v" + type.getVersion());
        }

        LineItem added = t.addLineItem(scheduleId, type.getId(), type.getVersion(), amount, source, entryData);
        List<ValidationMessage> findings = consistencyChecker.check(added, type);

        boolean hasLevel1Error = findings.stream()
            .anyMatch(m -> m.level() == ValidationLevel.LEVEL_1_FIELD
                && m.severity() == ValidationMessage.Severity.ERROR);
        if (hasLevel1Error) {
            throw new DomainException("line item failed Level 1 validation: " + findings);
        }
        if (!findings.isEmpty()) {
            t.flagLineItem(scheduleId, added.getId(), findings);
        }

        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
        return added;
    }
}
