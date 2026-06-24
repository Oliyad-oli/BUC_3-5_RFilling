package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitOfficerDecisionUseCase {
    private final OfficerReviewItemRepositoryPort reviewItemRepository;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(Command command) {
        OfficerReviewItem item = reviewItemRepository.findById(command.caseId())
            .orElseThrow(() -> new ResourceNotFoundException("OfficerReviewItem", command.caseId()));
            
        item.assign(command.officerId());
        item.decide(command.decision(), command.narrative());
        
        OfficerReviewItem saved = reviewItemRepository.save(item);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
    }
    
    public record Command(String caseId, String officerId, OfficerReviewDecision decision, String narrative) {}
}
