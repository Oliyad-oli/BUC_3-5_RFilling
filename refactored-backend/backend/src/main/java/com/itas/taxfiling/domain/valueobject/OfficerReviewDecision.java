package com.itas.taxfiling.domain.valueobject;

/** Officer decision on a MANUAL_REVIEW tax return. */
public enum OfficerReviewDecision {
    /** Return is cleared — proceeds to COMPLETED and certificate is issued. */
    CLEAR,
    /** Fraud is confirmed — hand-off to case-management (Phase 2). */
    CONFIRM_FRAUD
}
