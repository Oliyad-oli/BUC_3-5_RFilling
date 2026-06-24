package com.itas.taxfiling.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DraftTaxReturnRequest(
    @NotBlank String tin,
    @NotBlank String taxType,
    @NotBlank String filingPeriodId,
    String createdBy
) {}
