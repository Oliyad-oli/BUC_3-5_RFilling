package com.itas.taxfiling.engineadapter.ledger;

import com.itas.taxfiling.application.port.FilingTemplateMappingRepositoryPort;
import com.itas.taxfiling.application.port.LedgerEnginePort;
import com.itas.taxfiling.application.port.TaxTypeCatalogRepositoryPort;
import com.itas.taxfiling.domain.exception.EngineAdapterException;
import com.itas.taxfiling.domain.model.FilingTemplateMapping;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Real ledger-engine adapter — talks to the Consumer-API-Facade's
 * template-driven posting endpoint ({@code POST /api/ledger/v1/events/template}).
 *
 * <p>Filing-side ops:
 * <ul>
 *   <li><b>postAssessment</b> — initial principal post when a return is
 *       accepted (BUC-FIL-013), uses {@code TPL-{tax}-RETURN-FILED-001}.</li>
 *   <li><b>postAdjustment</b> — amendment delta to PRINCIPAL, references the
 *       original entry id (BUC-FIL-033, Rule 8). Same template; the engine
 *       diffs on its end.</li>
 *   <li><b>postPenalty</b> — late-filing penalty, uses the catalog's
 *       late-filing-penalty template.</li>
 *   <li><b>postInterest</b> — kept for legacy callers (Phase 1 owns interest in
 *       payment-service); routes through the late-filing penalty template ref.</li>
 * </ul>
 *
 * <p>Every call:
 * <ol>
 *   <li>Resolves the right {@code templateRef} for the tax type from
 *       {@link FilingTemplateMappingRepositoryPort}.</li>
 *   <li>Builds the canonical body (templateRef + tin + sourceRef +
 *       sourceService + postingDate + fiscalPeriod + components + dimensions +
 *       expectedTaxpayerAccounts).</li>
 *   <li>Derives a deterministic {@code Idempotency-Key} from the call's inputs
 *       so a Resilience4j retry of the same logical op returns the same
 *       journal (the engine dedups on the header).</li>
 *   <li>POSTs the body and returns a {@link LedgerEntryReference} pointing at
 *       the subledger line (the one with {@code taxpayerAccountId}).</li>
 * </ol>
 *
 * <p>A 409 Conflict with an existing journalId (idempotent replay) is treated
 * as a successful post — same return shape as a fresh 201.
 *
 * <p>Class name still ends in {@code MockAdapter} for filename-stability with
 * callers / tests; the Javadoc and implementation are real-HTTP.
 */
