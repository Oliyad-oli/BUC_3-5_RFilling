package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/** Reference to subledger group for a (TIN × tax type) pair. */
public record SubledgerGroupReference(String tin, TaxTypeCode taxType) {

    public SubledgerGroupReference {
        Objects.requireNonNull(tin, "tin");
        Objects.requireNonNull(taxType, "taxType");
    }
}
