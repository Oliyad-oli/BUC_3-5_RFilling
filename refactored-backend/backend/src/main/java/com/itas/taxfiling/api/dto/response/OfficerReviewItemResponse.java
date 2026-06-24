package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.OfficerReviewItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfficerReviewItemResponse(
    UUID id,
    UUID taxReturnId,
    String kind,
    String priority,
    String riskJustification,
    List<String> riskIndicators,
    String assignedOfficerActorId,
    String decision,
    String decisionNarrative,
    Instant decidedAt,
    Instant createdAt
) {
    public static OfficerReviewItemResponse from(OfficerReviewItem o) {
        return new OfficerReviewItemResponse(
            o.getId(), o.getTaxReturnId(), o.getKind().name(), o.getPriority().name(),
            o.getRiskJustification(), o.getRiskIndicators(),
            o.getAssignedOfficerActorId().orElse(null),
            o.getDecision().map(Enum::name).orElse(null),
            o.getDecisionNarrative(), o.getDecidedAt(), o.getCreatedAt());
    }
}
