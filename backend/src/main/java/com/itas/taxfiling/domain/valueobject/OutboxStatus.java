package com.itas.taxfiling.domain.valueobject;

/**
 * Outbox Status
 * 
 * Represents the status of an outbox entry for reliable event publishing
 */
public enum OutboxStatus {
    /**
     * Event is pending publication
     */
    PENDING,
    
    /**
     * Event has been successfully sent
     */
    SENT,
    
    /**
     * Event publication failed
     */
    FAILED;
    
    public boolean isPending() {
        return this == PENDING;
    }
    
    public boolean isSent() {
        return this == SENT;
    }
    
    public boolean hasFailed() {
        return this == FAILED;
    }
}
