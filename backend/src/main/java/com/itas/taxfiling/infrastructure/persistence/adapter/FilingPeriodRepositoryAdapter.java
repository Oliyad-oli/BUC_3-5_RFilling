package com.itas.taxfiling.infrastructure.persistence.adapter;

import com.itas.taxfiling.application.port.FilingPeriodRepositoryPort;
import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.infrastructure.persistence.entity.FilingPeriodJpaEntity;
import com.itas.taxfiling.infrastructure.persistence.mapper.ReflectionMapper;
import com.itas.taxfiling.infrastructure.persistence.repository.FilingPeriodJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FilingPeriodRepositoryAdapter implements FilingPeriodRepositoryPort {

    private final FilingPeriodJpaRepository repository;

    @Override
    public FilingPeriod save(FilingPeriod period) {
        FilingPeriodJpaEntity entity = mapToEntity(period);
        FilingPeriodJpaEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<FilingPeriod> findById(String id) {
        return repository.findById(id).map(this::mapToDomain);
    }

    @Override
    public List<FilingPeriod> findByTin(String tin) {
        return repository.findByTin(tin).stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<FilingPeriod> findByTinAndStatus(String tin, FilingPeriodStatus status) {
        return repository.findByTinAndStatus(tin, status.name()).stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<FilingPeriod> findByTinAndTaxType(String tin, TaxTypeCode taxType) {
        return repository.findByTinAndTaxType(tin, taxType.name()).stream().map(this::mapToDomain).collect(Collectors.toList());
    }

    private FilingPeriodJpaEntity mapToEntity(FilingPeriod domain) {
        if (domain == null) return null;
        FilingPeriodJpaEntity entity = new FilingPeriodJpaEntity();
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
        entity.setDueDate(domain.getDueDate());
        entity.setFiledDate(domain.getFiledDate());
        entity.setReturnId(domain.getReturnId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    private FilingPeriod mapToDomain(FilingPeriodJpaEntity entity) {
        if (entity == null) return null;
        FilingPeriod domain = ReflectionMapper.createInstance(FilingPeriod.class);
        ReflectionMapper.setField(domain, "id", entity.getId());
        ReflectionMapper.setField(domain, "tin", entity.getTin());
        if (entity.getTaxType() != null) {
            ReflectionMapper.setField(domain, "taxType", TaxTypeCode.valueOf(entity.getTaxType()));
        }
        if (entity.getPeriodStartDate() != null && entity.getPeriodEndDate() != null) {
            ReflectionMapper.setField(domain, "period", Period.of(entity.getPeriodStartDate(), entity.getPeriodEndDate()));
        }
        if (entity.getStatus() != null) {
            ReflectionMapper.setField(domain, "status", FilingPeriodStatus.valueOf(entity.getStatus()));
        }
        ReflectionMapper.setField(domain, "dueDate", entity.getDueDate());
        ReflectionMapper.setField(domain, "filedDate", entity.getFiledDate());
        ReflectionMapper.setField(domain, "returnId", entity.getReturnId());
        ReflectionMapper.setField(domain, "createdAt", entity.getCreatedAt());
        ReflectionMapper.setField(domain, "updatedAt", entity.getUpdatedAt());
        return domain;
    }
}
