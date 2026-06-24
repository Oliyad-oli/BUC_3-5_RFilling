package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculation_iterations")
@Getter
@Setter
public class CalculationIterationJpaEntity {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_return_id")
    private TaxReturnJpaEntity taxReturn;
    
    private int iterationNumber;
    
    private BigDecimal grossTaxAmount;
    private String grossTaxCurrency;
    
    private BigDecimal inputCreditAmount;
    private String inputCreditCurrency;
    
    private BigDecimal netTaxAmount;
    private String netTaxCurrency;
    
    @OneToMany(mappedBy = "iteration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineItemJpaEntity> computedLineItems = new ArrayList<>();
    
    private Instant calculatedAt;
    private boolean accepted;
}
