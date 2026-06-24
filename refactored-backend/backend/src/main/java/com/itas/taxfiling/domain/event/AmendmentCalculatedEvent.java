package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.CalculationOutcome;

import java.time.Instant;
import java.util.UUID;

/** Emitted when the amendment recalculation completes (BUC-FIL-031). */
public record AmendmentCalculatedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    int iterationNumber,
    CalculationOutcome outcome
) implements DomainEvent {}
