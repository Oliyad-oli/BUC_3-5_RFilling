package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.domain.valueobject.TaxpayerReference;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted by the OpenFilingPeriodJob (BUC-FIL-001) after a TaxReturn is created
 * with pre-populated empty schedules. CarryForwardLineItemsHandler subscribes
 * to this event (BUC-FIL-005).
 */
public record TaxReturnPeriodOpenedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    TaxpayerReference taxpayer,
    TaxTypeCode taxType,
    Period period
) implements DomainEvent {}
