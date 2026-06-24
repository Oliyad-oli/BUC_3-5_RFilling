package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.Money;

import java.time.Instant;
import java.util.UUID;

/** Emitted when the taxpayer accepts a calculation result (BUC-FIL-013). */
public record CalculationAcceptedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    int iterationNumber,
    Money netTax
) implements DomainEvent {}
