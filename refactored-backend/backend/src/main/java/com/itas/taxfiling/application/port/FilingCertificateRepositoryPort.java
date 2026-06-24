package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.FilingCertificate;

import java.util.Optional;
import java.util.UUID;

/** Repository for FilingCertificate (dms reference + audit metadata). */
public interface FilingCertificateRepositoryPort {

    FilingCertificate save(FilingCertificate certificate);

    Optional<FilingCertificate> findById(UUID id);

    Optional<FilingCertificate> findByTaxReturnId(UUID taxReturnId);
}
