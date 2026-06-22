package com.itas.taxfiling.domain.exception;

/**
 * Base Domain Exception
 * 
 * All domain-specific exceptions extend this class
 */
public abstract class DomainException extends RuntimeException {
    
    protected DomainException(String message) {
        super(message);
    }
    
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
