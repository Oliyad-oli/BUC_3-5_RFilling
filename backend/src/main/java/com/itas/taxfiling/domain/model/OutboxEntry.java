package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.OutboxStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Entry Aggregate Root
 * 
 * Implements the Outbox Pattern for reliable event publishing
 */
@Getter
public class OutboxEntry {
    private String id;
    private String aggregateType;  // e.g., "TaxReturn", "OfficerReviewItem"
    private String aggregateId;
    private String eventType;      // e.g., "TaxReturnDraftedEvent"
    private String payload;        // JSON serialized event
    private OutboxStatus status;
    private Instant createdAt;
    private Instant sentAt;
    private int retryCount;
    private String errorMessage;
    
    // Private constructor
    private OutboxEntry() {}
    
    /**
     * Create a new outbox entry
     */
    public static OutboxEntry create(
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload
    ) {
        OutboxEntry entry = new OutboxEntry();
        entry.id = UUID.randomUUID().toString();
        entry.aggregateType = aggregateType;
        entry.aggregateId = aggregateId;
        entry.eventType = eventType;
        entry.payload = payload;
        entry.status = OutboxStatus.PENDING;
        entry.createdAt = Instant.now();
        entry.retryCount = 0;
        
        return entry;
    }
    
    /**
     * Mark as sent
     */
    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
    }
    
    /**
     * Mark as failed
     */
    public void markFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
    
    /**
     * Retry sending
     */
    public void retry() {
        if (status == OutboxStatus.FAILED) {
            this.status = OutboxStatus.PENDING;
            this.errorMessage = null;
        }
    }
    
    public boolean isPending() {
        return status == OutboxStatus.PENDING;
    }
    
    public boolean isSent() {
        return status == OutboxStatus.SENT;
    }
    
    public boolean hasFailed() {
        return status == OutboxStatus.FAILED;
    }
    
    public boolean canRetry() {
        return hasFailed() && retryCount < 5;  // Max 5 retries
    }
}
