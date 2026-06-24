package com.itas.taxfiling.engineadapter.taxtype;

import com.itas.taxfiling.application.port.RulePackageCachePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Rule 9 — caching wrapper around the tax-type-engine SDK. Consults
 * {@link RulePackageCachePort} first; on miss or expiry, calls the underlying
 * SDK adapter and writes back to the cache. Marked @Primary so it shadows the
 * bare SDK bean for normal injection.
 *
 * Listing-style methods (listAvailableTaxTypes / nextPeriod) pass through to
 * the SDK — they're fast-changing reads that don't benefit from this cache
 * shape. defaultFieldsFor is also a pass-through.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class TaxTypeEngineCachingAdapter implements TaxTypeEnginePort {

    private static final long CACHE_TTL_HOURS = 6;

    private final TaxTypeEngineMockAdapter sdk;
    private final RulePackageCachePort cache;

    @Override
    public RulePackageVersion currentRulePackage(TaxTypeCode taxType, LocalDate effectiveOn) {
        Optional<RulePackageCachePort.CachedRulePackage> hit = cache.find(taxType, effectiveOn);
        if (hit.isPresent() && hit.get().expiresAt().isAfter(Instant.now())) {
            log.debug("rule package cache HIT taxType={} effectiveOn={}", taxType, effectiveOn);
            return hit.get().version();
        }
        log.debug("rule package cache MISS taxType={} effectiveOn={} — fetching from SDK",
            taxType, effectiveOn);
        RulePackageVersion fresh = sdk.currentRulePackage(taxType, effectiveOn);
        Instant now = Instant.now();
        cache.upsert(taxType, effectiveOn, fresh, now, now.plus(CACHE_TTL_HOURS, ChronoUnit.HOURS));
        return fresh;
    }

    @Override
    public List<EntryFieldDefinition> defaultFieldsFor(TaxTypeCode taxType, ScheduleKind kind,
                                                       RulePackageVersion rulePackage) {
        return sdk.defaultFieldsFor(taxType, kind, rulePackage);
    }

    @Override
    public List<TaxTypeSummary> listAvailableTaxTypes() {
        return sdk.listAvailableTaxTypes();
    }

    @Override
    public UpcomingPeriod nextPeriod(TaxTypeCode taxType, LocalDate after) {
        return sdk.nextPeriod(taxType, after);
    }

    @Override
    public List<ScheduleSpec> schedulesFor(TaxTypeCode taxType, RulePackageVersion rulePackage) {
        return sdk.schedulesFor(taxType, rulePackage);
    }

    @Override
    public List<CalendarPeriod> getCalendar(TaxTypeCode taxType, LocalDate from, int horizonMonths) {
        // Pass-through for now. Future: persist to a calendar_cache table and
        // refresh from the safety-net cron, same pattern as currentRulePackage.
        return sdk.getCalendar(taxType, from, horizonMonths);
    }

    /** Refresh near-expired cache rows hourly so the warm path stays warm. */
    @Scheduled(fixedDelayString = "${itas.tax-type-cache.refresh-interval-ms:3600000}")
    public void refreshExpired() {
        Instant now = Instant.now();
        for (RulePackageCachePort.CachedRulePackage row : cache.findAll()) {
            if (row.expiresAt().isBefore(now.plus(1, ChronoUnit.HOURS))) {
                try {
                    RulePackageVersion fresh = sdk.currentRulePackage(row.taxType(), row.effectiveOn());
                    cache.upsert(row.taxType(), row.effectiveOn(), fresh, now,
                        now.plus(CACHE_TTL_HOURS, ChronoUnit.HOURS));
                } catch (Exception e) {
                    log.warn("rule package cache refresh failed taxType={} effectiveOn={}",
                        row.taxType(), row.effectiveOn(), e);
                }
            }
        }
    }
}
