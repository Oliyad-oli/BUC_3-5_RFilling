package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;

import java.time.Instant;
import java.util.UUID;

/** Emitted when a filing period's status changes (date-driven or by submission). */
public record FilingPeriodStatusChangedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID filingPeriodId,
    String tin,
    String taxTypeCode,
    String periodLabel,
    FilingPeriodStatus from,
    FilingPeriodStatus to
) implements DomainEvent {}
