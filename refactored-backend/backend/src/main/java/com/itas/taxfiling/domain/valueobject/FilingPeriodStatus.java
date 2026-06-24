package com.itas.taxfiling.domain.valueobject;

/**
 * Filing period status (Layer-2 calendar model).
 * Transitions are date-driven and managed by the daily status job.
 */
public enum FilingPeriodStatus {
    /** Period not yet open — covers a future window. */
    FUTURE,
    /** Taxpayer can file — within the coverage window. */
    OPEN,
    /** Coverage window passed but still within grace. */
    DUE,
    /** Past the due date — compliance risk. */
    OVERDUE,
    /** TaxReturn completed — period finalised. */
    FILED
}
