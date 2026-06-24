package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.FilingCertificateRepositoryPort;
import com.itas.taxfiling.domain.model.FilingCertificate;
import com.itas.taxfiling.domain.valueobject.CertificateReference;
import com.itas.taxfiling.persistence.jpa.entity.FilingCertificateEntity;
import com.itas.taxfiling.persistence.jpa.repository.FilingCertificateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FilingCertificatePersistenceAdapter implements FilingCertificateRepositoryPort {

    private final FilingCertificateJpaRepository repository;

    @Override
    @Transactional
    public FilingCertificate save(FilingCertificate certificate) {
        FilingCertificateEntity entity = repository.findById(certificate.getId())
            .orElseGet(FilingCertificateEntity::new);
        applyTo(entity, certificate);
        repository.save(entity);
        // Return the input instance — events still attached for the caller to publish.
        return certificate;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilingCertificate> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilingCertificate> findByTaxReturnId(UUID taxReturnId) {
        return repository.findByTaxReturnId(taxReturnId).map(this::toDomain);
    }

    private void applyTo(FilingCertificateEntity e, FilingCertificate c) {
        e.setId(c.getId());
        e.setTaxReturnId(c.getTaxReturnId());
        e.setDmsDocumentId(c.getReference().dmsDocumentId());
        e.setCertificateNumber(c.getReference().certificateNumber());
        e.setIssuedAt(c.getIssuedAt());
    }

    private FilingCertificate toDomain(FilingCertificateEntity e) {
        CertificateReference ref = new CertificateReference(
            e.getDmsDocumentId(), e.getCertificateNumber(), e.getIssuedAt());
        return FilingCertificate.rehydrate(
            e.getId(), e.getTaxReturnId(), ref, e.getIssuedAt(), e.getVersion());
    }
}
