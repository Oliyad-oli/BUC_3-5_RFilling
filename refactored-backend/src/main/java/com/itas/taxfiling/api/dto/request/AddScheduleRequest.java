package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import jakarta.validation.constraints.NotNull;

public record AddScheduleRequest(@NotNull ScheduleKind kind, String label) {}
