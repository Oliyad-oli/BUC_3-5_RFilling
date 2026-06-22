package com.itas.taxfiling.domain.exception;

/**
 * Invalid State Transition Exception
 * 
 * Thrown when attempting an invalid state transition on an aggregate
 */
public class InvalidStateTransitionException extends DomainException {
    
    public InvalidStateTransitionException(String currentState, String attemptedTransition) {
        super(String.format("Cannot perform '%s' from current state '%s'", 
            attemptedTransition, currentState));
    }
    
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
