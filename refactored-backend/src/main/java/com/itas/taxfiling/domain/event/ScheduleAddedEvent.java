package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.ScheduleKind;

import java.time.Instant;
import java.util.UUID;

/** Emitted when a Schedule is attached to a TaxReturn (BUC-FIL-003). */
public record ScheduleAddedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    UUID scheduleId,
    ScheduleKind kind
) implements DomainEvent {}
