package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.OfficerReviewItem;
import java.util.List;
import java.util.Optional;

/**
 * Officer Review Item Repository Port
 * 
 * Port interface for officer review item persistence operations
 */
public interface OfficerReviewItemRepositoryPort {
    
    OfficerReviewItem save(OfficerReviewItem item);
    
    Optional<OfficerReviewItem> findById(String id);
    
    List<OfficerReviewItem> findByStatus(String status);
    
    List<OfficerReviewItem> findByAssignedOfficer(String officerId);
    
    Optional<OfficerReviewItem> findByReturnId(String returnId);
}
