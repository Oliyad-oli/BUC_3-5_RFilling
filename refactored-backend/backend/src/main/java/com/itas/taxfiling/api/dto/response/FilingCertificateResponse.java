package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.FilingCertificate;

import java.time.Instant;
import java.util.UUID;

public record FilingCertificateResponse(
    UUID id,
    UUID taxReturnId,
    UUID dmsDocumentId,
    String certificateNumber,
    Instant issuedAt
) {
    public static FilingCertificateResponse from(FilingCertificate c) {
        return new FilingCertificateResponse(
            c.getId(), c.getTaxReturnId(),
            c.getReference().dmsDocumentId(),
            c.getReference().certificateNumber(),
            c.getIssuedAt());
    }
}
