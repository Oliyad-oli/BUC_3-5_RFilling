package com.itas.taxfiling.infrastructure.persistence.repository;

import com.itas.taxfiling.infrastructure.persistence.entity.IdempotencyStoreJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyStoreJpaRepository extends JpaRepository<IdempotencyStoreJpaEntity, String> {
}
