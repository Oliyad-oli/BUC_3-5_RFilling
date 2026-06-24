package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;

import java.time.Instant;
import java.util.UUID;

/** Emitted when a LineItem is added to a Schedule (BUC-FIL-004/005/006). */
public record LineItemAddedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    UUID scheduleId,
    UUID lineItemId,
    UUID entryTypeId,
    Money amount,
    LineItemSource source
) implements DomainEvent {}
