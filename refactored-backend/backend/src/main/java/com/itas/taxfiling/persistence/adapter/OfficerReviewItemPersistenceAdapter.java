package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.persistence.jpa.entity.OfficerReviewItemEntity;
import com.itas.taxfiling.persistence.jpa.repository.OfficerReviewItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OfficerReviewItemPersistenceAdapter implements OfficerReviewItemRepositoryPort {

    private final OfficerReviewItemJpaRepository repository;

    @Override
    @Transactional
    public OfficerReviewItem save(OfficerReviewItem item) {
        OfficerReviewItemEntity entity = repository.findById(item.getId())
            .orElseGet(OfficerReviewItemEntity::new);
        applyTo(entity, item);
        repository.save(entity);
        // Return the input instance — events still attached for the caller to publish.
        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OfficerReviewItem> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficerReviewItem> findOpenQueue() {
        return repository.findByDecisionIsNullOrderByPriorityDescCreatedAtAsc()
            .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficerReviewItem> findByAssignedOfficer(String officerActorId) {
        return repository.findByAssignedOfficerActorId(officerActorId)
            .stream().map(this::toDomain).toList();
    }

    private void applyTo(OfficerReviewItemEntity e, OfficerReviewItem o) {
        e.setId(o.getId());
        e.setTaxReturnId(o.getTaxReturnId());
        e.setKind(o.getKind());
        e.setPriority(o.getPriority());
        e.setRiskJustification(o.getRiskJustification());
        e.setRiskIndicators(Map.of("indicators", o.getRiskIndicators()));
        e.setAssignedOfficerActorId(o.getAssignedOfficerActorId().orElse(null));
        e.setDecision(o.getDecision().orElse(null));
        e.setDecisionNarrative(o.getDecisionNarrative());
        e.setDecidedAt(o.getDecidedAt());
        e.setCreatedAt(o.getCreatedAt());
    }

    @SuppressWarnings("unchecked")
    private OfficerReviewItem toDomain(OfficerReviewItemEntity e) {
        List<String> indicators = e.getRiskIndicators() == null
            ? List.of()
            : (List<String>) e.getRiskIndicators().getOrDefault("indicators", List.of());
        return OfficerReviewItem.rehydrate(
            e.getId(), e.getTaxReturnId(), e.getKind(), e.getPriority(),
            e.getRiskJustification(), indicators,
            e.getAssignedOfficerActorId(), e.getDecision(), e.getDecisionNarrative(),
            e.getDecidedAt(), e.getCreatedAt(), e.getVersion());
    }
}
