package com.itas.taxfiling.domain.exception;

import java.util.UUID;

/**
 * Thrown when a requested aggregate is not found in the repository.
 * Maps to HTTP 404 Not Found.
 */
public class AggregateNotFoundException extends DomainException {

    public AggregateNotFoundException(String aggregateType, UUID id) {
        super(aggregateType + " not found: " + id);
    }

    public AggregateNotFoundException(String aggregateType, String key) {
        super(aggregateType + " not found: " + key);
    }
}
