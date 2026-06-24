package com.itas.taxfiling.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** JPA entity for stored idempotency-key responses (TTL-driven cleanup). */
@Entity
@Table(name = "idempotency_keys",
    indexes = {
        @Index(name = "ix_idem_key_endpoint", columnList = "key_value, endpoint", unique = true),
        @Index(name = "ix_idem_expires_at", columnList = "expires_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 256)
    private String id;

    @Column(name = "key_value", nullable = false, updatable = false, length = 128)
    private String keyValue;

    @Column(name = "endpoint", nullable = false, updatable = false, length = 256)
    private String endpoint;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 128)
    private String requestHash;

    @Column(name = "status_code", nullable = false, updatable = false)
    private int statusCode;

    @Column(name = "response_body", nullable = false, updatable = false, columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
}
