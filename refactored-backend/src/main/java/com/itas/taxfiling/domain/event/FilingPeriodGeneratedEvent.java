package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Emitted when a per-taxpayer filing period is generated from the global calendar. */
public record FilingPeriodGeneratedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID filingPeriodId,
    UUID obligationId,
    String tin,
    String taxTypeCode,
    String periodLabel,
    LocalDate coversFrom,
    LocalDate coversTo,
    LocalDate dueDate,
    boolean isPartial,
    FilingPeriodStatus initialStatus
) implements DomainEvent {}
