package com.itas.taxfiling.domain.valueobject;

import java.time.Instant;
import java.util.Objects;

/**
 * Reference to a certificate stored in DMS. Carries the DMS document ID,
 * a human-readable certificate number, and the issuance timestamp.
 */
public record CertificateReference(String dmsDocumentId, String certificateNumber, Instant issuedAt) {

    public CertificateReference {
        Objects.requireNonNull(dmsDocumentId, "dmsDocumentId");
        Objects.requireNonNull(certificateNumber, "certificateNumber");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
