package com.itas.taxfiling.application.usecase.entrytype;

import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryLineItemEntryTypeUseCase {

    private final LineItemEntryTypeRepositoryPort entryTypes;

    @Transactional(readOnly = true)
    public LineItemEntryType execute(UUID id) {
        return entryTypes.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("entry type not found: " + id));
    }
}
