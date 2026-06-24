package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.RulePackageCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RulePackageCacheJpaRepository extends JpaRepository<RulePackageCacheEntity, String> {

    Optional<RulePackageCacheEntity> findByTaxTypeAndEffectiveOn(String taxType, LocalDate effectiveOn);
}
