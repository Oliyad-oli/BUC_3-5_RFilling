package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Add Line Item Use Case
 * 
 * Adds a line item to a schedule in a tax return
 */
@Service
@RequiredArgsConstructor
public class AddLineItemUseCase {
    
    private final TaxReturnRepositoryPort taxReturnRepository;
    
    @Transactional
    public TaxReturn execute(Command command) {
        // Load return
        TaxReturn taxReturn = taxReturnRepository.findById(command.returnId())
            .orElseThrow(() -> new ResourceNotFoundException("TaxReturn", command.returnId()));
        
        // Verify can add line items
        if (!taxReturn.canAddSchedules()) {
            throw new IllegalStateException("Cannot add line items in status: " + taxReturn.getStatus());
        }
        
        // Find or create schedule
        Schedule schedule = taxReturn.getSchedules().stream()
            .filter(s -> s.getCode().equals(command.scheduleCode()))
            .findFirst()
            .orElseGet(() -> {
                Schedule newSchedule = Schedule.create(command.scheduleCode(), command.scheduleLabel());
                taxReturn.addSchedule(newSchedule);
                return newSchedule;
            });
        
        // Create line item
        LineItem lineItem = LineItem.fromUserInput(
            command.lineCode(),
            command.description(),
            command.amount()
        );
        
        // Add to schedule
        schedule.addLineItem(lineItem);
        
        // Save
        return taxReturnRepository.save(taxReturn);
    }
    
    public record Command(
        String returnId,
        String scheduleCode,
        String scheduleLabel,
        String lineCode,
        String description,
        Money amount
    ) {}
}
