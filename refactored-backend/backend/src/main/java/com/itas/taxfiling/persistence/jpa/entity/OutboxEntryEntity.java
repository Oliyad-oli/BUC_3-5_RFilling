package com.itas.taxfiling.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** JPA entity for outbox rows. Polled by the dispatcher. */
@Entity
@Table(name = "outbox_entries",
    indexes = {
        @Index(name = "ix_outbox_status_next_attempt", columnList = "status, next_attempt_at"),
        @Index(name = "ix_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
    })
@Getter
@Setter
@NoArgsConstructor
public class OutboxEntryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(nullable = false, updatable = false, length = 64)
    private String topic;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.OutboxStatus status;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private com.itas.taxfiling.domain.valueobject.Priority priority;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 4096)
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Version
    private Long version;
}
