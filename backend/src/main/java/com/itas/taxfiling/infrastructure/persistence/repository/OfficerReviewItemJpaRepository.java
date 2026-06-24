package com.itas.taxfiling.infrastructure.persistence.repository;

import com.itas.taxfiling.infrastructure.persistence.entity.OfficerReviewItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficerReviewItemJpaRepository extends JpaRepository<OfficerReviewItemJpaEntity, String> {
    List<OfficerReviewItemJpaEntity> findByStatus(String status);
    List<OfficerReviewItemJpaEntity> findByAssignedOfficer(String assignedOfficer);
    Optional<OfficerReviewItemJpaEntity> findByReturnId(String returnId);
}
