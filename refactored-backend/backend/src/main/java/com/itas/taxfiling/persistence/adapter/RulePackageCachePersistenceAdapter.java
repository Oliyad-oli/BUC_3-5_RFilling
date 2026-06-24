package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.RulePackageCachePort;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.persistence.jpa.entity.RulePackageCacheEntity;
import com.itas.taxfiling.persistence.jpa.repository.RulePackageCacheJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RulePackageCachePersistenceAdapter implements RulePackageCachePort {

    private final RulePackageCacheJpaRepository repository;
    @PersistenceContext private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Optional<CachedRulePackage> find(TaxTypeCode taxType, LocalDate effectiveOn) {
        return repository.findByTaxTypeAndEffectiveOn(taxType.value(), effectiveOn)
            .map(this::toDomain);
    }

    @Override
    @Transactional
    public void upsert(TaxTypeCode taxType, LocalDate effectiveOn, RulePackageVersion version,
                       Instant fetchedAt, Instant expiresAt) {
        String id = taxType.value() + "::" + effectiveOn;
        RulePackageCacheEntity row = repository.findById(id).orElseGet(RulePackageCacheEntity::new);
        row.setId(id);
        row.setTaxType(taxType.value());
        row.setEffectiveOn(effectiveOn);
        row.setVersion(version.version());
        var meta = new HashMap<String, Object>();
        meta.put("source", "tax-type-engine");
        row.setMetadata(meta);
        row.setFetchedAt(fetchedAt);
        row.setExpiresAt(expiresAt);
        // String @Id with no @Version makes Spring Data's isNew() ambiguous —
        // calling repository.save() can route to persist() and trigger a
        // duplicate-key INSERT. em.merge() guarantees an UPDATE for existing
        // rows and an INSERT for genuinely new ones.
        //
        // Concurrent first-time fetches (e.g. React StrictMode firing the start
        // request twice) can race on the INSERT — both transactions see the row
        // as absent. The cache row is identical in both writes, so we swallow
        // the unique-constraint failure: the row is already there, our caller's
        // intent is satisfied.
        try {
            em.merge(row);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Already inserted by a concurrent request. No-op.
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CachedRulePackage> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private CachedRulePackage toDomain(RulePackageCacheEntity e) {
        return new CachedRulePackage(
            new TaxTypeCode(e.getTaxType()),
            e.getEffectiveOn(),
            new RulePackageVersion(e.getTaxType(), e.getVersion()),
            e.getFetchedAt(),
            e.getExpiresAt());
    }
}
