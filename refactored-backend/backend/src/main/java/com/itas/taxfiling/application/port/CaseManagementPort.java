package com.itas.taxfiling.application.port;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Case-management integration. Two distinct paths:
 *   - {@link #openFraudCase} — called after an officer CONFIRMS fraud at
 *     BUC-FIL-051 (officer-decision follow-up).
 *   - {@link #openCaseFromError} — Rule 13 "open a support case" path,
 *     surfaced to the user / officer when an engine call fails (Rule 2 → 503).
 *
 * The dropdown for issue categories comes from {@link #listCategoriesFor}; the
 * catalog is owned by case-management-service and cached locally.
 */
public interface CaseManagementPort {

    UUID openFraudCase(UUID taxReturnId, String tin, String narrative, String officerActorId);

    CaseHandle openCaseFromError(OpenCaseFromErrorRequest request);

    List<IssueCategory> listCategoriesFor(SourceContext context);

    record OpenCaseFromErrorRequest(
        String sourceService,
        String sourceBuc,
        String errorCode,
        UUID errorCorrelationId,
        @Nullable UUID reporterPartyId,
        @Nullable PreAuthIdentity reporterIdentity,
        String issueCategoryCode,
        String description,
        @Nullable Map<String, Object> contextSnapshot,
        Instant occurredAt,
        String channel
    ) {}

    record PreAuthIdentity(String fullName, String nid, String phone, @Nullable String email) {}

    record CaseHandle(UUID caseId, String caseReferenceNumber, String trackingUrl) {}

    record SourceContext(String sourceService, String sourceBuc) {}

    record IssueCategory(String code, String label, boolean active) {}
}
