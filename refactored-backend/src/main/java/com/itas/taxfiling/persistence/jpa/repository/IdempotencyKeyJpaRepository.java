package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.persistence.jpa.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    Optional<IdempotencyKeyEntity> findByKeyValueAndEndpoint(String keyValue, String endpoint);
}
