package com.itas.taxfiling.domain.valueobject;

/**
 * Subledger account categories. Each (TIN × tax type) has four subledgers.
 * Filing posts to PRINCIPAL (assessment) and optionally PENALTY (late filing).
 */
public enum AccountCategory {
    PRINCIPAL,
    PENALTY,
    INTEREST,
    REFUND
}
