package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Emitted when a (TIN × tax type) obligation is registered with filing-service. */
public record TaxpayerObligationCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID obligationId,
    String tin,
    String partyId,
    TaxTypeCode taxType,
    PeriodFrequency frequency,
    LocalDate effectiveFrom
) implements DomainEvent {}
