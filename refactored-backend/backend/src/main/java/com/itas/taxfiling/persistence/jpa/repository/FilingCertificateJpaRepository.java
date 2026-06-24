package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.FilingCertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FilingCertificateJpaRepository extends JpaRepository<FilingCertificateEntity, UUID> {

    Optional<FilingCertificateEntity> findByTaxReturnId(UUID taxReturnId);
}
