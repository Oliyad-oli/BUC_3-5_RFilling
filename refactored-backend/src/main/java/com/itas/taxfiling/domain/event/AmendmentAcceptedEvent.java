package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.AmendmentDelta;

import java.time.Instant;
import java.util.UUID;

/** Emitted when the amendment delta is accepted and queued for posting (BUC-FIL-032). */
public record AmendmentAcceptedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    AmendmentDelta delta
) implements DomainEvent {}
