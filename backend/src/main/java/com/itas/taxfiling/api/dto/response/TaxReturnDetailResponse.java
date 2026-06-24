package com.itas.taxfiling.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaxReturnDetailResponse(
    String id,
    String tin,
    String taxType,
    String status,
    LocalDate periodStartDate,
    LocalDate periodEndDate,
    String currentIterationId,
    BigDecimal netTaxAmount,
    String currency,
    Instant createdAt,
    Instant updatedAt,
    String createdBy,
    List<ScheduleResponse> schedules,
    List<CalculationIterationResponse> iterations
) {
    public record ScheduleResponse(
        String id,
        String code,
        String name,
        List<LineItemResponse> lineItems
    ) {}
    
    public record LineItemResponse(
        String id,
        String lineCode,
        String description,
        BigDecimal amount,
        String currency,
        String source,
        String referenceId
    ) {}
    
    public record CalculationIterationResponse(
        String id,
        int iterationNumber,
        BigDecimal grossTax,
        BigDecimal inputCredit,
        BigDecimal netTax,
        String currency,
        Instant calculatedAt,
        boolean accepted
    ) {}
}
