package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOfficerReviewItemUseCase {
    private final OfficerReviewItemRepositoryPort reviewItemRepository;

    @Transactional(readOnly = true)
    public OfficerReviewItem execute(String caseId) {
        return reviewItemRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("OfficerReviewItem", caseId));
    }
}
