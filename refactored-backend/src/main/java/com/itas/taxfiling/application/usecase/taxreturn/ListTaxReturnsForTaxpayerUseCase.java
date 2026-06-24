package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListTaxReturnsForTaxpayerUseCase {

    private final TaxReturnRepositoryPort taxReturns;

    @Transactional(readOnly = true)
    public List<TaxReturn> execute(String tin) {
        return taxReturns.findByTin(tin);
    }
}
