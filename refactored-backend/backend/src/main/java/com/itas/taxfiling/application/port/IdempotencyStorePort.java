package com.itas.taxfiling.application.port;

import java.time.Instant;
import java.util.Optional;

/**
 * Storage for idempotency keys consumed by IdempotencyFilter. Implementations
 * persist (key, endpoint) → request hash + cached response, with TTL eviction.
 */
public interface IdempotencyStorePort {

    Optional<StoredResponse> find(String key, String endpoint);

    void store(String key, String endpoint, String requestHash, int statusCode,
               String responseBody, Instant expiresAt);

    record StoredResponse(String requestHash, int statusCode, String responseBody, Instant expiresAt) {}
}
