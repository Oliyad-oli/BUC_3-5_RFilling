package com.itas.taxfiling.infrastructure.persistence.adapter;

import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind;
import com.itas.taxfiling.infrastructure.persistence.entity.OfficerReviewItemJpaEntity;
import com.itas.taxfiling.infrastructure.persistence.mapper.ReflectionMapper;
import com.itas.taxfiling.infrastructure.persistence.repository.OfficerReviewItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OfficerReviewItemRepositoryAdapter implements OfficerReviewItemRepositoryPort {

    private final OfficerReviewItemJpaRepository repository;

    @Override
    public OfficerReviewItem save(OfficerReviewItem item) {
        OfficerReviewItemJpaEntity entity = mapToEntity(item);
        OfficerReviewItemJpaEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<OfficerReviewItem> findById(String id) {
        return repository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<OfficerReviewItem> findByStatus(String status) {
        return repository.findByStatus(status).stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<OfficerReviewItem> findByAssignedOfficer(String officerId) {
        return repository.findByAssignedOfficer(officerId).stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<OfficerReviewItem> findByReturnId(String returnId) {
        return repository.findByReturnId(returnId).map(this::mapToDomain);
    }

    private OfficerReviewItemJpaEntity mapToEntity(OfficerReviewItem domain) {
        if (domain == null) return null;
        OfficerReviewItemJpaEntity entity = new OfficerReviewItemJpaEntity();
        entity.setId(domain.getId());
        entity.setReturnId(domain.getReturnId());
        if (domain.getKind() != null) {
            entity.setKind(domain.getKind().name());
        }
        entity.setPriority(domain.getPriority());
        entity.setStatus(domain.getStatus());
        entity.setAssignedOfficer(domain.getAssignedOfficer());
        if (domain.getDecision() != null) {
            entity.setDecision(domain.getDecision().name());
        }
        entity.setDecisionNotes(domain.getDecisionNotes());
        entity.setEvidencePayload(domain.getEvidencePayload());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setAssignedAt(domain.getAssignedAt());
        entity.setDecidedAt(domain.getDecidedAt());
        return entity;
    }

    private OfficerReviewItem mapToDomain(OfficerReviewItemJpaEntity entity) {
        if (entity == null) return null;
        OfficerReviewItem domain = ReflectionMapper.createInstance(OfficerReviewItem.class);
        ReflectionMapper.setField(domain, "id", entity.getId());
        ReflectionMapper.setField(domain, "returnId", entity.getReturnId());
        if (entity.getKind() != null) {
            ReflectionMapper.setField(domain, "kind", OfficerReviewItemKind.valueOf(entity.getKind()));
        }
        ReflectionMapper.setField(domain, "priority", entity.getPriority());
        ReflectionMapper.setField(domain, "status", entity.getStatus());
        ReflectionMapper.setField(domain, "assignedOfficer", entity.getAssignedOfficer());
        if (entity.getDecision() != null) {
            ReflectionMapper.setField(domain, "decision", OfficerReviewDecision.valueOf(entity.getDecision()));
        }
        ReflectionMapper.setField(domain, "decisionNotes", entity.getDecisionNotes());
        ReflectionMapper.setField(domain, "evidencePayload", entity.getEvidencePayload());
        ReflectionMapper.setField(domain, "createdAt", entity.getCreatedAt());
        ReflectionMapper.setField(domain, "assignedAt", entity.getAssignedAt());
        ReflectionMapper.setField(domain, "decidedAt", entity.getDecidedAt());
        
        ReflectionMapper.setField(domain, "domainEvents", new java.util.ArrayList<>());
        return domain;
    }
}
