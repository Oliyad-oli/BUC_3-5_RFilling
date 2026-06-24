package com.itas.taxfiling.domain.valueobject;

/**
 * Supported tax types in the ITAS filing system.
 * New tax types are added here when the LineItemEntryType catalog is extended.
 */
public enum TaxTypeCode {
    VAT,
    WHT_PAYE,
    WHT_SUPPLIER,
    CIT,
    TOT,
    EXCISE
}
