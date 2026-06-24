package com.itas.taxfiling.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaxReturnResponse(
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
    String createdBy
) {}
