package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.LineItemEntryTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LineItemEntryTypeJpaRepository extends JpaRepository<LineItemEntryTypeEntity, UUID> {

    Optional<LineItemEntryTypeEntity> findByCodeAndVersion(String code, int version);

    List<LineItemEntryTypeEntity> findByTaxTypeAndScheduleKindAndStatus(
        String taxType,
        com.itas.taxfiling.domain.valueobject.ScheduleKind scheduleKind,
        com.itas.taxfiling.domain.valueobject.EntryTypeStatus status);

    @Query("select coalesce(max(t.version), 0) + 1 from LineItemEntryTypeEntity t where t.code = :code")
    int nextVersionForCode(String code);
}
