package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an accepted TaxReturn is queued for ledger posting via outbox
 * (BUC-FIL-013 → BUC-FIL-020). Carries TIN-only ledger payload (Rule 3).
 */
public record PostingToLedgerEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId,
    String tin,
    TaxTypeCode taxType,
    Period period,
    Money amount,
    AccountCategory category
) implements DomainEvent {}
