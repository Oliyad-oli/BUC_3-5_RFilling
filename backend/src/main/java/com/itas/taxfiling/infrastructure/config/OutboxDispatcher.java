package com.itas.taxfiling.infrastructure.config;

import com.itas.taxfiling.application.port.OutboxRepositoryPort;
import com.itas.taxfiling.domain.model.OutboxEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox Dispatcher
 * 
 * Scheduled job that processes pending outbox entries
 * and dispatches events to the message broker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final OutboxRepositoryPort outboxRepository;

    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    public void dispatchPendingEvents() {
        List<OutboxEntry> pending = outboxRepository.findPendingEntries(50);
        if (pending.isEmpty()) return;

        log.debug("Dispatching {} pending outbox entries", pending.size());
        for (OutboxEntry entry : pending) {
            try {
                // In production: publish to Kafka/RabbitMQ
                log.info("[OUTBOX] Dispatched event: type={}, aggregate={}",
                        entry.getEventType(), entry.getAggregateId());
                entry.markSent();
                outboxRepository.save(entry);
            } catch (Exception ex) {
                log.error("[OUTBOX] Failed to dispatch: {}", entry.getId(), ex);
                entry.markFailed(ex.getMessage());
                outboxRepository.save(entry);
            }
        }
    }
}
