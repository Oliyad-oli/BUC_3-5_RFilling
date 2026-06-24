package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FilingPeriodRepositoryPort {

    FilingPeriod save(FilingPeriod period);

    Optional<FilingPeriod> findById(UUID id);

    Optional<FilingPeriod> findByObligationAndLabel(UUID obligationId, String periodLabel);

    Optional<FilingPeriod> findByTaxReturnId(UUID taxReturnId);

    /** All non-FILED periods for a TIN, ordered by due date. */
    List<FilingPeriod> findOutstandingByTin(String tin);

    /** Every period for a TIN, regardless of status. */
    List<FilingPeriod> findAllByTin(String tin);

    /** For the daily status job. Returns periods whose date-derived status differs from current. */
    List<FilingPeriod> findCandidatesForStatusFlip(LocalDate today);
}
