package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;

import java.math.BigDecimal;
import java.util.UUID;

public record CalculationIterationResponse(
    UUID id,
    int sequence,
    boolean accepted,
    OutcomeDto outcome
) {
    public static CalculationIterationResponse from(CalculationIteration it) {
        return new CalculationIterationResponse(
            it.getId(), it.getSequence(), it.isAccepted(),
            it.getOutcome() == null ? null : OutcomeDto.from(it.getOutcome()));
    }

    public record OutcomeDto(BigDecimal grossTax, BigDecimal credits, BigDecimal netTax,
                             String currency, String rulePackageVersion) {
        public static OutcomeDto from(CalculationOutcome o) {
            return new OutcomeDto(
                o.grossTax().amount(), o.credits().amount(), o.netTax().amount(),
                o.netTax().currency(), o.rulePackage().version());
        }
    }
}
