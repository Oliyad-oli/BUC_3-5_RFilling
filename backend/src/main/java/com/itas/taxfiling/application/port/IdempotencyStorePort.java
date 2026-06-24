package com.itas.taxfiling.application.port;

import java.util.Optional;

public interface IdempotencyStorePort {
    boolean hasBeenProcessed(String idempotencyKey);
    void saveResponse(String idempotencyKey, String responsePayload);
    Optional<String> getResponse(String idempotencyKey);
}
