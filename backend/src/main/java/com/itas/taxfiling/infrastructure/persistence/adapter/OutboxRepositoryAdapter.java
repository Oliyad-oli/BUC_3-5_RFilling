package com.itas.taxfiling.infrastructure.persistence.adapter;

import com.itas.taxfiling.application.port.OutboxRepositoryPort;
import com.itas.taxfiling.domain.model.OutboxEntry;
import com.itas.taxfiling.infrastructure.persistence.entity.OutboxEntryJpaEntity;
import com.itas.taxfiling.infrastructure.persistence.repository.OutboxEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final OutboxEntryJpaRepository repository;

    @Override
    public OutboxEntry save(OutboxEntry entry) {
        OutboxEntryJpaEntity entity = new OutboxEntryJpaEntity();
        entity.setId(entry.getId());
        entity.setAggregateType(entry.getAggregateType());
        entity.setAggregateId(entry.getAggregateId());
        entity.setEventType(entry.getEventType());
        entity.setPayload(entry.getPayload());
        entity.setStatus(entry.getStatus().name());
        entity.setCreatedAt(entry.getCreatedAt());
        entity.setSentAt(entry.getSentAt());
        entity.setRetryCount(entry.getRetryCount());
        entity.setErrorMessage(entry.getErrorMessage());

        OutboxEntryJpaEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public List<OutboxEntry> findPendingEntries(int limit) {
        return repository.findPending(limit).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    private OutboxEntry mapToDomain(OutboxEntryJpaEntity entity) {
        // As OutboxEntry is an aggregate with private constructor and no builder, 
        // normally we would map via reflection or add a reconstituted method in the domain.
        // For simplicity and adherence to DDD, let's use spring data mapping internally or a minimal reflection tool.
        // Alternatively, OutboxEntry.create() doesn't set ID, it generates one. 
        // We will just do a workaround for now by modifying the domain slightly if needed, or by using reflection.
        try {
            java.lang.reflect.Constructor<OutboxEntry> constructor = OutboxEntry.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            OutboxEntry domain = constructor.newInstance();
            
            setField(domain, "id", entity.getId());
            setField(domain, "aggregateType", entity.getAggregateType());
            setField(domain, "aggregateId", entity.getAggregateId());
            setField(domain, "eventType", entity.getEventType());
            setField(domain, "payload", entity.getPayload());
            setField(domain, "status", com.itas.taxfiling.domain.valueobject.OutboxStatus.valueOf(entity.getStatus()));
            setField(domain, "createdAt", entity.getCreatedAt());
            setField(domain, "sentAt", entity.getSentAt());
            setField(domain, "retryCount", entity.getRetryCount());
            setField(domain, "errorMessage", entity.getErrorMessage());
            
            return domain;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map OutboxEntryJpaEntity to OutboxEntry", e);
        }
    }
    
    private void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
