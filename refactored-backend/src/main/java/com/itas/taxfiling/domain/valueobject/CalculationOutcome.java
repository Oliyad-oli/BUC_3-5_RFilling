package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/**
 * Result of a single rule-engine calculation pass (BUC-FIL-010–013).
 * Carried on a CalculationIteration.
 */
public record CalculationOutcome(Money grossTax, Money credits, Money netTax,
                                 String currency, String rulePackageVersion) {
    public CalculationOutcome {
        Objects.requireNonNull(grossTax, "grossTax");
        Objects.requireNonNull(credits, "credits");
        Objects.requireNonNull(netTax, "netTax");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(rulePackageVersion, "rulePackageVersion");
    }
}
