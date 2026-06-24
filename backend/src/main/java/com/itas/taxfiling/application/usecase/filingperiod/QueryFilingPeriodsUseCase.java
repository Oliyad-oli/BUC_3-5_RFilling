package com.itas.taxfiling.application.usecase.filingperiod;

import com.itas.taxfiling.application.port.FilingPeriodRepositoryPort;
import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query Filing Periods Use Case
 * 
 * Retrieves filing periods for a taxpayer, optionally filtered by status
 */
@Service
@RequiredArgsConstructor
public class QueryFilingPeriodsUseCase {
    
    private final FilingPeriodRepositoryPort filingPeriodRepository;
    
    @Transactional(readOnly = true)
    public List<FilingPeriod> execute(String tin, FilingPeriodStatus status) {
        if (status != null) {
            return filingPeriodRepository.findByTinAndStatus(tin, status);
        }
        return filingPeriodRepository.findByTin(tin);
    }
}
