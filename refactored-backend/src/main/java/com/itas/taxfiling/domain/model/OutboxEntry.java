package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.aggregate.AggregateRoot;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.valueobject.OutboxStatus;
import com.itas.taxfiling.domain.valueobject.Priority;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Outbox row written in the same transaction as the domain change so the
 * external dispatch (ledger post, notification, workflow trigger, etc.) is
 * eventually consistent and at-least-once.
 *
 * Filing-service uses the outbox primarily for ledger-engine posts (initial
 * principal post + amendment delta) and for fan-out events that other services
 * subscribe to.
 */
public class OutboxEntry extends AggregateRoot {

    private final UUID id;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String topic;
    private final String payload;
    private OutboxStatus status;
    private final Priority priority;
    private int attempts;
    private String lastError;
    private Instant nextAttemptAt;
    private final Instant createdAt;
    private Instant sentAt;
    private Long version;

    private OutboxEntry(UUID id, String aggregateType, UUID aggregateId, String topic,
                        String payload, Priority priority, Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.payload = payload;
        this.priority = priority;
        this.createdAt = createdAt;
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = createdAt;
    }

    public static OutboxEntry pending(String aggregateType, UUID aggregateId, String topic,
                                      String payload, Priority priority) {
        Objects.requireNonNull(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(topic, "topic");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(priority, "priority");
        return new OutboxEntry(UUID.randomUUID(), aggregateType, aggregateId, topic, payload,
            priority, Instant.now());
    }

    public void markSent() {
        if (status != OutboxStatus.PENDING) {
            throw new DomainException("only PENDING entries can be sent (was " + status + ")");
        }
        status = OutboxStatus.SENT;
        sentAt = Instant.now();
    }

    public void markFailed(String error, Instant nextAttempt) {
        attempts++;
        lastError = error;
        nextAttemptAt = nextAttempt;
    }

    public void markPermanentlyFailed(String error) {
        status = OutboxStatus.FAILED;
        lastError = error;
        attempts++;
    }

    @Override public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public Long getVersion() { return version; }

    public static OutboxEntry rehydrate(UUID id, String aggregateType, UUID aggregateId, String topic,
                                        String payload, OutboxStatus status, Priority priority,
                                        int attempts, String lastError, Instant nextAttemptAt,
                                        Instant createdAt, Instant sentAt, Long version) {
        OutboxEntry e = new OutboxEntry(id, aggregateType, aggregateId, topic, payload, priority, createdAt);
        e.status = status;
        e.attempts = attempts;
        e.lastError = lastError;
        e.nextAttemptAt = nextAttemptAt;
        e.sentAt = sentAt;
        e.version = version;
        e.pullEvents();
        return e;
    }
}
