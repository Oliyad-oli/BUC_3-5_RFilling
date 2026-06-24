package com.itas.taxfiling.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record FilingPeriodResponse(
    String id,
    String tin,
    String taxType,
    LocalDate periodStartDate,
    LocalDate periodEndDate,
    String status,
    LocalDate dueDate,
    LocalDate filedDate,
    String returnId,
    Instant createdAt
) {}
