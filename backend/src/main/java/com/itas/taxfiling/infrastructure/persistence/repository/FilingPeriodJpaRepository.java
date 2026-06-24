package com.itas.taxfiling.infrastructure.persistence.repository;

import com.itas.taxfiling.infrastructure.persistence.entity.FilingPeriodJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilingPeriodJpaRepository extends JpaRepository<FilingPeriodJpaEntity, String> {
    List<FilingPeriodJpaEntity> findByTin(String tin);
    List<FilingPeriodJpaEntity> findByTinAndStatus(String tin, String status);
    List<FilingPeriodJpaEntity> findByTinAndTaxType(String tin, String taxType);
}
