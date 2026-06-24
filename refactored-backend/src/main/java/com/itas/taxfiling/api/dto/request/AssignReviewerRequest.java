package com.itas.taxfiling.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AssignReviewerRequest(@NotBlank String officerActorId) {}
