package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tax_returns")
@Getter
@Setter
public class TaxReturnJpaEntity {
    @Id
    private String id;
    private String tin;
    private String taxType;
    
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    
    private String status;
    private String currentIterationId;
    
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    
    @OneToMany(mappedBy = "taxReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleJpaEntity> schedules = new ArrayList<>();
    
    @OneToMany(mappedBy = "taxReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalculationIterationJpaEntity> iterations = new ArrayList<>();
    
    @OneToMany(mappedBy = "taxReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AmendmentJpaEntity> amendments = new ArrayList<>();
}
