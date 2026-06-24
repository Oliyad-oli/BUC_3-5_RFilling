package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.CertificateReference;

import java.time.Instant;
import java.util.UUID;

/** Emitted when a FilingCertificate is generated and stored in dms (BUC-FIL-040). */
public record FilingCertificateIssuedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    CertificateReference certificate
) implements DomainEvent {}
