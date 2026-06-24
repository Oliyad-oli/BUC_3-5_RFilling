package com.itas.taxfiling.infrastructure.persistence.adapter;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.infrastructure.persistence.entity.TaxReturnJpaEntity;
import com.itas.taxfiling.infrastructure.persistence.mapper.ReflectionMapper;
import com.itas.taxfiling.infrastructure.persistence.repository.TaxReturnJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaxReturnRepositoryAdapter implements TaxReturnRepositoryPort {

    private final TaxReturnJpaRepository repository;

    @Override
    public TaxReturn save(TaxReturn taxReturn) {
        TaxReturnJpaEntity entity = mapToEntity(taxReturn);
        TaxReturnJpaEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<TaxReturn> findById(String id) {
        return repository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<TaxReturn> findByTin(String tin) {
        return repository.findByTin(tin).stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    private TaxReturnJpaEntity mapToEntity(TaxReturn domain) {
        if (domain == null) return null;
        TaxReturnJpaEntity entity = new TaxReturnJpaEntity();
        entity.setId(domain.getId());
        entity.setTin(domain.getTin());
        if (domain.getTaxType() != null) {
            entity.setTaxType(domain.getTaxType().name());
        }
        if (domain.getPeriod() != null) {
            entity.setPeriodStartDate(domain.getPeriod().getStartDate());
            entity.setPeriodEndDate(domain.getPeriod().getEndDate());
        }
        if (domain.getStatus() != null) {
            entity.setStatus(domain.getStatus().name());
        }
        entity.setCurrentIterationId(domain.getCurrentIterationId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        return entity;
    }

    private TaxReturn mapToDomain(TaxReturnJpaEntity entity) {
        if (entity == null) return null;
        TaxReturn domain = ReflectionMapper.createInstance(TaxReturn.class);
        ReflectionMapper.setField(domain, "id", entity.getId());
        ReflectionMapper.setField(domain, "tin", entity.getTin());
        if (entity.getTaxType() != null) {
            ReflectionMapper.setField(domain, "taxType", TaxTypeCode.valueOf(entity.getTaxType()));
        }
        if (entity.getPeriodStartDate() != null && entity.getPeriodEndDate() != null) {
            ReflectionMapper.setField(domain, "period", Period.of(entity.getPeriodStartDate(), entity.getPeriodEndDate()));
        }
        if (entity.getStatus() != null) {
            ReflectionMapper.setField(domain, "status", TaxReturnStatus.valueOf(entity.getStatus()));
        }
        ReflectionMapper.setField(domain, "currentIterationId", entity.getCurrentIterationId());
        ReflectionMapper.setField(domain, "createdAt", entity.getCreatedAt());
        ReflectionMapper.setField(domain, "updatedAt", entity.getUpdatedAt());
        ReflectionMapper.setField(domain, "createdBy", entity.getCreatedBy());
        
        // Ensure lists are initialized
        ReflectionMapper.setField(domain, "schedules", new java.util.ArrayList<>());
        ReflectionMapper.setField(domain, "iterations", new java.util.ArrayList<>());
        ReflectionMapper.setField(domain, "amendments", new java.util.ArrayList<>());
        ReflectionMapper.setField(domain, "domainEvents", new java.util.ArrayList<>());
        
        return domain;
    }
}
