package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.RuleOutcome;

import java.time.Instant;
import java.util.UUID;

/** Emitted when post-ledger rule-engine returns its evaluation (BUC-FIL-021). */
public record RuleOutcomeReceivedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    RuleOutcome outcome
) implements DomainEvent {}
