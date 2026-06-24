package com.itas.taxfiling.engineadapter.taxtype;

import com.itas.taxfiling.application.port.TaxTypeCatalogRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeCatalogRepositoryPort.TaxTypeCatalogEntry;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * [MOCK] tax-type-engine SDK adapter. Reads the canonical Ethiopian tax-type
 * catalog seeded by Flyway (V6 — {@code tax_type_catalog} table) until the
 * real engine is wired in. This is the Rule 9 cache pattern: the catalog rows
 * are the local cache; when the SDK ships, this adapter switches to a
 * read-through over it without any caller-side change.
 *
 * <p>The {@link #generateCalendar} helpers stay in this class because period
 * shapes (monthly / quarterly fiscal-year / annual fiscal-year) are
 * deterministic from the catalog's {@code frequency} + {@code dueOffsetDays}.
 */
@Slf4j
@Component
public class TaxTypeEngineMockAdapter extends BaseEngineAdapter implements TaxTypeEnginePort {

    private final TaxTypeCatalogRepositoryPort catalog;

    public TaxTypeEngineMockAdapter(TaxTypeCatalogRepositoryPort catalog) {
        super("tax-type-engine");
        this.catalog = catalog;
    }

    @Override
    @CircuitBreaker(name = "tax-type-engine", fallbackMethod = "currentRulePackageFallback")
    @Retry(name = "tax-type-engine")
    public RulePackageVersion currentRulePackage(TaxTypeCode taxType, LocalDate effectiveOn) {
        log.info("[MOCK] tax-type-engine currentRulePackage taxType={} effectiveOn={}", taxType, effectiveOn);
        return new RulePackageVersion(taxType.value(), "1.0.0");
    }

    private RulePackageVersion currentRulePackageFallback(TaxTypeCode taxType, LocalDate effectiveOn, Exception ex) {
        throw wrapException("currentRulePackage", ex);
    }

    @Override
    @CircuitBreaker(name = "tax-type-engine", fallbackMethod = "defaultFieldsForFallback")
    @Retry(name = "tax-type-engine")
    public List<EntryFieldDefinition> defaultFieldsFor(TaxTypeCode taxType, ScheduleKind kind,
                                                       RulePackageVersion rulePackage) {
        log.info("[MOCK] tax-type-engine defaultFieldsFor taxType={} kind={} package={}",
            taxType, kind, rulePackage);
        // Default fallback shape — admins register richer entry types via the
        // LineItemEntryType catalog (BUC-FIL-CONFIG-01). This is only consulted
        // when no admin row exists.
        return List.of(
            new EntryFieldDefinition("invoiceNumber", "Invoice Number",
                EntryFieldType.STRING, true, List.of(), null),
            new EntryFieldDefinition("counterpartyTin", "Counterparty TIN",
                EntryFieldType.STRING, false, List.of(), "\\d{9,12}")
        );
    }

    private List<EntryFieldDefinition> defaultFieldsForFallback(TaxTypeCode taxType, ScheduleKind kind,
                                                                RulePackageVersion rulePackage, Exception ex) {
        throw wrapException("defaultFieldsFor", ex);
    }

    @Override
    @CircuitBreaker(name = "tax-type-engine", fallbackMethod = "listAvailableTaxTypesFallback")
    @Retry(name = "tax-type-engine")
    public List<TaxTypeSummary> listAvailableTaxTypes() {
        log.info("[MOCK] tax-type-engine listAvailableTaxTypes — reading from tax_type_catalog");
        return catalog.findAllActive().stream()
            .map(c -> new TaxTypeSummary(c.code(), c.name(), c.frequency(), c.active()))
            .toList();
    }

    private List<TaxTypeSummary> listAvailableTaxTypesFallback(Exception ex) {
        throw wrapException("listAvailableTaxTypes", ex);
    }

    @Override
    @CircuitBreaker(name = "tax-type-engine", fallbackMethod = "nextPeriodFallback")
    @Retry(name = "tax-type-engine")
    public UpcomingPeriod nextPeriod(TaxTypeCode taxType, LocalDate after) {
        log.info("[MOCK] tax-type-engine nextPeriod taxType={} after={}", taxType, after);
        TaxTypeCatalogEntry entry = requireCatalogEntry(taxType);
        var freq = entry.frequency();
        var calendar = generateCalendar(taxType, freq, after, 2, entry.dueOffsetDays());
        // Take the first calendar row strictly after `after`.
        for (CalendarPeriod cp : calendar) {
            if (!cp.startsOn().isBefore(after)) {
                return new UpcomingPeriod(taxType, cp.startsOn(), cp.endsOn(), cp.dueOn(), cp.frequency());
            }
        }
        // Fall through — shouldn't happen with the generator's horizon, but
        // protect against an empty list.
        var first = calendar.isEmpty() ? null : calendar.get(0);
        if (first == null) throw new DomainException("calendar generator returned no rows");
        return new UpcomingPeriod(taxType, first.startsOn(), first.endsOn(), first.dueOn(), first.frequency());
    }

    private UpcomingPeriod nextPeriodFallback(TaxTypeCode taxType, LocalDate after, Exception ex) {
        throw wrapException("nextPeriod", ex);
    }

    @Override
    @CircuitBreaker(name = "tax-type-engine", fallbackMethod = "schedulesForFallback")
    @Retry(name = "tax-type-engine")
    public List<ScheduleSpec> schedulesFor(TaxTypeCode taxType, RulePackageVersion rulePackage) {
        log.info("[MOCK] tax-type-engine schedulesFor taxType={} package={}", taxType, rulePackage);
        return catalog.findByCode(taxType)
            .map(e -> e.schedules().stream()
                .map(s -> new ScheduleSpec(s.kind(), s.label(), s.supportsCarryForward()))
                .toList())
            // Unknown tax types fall back to a single generic schedule so the
            // wizard still renders. Catalog should be the source of truth, but
            // this protects newly-added codes that haven't been seeded yet.
            .orElseGet(() -> List.of(new ScheduleSpec(ScheduleKind.OTHER, "Entries", false)));
    }

    private List<ScheduleSpec> schedulesForFallback(TaxTypeCode taxType, RulePackageVersion rulePackage,
                                                    Exception ex) {
        throw wrapException("schedulesFor", ex);
    }

    @Override
    @CircuitBreaker(name = "tax-type-engine", fallbackMethod = "getCalendarFallback")
    @Retry(name = "tax-type-engine")
    public List<CalendarPeriod> getCalendar(TaxTypeCode taxType, LocalDate from, int horizonMonths) {
        log.info("[MOCK] tax-type-engine getCalendar taxType={} from={} horizonMonths={}",
            taxType, from, horizonMonths);
        TaxTypeCatalogEntry entry = requireCatalogEntry(taxType);
        return generateCalendar(taxType, entry.frequency(), from, horizonMonths, entry.dueOffsetDays());
    }

    private List<CalendarPeriod> getCalendarFallback(TaxTypeCode taxType, LocalDate from,
                                                     int horizonMonths, Exception ex) {
        throw wrapException("getCalendar", ex);
    }

    // ─── Catalog lookup ─────────────────────────────────────────────────────

    private TaxTypeCatalogEntry requireCatalogEntry(TaxTypeCode taxType) {
        return catalog.findByCode(taxType)
            .orElseThrow(() -> new DomainException(
                "tax-type-catalog has no row for code='" + taxType.value() + "' — seed it via Flyway"));
    }

    // ─── Period generator (deterministic) ───────────────────────────────────
    // Real engine returns rows from a published table; the math is the same.

    private static List<CalendarPeriod> generateCalendar(
            TaxTypeCode taxType, PeriodFrequency freq,
            LocalDate from, int horizonMonths, int dueOffsetDays) {
        java.util.List<CalendarPeriod> out = new java.util.ArrayList<>();
        switch (freq) {
            case MONTHLY: {
                java.time.YearMonth ym = java.time.YearMonth.from(from);
                for (int i = 0; i < horizonMonths; i++) {
                    LocalDate s = ym.atDay(1);
                    LocalDate e = ym.atEndOfMonth();
                    out.add(new CalendarPeriod(taxType, freq,
                        ym.format(java.time.format.DateTimeFormatter.ofPattern("MMM-yyyy")).toUpperCase(),
                        s, e, e.plusDays(dueOffsetDays)));
                    ym = ym.plusMonths(1);
                }
                break;
            }
            case QUARTERLY: {
                // Ethiopian fiscal-year-anchored quarters per the reference doc:
                //   Q1: Sep–Nov, Q2: Dec–Feb, Q3: Mar–May, Q4: Jun–Aug.
                LocalDate cursor = quarterStartFor(from);
                for (int i = 0; i < (horizonMonths / 3) + 1; i++) {
                    LocalDate s = cursor;
                    LocalDate e = cursor.plusMonths(3).minusDays(1);
                    String label = "Q" + quarterIndex(s) + "-" + fiscalYearLabel(s);
                    out.add(new CalendarPeriod(taxType, freq, label, s, e, e.plusDays(dueOffsetDays)));
                    cursor = cursor.plusMonths(3);
                }
                break;
            }
            case ANNUAL: {
                // Ethiopian fiscal year: 8 July → 7 July
                LocalDate s = fiscalYearStartFor(from);
                int years = Math.max(1, horizonMonths / 12);
                for (int i = 0; i < years + 1; i++) {
                    LocalDate e = s.plusYears(1).minusDays(1);
                    String label = "FY-" + s.getYear() + "/" + String.format("%02d", (s.getYear() + 1) % 100);
                    out.add(new CalendarPeriod(taxType, freq, label, s, e, e.plusDays(dueOffsetDays)));
                    s = s.plusYears(1);
                }
                break;
            }
            default: break;
        }
        return out;
    }

    private static LocalDate quarterStartFor(LocalDate d) {
        int m = d.getMonthValue();
        if (m >= 9)        return LocalDate.of(d.getYear(),     9, 1);
        if (m >= 6)        return LocalDate.of(d.getYear(),     6, 1);
        if (m >= 3)        return LocalDate.of(d.getYear(),     3, 1);
        return                    LocalDate.of(d.getYear() - 1, 12, 1);
    }

    private static int quarterIndex(LocalDate s) {
        int m = s.getMonthValue();
        if (m == 9)  return 1;
        if (m == 12) return 2;
        if (m == 3)  return 3;
        return 4;
    }

    private static String fiscalYearLabel(LocalDate s) {
        int y = s.getMonthValue() >= 9 ? s.getYear() : s.getYear() - 1;
        return y + "/" + String.format("%02d", (y + 1) % 100);
    }

    private static LocalDate fiscalYearStartFor(LocalDate d) {
        // Fiscal year: 8 July → 7 July (Ethiopian)
        LocalDate fyStartThisCal = LocalDate.of(d.getYear(), 7, 8);
        return d.isBefore(fyStartThisCal)
            ? LocalDate.of(d.getYear() - 1, 7, 8)
            : fyStartThisCal;
    }
}
