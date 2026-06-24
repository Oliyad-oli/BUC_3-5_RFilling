package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.TaxpayerObligationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxpayerObligationJpaRepository extends JpaRepository<TaxpayerObligationEntity, UUID> {
    Optional<TaxpayerObligationEntity> findByTinAndTaxTypeCode(String tin, String taxTypeCode);
    List<TaxpayerObligationEntity> findByTin(String tin);
}
