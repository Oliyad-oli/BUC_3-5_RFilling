package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OfficerDecisionRequest(
    @NotNull OfficerReviewDecision decision,
    @NotBlank String officerActorId,
    String narrative
) {}
