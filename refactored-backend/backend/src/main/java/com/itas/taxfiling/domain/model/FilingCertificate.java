package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.aggregate.AggregateRoot;
import com.itas.taxfiling.domain.event.FilingCertificateIssuedEvent;
import com.itas.taxfiling.domain.valueobject.CertificateReference;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Reference to a filing certificate stored in dms (BUC-FIL-040). Filing service
 * holds only the dms pointer + audit metadata; the binary lives in dms.
 */
public class FilingCertificate extends AggregateRoot {

    private final UUID id;
    private final UUID taxReturnId;
    private final CertificateReference reference;
    private final Instant issuedAt;
    private Long version;

    private FilingCertificate(UUID id, UUID taxReturnId, CertificateReference reference, Instant issuedAt) {
        this.id = id;
        this.taxReturnId = taxReturnId;
        this.reference = reference;
        this.issuedAt = issuedAt;
    }

    public static FilingCertificate issue(UUID taxReturnId, CertificateReference reference) {
        Objects.requireNonNull(taxReturnId, "taxReturnId");
        Objects.requireNonNull(reference, "reference");
        FilingCertificate fc = new FilingCertificate(
            UUID.randomUUID(), taxReturnId, reference, reference.issuedAt());
        fc.registerEvent(new FilingCertificateIssuedEvent(
            UUID.randomUUID(), Instant.now(), taxReturnId, reference));
        return fc;
    }

    @Override public UUID getId() { return id; }
    public UUID getTaxReturnId() { return taxReturnId; }
    public CertificateReference getReference() { return reference; }
    public Instant getIssuedAt() { return issuedAt; }
    public Long getVersion() { return version; }

    public static FilingCertificate rehydrate(UUID id, UUID taxReturnId, CertificateReference reference,
                                              Instant issuedAt, Long version) {
        FilingCertificate fc = new FilingCertificate(id, taxReturnId, reference, issuedAt);
        fc.version = version;
        fc.pullEvents();
        return fc;
    }
}
