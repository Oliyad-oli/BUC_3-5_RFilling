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
 * Local read-model of (TIN × tax type) → 4 subledger UUIDs. Refreshed by
 * registration-service projection consumer.
 */
@Entity
@Table(name = "subledger_projection",
    indexes = {
        @Index(name = "ix_sp_tin_taxtype", columnList = "tin, tax_type", unique = true)
    })
@Getter
@Setter
@NoArgsConstructor
public class SubledgerProjectionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String tin;

    @Column(name = "tax_type", nullable = false, length = 32)
    private String taxType;

    @Column(name = "principal_subledger_id", nullable = false)
    private UUID principalSubledgerId;

    @Column(name = "penalty_subledger_id", nullable = false)
    private UUID penaltySubledgerId;

    @Column(name = "interest_subledger_id", nullable = false)
    private UUID interestSubledgerId;

    @Column(name = "refund_subledger_id", nullable = false)
    private UUID refundSubledgerId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
