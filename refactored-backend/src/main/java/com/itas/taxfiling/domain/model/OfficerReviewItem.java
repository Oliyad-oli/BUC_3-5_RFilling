package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.aggregate.AggregateRoot;
import com.itas.taxfiling.domain.event.OfficerReviewDecidedEvent;
import com.itas.taxfiling.domain.event.OfficerReviewItemCreatedEvent;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind;
import com.itas.taxfiling.domain.valueobject.Priority;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * In-house officer review queue item (Rule 1, BUC-FIL-050/051). Phase 1 only
 * carries kind = FRAUD_FLAGGED, populated from risk-engine HIGH outcomes at
 * BUC-FIL-022. case-management is NOT involved here — only after CONFIRM_FRAUD
 * (Rule 13 Flow B).
 */
public class OfficerReviewItem extends AggregateRoot {

    private final UUID id;
    private final UUID taxReturnId;
    private final OfficerReviewItemKind kind;
    private Priority priority;
    private final String riskJustification;
    private final List<String> riskIndicators;
    private String assignedOfficerActorId;
    private OfficerReviewDecision decision;
    private String decisionNarrative;
    private Instant decidedAt;
    private final Instant createdAt;
    private Long version;

    private OfficerReviewItem(UUID id, UUID taxReturnId, OfficerReviewItemKind kind,
                              Priority priority, String riskJustification,
                              List<String> riskIndicators, Instant createdAt) {
        this.id = id;
        this.taxReturnId = taxReturnId;
        this.kind = kind;
        this.priority = priority;
        this.riskJustification = riskJustification;
        this.riskIndicators = riskIndicators == null ? List.of() : List.copyOf(riskIndicators);
        this.createdAt = createdAt;
    }

    public static OfficerReviewItem queue(UUID taxReturnId, OfficerReviewItemKind kind,
                                          Priority priority, String riskJustification,
                                          List<String> riskIndicators) {
        Objects.requireNonNull(taxReturnId, "taxReturnId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(riskJustification, "riskJustification");

        OfficerReviewItem item = new OfficerReviewItem(
            UUID.randomUUID(), taxReturnId, kind, priority,
            riskJustification, riskIndicators, Instant.now());
        item.registerEvent(new OfficerReviewItemCreatedEvent(
            UUID.randomUUID(), Instant.now(), item.id, taxReturnId, kind, priority,
            riskJustification, item.riskIndicators));
        return item;
    }

    public void assign(String officerActorId) {
        if (decision != null) throw new DomainException("review already decided");
        this.assignedOfficerActorId = Objects.requireNonNull(officerActorId, "officerActorId");
    }

    public void decide(OfficerReviewDecision decision, String officerActorId, String narrative) {
        if (this.decision != null) throw new DomainException("review already decided");
        this.decision = Objects.requireNonNull(decision, "decision");
        this.assignedOfficerActorId = Objects.requireNonNull(officerActorId, "officerActorId");
        this.decisionNarrative = narrative;
        this.decidedAt = Instant.now();
        registerEvent(new OfficerReviewDecidedEvent(
            UUID.randomUUID(), Instant.now(), id, taxReturnId, decision, officerActorId, narrative));
    }

    @Override public UUID getId() { return id; }
    public UUID getTaxReturnId() { return taxReturnId; }
    public OfficerReviewItemKind getKind() { return kind; }
    public Priority getPriority() { return priority; }
    public String getRiskJustification() { return riskJustification; }
    public List<String> getRiskIndicators() { return riskIndicators; }
    public Optional<String> getAssignedOfficerActorId() { return Optional.ofNullable(assignedOfficerActorId); }
    public Optional<OfficerReviewDecision> getDecision() { return Optional.ofNullable(decision); }
    public String getDecisionNarrative() { return decisionNarrative; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }

    public static OfficerReviewItem rehydrate(UUID id, UUID taxReturnId, OfficerReviewItemKind kind,
                                              Priority priority, String riskJustification,
                                              List<String> riskIndicators,
                                              String assignedOfficer,
                                              OfficerReviewDecision decision, String narrative,
                                              Instant decidedAt, Instant createdAt, Long version) {
        OfficerReviewItem item = new OfficerReviewItem(
            id, taxReturnId, kind, priority, riskJustification, riskIndicators, createdAt);
        item.assignedOfficerActorId = assignedOfficer;
        item.decision = decision;
        item.decisionNarrative = narrative;
        item.decidedAt = decidedAt;
        item.version = version;
        item.pullEvents();
        return item;
    }
}
