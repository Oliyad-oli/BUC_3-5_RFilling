package com.itas.taxfiling.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted to fan out parallel risk + rule evaluation after ledger posting
 * (Rule 7, BUC-FIL-020/021).
 */
public record PostLedgerValidationStartedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId
) implements DomainEvent {}
