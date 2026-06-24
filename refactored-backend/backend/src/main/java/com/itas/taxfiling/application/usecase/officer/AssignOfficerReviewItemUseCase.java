package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignOfficerReviewItemUseCase {

    private final OfficerReviewItemRepositoryPort items;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public OfficerReviewItem execute(UUID reviewItemId, String officerActorId) {
        OfficerReviewItem item = items.findById(reviewItemId)
            .orElseThrow(() -> new ResourceNotFoundException("review item not found: " + reviewItemId));
        item.assign(officerActorId);
        OfficerReviewItem saved = items.save(item);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
