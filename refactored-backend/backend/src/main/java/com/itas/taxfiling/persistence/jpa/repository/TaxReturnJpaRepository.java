package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.TaxReturnEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxReturnJpaRepository extends JpaRepository<TaxReturnEntity, UUID> {
    Optional<TaxReturnEntity> findByTinAndTaxTypeAndPeriodLabel(String tin, String taxType, String periodLabel);

    List<TaxReturnEntity> findByTinOrderByPeriodEndDesc(String tin);

    @Query("""
           select t from TaxReturnEntity t
            where t.tin = :tin and t.taxType = :taxType
              and t.status = com.itas.taxfiling.domain.valueobject.TaxReturnStatus.COMPLETED
              and t.periodEnd < :before
            order by t.periodEnd desc
           """)
    List<TaxReturnEntity> findPriorCompleted(String tin, String taxType, LocalDate before, Pageable pageable);
}
