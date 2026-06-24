package com.itas.taxfiling.domain.exception;

/**
 * Base class for all domain rule violations in the Tax Filing domain.
 * Subtypes carry specific semantic meaning and are mapped to HTTP status
 * codes by the API advice layer (GlobalExceptionHandler).
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
