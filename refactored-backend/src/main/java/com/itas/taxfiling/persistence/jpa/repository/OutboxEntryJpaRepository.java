package com.itas.taxfiling.persistence.jpa.repository;

import com.itas.taxfiling.domain.valueobject.OutboxStatus;
import com.itas.taxfiling.persistence.jpa.entity.OutboxEntryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEntryJpaRepository extends JpaRepository<OutboxEntryEntity, UUID> {

    @Query("""
           select e from OutboxEntryEntity e
            where e.status = :status
              and e.nextAttemptAt <= :now
            order by e.priority desc, e.createdAt asc
           """)
    List<OutboxEntryEntity> findReady(OutboxStatus status, Instant now, Pageable pageable);
}
