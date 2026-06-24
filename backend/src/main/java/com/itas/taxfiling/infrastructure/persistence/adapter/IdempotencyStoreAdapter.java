package com.itas.taxfiling.infrastructure.persistence.adapter;

import com.itas.taxfiling.application.port.IdempotencyStorePort;
import com.itas.taxfiling.infrastructure.persistence.entity.IdempotencyStoreJpaEntity;
import com.itas.taxfiling.infrastructure.persistence.repository.IdempotencyStoreJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyStoreAdapter implements IdempotencyStorePort {

    private final IdempotencyStoreJpaRepository repository;

    @Override
    public boolean hasBeenProcessed(String idempotencyKey) {
        return repository.existsById(idempotencyKey);
    }

    @Override
    public void saveResponse(String idempotencyKey, String responsePayload) {
        IdempotencyStoreJpaEntity entity = new IdempotencyStoreJpaEntity();
        entity.setIdempotencyKey(idempotencyKey);
        entity.setResponsePayload(responsePayload);
        entity.setCreatedAt(Instant.now());
        repository.save(entity);
    }

    @Override
    public Optional<String> getResponse(String idempotencyKey) {
        return repository.findById(idempotencyKey)
                .map(IdempotencyStoreJpaEntity::getResponsePayload);
    }
}
