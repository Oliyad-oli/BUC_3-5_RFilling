package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/**
 * The computed net delta of an amendment — the difference between the
 * amended figures and the original assessment (Rule 8).
 */
public record AmendmentDelta(Money netDelta, String currency, String iterationId) {

    public AmendmentDelta {
        Objects.requireNonNull(netDelta, "netDelta");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(iterationId, "iterationId");
    }
}
