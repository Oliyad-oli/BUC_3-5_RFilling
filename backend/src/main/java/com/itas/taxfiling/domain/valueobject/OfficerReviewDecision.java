package com.itas.taxfiling.domain.valueobject;

/**
 * Officer Review Decision
 * 
 * Represents the decision an officer can make on a review item
 */
public enum OfficerReviewDecision {
    /**
     * Clear the return - no issues found
     */
    CLEAR,
    
    /**
     * Request taxpayer to amend the return
     */
    REQUEST_AMENDMENT,
    
    /**
     * Confirm fraud and escalate
     */
    CONFIRM_FRAUD;
    
    public boolean isClear() {
        return this == CLEAR;
    }
    
    public boolean requiresAmendment() {
        return this == REQUEST_AMENDMENT;
    }
    
    public boolean isFraudConfirmed() {
        return this == CONFIRM_FRAUD;
    }
}
