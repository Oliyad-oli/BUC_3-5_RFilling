package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.OfficerReviewItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfficerReviewItemJpaRepository extends JpaRepository<OfficerReviewItemEntity, UUID> {

    List<OfficerReviewItemEntity> findByDecisionIsNullOrderByPriorityDescCreatedAtAsc();

    List<OfficerReviewItemEntity> findByAssignedOfficerActorId(String officerActorId);
}
