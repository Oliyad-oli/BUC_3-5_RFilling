package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.RiskOutcome;

import java.time.Instant;
import java.util.UUID;

/** Emitted when risk-engine returns its evaluation (BUC-FIL-020). */
public record RiskOutcomeReceivedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    RiskOutcome outcome
) implements DomainEvent {}
