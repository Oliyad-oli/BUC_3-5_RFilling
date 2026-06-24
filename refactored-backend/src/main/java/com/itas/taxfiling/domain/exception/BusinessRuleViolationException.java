package com.itas.taxfiling.domain.exception;

/**
 * Thrown when a business rule is violated that doesn't fit the state-machine
 * category (e.g., duplicate return, taxpayer not active, blank required field).
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
