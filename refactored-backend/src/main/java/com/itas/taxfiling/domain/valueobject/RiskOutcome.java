package com.itas.taxfiling.domain.valueobject;

import java.util.List;
import java.util.Objects;

/**
 * Risk-engine evaluation result (BUC-FIL-020).
 * {@code justification} is load-bearing — it flows to the officer review item
 * so the reviewer knows WHY the return was flagged.
 */
public record RiskOutcome(RiskLevel level, double score,
                          List<String> indicators, String justification) {
    public RiskOutcome {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(justification, "justification");
        if (justification.isBlank()) throw new IllegalArgumentException("justification must not be blank");
        indicators = indicators == null ? List.of() : List.copyOf(indicators);
    }
}
