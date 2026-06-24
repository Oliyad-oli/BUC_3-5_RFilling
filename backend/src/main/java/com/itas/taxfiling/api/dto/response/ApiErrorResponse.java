package com.itas.taxfiling.api.dto.response;

import java.time.Instant;

public record ApiErrorResponse(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp
) {}
