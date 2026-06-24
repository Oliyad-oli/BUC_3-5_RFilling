package com.itas.taxfiling.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RetireEntryTypeRequest(@NotBlank String adminActorId) {}
