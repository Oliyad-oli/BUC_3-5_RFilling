package com.itas.taxfiling.application.usecase.filingperiod;

import com.itas.taxfiling.application.port.FilingPeriodRepositoryPort;
import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GenerateFilingPeriodUseCase {
    private final FilingPeriodRepositoryPort filingPeriodRepository;

    @Transactional
    public FilingPeriod execute(Command command) {
        FilingPeriod period = FilingPeriod.generate(
            command.tin(),
            command.taxType(),
            command.period(),
            command.dueDate()
        );
        
        return filingPeriodRepository.save(period);
    }
    
    public record Command(String tin, TaxTypeCode taxType, Period period, LocalDate dueDate) {}
}
