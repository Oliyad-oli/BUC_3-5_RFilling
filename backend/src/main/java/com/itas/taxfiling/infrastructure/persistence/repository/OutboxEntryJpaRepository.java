package com.itas.taxfiling.infrastructure.persistence.repository;

import com.itas.taxfiling.infrastructure.persistence.entity.OutboxEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEntryJpaRepository extends JpaRepository<OutboxEntryJpaEntity, String> {
    @Query("SELECT e FROM OutboxEntryJpaEntity e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC LIMIT :limit")
    List<OutboxEntryJpaEntity> findPending(int limit);
}
