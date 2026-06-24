package com.itas.taxfiling.infrastructure.mock;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.domain.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Spring Event Publisher Adapter
 * 
 * Publishes domain events to the application event bus.
 * In production, this could also write to the outbox table.
 */
@Slf4j
@Component
public class SpringEventPublisherAdapter implements EventPublisherPort {

    @Override
    public void publish(DomainEvent event) {
        log.info("[EVENT] Published: {}", event.getClass().getSimpleName());
    }

    @Override
    public void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
