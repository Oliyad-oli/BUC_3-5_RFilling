package com.itas.taxfiling.domain.valueobject;

/** How a line item was sourced. */
public enum LineItemSource {
    MANUAL,
    E_INVOICE,
    BULK_UPLOAD,
    CARRY_FORWARD,
    SYSTEM
}
