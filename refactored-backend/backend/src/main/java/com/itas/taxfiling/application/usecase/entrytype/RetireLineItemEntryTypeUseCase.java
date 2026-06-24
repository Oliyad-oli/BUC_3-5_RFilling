package com.itas.taxfiling.application.usecase.entrytype;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetireLineItemEntryTypeUseCase {

    private final LineItemEntryTypeRepositoryPort entryTypes;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public LineItemEntryType execute(UUID id, String adminActorId) {
        LineItemEntryType type = entryTypes.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("entry type not found: " + id));
        type.retire(adminActorId);
        LineItemEntryType saved = entryTypes.save(type);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
