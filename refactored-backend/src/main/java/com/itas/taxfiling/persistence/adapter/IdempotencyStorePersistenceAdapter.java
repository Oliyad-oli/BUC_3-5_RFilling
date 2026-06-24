package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.IdempotencyStorePort;
import com.itas.taxfiling.persistence.jpa.entity.IdempotencyKeyEntity;
import com.itas.taxfiling.persistence.jpa.repository.IdempotencyKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyStorePersistenceAdapter implements IdempotencyStorePort {

    private final IdempotencyKeyJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredResponse> find(String key, String endpoint) {
        return repository.findByKeyValueAndEndpoint(key, endpoint)
            .filter(e -> e.getExpiresAt().isAfter(Instant.now()))
            .map(e -> new StoredResponse(e.getRequestHash(), e.getStatusCode(),
                e.getResponseBody(), e.getExpiresAt()));
    }

    @Override
    @Transactional
    public void store(String key, String endpoint, String requestHash, int statusCode,
                      String responseBody, Instant expiresAt) {
        IdempotencyKeyEntity e = new IdempotencyKeyEntity();
        e.setId(key + "::" + endpoint);
        e.setKeyValue(key);
        e.setEndpoint(endpoint);
        e.setRequestHash(requestHash);
        e.setStatusCode(statusCode);
        e.setResponseBody(responseBody);
        e.setCreatedAt(Instant.now());
        e.setExpiresAt(expiresAt);
        repository.save(e);
    }
}
