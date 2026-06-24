package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.AmendmentReason;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an amendment is opened on a COMPLETED return — either by the
 * taxpayer (BUC-FIL-030) or by an officer at fraud review (BUC-FIL-051).
 */
public record AmendmentRequestedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    AmendmentReason reason,
    String requestedByActorId
) implements DomainEvent {}
