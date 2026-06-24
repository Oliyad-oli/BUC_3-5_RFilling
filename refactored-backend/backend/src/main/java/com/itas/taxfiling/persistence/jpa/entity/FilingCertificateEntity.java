package com.itas.taxfiling.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** JPA entity for FilingCertificate (dms reference). */
@Entity
@Table(name = "filing_certificates",
    indexes = {
        @Index(name = "ix_fc_tax_return_id", columnList = "tax_return_id", unique = true),
        @Index(name = "ix_fc_certificate_number", columnList = "certificate_number", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
public class FilingCertificateEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tax_return_id", nullable = false, updatable = false)
    private UUID taxReturnId;

    @Column(name = "dms_document_id", nullable = false, updatable = false)
    private UUID dmsDocumentId;

    @Column(name = "certificate_number", nullable = false, updatable = false, length = 64)
    private String certificateNumber;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Version
    private Long version;
}
