package com.itas.taxfiling.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an officer confirms fraud (BUC-FIL-051 outcome=CONFIRM_FRAUD).
 * case-management subscribes and opens an investigation (Rule 13 Flow B).
 */
public record FraudConfirmedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    String officerActorId,
    String narrative
) implements DomainEvent {}
