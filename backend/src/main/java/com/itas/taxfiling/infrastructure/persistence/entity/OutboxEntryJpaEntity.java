package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "outbox_entries")
@Getter
@Setter
public class OutboxEntryJpaEntity {
    @Id
    private String id;
    
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    private String status; // PENDING, SENT, FAILED
    private Instant createdAt;
    private Instant sentAt;
    private int retryCount;
    private String errorMessage;
}
