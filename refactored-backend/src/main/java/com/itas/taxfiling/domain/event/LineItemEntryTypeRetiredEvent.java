package com.itas.taxfiling.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an admin retires a LineItemEntryType version (BUC-FIL-CONFIG-01).
 * Existing line items keep their original definition (immutable-rows pattern).
 */
public record LineItemEntryTypeRetiredEvent(
    UUID eventId,
    Instant occurredAt,
    UUID entryTypeId,
    String adminActorId
) implements DomainEvent {}
