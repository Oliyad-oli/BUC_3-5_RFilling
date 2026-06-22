package com.itas.taxfiling.domain.event;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

/**
 * Base Domain Event
 * 
 * All domain events extend this class
 */
@Getter
public abstract class DomainEvent {
    private final String eventId;
    private final Instant occurredAt;
    
    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }
    
    public abstract String getAggregateId();
    
    public abstract String getEventType();
}
