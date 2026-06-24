package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.TaxReturn;
import java.util.List;
import java.util.Optional;

/**
 * Tax Return Repository Port
 * 
 * Port interface for tax return persistence operations
 */
public interface TaxReturnRepositoryPort {
    
    TaxReturn save(TaxReturn taxReturn);
    
    Optional<TaxReturn> findById(String id);
    
    List<TaxReturn> findByTin(String tin);
    
    boolean existsById(String id);
    
    void delete(String id);
}
