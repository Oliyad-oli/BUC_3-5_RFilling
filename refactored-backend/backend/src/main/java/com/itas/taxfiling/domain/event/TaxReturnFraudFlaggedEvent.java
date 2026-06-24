package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.RiskOutcome;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when risk-engine returns HIGH (BUC-FIL-022). Triggers in-house officer
 * review queue (BUC-FIL-050) — case-management is NOT involved at this stage
 * (Rule 1).
 */
public record TaxReturnFraudFlaggedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    RiskOutcome outcome
) implements DomainEvent {}
