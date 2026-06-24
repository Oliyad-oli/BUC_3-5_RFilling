package com.itas.taxfiling.domain.valueobject;

/** Validation state of a line item (Rule 12 level outcomes). */
public enum LineItemValidationState {
    /** No issues found — safe to proceed. */
    CLEAN,
    /** Level-2 findings attached — advisory only, does not block submission. */
    FLAGGED
}
