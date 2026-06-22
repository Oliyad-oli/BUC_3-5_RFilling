package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.event.DomainEvent;
import com.itas.taxfiling.domain.exception.InvalidStateTransitionException;
import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Officer Review Item Aggregate Root
 * 
 * Core aggregate for BUC-005 (Return Processing)
 * Represents a tax return that requires officer review
 */
@Getter
public class OfficerReviewItem {
    private String id;
    private String returnId;
    private OfficerReviewItemKind kind;
    private String priority;  // HIGH, MEDIUM, LOW
    private String status;    // OPEN, ASSIGNED, DECIDED, CLOSED
    private String assignedOfficer;
    private OfficerReviewDecision decision;
    private String decisionNotes;
    private String evidencePayload;  // JSON with evidence details
    private Instant createdAt;
    private Instant assignedAt;
    private Instant decidedAt;
    
    // Domain events
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    // Private constructor
    private OfficerReviewItem() {}
    
    /**
     * Create a new review item
     */
    public static OfficerReviewItem create(
        String returnId,
        OfficerReviewItemKind kind,
        String priority,
        String evidencePayload
    ) {
        OfficerReviewItem item = new OfficerReviewItem();
        item.id = UUID.randomUUID().toString();
        item.returnId = returnId;
        item.kind = kind;
        item.priority = priority;
        item.status = "OPEN";
        item.evidencePayload = evidencePayload;
        item.createdAt = Instant.now();
        
        // addEvent(new OfficerReviewItemCreatedEvent(item.id, returnId, kind));
        
        return item;
    }
    
    /**
     * Assign to an officer
     */
    public void assign(String officerId) {
        if (!"OPEN".equals(status) && !"ASSIGNED".equals(status)) {
            throw new InvalidStateTransitionException(status, "assign");
        }
        
        this.assignedOfficer = officerId;
        this.status = "ASSIGNED";
        this.assignedAt = Instant.now();
    }
    
    /**
     * Submit officer decision
     */
    public void decide(OfficerReviewDecision decision, String notes) {
        if (!"ASSIGNED".equals(status)) {
            throw new InvalidStateTransitionException(status, "decide");
        }
        
        this.decision = decision;
        this.decisionNotes = notes;
        this.status = "DECIDED";
        this.decidedAt = Instant.now();
        
        // addEvent(new OfficerReviewDecidedEvent(this.id, this.returnId, decision));
    }
    
    /**
     * Close the review item
     */
    public void close() {
        if (!"DECIDED".equals(status)) {
            throw new InvalidStateTransitionException(status, "close");
        }
        
        this.status = "CLOSED";
    }
    
    // Helper methods
    
    public boolean isOpen() {
        return "OPEN".equals(status);
    }
    
    public boolean isAssigned() {
        return "ASSIGNED".equals(status);
    }
    
    public boolean isDecided() {
        return "DECIDED".equals(status);
    }
    
    public boolean isClosed() {
        return "CLOSED".equals(status);
    }
    
    public boolean canBeAssigned() {
        return isOpen() || isAssigned();
    }
    
    public boolean canBeDecided() {
        return isAssigned();
    }
    
    // Event management
    
    private void addEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
