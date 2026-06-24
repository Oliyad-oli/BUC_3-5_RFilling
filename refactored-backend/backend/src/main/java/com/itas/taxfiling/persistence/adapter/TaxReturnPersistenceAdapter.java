package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.model.Amendment;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AmendmentDelta;
import com.itas.taxfiling.domain.valueobject.AmendmentReason;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.LineItemValidationState;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RiskLevel;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import com.itas.taxfiling.domain.valueobject.RuleOutcome;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.domain.valueobject.TaxpayerReference;
import com.itas.taxfiling.domain.valueobject.ValidationLevel;
import com.itas.taxfiling.domain.valueobject.ValidationMessage;
import com.itas.taxfiling.persistence.jpa.entity.TaxReturnEntity;
import com.itas.taxfiling.persistence.jpa.repository.TaxReturnJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * TaxReturn ↔ JPA entity bridge. Schedules + iterations + amendments are
 * persisted as JSONB columns to keep the polymorphic line-item model (Rule 5)
 * coherent with the aggregate boundary. The conversion lives in this adapter
 * and never leaks into the domain.
 */
@Component
@RequiredArgsConstructor
public class TaxReturnPersistenceAdapter implements TaxReturnRepositoryPort {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TaxReturnJpaRepository repository;

    @Override
    @Transactional
    public TaxReturn save(TaxReturn taxReturn) {
        TaxReturnEntity entity = repository.findById(taxReturn.getId())
            .orElseGet(TaxReturnEntity::new);
        applyTo(entity, taxReturn);
        repository.save(entity);
        // Return the input instance, NOT a fresh rehydration — the input still
        // carries the registered domain events. Rehydrating would call pullEvents
        // internally and silently drop them, so use cases would publish nothing.
        return taxReturn;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxReturn> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxReturn> findByTinAndTaxTypeAndPeriod(String tin, TaxTypeCode taxType, Period period) {
        return repository.findByTinAndTaxTypeAndPeriodLabel(tin, taxType.value(), period.label())
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<TaxReturn> findByTin(String tin) {
        return repository.findByTinOrderByPeriodEndDesc(tin).stream()
            .map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxReturn> findPriorCompleted(String tin, TaxTypeCode taxType,
                                                  java.time.LocalDate periodStart) {
        return repository.findPriorCompleted(
                tin, taxType.value(), periodStart,
                org.springframework.data.domain.PageRequest.of(0, 1))
            .stream().findFirst().map(this::toDomain);
    }

    private void applyTo(TaxReturnEntity e, TaxReturn t) {
        e.setId(t.getId());
        e.setTin(t.getTaxpayer().tin());
        e.setPartyId(t.getTaxpayer().partyId());
        e.setTaxType(t.getTaxType().value());
        e.setPeriodStart(t.getPeriod().start());
        e.setPeriodEnd(t.getPeriod().end());
        e.setPeriodLabel(t.getPeriod().label());
        e.setPeriodFrequency(t.getPeriod().frequency());
        e.setFilingMethod(t.getMethod());
        e.setRulePackageVersion(t.getRulePackage().version());
        e.setStatus(t.getStatus());
        e.setSchedules(Map.of("schedules", t.getSchedules().stream().map(this::scheduleToMap).toList()));
        e.setIterations(Map.of("iterations", t.getIterations().stream().map(this::iterationToMap).toList()));
        e.setOpenAmendment(t.getOpenAmendment().map(this::amendmentToMap).orElse(null));
        e.setHistoricalAmendments(Map.of("amendments",
            t.getHistoricalAmendments().stream().map(this::amendmentToMap).toList()));
        t.getPrincipalLedgerEntry().ifPresentOrElse(
            le -> { e.setPrincipalLedgerEntryId(le.entryId()); e.setPrincipalLedgerEntryAt(le.postedAt()); },
            () -> { e.setPrincipalLedgerEntryId(null); e.setPrincipalLedgerEntryAt(null); });
        t.getLastRisk().ifPresentOrElse(applyRisk(e), () -> clearRisk(e));
        t.getLastRule().ifPresentOrElse(applyRule(e), () -> clearRule(e));
        if (e.getCreatedAt() == null) e.setCreatedAt(t.getCreatedAt());
        e.setUpdatedAt(t.getUpdatedAt());
    }

    private java.util.function.Consumer<RiskOutcome> applyRisk(TaxReturnEntity e) {
        return r -> {
            e.setLastRiskLevel(r.level());
            e.setLastRiskScore(new BigDecimal(r.score()));
            e.setLastRiskIndicators(Map.of("indicators", r.indicators()));
            e.setLastRiskJustification(r.justification());
        };
    }
    private void clearRisk(TaxReturnEntity e) {
        e.setLastRiskLevel(null); e.setLastRiskScore(null);
        e.setLastRiskIndicators(null); e.setLastRiskJustification(null);
    }
    private java.util.function.Consumer<RuleOutcome> applyRule(TaxReturnEntity e) {
        return r -> {
            e.setLastRulePassed(r.passed());
            e.setLastRuleFindings(Map.of("findings", r.findings().stream().map(this::messageToMap).toList()));
        };
    }
    private void clearRule(TaxReturnEntity e) {
        e.setLastRulePassed(null); e.setLastRuleFindings(null);
    }

    private TaxReturn toDomain(TaxReturnEntity e) {
        Period period = new Period(e.getPeriodStart(), e.getPeriodEnd(), e.getPeriodFrequency());
        RulePackageVersion pkg = new RulePackageVersion(e.getTaxType(), e.getRulePackageVersion());
        TaxpayerReference taxpayer = new TaxpayerReference(e.getTin(), e.getPartyId());

        List<Schedule> schedules = listFromJson(e.getSchedules(), "schedules", this::scheduleFromMap);
        List<CalculationIteration> iterations = listFromJson(
            e.getIterations(), "iterations", this::iterationFromMap);

        Amendment openAmendment = (e.getOpenAmendment() == null
                                   || e.getOpenAmendment().isEmpty()
                                   || e.getOpenAmendment().get("id") == null)
            ? null
            : amendmentFromMap(e.getOpenAmendment());
        List<Amendment> history = listFromJson(
            e.getHistoricalAmendments(), "amendments", this::amendmentFromMap);

        LedgerEntryReference principal = e.getPrincipalLedgerEntryId() == null ? null
            : new LedgerEntryReference(
                e.getPrincipalLedgerEntryId(),
                com.itas.taxfiling.domain.valueobject.AccountCategory.PRINCIPAL,
                e.getPrincipalLedgerEntryAt());

        RiskOutcome risk = e.getLastRiskLevel() == null ? null
            : new RiskOutcome(
                e.getLastRiskLevel(),
                e.getLastRiskScore() == null ? "0" : e.getLastRiskScore().toPlainString(),
                indicatorsFromJson(e.getLastRiskIndicators()),
                e.getLastRiskJustification() == null ? "" : e.getLastRiskJustification());

        RuleOutcome rule = e.getLastRulePassed() == null ? null
            : new RuleOutcome(
                e.getLastRulePassed(),
                ruleFindingsFromJson(e.getLastRuleFindings()));

        return TaxReturn.rehydrate(
            e.getId(), taxpayer, new TaxTypeCode(e.getTaxType()), period, e.getFilingMethod(), pkg,
            e.getStatus(), schedules, iterations, openAmendment, history,
            principal, risk, rule, e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    private <T> List<T> listFromJson(Map<String, Object> json, String key,
                                     java.util.function.Function<Map<String, Object>, T> mapper) {
        if (json == null) return List.of();
        Object raw = json.get(key);
        if (raw == null) return List.of();
        List<Map<String, Object>> rows =
            MAPPER.convertValue(raw, new TypeReference<List<Map<String, Object>>>() {});
        return rows.stream().map(mapper).toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> indicatorsFromJson(Map<String, Object> json) {
        if (json == null) return List.of();
        Object raw = json.get("indicators");
        if (raw == null) return List.of();
        return (List<String>) raw;
    }

    private List<ValidationMessage> ruleFindingsFromJson(Map<String, Object> json) {
        if (json == null) return List.of();
        Object raw = json.get("findings");
        if (raw == null) return List.of();
        List<Map<String, Object>> rows =
            MAPPER.convertValue(raw, new TypeReference<List<Map<String, Object>>>() {});
        return rows.stream().map(this::messageFromMap).toList();
    }

    private Map<String, Object> scheduleToMap(Schedule s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId().toString());
        m.put("kind", s.getKind().name());
        m.put("label", s.getLabel());
        m.put("createdAt", s.getCreatedAt().toString());
        m.put("lineItems", s.getLineItems().stream().map(this::lineItemToMap).toList());
        return m;
    }

    @SuppressWarnings("unchecked")
    private Schedule scheduleFromMap(Map<String, Object> m) {
        UUID id = UUID.fromString((String) m.get("id"));
        ScheduleKind kind = ScheduleKind.valueOf((String) m.get("kind"));
        String label = (String) m.get("label");
        Instant createdAt = Instant.parse((String) m.get("createdAt"));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) m.getOrDefault("lineItems", List.of());
        List<LineItem> items = rows.stream().map(this::lineItemFromMap).toList();
        return Schedule.rehydrate(id, kind, label, items, createdAt);
    }

    private Map<String, Object> lineItemToMap(LineItem li) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", li.getId().toString());
        m.put("entryTypeId", li.getEntryTypeId().toString());
        m.put("entryTypeVersion", li.getEntryTypeVersion());
        m.put("amount", li.getAmount().amount().toPlainString());
        m.put("currency", li.getAmount().currency());
        m.put("source", li.getSource().name());
        m.put("entryData", li.getEntryData().values());
        m.put("validationState", li.getValidationState().name());
        m.put("messages", li.getMessages().stream().map(this::messageToMap).toList());
        m.put("createdAt", li.getCreatedAt().toString());
        m.put("updatedAt", li.getUpdatedAt().toString());
        return m;
    }

    @SuppressWarnings("unchecked")
    private LineItem lineItemFromMap(Map<String, Object> m) {
        UUID id = UUID.fromString((String) m.get("id"));
        UUID entryTypeId = UUID.fromString((String) m.get("entryTypeId"));
        int entryTypeVersion = ((Number) m.get("entryTypeVersion")).intValue();
        Money amount = new Money(new BigDecimal((String) m.get("amount")), (String) m.get("currency"));
        LineItemSource source = LineItemSource.valueOf((String) m.get("source"));
        Map<String, Object> data = (Map<String, Object>) m.getOrDefault("entryData", Map.of());
        EntrySpecificData entryData = new EntrySpecificData(data);
        LineItemValidationState state = LineItemValidationState.valueOf((String) m.get("validationState"));
        List<Map<String, Object>> msgRows = (List<Map<String, Object>>) m.getOrDefault("messages", List.of());
        List<ValidationMessage> messages = msgRows.stream().map(this::messageFromMap).toList();
        Instant createdAt = Instant.parse((String) m.get("createdAt"));
        Instant updatedAt = Instant.parse((String) m.get("updatedAt"));
        return LineItem.rehydrate(id, entryTypeId, entryTypeVersion, amount, source, entryData,
            state, messages, createdAt, updatedAt);
    }

    private Map<String, Object> messageToMap(ValidationMessage v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", v.level().name());
        m.put("severity", v.severity().name());
        m.put("code", v.code());
        m.put("fieldPath", v.fieldPath());
        m.put("message", v.message());
        return m;
    }

    private ValidationMessage messageFromMap(Map<String, Object> m) {
        return new ValidationMessage(
            ValidationLevel.valueOf((String) m.get("level")),
            ValidationMessage.Severity.valueOf((String) m.get("severity")),
            (String) m.get("code"),
            (String) m.get("fieldPath"),
            (String) m.get("message"));
    }

    private Map<String, Object> iterationToMap(CalculationIteration it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", it.getId().toString());
        m.put("sequence", it.getSequence());
        m.put("answers", it.getAnswers().answers());
        m.put("requestedAt", it.getRequestedAt().toString());
        if (it.getOutcome() != null) {
            m.put("outcome", outcomeToMap(it.getOutcome()));
        }
        m.put("accepted", it.isAccepted());
        if (it.getCompletedAt() != null) m.put("completedAt", it.getCompletedAt().toString());
        return m;
    }

    @SuppressWarnings("unchecked")
    private CalculationIteration iterationFromMap(Map<String, Object> m) {
        UUID id = UUID.fromString((String) m.get("id"));
        int seq = ((Number) m.get("sequence")).intValue();
        QuestionnaireAnswers answers = new QuestionnaireAnswers(
            (Map<String, Object>) m.getOrDefault("answers", Map.of()));
        Instant requestedAt = Instant.parse((String) m.get("requestedAt"));
        CalculationOutcome outcome = m.get("outcome") == null ? null
            : outcomeFromMap((Map<String, Object>) m.get("outcome"));
        boolean accepted = Boolean.TRUE.equals(m.get("accepted"));
        Instant completedAt = m.get("completedAt") == null ? null
            : Instant.parse((String) m.get("completedAt"));
        return CalculationIteration.rehydrate(id, seq, answers, requestedAt, outcome, accepted, completedAt);
    }

    private Map<String, Object> outcomeToMap(CalculationOutcome o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("grossTax", o.grossTax().amount().toPlainString());
        m.put("credits", o.credits().amount().toPlainString());
        m.put("netTax", o.netTax().amount().toPlainString());
        m.put("currency", o.netTax().currency());
        m.put("messages", o.messages().stream().map(this::messageToMap).toList());
        m.put("rulePackageTaxType", o.rulePackage().taxTypeCode());
        m.put("rulePackageVersion", o.rulePackage().version());
        return m;
    }

    @SuppressWarnings("unchecked")
    private CalculationOutcome outcomeFromMap(Map<String, Object> m) {
        String currency = (String) m.get("currency");
        Money gross = new Money(new BigDecimal((String) m.get("grossTax")), currency);
        Money credits = new Money(new BigDecimal((String) m.get("credits")), currency);
        Money net = new Money(new BigDecimal((String) m.get("netTax")), currency);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) m.getOrDefault("messages", List.of());
        List<ValidationMessage> messages = new ArrayList<>(rows.stream().map(this::messageFromMap).toList());
        RulePackageVersion pkg = new RulePackageVersion(
            (String) m.get("rulePackageTaxType"), (String) m.get("rulePackageVersion"));
        return new CalculationOutcome(gross, credits, net, messages, pkg);
    }

    private Map<String, Object> amendmentToMap(Amendment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId().toString());
        m.put("reason", a.getReason().name());
        m.put("requestedByActorId", a.getRequestedByActorId());
        m.put("requestedAt", a.getRequestedAt().toString());
        if (a.getDelta() != null) {
            m.put("deltaAmount", a.getDelta().delta().amount().toPlainString());
            m.put("deltaCurrency", a.getDelta().delta().currency());
            m.put("deltaOriginalLedgerEntryId", a.getDelta().originalLedgerEntryId().toString());
        }
        if (a.getPostedDelta() != null) {
            m.put("postedEntryId", a.getPostedDelta().entryId().toString());
            m.put("postedCategory", a.getPostedDelta().category().name());
            m.put("postedAt", a.getPostedDelta().postedAt().toString());
        }
        if (a.getFinalisedAt() != null) m.put("finalisedAt", a.getFinalisedAt().toString());
        return m;
    }

    private Amendment amendmentFromMap(Map<String, Object> m) {
        UUID id = UUID.fromString((String) m.get("id"));
        AmendmentReason reason = AmendmentReason.valueOf((String) m.get("reason"));
        String requestedBy = (String) m.get("requestedByActorId");
        Instant requestedAt = Instant.parse((String) m.get("requestedAt"));
        AmendmentDelta delta = m.get("deltaAmount") == null ? null
            : new AmendmentDelta(
                new Money(new BigDecimal((String) m.get("deltaAmount")), (String) m.get("deltaCurrency")),
                UUID.fromString((String) m.get("deltaOriginalLedgerEntryId")));
        LedgerEntryReference posted = m.get("postedEntryId") == null ? null
            : new LedgerEntryReference(
                UUID.fromString((String) m.get("postedEntryId")),
                com.itas.taxfiling.domain.valueobject.AccountCategory.valueOf((String) m.get("postedCategory")),
                Instant.parse((String) m.get("postedAt")));
        Instant finalisedAt = m.get("finalisedAt") == null ? null
            : Instant.parse((String) m.get("finalisedAt"));
        return Amendment.rehydrate(id, reason, requestedBy, requestedAt, delta, posted, finalisedAt);
    }

    @SuppressWarnings("unused")
    private static RiskLevel rl(String name) { return name == null ? null : RiskLevel.valueOf(name); }
}
