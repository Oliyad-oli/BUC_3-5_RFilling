package com.itas.taxfiling.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CalculationResultResponse(
    String iterationId,
    int iterationNumber,
    BigDecimal grossTax,
    BigDecimal inputCredit,
    BigDecimal netTax,
    String currency,
    List<ComputedLineItemResponse> computedLineItems
) {
    public record ComputedLineItemResponse(
        String lineCode,
        String description,
        BigDecimal amount,
        String currency
    ) {}
}
