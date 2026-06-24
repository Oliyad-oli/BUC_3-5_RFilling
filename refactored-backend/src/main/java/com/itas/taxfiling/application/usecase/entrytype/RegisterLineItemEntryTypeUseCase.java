package com.itas.taxfiling.application.usecase.entrytype;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registers a new LineItemEntryType version (BUC-FIL-CONFIG-01). Immutable-rows
 * pattern: each call inserts a new (code, version) row; never updates existing rows.
 */
@Service
@RequiredArgsConstructor
public class RegisterLineItemEntryTypeUseCase {

    private final LineItemEntryTypeRepositoryPort entryTypes;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public LineItemEntryType execute(String code, TaxTypeCode taxType, ScheduleKind scheduleKind,
                                     List<EntryFieldDefinition> fields, String adminActorId) {
        int version = entryTypes.nextVersionForCode(code);
        LineItemEntryType type = LineItemEntryType.register(
            code, taxType, scheduleKind, version, fields, adminActorId);
        LineItemEntryType saved = entryTypes.save(type);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
