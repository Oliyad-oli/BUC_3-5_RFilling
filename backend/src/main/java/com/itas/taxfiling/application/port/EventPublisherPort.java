package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.event.DomainEvent;

/**
 * Event Publisher Port
 * 
 * Port interface for publishing domain events
 */
public interface EventPublisherPort {
    
    /**
     * Publish a domain event
     */
    void publish(DomainEvent event);
    
    /**
     * Publish multiple domain events
     */
    void publishAll(Iterable<DomainEvent> events);
}
