package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/** Taxpayer identity: TIN + partyId from the registration service. */
public record TaxpayerReference(String tin, String partyId) {

    public TaxpayerReference {
        Objects.requireNonNull(tin, "tin");
        Objects.requireNonNull(partyId, "partyId");
        if (tin.isBlank()) throw new IllegalArgumentException("tin must not be blank");
    }
}
