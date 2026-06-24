package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.application.port.WorkflowEnginePort;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind;
import com.itas.taxfiling.domain.valueobject.Priority;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Internal — called by FraudFlaggedHandler when risk-engine returns HIGH at
 * BUC-FIL-022. Creates an in-house review item and starts a workflow so the
 * SLA can be tracked.
 *
 * Phase 1 only handles kind = FRAUD_FLAGGED. The review item stays in
 * filing-service; case-management is only involved if the officer later
 * confirms fraud (Rule 13 Flow B).
 */
@Service
@RequiredArgsConstructor
public class CreateOfficerReviewItemUseCase {

    private final OfficerReviewItemRepositoryPort items;
    private final WorkflowEnginePort workflowEngine;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public OfficerReviewItem execute(UUID taxReturnId, RiskOutcome riskOutcome) {
        Priority priority = mapPriority(riskOutcome);
        OfficerReviewItem item = OfficerReviewItem.queue(
            taxReturnId, OfficerReviewItemKind.FRAUD_FLAGGED, priority,
            riskOutcome.justification(), riskOutcome.indicators());
        OfficerReviewItem saved = items.save(item);
        saved.pullEvents().forEach(eventPublisher::publish);

        workflowEngine.startWorkflow("filing.officer-review", Map.of(
            "reviewItemId", saved.getId().toString(),
            "taxReturnId", taxReturnId.toString(),
            "priority", priority.name()
        ));
        return saved;
    }

    private Priority mapPriority(RiskOutcome r) {
        // Indicators count is the simplest mock heuristic; real tuning lives in
        // risk-engine response. Keeps mapping deterministic for tests.
        int n = r.indicators().size();
        if (n >= 5) return Priority.CRITICAL;
        if (n >= 3) return Priority.HIGH;
        if (n >= 1) return Priority.MEDIUM;
        return Priority.LOW;
    }
}
