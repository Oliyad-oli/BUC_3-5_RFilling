package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;
import com.itas.taxfiling.persistence.jpa.entity.FilingPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FilingPeriodJpaRepository extends JpaRepository<FilingPeriodEntity, UUID> {

    Optional<FilingPeriodEntity> findByTaxpayerObligationIdAndPeriodLabel(UUID obligationId, String periodLabel);

    List<FilingPeriodEntity> findByTinAndStatusInOrderByDueDateAsc(String tin, List<FilingPeriodStatus> statuses);

    List<FilingPeriodEntity> findByTinOrderByDueDateAsc(String tin);

    List<FilingPeriodEntity> findByStatusAndCoversFromLessThanEqual(FilingPeriodStatus status, LocalDate today);

    List<FilingPeriodEntity> findByStatusAndCoversToLessThan(FilingPeriodStatus status, LocalDate today);

    List<FilingPeriodEntity> findByStatusAndDueDateLessThan(FilingPeriodStatus status, LocalDate today);

    Optional<FilingPeriodEntity> findByTaxReturnId(UUID taxReturnId);
}
