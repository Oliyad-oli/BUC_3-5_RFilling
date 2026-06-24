package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * List Officer Review Queue Use Case
 * 
 * Retrieves open review items for the officer queue
 */
@Service
@RequiredArgsConstructor
public class ListOfficerReviewQueueUseCase {
    
    private final OfficerReviewItemRepositoryPort reviewItemRepository;
    
    @Transactional(readOnly = true)
    public List<OfficerReviewItem> execute(String status) {
        if (status != null) {
            return reviewItemRepository.findByStatus(status);
        }
        return reviewItemRepository.findByStatus("OPEN");
    }
}
