package com.itas.taxfiling.infrastructure.persistence.repository;

import com.itas.taxfiling.infrastructure.persistence.entity.TaxReturnJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxReturnJpaRepository extends JpaRepository<TaxReturnJpaEntity, String> {
    List<TaxReturnJpaEntity> findByTin(String tin);
}
