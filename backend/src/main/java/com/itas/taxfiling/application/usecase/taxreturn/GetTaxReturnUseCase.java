package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTaxReturnUseCase {
    private final TaxReturnRepositoryPort taxReturnRepository;

    @Transactional(readOnly = true)
    public TaxReturn execute(String returnId) {
        return taxReturnRepository.findById(returnId)
            .orElseThrow(() -> new ResourceNotFoundException("TaxReturn", returnId));
    }
}
