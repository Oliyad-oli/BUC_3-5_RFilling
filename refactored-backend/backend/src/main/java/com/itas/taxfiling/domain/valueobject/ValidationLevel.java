package com.itas.taxfiling.domain.valueobject;

/**
 * Rule 12 validation cascade levels.
 * LEVEL_1 findings block the operation; LEVEL_2 flag the row for officer review.
 */
public enum ValidationLevel {
    /** Blocking — the line item is rejected. */
    LEVEL_1,
    /** Advisory — the line item is flagged but allowed through. */
    LEVEL_2
}
