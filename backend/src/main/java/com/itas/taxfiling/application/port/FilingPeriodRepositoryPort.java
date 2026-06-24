package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import java.util.List;
import java.util.Optional;

/**
 * Filing Period Repository Port
 * 
 * Port interface for filing period persistence operations
 */
public interface FilingPeriodRepositoryPort {
    
    FilingPeriod save(FilingPeriod period);
    
    Optional<FilingPeriod> findById(String id);
    
    List<FilingPeriod> findByTin(String tin);
    
    List<FilingPeriod> findByTinAndStatus(String tin, FilingPeriodStatus status);
    
    List<FilingPeriod> findByTinAndTaxType(String tin, TaxTypeCode taxType);
}
