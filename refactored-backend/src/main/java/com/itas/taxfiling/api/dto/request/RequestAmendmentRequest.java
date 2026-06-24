package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.AmendmentReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestAmendmentRequest(
    @NotNull AmendmentReason reason,
    @NotBlank String requestedByActorId
) {}
