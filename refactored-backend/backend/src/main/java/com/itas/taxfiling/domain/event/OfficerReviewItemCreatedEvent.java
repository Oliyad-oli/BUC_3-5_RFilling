package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind;
import com.itas.taxfiling.domain.valueobject.Priority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Emitted when an OfficerReviewItem is added to the in-house queue (BUC-FIL-050). */
public record OfficerReviewItemCreatedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID reviewItemId,
    UUID taxReturnId,
    OfficerReviewItemKind kind,
    Priority priority,
    String riskJustification,
    List<String> riskIndicators
) implements DomainEvent {}
