package com.itas.taxfiling.domain.valueobject;

/**
 * Officer Review Item Kind
 * 
 * Represents the reason why a return was flagged for officer review
 */
public enum OfficerReviewItemKind {
    /**
     * Risk engine flagged as potential fraud
     */
    RISK_FRAUD,
    
    /**
     * Post-ledger rule validation failed
     */
    POST_LEDGER_RULE_FAIL,
    
    /**
     * Claim amount mismatch after ledger posting
     */
    POST_LEDGER_CLAIM_MISMATCH,
    
    /**
     * Exceeded maximum calculation iteration attempts
     */
    ITERATION_CAP,
    
    /**
     * Calculation engine failure
     */
    CALC_FAILURE,
    
    /**
     * Amendment shows unusual patterns
     */
    AMENDMENT_ANOMALY;
    
    public boolean isFraudRelated() {
        return this == RISK_FRAUD;
    }
    
    public boolean isPostLedgerIssue() {
        return this == POST_LEDGER_RULE_FAIL || this == POST_LEDGER_CLAIM_MISMATCH;
    }
    
    public boolean isSystemFailure() {
        return this == CALC_FAILURE || this == ITERATION_CAP;
    }
}
