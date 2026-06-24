package com.itas.taxfiling.persistence.jpa.entity;

import com.itas.taxfiling.persistence.converter.JsonbConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** JPA entity for in-house officer review queue items. */
@Entity
@Table(name = "officer_review_items",
    indexes = {
        @Index(name = "ix_ori_status_priority", columnList = "decision, priority"),
        @Index(name = "ix_ori_assigned", columnList = "assigned_officer_actor_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class OfficerReviewItemEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tax_return_id", nullable = false, updatable = false)
    private UUID taxReturnId;

    @Column(nullable = false, updatable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind kind;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.Priority priority;

    @Column(name = "risk_justification", length = 4096, nullable = false, updatable = false)
    private String riskJustification;

    @Column(name = "risk_indicators_json", columnDefinition = "jsonb", updatable = false)
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> riskIndicators;

    @Column(name = "assigned_officer_actor_id", length = 128)
    private String assignedOfficerActorId;

    @Column(length = 32)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.OfficerReviewDecision decision;

    @Column(name = "decision_narrative", length = 2048)
    private String decisionNarrative;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
