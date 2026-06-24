package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.event.DomainEvent;

/**
 * Lightweight wrapper around Spring's ApplicationEventPublisher. Use cases
 * publish drained events through this port so the application layer doesn't
 * depend on Spring directly.
 */
public interface EventPublisherPort {
    void publish(DomainEvent event);
}
