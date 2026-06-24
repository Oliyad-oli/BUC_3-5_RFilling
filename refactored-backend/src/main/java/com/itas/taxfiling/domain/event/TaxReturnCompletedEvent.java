package com.itas.taxfiling.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when both risk + rule arms clear and the return reaches COMPLETED
 * (BUC-FIL-023). Triggers FilingCertificate generation (BUC-FIL-040).
 */
public record TaxReturnCompletedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID taxReturnId
) implements DomainEvent {}
