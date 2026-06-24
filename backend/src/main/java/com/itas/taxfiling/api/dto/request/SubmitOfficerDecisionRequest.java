package com.itas.taxfiling.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitOfficerDecisionRequest(
    @NotNull String decision,
    @NotBlank String officerActorId,
    String narrative,
    String externalCaseId
) {}
