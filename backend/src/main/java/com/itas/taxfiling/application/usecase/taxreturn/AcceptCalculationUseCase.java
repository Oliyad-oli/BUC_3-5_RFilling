package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcceptCalculationUseCase {
    private final TaxReturnRepositoryPort taxReturnRepository;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public void execute(Command command) {
        TaxReturn taxReturn = taxReturnRepository.findById(command.returnId())
            .orElseThrow(() -> new ResourceNotFoundException("TaxReturn", command.returnId()));
            
        taxReturn.acceptCalculation(command.iterationId());
        
        // Note: Actual ledger posting might happen via an event handler or another port
        taxReturn.markPostedToLedger();
        
        TaxReturn saved = taxReturnRepository.save(taxReturn);
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
    }
    
    public record Command(String returnId, String iterationId) {}
}
