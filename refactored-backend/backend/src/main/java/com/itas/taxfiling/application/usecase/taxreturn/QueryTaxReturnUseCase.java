package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryTaxReturnUseCase {

    private final TaxReturnRepositoryPort taxReturns;

    @Transactional(readOnly = true)
    public TaxReturn execute(UUID id) {
        return taxReturns.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + id));
    }
}
