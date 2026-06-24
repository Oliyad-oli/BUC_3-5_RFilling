package com.itas.taxfiling.application.usecase.certificate;

import com.itas.taxfiling.application.port.FilingCertificateRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.FilingCertificate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryFilingCertificateUseCase {

    private final FilingCertificateRepositoryPort certificates;

    @Transactional(readOnly = true)
    public FilingCertificate executeByTaxReturnId(UUID taxReturnId) {
        return certificates.findByTaxReturnId(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "filing certificate not found for tax return: " + taxReturnId));
    }
}
