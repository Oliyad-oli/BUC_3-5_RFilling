package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.ValidationMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Emitted when a LineItem fails Level 2 (cross-field) validation (Rule 12). */
public record LineItemFlaggedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    UUID lineItemId,
    List<ValidationMessage> messages
) implements DomainEvent {}
