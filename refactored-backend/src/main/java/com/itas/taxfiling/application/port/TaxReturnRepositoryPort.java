package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TaxReturn aggregate. Implementations live in persistence/adapter.
 */
public interface TaxReturnRepositoryPort {

    TaxReturn save(TaxReturn taxReturn);

    Optional<TaxReturn> findById(UUID id);

    Optional<TaxReturn> findByTinAndTaxTypeAndPeriod(String tin, TaxTypeCode taxType, Period period);

    List<TaxReturn> findByTin(String tin);

    /** Most recent COMPLETED return for (tin × taxType) with periodEnd strictly before periodStart. */
    Optional<TaxReturn> findPriorCompleted(String tin, TaxTypeCode taxType, java.time.LocalDate periodStart);
}
