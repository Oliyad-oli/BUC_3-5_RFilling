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
import java.util.UUID;

/**
 * Local read-model projected from registration-service events (Rule 10). Filing
 * never calls registration-service or party-service synchronously — every field
 * here is fed by an event consumer.
 */
@Entity
@Table(name = "taxpayer_projection",
    indexes = {
        @Index(name = "ix_tp_tin", columnList = "tin", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
public class TaxpayerProjectionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String tin;

    @Column(name = "party_id", nullable = false, length = 64)
    private String partyId;

    @Column(name = "legal_name", nullable = false, length = 256)
    private String legalName;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
