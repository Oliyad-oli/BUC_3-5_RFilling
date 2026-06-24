package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.Instant;
import java.util.UUID;

/** Emitted when an admin registers a new LineItemEntryType (BUC-FIL-CONFIG-01). */
public record LineItemEntryTypeRegisteredEvent(
    UUID eventId,
    Instant occurredAt,
    UUID entryTypeId,
    String code,
    TaxTypeCode taxType,
    int version,
    String adminActorId
) implements DomainEvent {}
