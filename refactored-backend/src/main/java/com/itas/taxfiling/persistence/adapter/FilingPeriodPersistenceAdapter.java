package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.FilingPeriodRepositoryPort;
import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;
import com.itas.taxfiling.persistence.jpa.entity.FilingPeriodEntity;
import com.itas.taxfiling.persistence.jpa.repository.FilingPeriodJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FilingPeriodPersistenceAdapter implements FilingPeriodRepositoryPort {

    private final FilingPeriodJpaRepository repo;

    @Override
    @Transactional
    public FilingPeriod save(FilingPeriod p) {
        repo.save(toEntity(p));
        // Return the input — events still attached for the caller to publish.
        return p;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilingPeriod> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilingPeriod> findByObligationAndLabel(UUID obligationId, String periodLabel) {
        return repo.findByTaxpayerObligationIdAndPeriodLabel(obligationId, periodLabel).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FilingPeriod> findByTaxReturnId(UUID taxReturnId) {
        return repo.findByTaxReturnId(taxReturnId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilingPeriod> findOutstandingByTin(String tin) {
        // Outstanding = "what does the user owe right now". FUTURE periods
        // (window hasn't opened) and FILED periods (terminal) are not.
        return repo.findByTinAndStatusInOrderByDueDateAsc(tin,
            List.of(FilingPeriodStatus.OPEN, FilingPeriodStatus.DUE, FilingPeriodStatus.OVERDUE))
            .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilingPeriod> findAllByTin(String tin) {
        return repo.findByTinOrderByDueDateAsc(tin).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilingPeriod> findCandidatesForStatusFlip(LocalDate today) {
        // Union of:
        //   FUTURE rows whose coversFrom ≤ today  (→ should become OPEN)
        //   OPEN rows whose coversTo < today      (→ should become DUE)
        //   DUE rows whose dueDate < today        (→ should become OVERDUE)
        var u = new LinkedHashSet<FilingPeriodEntity>();
        u.addAll(repo.findByStatusAndCoversFromLessThanEqual(FilingPeriodStatus.FUTURE, today));
        u.addAll(repo.findByStatusAndCoversToLessThan(FilingPeriodStatus.OPEN, today));
        u.addAll(repo.findByStatusAndDueDateLessThan(FilingPeriodStatus.DUE, today));
        return u.stream().map(this::toDomain).toList();
    }

    private FilingPeriodEntity toEntity(FilingPeriod p) {
        FilingPeriodEntity e = repo.findById(p.getId()).orElseGet(FilingPeriodEntity::new);
        e.setId(p.getId());
        e.setTaxpayerObligationId(p.getObligationId());
        e.setTin(p.getTin());
        e.setTaxTypeCode(p.getTaxTypeCode());
        e.setPeriodLabel(p.getPeriodLabel());
        e.setCoversFrom(p.getCoversFrom());
        e.setCoversTo(p.getCoversTo());
        e.setDueDate(p.getDueDate());
        e.setPartial(p.isPartial());
        e.setStatus(p.getStatus());
        e.setTaxReturnId(p.getTaxReturnId().orElse(null));
        e.setFiledAt(p.getFiledAt().orElse(null));
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(p.getCreatedAt());
            // Lazy-period model: the FilingPeriod row is created on first
            // materialization; stamp the moment it lands in the DB.
            e.setMaterializedAt(p.getCreatedAt());
        }
        return e;
    }

    private FilingPeriod toDomain(FilingPeriodEntity e) {
        return FilingPeriod.rehydrate(
            e.getId(), e.getTaxpayerObligationId(), e.getTin(), e.getTaxTypeCode(),
            e.getPeriodLabel(), e.getCoversFrom(), e.getCoversTo(), e.getDueDate(),
            e.isPartial(), e.getStatus(),
            e.getTaxReturnId(), e.getFiledAt(),
            e.getCreatedAt(), e.getVersion());
    }
}
