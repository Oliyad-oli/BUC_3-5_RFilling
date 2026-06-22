package com.itas.taxfiling.domain.exception;

/**
 * Business Rule Violation Exception
 * 
 * Thrown when a business rule is violated
 */
public class BusinessRuleViolationException extends DomainException {
    
    public BusinessRuleViolationException(String rule, String violation) {
        super(String.format("Business rule '%s' violated: %s", rule, violation));
    }
    
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