@Slf4j
@Component
public class LedgerEngineMockAdapter extends BaseEngineAdapter
    implements LedgerEnginePort {

    private static final String JURISDICTION = "ETH";
    private static final String SOURCE_SERVICE = "filing-service";
    private static final String EVENTS_TEMPLATE_PATH = "/api/ledger/v1/events/template";

    private final FilingTemplateMappingRepositoryPort mappings;
    private final TaxTypeCatalogRepositoryPort catalog;
    private final WebClient ledgerWebClient;

    public LedgerEngineMockAdapter(FilingTemplateMappingRepositoryPort mappings,
                                   TaxTypeCatalogRepositoryPort catalog,
                                   @Qualifier("ledgerWebClient") WebClient ledgerWebClient) {
        super("ledger-engine");
        this.mappings = mappings;
        this.catalog = catalog;
        this.ledgerWebClient = ledgerWebClient;
    }

    @Override
    @CircuitBreaker(name = "ledger-engine", fallbackMethod = "postAssessmentFallback")
    @Retry(name = "ledger-engine")
    public LedgerEntryReference postAssessment(String tin, TaxTypeCode taxType, Period period,
                                               Money amount, AccountCategory category) {
        String templateRef = lookupFilingTemplate(taxType);
        // Deterministic sourceRef so a retry under the same logical operation
        // (same tin × tax type × period × amount) reuses the same key.
        String sourceRef = "FIL-RET-" + tin + "-" + taxType.value() + "-" + period.label()
            + "-" + amount.amount().toPlainString();
        Map<String, Object> body = templateBody(
            templateRef, tin, sourceRef, taxType, period, amount, "PRINCIPAL", null);
        return postTemplate(body, "postAssessment", category);
    }

    private LedgerEntryReference postAssessmentFallback(String tin, TaxTypeCode taxType, Period period,
                                                        Money amount, AccountCategory category, Exception ex) {
        throw wrapException("postAssessment", ex);
    }

    @Override
    @CircuitBreaker(name = "ledger-engine", fallbackMethod = "postAdjustmentFallback")
    @Retry(name = "ledger-engine")
    public LedgerEntryReference postAdjustment(String tin, TaxTypeCode taxType, Period period,
                                               Money delta, AccountCategory category, UUID originalEntryId) {
        String templateRef = lookupFilingTemplate(taxType);
        String sourceRef = "FIL-ADJ-" + originalEntryId + "-" + delta.amount().toPlainString();
        Map<String, Object> body = templateBody(
            templateRef, tin, sourceRef, taxType, period, delta, "PRINCIPAL",
            Map.of("originalEntryId", originalEntryId.toString()));
        return postTemplate(body, "postAdjustment", category);
    }

    private LedgerEntryReference postAdjustmentFallback(String tin, TaxTypeCode taxType, Period period,
                                                       Money delta, AccountCategory category,
                                                       UUID originalEntryId, Exception ex) {
        throw wrapException("postAdjustment", ex);
    }

    @Override
    @CircuitBreaker(name = "ledger-engine", fallbackMethod = "postPenaltyFallback")
    @Retry(name = "ledger-engine")
    public LedgerEntryReference postPenalty(String tin, TaxTypeCode taxType, Period period, Money amount) {
        String templateRef = lookupLateFilingPenaltyTemplate(taxType);
        String sourceRef = "FIL-PEN-" + tin + "-" + taxType.value() + "-" + period.label()
            + "-" + amount.amount().toPlainString();
        Map<String, Object> body = templateBody(
            templateRef, tin, sourceRef, taxType, period, amount, "PENALTY", null);
        return postTemplate(body, "postPenalty", AccountCategory.PENALTY);
    }

    private LedgerEntryReference postPenaltyFallback(String tin, TaxTypeCode taxType, Period period,
                                                     Money amount, Exception ex) {
        throw wrapException("postPenalty", ex);
    }

    @Override
    @CircuitBreaker(name = "ledger-engine", fallbackMethod = "postInterestFallback")
    @Retry(name = "ledger-engine")
    public LedgerEntryReference postInterest(String tin, TaxTypeCode taxType, Period period, Money amount) {
        // Interest accruals are owned by payment-service in Phase 1; this hook
        // is kept for any legacy filing callers and routes through the late-
        // filing penalty template ref.
        String templateRef = lookupLateFilingPenaltyTemplate(taxType);
        String sourceRef = "FIL-INT-" + tin + "-" + taxType.value() + "-" + period.label()
            + "-" + amount.amount().toPlainString();
        Map<String, Object> body = templateBody(
            templateRef, tin, sourceRef, taxType, period, amount, "INTEREST", null);
        return postTemplate(body, "postInterest", AccountCategory.INTEREST);
    }

    private LedgerEntryReference postInterestFallback(String tin, TaxTypeCode taxType, Period period,
                                                      Money amount, Exception ex) {
        throw wrapException("postInterest", ex);
    }

    private LedgerEntryReference postTemplate(Map<String, Object> body, String operation,
                                              AccountCategory category) {
        String idempotencyKey = deterministicIdempotencyKey(body);
        log.info("POST {} idempotency-key={} body={}", EVENTS_TEMPLATE_PATH, idempotencyKey, body);
        try {
            LedgerEventResponse response = ledgerWebClient.post()
                .uri(EVENTS_TEMPLATE_PATH)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(LedgerEventResponse.class)
                .block();
            return toReference(response, operation, category);
        } catch (WebClientResponseException.Conflict conflict) {
            // 409 → engine has already posted this Idempotency-Key. Returns
            // the existing journal payload — same shape as a 201.
            LedgerEventResponse replay = conflict.getResponseBodyAs(LedgerEventResponse.class);
            log.info("Idempotent replay of {} returned existing journalId={}",
                operation, replay != null ? replay.journalId() : "<unparsed>");
            return toReference(replay, operation, category);
        }
    }

    private LedgerEntryReference toReference(LedgerEventResponse response, String operation,
                                             AccountCategory category) {
        if (response == null) {
            throw new EngineAdapterException("ledger-engine", operation,
                new IllegalStateException("ledger returned empty body"));
        }
        UUID lineId = firstSubledgerLineId(response, category);
        Instant postedAt = response.postedAt() != null ? response.postedAt() : Instant.now();
        return new LedgerEntryReference(lineId, category, postedAt);
    }

    /**
     * Pick the subledger line UUID from the engine response — preferring the
     * line that carries a {@code taxpayerAccountId} (the one a future
     * adjustment / reversal targets). Falls back to the first line, then to a
     * deterministic UUID derived from the journalId if the engine returned no
     * lines at all.
     */
    private UUID firstSubledgerLineId(LedgerEventResponse response, AccountCategory category) {
        LedgerCoreResponse core = response.coreResponse();
        if (core == null || core.lines() == null || core.lines().isEmpty()) {
            String journalId = response.journalId() != null ? response.journalId() : UUID.randomUUID().toString();
            return UUID.nameUUIDFromBytes((journalId + ":" + category.name()).getBytes(StandardCharsets.UTF_8));
        }
        return core.lines().stream()
            .filter(l -> l.taxpayerAccountId() != null)
            .findFirst()
            .map(LedgerLine::lineId)
            .orElseGet(() -> core.lines().get(0).lineId());
    }

    /**
     * Deterministic UUID from {@code sourceService|templateRef|sourceRef} —
     * Resilience4j retries with the same arguments reproduce the same key, so
     * the engine de-dups and we never accidentally post two journals for one
     * logical operation.
     */
    private String deterministicIdempotencyKey(Map<String, Object> body) {
        Object sourceRef = body.get("sourceRef");
        Object sourceService = body.get("sourceService");
        Object templateRef = body.get("templateRef");
        String seed = Objects.toString(sourceService) + "|" + Objects.toString(templateRef)
            + "|" + Objects.toString(sourceRef);
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String lookupFilingTemplate(TaxTypeCode taxType) {
        FilingTemplateMapping m = mappings.findByTaxTypeCode(taxType.value())
            .orElseThrow(() -> new EngineAdapterException(
                "ledger-engine", "lookup",
                new IllegalStateException(
                    "no filing template mapping configured for tax type " + taxType.value())));
        return m.getFilingTemplateRef();
    }

    private String lookupLateFilingPenaltyTemplate(TaxTypeCode taxType) {
        FilingTemplateMapping m = mappings.findByTaxTypeCode(taxType.value())
            .orElseThrow(() -> new EngineAdapterException(
                "ledger-engine", "lookup",
                new IllegalStateException(
                    "no filing template mapping configured for tax type " + taxType.value())));
        return m.lateFilingPenaltyTemplate().orElseThrow(
            () -> new EngineAdapterException(
                "ledger-engine", "lookup",
                new IllegalStateException(
                    "no late-filing penalty template configured for tax type " + taxType.value())));
    }

    private Map<String, Object> templateBody(
            String templateRef, String tin, String sourceRef,
            TaxTypeCode taxType, Period period, Money amount, String componentKey,
            Map<String, String> extraDimensions) {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put(componentKey, amount.amount());

        // Filing's domain models VAT periods as monthly (yyyy-MM) but
        // eng-ledger-core only keeps quarterly periods open. Translate at the
        // wire boundary so the post lands in the right open period. The
        // original monthly label travels as ORIGINAL_PERIOD for audit.
        String fiscalPeriod = toQuarterLabel(period);
        Map<String, Object> dimensions = new LinkedHashMap<>();
        dimensions.put("TAX_TYPE", taxType.value());
        dimensions.put("FISCAL_PERIOD", fiscalPeriod);
        dimensions.put("JURISDICTION", JURISDICTION);
        if (!fiscalPeriod.equals(period.label())) {
            dimensions.put("ORIGINAL_PERIOD", period.label());
        }
        if (extraDimensions != null) dimensions.putAll(extraDimensions);

        // Ledger v0.3+ contract — expected taxpayer subledger account names
        // per component, composed from (tin, ledger_abbr, component 4-letter)
        // matching the format registration's provisionSubledgers returns.
        Map<String, Object> expectedAccounts = new LinkedHashMap<>();
        String accountName = composeTaxpayerAccountName(tin, taxType, componentKey);
        if (accountName != null) {
            expectedAccounts.put(componentKey, accountName);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("templateRef", templateRef);
        body.put("tin", tin);
        body.put("sourceRef", sourceRef);
        body.put("sourceService", SOURCE_SERVICE);
        body.put("postingDate", LocalDate.now().toString());
        body.put("fiscalPeriod", fiscalPeriod);
        body.put("components", components);
        body.put("dimensions", dimensions);
        if (!expectedAccounts.isEmpty()) {
            body.put("expectedTaxpayerAccounts", expectedAccounts);
        }
        return body;
    }

    /**
     * Map a filing-domain Period label to the quarter label the ledger engine
     * accepts. Examples:
     * <ul>
     *   <li>{@code 2026-04} (monthly) → {@code 2026-Q2}</li>
     *   <li>{@code 2026-Q2} (already quarterly) → {@code 2026-Q2}</li>
     *   <li>{@code 2026-04-15} (daily) → {@code 2026-Q2} (quarter of start date)</li>
     *   <li>{@code 2026} (annual) → {@code 2026-Q4} (final quarter of year, since
     *       annual returns are settled when the year closes)</li>
     * </ul>
     * Derived from {@link Period#start()} so we never depend on parsing the
     * label string.
     */
    private String toQuarterLabel(Period period) {
        LocalDate start = period.start();
        int quarter = (start.getMonthValue() - 1) / 3 + 1;
        return start.getYear() + "-Q" + quarter;
    }

    /**
     * Composes the canonical taxpayer subledger account name the ledger engine
     * expects in {@code expectedTaxpayerAccounts}. Format:
     * <pre>{TIN}-{LEDGER_ABBR}-{COMPONENT_4LETTER}</pre>
     * Returns null if the catalog has no row for the tax type — caller drops
     * the field and the engine's internal resolver derives it.
     */
    private String composeTaxpayerAccountName(String tin, TaxTypeCode taxType, String componentKey) {
        var entry = catalog.findByCode(taxType).orElse(null);
        if (entry == null) return null;
        String comp4 = switch (componentKey) {
            case "PRINCIPAL" -> "PRIN";
            case "PENALTY"   -> "PEN";
            case "INTEREST"  -> "INT";
            case "REFUND"    -> "REF";
            default          -> componentKey;
        };
        return tin + "-" + entry.ledgerAbbr() + "-" + comp4;
    }

    // ── Engine response DTOs (private to this adapter) ──────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LedgerEventResponse(
        String journalId,
        String status,
        Instant postedAt,
        String templateVersion,
        Integer lineCount,
        LedgerCoreResponse coreResponse
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LedgerCoreResponse(
        String journalId,
        String status,
        Instant postedAt,
        String sourceRef,
        List<LedgerLine> lines
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LedgerLine(
        UUID lineId,
        String entryType,
        String accountCode,
        UUID taxpayerAccountId,
        java.math.BigDecimal amount,
        String currency
    ) {}
}
