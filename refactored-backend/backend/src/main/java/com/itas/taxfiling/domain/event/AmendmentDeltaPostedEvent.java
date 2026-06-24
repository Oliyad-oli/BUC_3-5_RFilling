package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.AmendmentDelta;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted after the amendment delta is posted to PRINCIPAL via outbox
 * (BUC-FIL-033, Rule 8). Penalty + interest re-runs in payment-service.
 */
public record AmendmentDeltaPostedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    AmendmentDelta delta,
    LedgerEntryReference newLedgerEntry
) implements DomainEvent {}
