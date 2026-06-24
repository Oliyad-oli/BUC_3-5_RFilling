package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;

import java.time.Instant;
import java.util.UUID;

/** Emitted after the outbox dispatcher confirms ledger-engine accepted the post. */
public record PostedToLedgerEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    LedgerEntryReference ledgerEntry
) implements DomainEvent {}
