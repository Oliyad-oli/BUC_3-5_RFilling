package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import java.util.List;

/**
 * Rule Engine Port
 * 
 * Port interface for rule engine integration (calculation & validation)
 */
public interface RuleEnginePort {
    
    /**
     * Calculate tax liability based on schedules and line items
     */
    CalculationIteration calculate(
        String returnId,
        TaxTypeCode taxType,
        List<Schedule> schedules,
        int iterationNumber
    );
    
    /**
     * Validate return after ledger posting
     */
    ValidationResult validatePostLedger(String returnId, String ledgerEntry);
    
    /**
     * Validation result
     */
    record ValidationResult(
        boolean passed,
        List<String> failures,
        String details
    ) {}
}
