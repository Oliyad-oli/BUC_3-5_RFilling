package com.itas.taxfiling.domain.valueobject;

/**
 * Filing Period Status
 * 
 * Represents the lifecycle of a filing period:
 * - FUTURE: Period not yet open for filing
 * - OPEN: Period open for filing
 * - DUE: Filing deadline approaching
 * - OVERDUE: Past filing deadline
 * - FILED: Return filed for this period
 */
public enum FilingPeriodStatus {
    FUTURE,
    OPEN,
    DUE,
    OVERDUE,
    FILED;
    
    public boolean canFile() {
        return this == OPEN || this == DUE || this == OVERDUE;
    }
    
    public boolean isFiled() {
        return this == FILED;
    }
    
    public boolean isOverdue() {
        return this == OVERDUE;
    }
}
