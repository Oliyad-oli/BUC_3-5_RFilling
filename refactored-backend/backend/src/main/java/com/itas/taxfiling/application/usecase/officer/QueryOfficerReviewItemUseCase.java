package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryOfficerReviewItemUseCase {

    private final OfficerReviewItemRepositoryPort items;

    @Transactional(readOnly = true)
    public OfficerReviewItem execute(UUID id) {
        return items.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("review item not found: " + id));
    }
}
