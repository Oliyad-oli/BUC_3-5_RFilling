package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "officer_review_items")
@Getter
@Setter
public class OfficerReviewItemJpaEntity {
    @Id
    private String id;
    
    private String returnId;
    private String kind; // OfficerReviewItemKind enum
    private String priority;
    private String status;
    private String assignedOfficer;
    
    private String decision; // OfficerReviewDecision enum
    private String decisionNotes;
    
    @Column(columnDefinition = "TEXT")
    private String evidencePayload;
    
    private Instant createdAt;
    private Instant assignedAt;
    private Instant decidedAt;
}
