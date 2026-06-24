package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.SubledgerProjectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubledgerProjectionJpaRepository extends JpaRepository<SubledgerProjectionEntity, UUID> {
    Optional<SubledgerProjectionEntity> findByTinAndTaxType(String tin, String taxType);
}
