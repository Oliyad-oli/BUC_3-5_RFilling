package com.itas.taxfiling.api.dto.response;

import java.time.Instant;

public record OfficerReviewItemResponse(
    String id,
    String returnId,
    String kind,
    String priority,
    String status,
    String assignedOfficer,
    String decision,
    String decisionNotes,
    String evidencePayload,
    Instant createdAt,
    Instant assignedAt,
    Instant decidedAt
) {}
