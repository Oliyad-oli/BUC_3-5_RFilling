package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/** Rule-engine validation result (BUC-FIL-021). */
public record RuleOutcome(boolean passed, String rulePackageVersion, String details) {

    public RuleOutcome {
        Objects.requireNonNull(rulePackageVersion, "rulePackageVersion");
    }
}
