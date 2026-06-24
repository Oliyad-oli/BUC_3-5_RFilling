package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.FilingPeriodRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draft Tax Return Use Case
 * 
 * Creates a new draft tax return for a filing period
 */
@Service
@RequiredArgsConstructor
public class DraftTaxReturnUseCase {
    
    private final TaxReturnRepositoryPort taxReturnRepository;
    private final FilingPeriodRepositoryPort filingPeriodRepository;
    private final EventPublisherPort eventPublisher;
    
    @Transactional
    public TaxReturn execute(Command command) {
        // Verify filing period exists
        FilingPeriod period = filingPeriodRepository.findById(command.filingPeriodId())
            .orElseThrow(() -> new ResourceNotFoundException("FilingPeriod", command.filingPeriodId()));
        
        // Verify filing period can be filed
        if (!period.getStatus().canFile()) {
            throw new IllegalStateException("Filing period status does not allow filing: " + period.getStatus());
        }
        
        // Create draft return
        TaxReturn taxReturn = TaxReturn.draft(
            command.tin(),
            command.taxType(),
            period.getPeriod(),
            command.createdBy()
        );
        
        // Save
        TaxReturn saved = taxReturnRepository.save(taxReturn);
        
        // Publish events
        eventPublisher.publishAll(saved.getDomainEvents());
        saved.clearDomainEvents();
        
        return saved;
    }
    
    public record Command(
        String tin,
        TaxTypeCode taxType,
        String filingPeriodId,
        String createdBy
    ) {}
}
