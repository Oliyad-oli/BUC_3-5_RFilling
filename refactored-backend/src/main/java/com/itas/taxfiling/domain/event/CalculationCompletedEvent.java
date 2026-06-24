package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.CalculationOutcome;

import java.time.Instant;
import java.util.UUID;

/** Emitted when a rule-engine calculation iteration finishes (BUC-FIL-011/012). */
public record CalculationCompletedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    int iterationNumber,
    CalculationOutcome outcome
) implements DomainEvent {}
