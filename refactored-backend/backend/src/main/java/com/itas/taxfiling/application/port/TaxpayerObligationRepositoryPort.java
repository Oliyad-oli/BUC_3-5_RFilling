package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.TaxpayerObligation;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxpayerObligationRepositoryPort {

    TaxpayerObligation save(TaxpayerObligation obligation);

    Optional<TaxpayerObligation> findById(UUID id);

    Optional<TaxpayerObligation> findByTinAndTaxType(String tin, TaxTypeCode taxType);

    List<TaxpayerObligation> findByTin(String tin);
}
