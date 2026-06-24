package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "filing_periods")
@Getter
@Setter
public class FilingPeriodJpaEntity {
    @Id
    private String id;
    private String tin;
    private String taxType; // TaxTypeCode
    
    // Period value object
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    
    private String status; // FilingPeriodStatus
    private LocalDate dueDate;
    private LocalDate filedDate;
    private String returnId;
    
    private Instant createdAt;
    private Instant updatedAt;
}
