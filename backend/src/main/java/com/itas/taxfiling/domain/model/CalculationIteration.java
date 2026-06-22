package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Calculation Iteration Entity
 * 
 * Represents a calculation result from the rule engine
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CalculationIteration {
    private String id;
    private int iterationNumber;
    private Money grossTax;
    private Money inputCredit;
    private Money netTax;
    private List<LineItem> computedLineItems;
    private Instant calculatedAt;
    private boolean accepted;
    
    public static CalculationIteration create(
        int iterationNumber,
        Money grossTax,
        Money inputCredit,
        Money netTax,
        List<LineItem> computedLineItems
    ) {
        CalculationIteration iteration = new CalculationIteration();
        iteration.id = UUID.randomUUID().toString();
        iteration.iterationNumber = iterationNumber;
        iteration.grossTax = grossTax;
        iteration.inputCredit = inputCredit;
        iteration.netTax = netTax;
        iteration.computedLineItems = computedLineItems;
        iteration.calculatedAt = Instant.now();
        iteration.accepted = false;
        return iteration;
    }
    
    public void accept() {
        this.accepted = true;
    }
}
