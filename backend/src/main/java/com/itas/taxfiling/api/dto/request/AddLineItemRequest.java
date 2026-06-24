package com.itas.taxfiling.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AddLineItemRequest(
    @NotBlank String scheduleCode,
    String scheduleName,
    @NotBlank String lineCode,
    String description,
    @NotNull BigDecimal amount,
    String currency,
    String source,
    String referenceId
) {}
