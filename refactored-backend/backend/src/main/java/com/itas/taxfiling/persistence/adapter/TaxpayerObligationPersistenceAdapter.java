package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.TaxpayerObligationRepositoryPort;
import com.itas.taxfiling.domain.model.TaxpayerObligation;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.persistence.jpa.entity.TaxpayerObligationEntity;
import com.itas.taxfiling.persistence.jpa.repository.TaxpayerObligationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaxpayerObligationPersistenceAdapter implements TaxpayerObligationRepositoryPort {

    private final TaxpayerObligationJpaRepository repo;

    @Override
    @Transactional
    public TaxpayerObligation save(TaxpayerObligation o) {
        repo.save(toEntity(o));
        // Return the input instance — events are still on it. Rehydrating would
        // call pullEvents internally and silently drop them.
        return o;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxpayerObligation> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxpayerObligation> findByTinAndTaxType(String tin, TaxTypeCode taxType) {
        return repo.findByTinAndTaxTypeCode(tin, taxType.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxpayerObligation> findByTin(String tin) {
        return repo.findByTin(tin).stream().map(this::toDomain).toList();
    }

    private TaxpayerObligationEntity toEntity(TaxpayerObligation o) {
        TaxpayerObligationEntity e = repo.findById(o.getId()).orElseGet(TaxpayerObligationEntity::new);
        e.setId(o.getId());
        e.setTin(o.getTin());
        e.setPartyId(o.getPartyId());
        e.setTaxTypeCode(o.getTaxType().value());
        e.setFrequency(o.getFrequency());
        e.setEffectiveFrom(o.getEffectiveFrom());
        e.setEffectiveTo(o.getEffectiveTo().orElse(null));
        if (e.getCreatedAt() == null) e.setCreatedAt(o.getCreatedAt());
        return e;
    }

    private TaxpayerObligation toDomain(TaxpayerObligationEntity e) {
        return TaxpayerObligation.rehydrate(
            e.getId(), e.getTin(), e.getPartyId(),
            new TaxTypeCode(e.getTaxTypeCode()), e.getFrequency(),
            e.getEffectiveFrom(), e.getEffectiveTo(),
            e.getCreatedAt(), e.getVersion());
    }
}
