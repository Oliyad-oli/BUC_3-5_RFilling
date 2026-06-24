package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "amendments")
@Getter
@Setter
public class AmendmentJpaEntity {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_return_id")
    private TaxReturnJpaEntity taxReturn;
    
    private String reason;
    private Instant requestedAt;
    private Instant acceptedAt;
}
