package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.TaxReturn;

import java.time.Instant;
import java.util.UUID;

public record TaxReturnResponse(
    UUID id,
    String tin,
    String taxType,
    String periodLabel,
    String filingMethod,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaxReturnResponse from(TaxReturn t) {
        return new TaxReturnResponse(
            t.getId(),
            t.getTaxpayer().tin(),
            t.getTaxType().value(),
            t.getPeriod().label(),
            t.getMethod().name(),
            t.getStatus().name(),
            t.getCreatedAt(),
            t.getUpdatedAt());
    }
}
