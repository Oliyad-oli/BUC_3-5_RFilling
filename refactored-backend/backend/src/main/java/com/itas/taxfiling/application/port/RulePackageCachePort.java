package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Local cache for tax-type-engine SDK responses (Rule 9). The
 * TaxTypeEngineCachingAdapter consults this port first; on miss or expiry it
 * falls back to the SDK and writes back through {@link #upsert}.
 */
public interface RulePackageCachePort {

    Optional<CachedRulePackage> find(TaxTypeCode taxType, LocalDate effectiveOn);

    void upsert(TaxTypeCode taxType, LocalDate effectiveOn, RulePackageVersion version,
                Instant fetchedAt, Instant expiresAt);

    List<CachedRulePackage> findAll();

    record CachedRulePackage(
        TaxTypeCode taxType,
        LocalDate effectiveOn,
        RulePackageVersion version,
        Instant fetchedAt,
        Instant expiresAt
    ) {}
}
