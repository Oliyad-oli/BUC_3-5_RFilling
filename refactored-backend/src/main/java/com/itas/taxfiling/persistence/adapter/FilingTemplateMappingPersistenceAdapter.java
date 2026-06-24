package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.FilingTemplateMappingRepositoryPort;
import com.itas.taxfiling.domain.model.FilingTemplateMapping;
import com.itas.taxfiling.persistence.jpa.entity.FilingTemplateMappingEntity;
import com.itas.taxfiling.persistence.jpa.repository.FilingTemplateMappingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FilingTemplateMappingPersistenceAdapter
    implements FilingTemplateMappingRepositoryPort {

    private final FilingTemplateMappingJpaRepository repo;

    @Override
    @Transactional
    public FilingTemplateMapping save(FilingTemplateMapping m) {
        FilingTemplateMappingEntity e = repo.findById(m.getTaxTypeCode())
            .orElseGet(FilingTemplateMappingEntity::new);
        e.setTaxTypeCode(m.getTaxTypeCode());
        e.setFilingTemplateRef(m.getFilingTemplateRef());
        e.setLateFilingPenaltyTemplateRef(m.getLateFilingPenaltyTemplateRef());
        if (e.getCreatedAt() == null) {
            e.setCreatedBy(m.getCreatedBy());
            e.setCreatedAt(m.getCreatedAt());
        }
        e.setUpdatedBy(m.getUpdatedBy());
        e.setUpdatedAt(m.getUpdatedAt());
        return toDomain(repo.save(e));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilingTemplateMapping> findByTaxTypeCode(String taxTypeCode) {
        return repo.findById(taxTypeCode).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilingTemplateMapping> findAll() {
        return repo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByTaxTypeCode(String taxTypeCode) {
        repo.deleteById(taxTypeCode);
    }

    private FilingTemplateMapping toDomain(FilingTemplateMappingEntity e) {
        return FilingTemplateMapping.rehydrate(
            e.getTaxTypeCode(),
            e.getFilingTemplateRef(),
            e.getLateFilingPenaltyTemplateRef(),
            e.getCreatedBy(),
            e.getCreatedAt(),
            e.getUpdatedBy(),
            e.getUpdatedAt());
    }
}
