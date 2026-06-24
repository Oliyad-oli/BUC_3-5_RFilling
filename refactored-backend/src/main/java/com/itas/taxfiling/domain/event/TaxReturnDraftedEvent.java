package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.domain.valueobject.TaxpayerReference;

import java.time.Instant;
import java.util.UUID;

/** Emitted when a TaxReturn is created in DRAFT (BUC-FIL-001/002). */
public record TaxReturnDraftedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    TaxpayerReference taxpayer,
    TaxTypeCode taxType,
    Period period,
    FilingMethod method,
    RulePackageVersion rulePackage
) implements DomainEvent {}
