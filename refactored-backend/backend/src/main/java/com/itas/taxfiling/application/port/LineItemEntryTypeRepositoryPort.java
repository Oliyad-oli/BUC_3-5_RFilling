package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the admin-configurable LineItemEntryType catalog (Rule 11).
 */
public interface LineItemEntryTypeRepositoryPort {

    LineItemEntryType save(LineItemEntryType type);

    Optional<LineItemEntryType> findById(UUID id);

    Optional<LineItemEntryType> findByCodeAndVersion(String code, int version);

    List<LineItemEntryType> findByTaxTypeAndKindAndStatus(
        TaxTypeCode taxType, ScheduleKind kind, EntryTypeStatus status);

    int nextVersionForCode(String code);
}
