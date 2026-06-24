package com.itas.taxfiling.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Emitted when the taxpayer requests a calculation iteration (BUC-FIL-010). */
public record CalculationRequestedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    int iterationNumber
) implements DomainEvent {}
