package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.CalendarPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarPeriodJpaRepository extends JpaRepository<CalendarPeriodEntity, UUID> {

    Optional<CalendarPeriodEntity> findByTaxTypeCodeAndPeriodLabel(String taxTypeCode, String periodLabel);

    @Query("""
        SELECT c FROM CalendarPeriodEntity c
        WHERE c.taxTypeCode = :taxTypeCode
          AND c.startsOn BETWEEN :from AND :to
        ORDER BY c.startsOn
    """)
    List<CalendarPeriodEntity> findByTaxTypeCodeInRange(
        @Param("taxTypeCode") String taxTypeCode,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);
}
