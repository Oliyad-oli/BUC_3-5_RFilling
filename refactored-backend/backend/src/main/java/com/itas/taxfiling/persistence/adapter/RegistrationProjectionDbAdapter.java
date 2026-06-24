package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.domain.valueobject.SubledgerGroupReference;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.persistence.jpa.repository.SubledgerProjectionJpaRepository;
import com.itas.taxfiling.persistence.jpa.repository.TaxpayerProjectionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DB-backed registration projection. @Primary so the local read-model wins
 * over the mock adapter when both are present.
 */
@Component
@Primary
@RequiredArgsConstructor
public class RegistrationProjectionDbAdapter implements RegistrationProjectionPort {

    private final TaxpayerProjectionJpaRepository taxpayers;
    private final SubledgerProjectionJpaRepository subledgers;

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxpayerSnapshot> findByTin(String tin) {
        return taxpayers.findByTin(tin)
            .map(t -> new TaxpayerSnapshot(t.getTin(), t.getPartyId(), t.getLegalName(),
                t.getStatus(), t.isActive()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubledgerGroupReference> findSubledgers(String tin, TaxTypeCode taxType) {
        return subledgers.findByTinAndTaxType(tin, taxType.value())
            .map(s -> new SubledgerGroupReference(
                s.getPrincipalSubledgerId(),
                s.getPenaltySubledgerId(),
                s.getInterestSubledgerId(),
                s.getRefundSubledgerId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxpayerSnapshot> listActive() {
        return taxpayers.findAll().stream()
            .filter(com.itas.taxfiling.persistence.jpa.entity.TaxpayerProjectionEntity::isActive)
            .map(t -> new TaxpayerSnapshot(t.getTin(), t.getPartyId(), t.getLegalName(),
                t.getStatus(), t.isActive()))
            .toList();
    }

    @Override
    @Transactional
    public void upsertTaxpayer(String tin, String partyId, String legalName,
                               String status, boolean active) {
        var row = taxpayers.findByTin(tin)
            .orElseGet(com.itas.taxfiling.persistence.jpa.entity.TaxpayerProjectionEntity::new);
        if (row.getId() == null) row.setId(java.util.UUID.randomUUID());
        row.setTin(tin);
        row.setPartyId(partyId);
        row.setLegalName(legalName);
        row.setStatus(status);
        row.setActive(active);
        row.setUpdatedAt(java.time.Instant.now());
        taxpayers.save(row);
    }

    @Override
    @Transactional
    public void upsertSubledgers(String tin, TaxTypeCode taxType,
                                 java.util.UUID principal, java.util.UUID penalty,
                                 java.util.UUID interest, java.util.UUID refund) {
        var row = subledgers.findByTinAndTaxType(tin, taxType.value())
            .orElseGet(com.itas.taxfiling.persistence.jpa.entity.SubledgerProjectionEntity::new);
        if (row.getId() == null) row.setId(java.util.UUID.randomUUID());
        row.setTin(tin);
        row.setTaxType(taxType.value());
        row.setPrincipalSubledgerId(principal);
        row.setPenaltySubledgerId(penalty);
        row.setInterestSubledgerId(interest);
        row.setRefundSubledgerId(refund);
        row.setUpdatedAt(java.time.Instant.now());
        subledgers.save(row);
    }
}
