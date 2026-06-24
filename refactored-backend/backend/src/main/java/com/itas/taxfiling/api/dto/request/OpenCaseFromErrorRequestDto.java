package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.application.port.CaseManagementPort.OpenCaseFromErrorRequest;
import com.itas.taxfiling.application.port.CaseManagementPort.PreAuthIdentity;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OpenCaseFromErrorRequestDto(
    @NotBlank String sourceBuc,
    @NotBlank String errorCode,
    UUID errorCorrelationId,
    UUID reporterPartyId,
    PreAuthIdentityDto reporterIdentity,
    @NotBlank String issueCategoryCode,
    String description,
    Map<String, Object> contextSnapshot,
    Instant occurredAt,
    String channel
) {
    public OpenCaseFromErrorRequest toDomain() {
        return new OpenCaseFromErrorRequest(
            "filing",
            sourceBuc,
            errorCode,
            errorCorrelationId != null ? errorCorrelationId : UUID.randomUUID(),
            reporterPartyId,
            reporterIdentity == null ? null : reporterIdentity.toDomain(),
            issueCategoryCode,
            description,
            contextSnapshot,
            occurredAt != null ? occurredAt : Instant.now(),
            channel != null ? channel : "PORTAL");
    }

    public record PreAuthIdentityDto(String fullName, String nid, String phone, String email) {
        public PreAuthIdentity toDomain() {
            return new PreAuthIdentity(fullName, nid, phone, email);
        }
    }
}
