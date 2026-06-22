package com.itas.taxfiling.domain.valueobject;

/**
 * Tax Return Status
 * 
 * Represents the lifecycle states of a tax return through:
 * - Drafting and calculation
 * - Submission and ledger posting
 * - Validation and review
 * - Amendment cycles
 */
public enum TaxReturnStatus {
    /**
     * Initial state - return is being drafted
     */
    DRAFT,
    
    /**
     * Calculation in progress
     */
    CALCULATING,
    
    /**
     * Calculation completed and accepted by taxpayer
     */
    ACCEPTED,
    
    /**
     * Tax liability posted to ledger
     */
    POSTED_TO_LEDGER,
    
    /**
     * Post-ledger validation in progress (risk + rule checks)
     */
    UNDER_VALIDATION,
    
    /**
     * Successfully completed - no issues found
     */
    COMPLETED,
    
    /**
     * Flagged for manual officer review
     */
    MANUAL_REVIEW,
    
    /**
     * Confirmed as fraudulent
     */
    FRAUD_CONFIRMED,
    
    /**
     * Amendment requested - drafting amendment
     */
    AMENDMENT_DRAFT,
    
    /**
     * Amendment calculation in progress
     */
    AMENDMENT_CALCULATING,
    
    /**
     * Amendment calculation accepted
     */
    AMENDMENT_ACCEPTED,
    
    /**
     * Amendment delta posted to ledger
     */
    AMENDMENT_POSTED,
    
    /**
     * Calculation failed (technical error)
     */
    CALCULATION_FAILED;
    
    public boolean isDraft() {
        return this == DRAFT || this == AMENDMENT_DRAFT;
    }
    
    public boolean isCalculating() {
        return this == CALCULATING || this == AMENDMENT_CALCULATING;
    }
    
    public boolean isAccepted() {
        return this == ACCEPTED || this == AMENDMENT_ACCEPTED;
    }
    
    public boolean isPostedToLedger() {
        return this == POSTED_TO_LEDGER || this == AMENDMENT_POSTED;
    }
    
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    
    public boolean requiresReview() {
        return this == MANUAL_REVIEW;
    }
    
    public boolean isFraudConfirmed() {
        return this == FRAUD_CONFIRMED;
    }
    
    public boolean canBeAmended() {
        return this == COMPLETED;
    }
    
    public boolean isFinal() {
        return this == COMPLETED || this == FRAUD_CONFIRMED;
    }
}
