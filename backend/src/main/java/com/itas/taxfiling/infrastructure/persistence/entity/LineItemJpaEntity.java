package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "line_items")
@Getter
@Setter
public class LineItemJpaEntity {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ScheduleJpaEntity schedule;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iteration_id")
    private CalculationIterationJpaEntity iteration;
    
    private String lineCode;
    private String description;
    
    private BigDecimal amount;
    private String currency;
    
    private String source;
    private String referenceId;
}
