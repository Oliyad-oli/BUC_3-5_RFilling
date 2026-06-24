package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.RuleEnginePort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Request Calculation Use Case
 * 
 * Requests calculation of tax liability from rule engine
 */
@Service
@RequiredArgsConstructor
public class RequestCalculationUseCase {
    
    private final TaxReturnRepositoryPort taxReturnRepository;
    private final RuleEnginePort ruleEngine;
    private final EventPublisherPort eventPublisher;
    
    @Transactional
    public CalculationResult execute(Command command) {
        // Load return
        TaxReturn taxReturn = taxReturnRepository.findById(command.returnId())
            .orElseThrow(() -> new ResourceNotFoundException("TaxReturn", command.returnId()));
        
        // Verify can calculate
        if (!taxReturn.canCalculate()) {
            throw new IllegalStateException("Cannot calculate in status: " + taxReturn.getStatus());
        }
        
        // Update status to CALCULATING
        taxReturn.requestCalculation();
        
        // Calculate
        int iterationNumber = taxReturn.getIterations().size() + 1;
        CalculationIteration iteration = ruleEngine.calculate(
            taxReturn.getId(),
            taxReturn.getTaxType(),
            taxReturn.getSchedules(),
            iterationNumber
        );
        
        // Record iteration
        taxReturn.recordCalculation(iteration);
        
        // Save
        TaxReturn saved = taxReturnRepository.save(taxReturn);
        
        // Publish events
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        
        return new CalculationResult(
            iteration.getId(),
            iteration.getIterationNumber(),
            iteration.getGrossTax(),
            iteration.getInputCredit(),
            iteration.getNetTax(),
            iteration.getComputedLineItems()
        );
    }
    
    public record Command(String returnId) {}
    
    public record CalculationResult(
        String iterationId,
        int iterationNumber,
        com.itas.taxfiling.domain.valueobject.Money grossTax,
        com.itas.taxfiling.domain.valueobject.Money inputCredit,
        com.itas.taxfiling.domain.valueobject.Money netTax,
        java.util.List<com.itas.taxfiling.domain.model.LineItem> computedLineItems
    ) {}
}
