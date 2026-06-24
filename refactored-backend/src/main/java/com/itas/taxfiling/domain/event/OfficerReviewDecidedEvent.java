package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;

import java.time.Instant;
import java.util.UUID;

/** Emitted when an officer decides on a fraud-flagged return (BUC-FIL-051). */
public record OfficerReviewDecidedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID reviewItemId,
    UUID taxReturnId,
    OfficerReviewDecision decision,
    String officerActorId,
    String narrative
) implements DomainEvent {}
