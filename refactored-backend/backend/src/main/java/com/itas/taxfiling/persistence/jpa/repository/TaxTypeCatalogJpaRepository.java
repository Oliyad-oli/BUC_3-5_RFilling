package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.TaxTypeCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxTypeCatalogJpaRepository extends JpaRepository<TaxTypeCatalogEntity, String> {

    List<TaxTypeCatalogEntity> findByActiveTrueOrderBySortOrder();

    Optional<TaxTypeCatalogEntity> findByCode(String code);
}
