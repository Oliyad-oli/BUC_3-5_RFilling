package com.itas.taxfiling.domain.exception;

/**
 * Thrown when an aggregate receives an operation that is not valid for its
 * current status (e.g., calling acceptCalculation() on an ACCEPTED return).
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(Object currentStatus, String operation) {
        super("Cannot perform '" + operation + "' on aggregate in status: " + currentStatus);
    }
}
