package com.itas.taxfiling.infrastructure.mock;

import com.itas.taxfiling.application.port.RuleEnginePort;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock Rule Engine Adapter
 * 
 * Simulates tax calculation logic for development/testing.
 * In production, this would call an external rule engine service.
 */
@Slf4j
@Component
public class RuleEngineMockAdapter implements RuleEnginePort {

    @Override
    public CalculationIteration calculate(
            String returnId,
            TaxTypeCode taxType,
            List<Schedule> schedules,
            int iterationNumber
    ) {
        log.info("[MOCK-RULE-ENGINE] Calculating tax for return={}, taxType={}, iteration={}",
                returnId, taxType, iterationNumber);

        // Sum all line item amounts from schedules
        BigDecimal totalTaxable = BigDecimal.ZERO;
        for (Schedule schedule : schedules) {
            if (schedule.getLineItems() != null) {
                for (LineItem item : schedule.getLineItems()) {
                    if (item.getAmount() != null) {
                        totalTaxable = totalTaxable.add(item.getAmount().getAmount());
                    }
                }
            }
        }

        // Apply mock tax rate based on tax type
        BigDecimal taxRate = getTaxRate(taxType);
        BigDecimal grossTax = totalTaxable.multiply(taxRate);
        BigDecimal inputCredit = grossTax.multiply(BigDecimal.valueOf(0.05)); // 5% mock credit
        BigDecimal netTax = grossTax.subtract(inputCredit);

        // Build computed line items
        List<LineItem> computedItems = new ArrayList<>();
        computedItems.add(LineItem.computed("GROSS_TAX", "Gross Tax Liability", Money.birr(grossTax)));
        computedItems.add(LineItem.computed("INPUT_CREDIT", "Input Tax Credit", Money.birr(inputCredit)));
        computedItems.add(LineItem.computed("NET_TAX", "Net Tax Payable", Money.birr(netTax)));

        log.info("[MOCK-RULE-ENGINE] Result: gross={}, credit={}, net={}", grossTax, inputCredit, netTax);

        return CalculationIteration.create(
                iterationNumber,
                Money.birr(grossTax),
                Money.birr(inputCredit),
                Money.birr(netTax),
                computedItems
        );
    }

    @Override
    public ValidationResult validatePostLedger(String returnId, String ledgerEntry) {
        log.info("[MOCK-RULE-ENGINE] Post-ledger validation for return={}, ledger={}", returnId, ledgerEntry);
        // Mock: always passes
        return new ValidationResult(true, List.of(), "Mock validation passed");
    }

    private BigDecimal getTaxRate(TaxTypeCode taxType) {
        if (taxType == null) return BigDecimal.valueOf(0.15);
        return switch (taxType) {
            case VAT -> BigDecimal.valueOf(0.15);
            case INCOME_TAX -> BigDecimal.valueOf(0.30);
            case EXCISE -> BigDecimal.valueOf(0.25);
            case WHT -> BigDecimal.valueOf(0.02);
            case PAYE -> BigDecimal.valueOf(0.20);
        };
    }
}
