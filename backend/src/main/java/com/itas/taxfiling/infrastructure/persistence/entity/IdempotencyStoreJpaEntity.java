package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "idempotency_store")
@Getter
@Setter
public class IdempotencyStoreJpaEntity {
    @Id
    private String idempotencyKey;
    
    @Column(columnDefinition = "TEXT")
    private String responsePayload;
    
    private Instant createdAt;
}
