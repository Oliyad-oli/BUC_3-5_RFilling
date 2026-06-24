package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.SubledgerProjectionEntity;
import com.itas.taxfiling.persistence.jpa.entity.TaxpayerProjectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaxpayerProjectionJpaRepository extends JpaRepository<TaxpayerProjectionEntity, UUID> {
    Optional<TaxpayerProjectionEntity> findByTin(String tin);
}
