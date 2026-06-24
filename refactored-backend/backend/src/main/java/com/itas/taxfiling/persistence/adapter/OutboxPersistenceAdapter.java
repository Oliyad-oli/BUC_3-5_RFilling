package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.OutboxPort;
import com.itas.taxfiling.domain.model.OutboxEntry;
import com.itas.taxfiling.domain.valueobject.OutboxStatus;
import com.itas.taxfiling.persistence.jpa.entity.OutboxEntryEntity;
import com.itas.taxfiling.persistence.jpa.repository.OutboxEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements OutboxPort {

    private final OutboxEntryJpaRepository repository;

    @Override
    @Transactional
    public OutboxEntry enqueue(OutboxEntry entry) {
        OutboxEntryEntity e = toEntity(entry);
        return toDomain(repository.save(e));
    }

    @Override
    @Transactional
    public List<OutboxEntry> claimReady(int batchSize, Instant now) {
        return repository.findReady(OutboxStatus.PENDING, now, PageRequest.of(0, batchSize))
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void markSent(OutboxEntry entry) {
        repository.findById(entry.getId()).ifPresent(e -> {
            e.setStatus(OutboxStatus.SENT);
            e.setSentAt(Instant.now());
            repository.save(e);
        });
    }

    @Override
    @Transactional
    public void markRetry(OutboxEntry entry, String error, Instant nextAttemptAt) {
        repository.findById(entry.getId()).ifPresent(e -> {
            e.setAttempts(e.getAttempts() + 1);
            e.setLastError(error);
            e.setNextAttemptAt(nextAttemptAt);
            repository.save(e);
        });
    }

    @Override
    @Transactional
    public void markFailed(OutboxEntry entry, String error) {
        repository.findById(entry.getId()).ifPresent(e -> {
            e.setStatus(OutboxStatus.FAILED);
            e.setLastError(error);
            e.setAttempts(e.getAttempts() + 1);
            repository.save(e);
        });
    }

    private OutboxEntryEntity toEntity(OutboxEntry o) {
        OutboxEntryEntity e = new OutboxEntryEntity();
        e.setId(o.getId());
        e.setAggregateType(o.getAggregateType());
        e.setAggregateId(o.getAggregateId());
        e.setTopic(o.getTopic());
        e.setPayload(o.getPayload());
        e.setStatus(o.getStatus());
        e.setPriority(o.getPriority());
        e.setAttempts(o.getAttempts());
        e.setLastError(o.getLastError());
        e.setNextAttemptAt(o.getNextAttemptAt());
        e.setCreatedAt(o.getCreatedAt());
        e.setSentAt(o.getSentAt());
        return e;
    }

    private OutboxEntry toDomain(OutboxEntryEntity e) {
        return OutboxEntry.rehydrate(
            e.getId(), e.getAggregateType(), e.getAggregateId(), e.getTopic(),
            e.getPayload(), e.getStatus(), e.getPriority(), e.getAttempts(),
            e.getLastError(), e.getNextAttemptAt(), e.getCreatedAt(), e.getSentAt(),
            e.getVersion());
    }
}
