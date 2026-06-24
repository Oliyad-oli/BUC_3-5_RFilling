package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.CalendarPeriodRepositoryPort;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.persistence.jpa.entity.CalendarPeriodEntity;
import com.itas.taxfiling.persistence.jpa.repository.CalendarPeriodJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CalendarPeriodPersistenceAdapter implements CalendarPeriodRepositoryPort {

    private final CalendarPeriodJpaRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Optional<CalendarPeriod> findByTaxTypeAndLabel(TaxTypeCode taxType, String periodLabel) {
        return repo.findByTaxTypeCodeAndPeriodLabel(taxType.value(), periodLabel).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarPeriod> findByTaxTypeInRange(TaxTypeCode taxType, LocalDate from, LocalDate to) {
        return repo.findByTaxTypeCodeInRange(taxType.value(), from, to).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void upsertBatch(List<CalendarPeriod> periods) {
        Instant now = Instant.now();
        for (CalendarPeriod p : periods) {
            CalendarPeriodEntity e = repo
                .findByTaxTypeCodeAndPeriodLabel(p.taxTypeCode(), p.periodLabel())
                .orElseGet(() -> {
                    CalendarPeriodEntity fresh = new CalendarPeriodEntity();
                    fresh.setId(UUID.randomUUID());
                    fresh.setSource("TAX_TYPE_ENGINE");
                    return fresh;
                });
            e.setTaxTypeCode(p.taxTypeCode());
            e.setPeriodLabel(p.periodLabel());
            e.setStartsOn(p.startsOn());
            e.setEndsOn(p.endsOn());
            e.setDueOn(p.dueOn());
            e.setFrequency(p.frequency());
            e.setRefreshedAt(now);
            repo.save(e);
        }
    }

    private CalendarPeriod toDomain(CalendarPeriodEntity e) {
        return new CalendarPeriod(
            e.getTaxTypeCode(),
            e.getPeriodLabel(),
            e.getStartsOn(),
            e.getEndsOn(),
            e.getDueOn(),
            e.getFrequency());
    }
}
