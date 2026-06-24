package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.OfficerReviewItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for the in-house officer review queue (BUC-FIL-050/051). */
public interface OfficerReviewItemRepositoryPort {

    OfficerReviewItem save(OfficerReviewItem item);

    Optional<OfficerReviewItem> findById(UUID id);

    List<OfficerReviewItem> findOpenQueue();

    List<OfficerReviewItem> findByAssignedOfficer(String officerActorId);
}
