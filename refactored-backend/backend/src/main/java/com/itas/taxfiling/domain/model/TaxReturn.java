package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.aggregate.AggregateRoot;
import com.itas.taxfiling.domain.event.*;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.exception.InvalidStateTransitionException;
import com.itas.taxfiling.domain.valueobject.*;

import java.time.Instant;
import java.util.*;

/**
 * TaxReturn aggregate root — the spine of BUC-003 and BUC-005.
 *
 * <p>Lifecycle:
 * DRAFT → CALCULATING ⇄ DRAFT (loop, Rule 6) → ACCEPTED → POSTED_TO_LEDGER →
 * UNDER_VALIDATION → (COMPLETED | MANUAL_REVIEW)
 *
 * <p>From COMPLETED an amendment can open:
 * COMPLETED → AMENDMENT_DRAFT → AMENDMENT_CALCULATING → AMENDMENT_DRAFT (loop) →
 * AMENDMENT_ACCEPTED → AMENDMENT_POSTED → UNDER_VALIDATION → COMPLETED
 *
 * <p>Officer decisions from MANUAL_REVIEW:
 * MANUAL_REVIEW → COMPLETED (CLEAR) | FRAUD_CONFIRMED (CONFIRM_FRAUD)
 *
 * <p>Invariants:
 * - State transitions are guarded — invalid transitions throw InvalidStateTransitionException.
 * - Schedules and line items can only be mutated in DRAFT or AMENDMENT_DRAFT.
 * - At most one Amendment is open at a time.
 * - At most one CalculationIteration is ACCEPTED.
 */
public class TaxReturn extends AggregateRoot {

    private final UUID id;
    private final TaxpayerReference taxpayer;
    private final TaxTypeCode taxType;
    private final Period period;
    private final FilingMethod method;
    private RulePackageVersion rulePackage;
    private TaxReturnStatus status;
    private final List<Schedule> schedules;
    private final List<CalculationIteration> iterations;
    private Amendment openAmendment;
    private final List<Amendment> historicalAmendments;
    private LedgerEntryReference principalLedgerEntry;
    private RiskOutcome lastRisk;
    private RuleOutcome lastRule;
    private final Instant createdAt;
    private Instant updatedAt;
    private Long version;

    private TaxReturn(UUID id, TaxpayerReference taxpayer, TaxTypeCode taxType, Period period,
                      FilingMethod method, RulePackageVersion rulePackage) {
        this.id = id;
        this.taxpayer = taxpayer;
        this.taxType = taxType;
        this.period = period;
        this.method = method;
        this.rulePackage = rulePackage;
        this.status = TaxReturnStatus.DRAFT;
        this.schedules = new ArrayList<>();
        this.iterations = new ArrayList<>();
        this.historicalAmendments = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    public static TaxReturn draft(TaxpayerReference taxpayer, TaxTypeCode taxType, Period period,
                                  FilingMethod method, RulePackageVersion rulePackage) {
        Objects.requireNonNull(taxpayer, "taxpayer");
        Objects.requireNonNull(taxType, "taxType");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(rulePackage, "rulePackage");

        TaxReturn tr = new TaxReturn(UUID.randomUUID(), taxpayer, taxType, period, method, rulePackage);
        tr.registerEvent(new TaxReturnDraftedEvent(
            UUID.randomUUID(), Instant.now(), tr.id, taxpayer, taxType, period, method, rulePackage));
        return tr;
    }

    // ── Schedule & Line Item mutations ────────────────────────────────────────

    public Schedule addSchedule(ScheduleKind kind, String label) {
        requireMutable();
        Schedule schedule = Schedule.open(kind, label);
        schedules.add(schedule);
        touch();
        registerEvent(new ScheduleAddedEvent(UUID.randomUUID(), Instant.now(), id, schedule.getId(), kind));
        return schedule;
    }

    public LineItem addLineItem(UUID scheduleId, UUID entryTypeId, int entryTypeVersion,
                                Money amount, LineItemSource source, EntrySpecificData entryData) {
        requireMutable();
        Schedule schedule = scheduleOrThrow(scheduleId);
        LineItem li = schedule.addLineItem(entryTypeId, entryTypeVersion, amount, source, entryData);
        touch();
        registerEvent(new LineItemAddedEvent(
            UUID.randomUUID(), Instant.now(), id, scheduleId, li.getId(), entryTypeId, amount, source));
        return li;
    }

    public void updateLineItem(UUID scheduleId, UUID lineItemId, Money amount, EntrySpecificData entryData) {
        requireMutable();
        Schedule schedule = scheduleOrThrow(scheduleId);
        LineItem li = schedule.findLineItem(lineItemId)
            .orElseThrow(() -> new LineItemNotFoundException(lineItemId));
        li.replaceAmount(amount);
        li.replaceEntryData(entryData);
        li.clearFindings();
        touch();
    }

    public void removeLineItem(UUID scheduleId, UUID lineItemId) {
        requireMutable();
        Schedule schedule = scheduleOrThrow(scheduleId);
        schedule.removeLineItem(lineItemId);
        touch();
    }

    public void flagLineItem(UUID scheduleId, UUID lineItemId, List<ValidationMessage> findings) {
        requireMutable();
        Schedule schedule = scheduleOrThrow(scheduleId);
        LineItem li = schedule.findLineItem(lineItemId)
            .orElseThrow(() -> new LineItemNotFoundException(lineItemId));
        li.flag(findings);
        touch();
        registerEvent(new LineItemFlaggedEvent(UUID.randomUUID(), Instant.now(), id, lineItemId, findings));
    }

    // ── Calculation loop (Rule 6) ─────────────────────────────────────────────

    public CalculationIteration requestCalculation(QuestionnaireAnswers answers) {
        if (status != TaxReturnStatus.DRAFT && status != TaxReturnStatus.AMENDMENT_DRAFT) {
            throw new InvalidStateTransitionException(status, "requestCalculation");
        }
        boolean amendment = (status == TaxReturnStatus.AMENDMENT_DRAFT);
        status = amendment ? TaxReturnStatus.AMENDMENT_CALCULATING : TaxReturnStatus.CALCULATING;
        int seq = iterations.size() + 1;
        CalculationIteration iteration = CalculationIteration.request(seq, answers);
        iterations.add(iteration);
        touch();
        registerEvent(new CalculationRequestedEvent(UUID.randomUUID(), Instant.now(), id, seq));
        return iteration;
    }

    public void completeCalculation(UUID iterationId, CalculationOutcome outcome) {
        if (status != TaxReturnStatus.CALCULATING && status != TaxReturnStatus.AMENDMENT_CALCULATING) {
            throw new InvalidStateTransitionException(status, "completeCalculation");
        }
        boolean amendment = (status == TaxReturnStatus.AMENDMENT_CALCULATING);
        CalculationIteration it = iterationOrThrow(iterationId);
        it.complete(outcome);
        status = amendment ? TaxReturnStatus.AMENDMENT_DRAFT : TaxReturnStatus.DRAFT;
        touch();
        if (amendment) {
            registerEvent(new AmendmentCalculatedEvent(
                UUID.randomUUID(), Instant.now(), id, it.getSequence(), outcome));
        } else {
            registerEvent(new CalculationCompletedEvent(
                UUID.randomUUID(), Instant.now(), id, it.getSequence(), outcome));
        }
    }

    public void acceptCalculation(UUID iterationId) {
        if (status != TaxReturnStatus.DRAFT) {
            throw new InvalidStateTransitionException(status, "acceptCalculation");
        }
        CalculationIteration it = iterationOrThrow(iterationId);
        if (it.getOutcome() == null) throw new DomainException("iteration not yet completed");
        it.accept();
        status = TaxReturnStatus.ACCEPTED;
        touch();
        registerEvent(new CalculationAcceptedEvent(
            UUID.randomUUID(), Instant.now(), id, it.getSequence(), it.getOutcome().netTax()));
    }

    // ── Ledger posting (BUC-005 entry) ───────────────────────────────────────

    public void enqueueLedgerPosting() {
        if (status != TaxReturnStatus.ACCEPTED) {
            throw new InvalidStateTransitionException(status, "enqueueLedgerPosting");
        }
        Money net = lastAcceptedIteration().getOutcome().netTax();
        if (net.isZero()) {
            // Zero-net-tax: short-circuit to COMPLETED — no ledger entry needed.
            status = TaxReturnStatus.COMPLETED;
            touch();
            registerEvent(new TaxReturnCompletedEvent(UUID.randomUUID(), Instant.now(), id));
            return;
        }
        registerEvent(new PostingToLedgerEvent(
            UUID.randomUUID(), Instant.now(), id,
            taxpayer.tin(), taxType, period, net, AccountCategory.PRINCIPAL));
    }

    public void recordLedgerPosted(LedgerEntryReference reference) {
        if (status != TaxReturnStatus.ACCEPTED) {
            throw new InvalidStateTransitionException(status, "recordLedgerPosted");
        }
        this.principalLedgerEntry = Objects.requireNonNull(reference, "reference");
        status = TaxReturnStatus.POSTED_TO_LEDGER;
        touch();
        registerEvent(new PostedToLedgerEvent(UUID.randomUUID(), Instant.now(), id, reference));
    }

    // ── Post-ledger validation (BUC-005) ─────────────────────────────────────

    public void startPostLedgerValidation() {
        if (status != TaxReturnStatus.POSTED_TO_LEDGER) {
            throw new InvalidStateTransitionException(status, "startPostLedgerValidation");
        }
        status = TaxReturnStatus.UNDER_VALIDATION;
        touch();
        registerEvent(new PostLedgerValidationStartedEvent(UUID.randomUUID(), Instant.now(), id));
    }

    public void recordRiskOutcome(RiskOutcome outcome) {
        if (status != TaxReturnStatus.UNDER_VALIDATION) {
            throw new InvalidStateTransitionException(status, "recordRiskOutcome");
        }
        this.lastRisk = Objects.requireNonNull(outcome, "outcome");
        touch();
        registerEvent(new RiskOutcomeReceivedEvent(UUID.randomUUID(), Instant.now(), id, outcome));
        if (outcome.level() == RiskLevel.HIGH) {
            registerEvent(new TaxReturnFraudFlaggedEvent(UUID.randomUUID(), Instant.now(), id, outcome));
        }
        maybeFinaliseValidation();
    }

    public void recordRuleOutcome(RuleOutcome outcome) {
        if (status != TaxReturnStatus.UNDER_VALIDATION) {
            throw new InvalidStateTransitionException(status, "recordRuleOutcome");
        }
        this.lastRule = Objects.requireNonNull(outcome, "outcome");
        touch();
        registerEvent(new RuleOutcomeReceivedEvent(UUID.randomUUID(), Instant.now(), id, outcome));
        maybeFinaliseValidation();
    }

    private void maybeFinaliseValidation() {
        if (lastRisk == null || lastRule == null) return;
        if (lastRisk.level() == RiskLevel.HIGH || !lastRule.passed()) {
            status = TaxReturnStatus.MANUAL_REVIEW;
        } else {
            if (openAmendment != null && openAmendment.getPostedDelta() != null) {
                historicalAmendments.add(openAmendment);
                openAmendment = null;
            }
            status = TaxReturnStatus.COMPLETED;
            registerEvent(new TaxReturnCompletedEvent(UUID.randomUUID(), Instant.now(), id));
        }
        touch();
    }

    // ── Officer review (BUC-005) ──────────────────────────────────────────────

    public void officerClear() {
        if (status != TaxReturnStatus.MANUAL_REVIEW) {
            throw new InvalidStateTransitionException(status, "officerClear");
        }
        if (openAmendment != null && openAmendment.getPostedDelta() != null) {
            historicalAmendments.add(openAmendment);
            openAmendment = null;
        }
        status = TaxReturnStatus.COMPLETED;
        touch();
        registerEvent(new TaxReturnCompletedEvent(UUID.randomUUID(), Instant.now(), id));
    }

    public void officerConfirmFraud(String officerActorId, String narrative) {
        if (status != TaxReturnStatus.MANUAL_REVIEW) {
            throw new InvalidStateTransitionException(status, "officerConfirmFraud");
        }
        status = TaxReturnStatus.FRAUD_CONFIRMED;
        touch();
        registerEvent(new FraudConfirmedEvent(UUID.randomUUID(), Instant.now(), id, officerActorId, narrative));
    }

    // ── Amendment (BUC-003) ───────────────────────────────────────────────────

    public void requestAmendment(AmendmentReason reason, String requestedByActorId) {
        if (status != TaxReturnStatus.COMPLETED && status != TaxReturnStatus.MANUAL_REVIEW) {
            throw new InvalidStateTransitionException(status, "requestAmendment");
        }
        if (openAmendment != null) throw new DomainException("an amendment is already open");
        openAmendment = Amendment.open(reason, requestedByActorId);
        status = TaxReturnStatus.AMENDMENT_DRAFT;
        touch();
        registerEvent(new AmendmentRequestedEvent(
            UUID.randomUUID(), Instant.now(), id, reason, requestedByActorId));
    }

    public void acceptAmendmentDelta(AmendmentDelta delta) {
        if (status != TaxReturnStatus.AMENDMENT_DRAFT) {
            throw new InvalidStateTransitionException(status, "acceptAmendmentDelta");
        }
        if (openAmendment == null) throw new DomainException("no open amendment");
        openAmendment.recordDelta(delta);
        status = TaxReturnStatus.AMENDMENT_ACCEPTED;
        touch();
        registerEvent(new AmendmentAcceptedEvent(UUID.randomUUID(), Instant.now(), id, delta));
    }

    public void recordAmendmentPosted(LedgerEntryReference reference) {
        if (status != TaxReturnStatus.AMENDMENT_ACCEPTED) {
            throw new InvalidStateTransitionException(status, "recordAmendmentPosted");
        }
        if (openAmendment == null) throw new DomainException("no open amendment");
        openAmendment.recordPosted(reference);
        status = TaxReturnStatus.AMENDMENT_POSTED;
        touch();
        registerEvent(new AmendmentDeltaPostedEvent(
            UUID.randomUUID(), Instant.now(), id, openAmendment.getDelta(), reference));
    }

    public void startAmendmentRevalidation() {
        if (status != TaxReturnStatus.AMENDMENT_POSTED) {
            throw new InvalidStateTransitionException(status, "startAmendmentRevalidation");
        }
        lastRisk = null;
        lastRule = null;
        status = TaxReturnStatus.UNDER_VALIDATION;
        touch();
        registerEvent(new PostLedgerValidationStartedEvent(UUID.randomUUID(), Instant.now(), id));
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    private void requireMutable() {
        if (status != TaxReturnStatus.DRAFT && status != TaxReturnStatus.AMENDMENT_DRAFT) {
            throw new InvalidStateTransitionException(status, "mutate schedules/line-items");
        }
    }

    private Schedule scheduleOrThrow(UUID scheduleId) {
        return schedules.stream()
            .filter(s -> s.getId().equals(scheduleId))
            .findFirst()
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    private CalculationIteration iterationOrThrow(UUID iterationId) {
        return iterations.stream()
            .filter(it -> it.getId().equals(iterationId))
            .findFirst()
            .orElseThrow(() -> new IterationNotFoundException(iterationId));
    }

    private CalculationIteration lastAcceptedIteration() {
        return iterations.stream()
            .filter(CalculationIteration::isAccepted)
            .reduce((a, b) -> b)
            .orElseThrow(() -> new DomainException("no accepted iteration"));
    }

    private void touch() { this.updatedAt = Instant.now(); }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @Override public UUID getId() { return id; }
    public TaxpayerReference getTaxpayer() { return taxpayer; }
    public TaxTypeCode getTaxType() { return taxType; }
    public Period getPeriod() { return period; }
    public FilingMethod getMethod() { return method; }
    public RulePackageVersion getRulePackage() { return rulePackage; }
    public TaxReturnStatus getStatus() { return status; }
    public List<Schedule> getSchedules() { return List.copyOf(schedules); }
    public List<CalculationIteration> getIterations() { return List.copyOf(iterations); }
    public Optional<Amendment> getOpenAmendment() { return Optional.ofNullable(openAmendment); }
    public List<Amendment> getHistoricalAmendments() { return List.copyOf(historicalAmendments); }
    public Optional<LedgerEntryReference> getPrincipalLedgerEntry() { return Optional.ofNullable(principalLedgerEntry); }
    public Optional<RiskOutcome> getLastRisk() { return Optional.ofNullable(lastRisk); }
    public Optional<RuleOutcome> getLastRule() { return Optional.ofNullable(lastRule); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    // ── Rehydration (persistence adapter only) ────────────────────────────────

    public static TaxReturn rehydrate(UUID id, TaxpayerReference taxpayer, TaxTypeCode taxType,
                                      Period period, FilingMethod method, RulePackageVersion rulePackage,
                                      TaxReturnStatus status, List<Schedule> schedules,
                                      List<CalculationIteration> iterations,
                                      Amendment openAmendment, List<Amendment> historicalAmendments,
                                      LedgerEntryReference principalLedgerEntry,
                                      RiskOutcome lastRisk, RuleOutcome lastRule,
                                      Instant createdAt, Instant updatedAt, Long version) {
        TaxReturn tr = new TaxReturn(id, taxpayer, taxType, period, method, rulePackage);
        tr.status = status;
        tr.schedules.addAll(schedules);
        tr.iterations.addAll(iterations);
        tr.openAmendment = openAmendment;
        tr.historicalAmendments.addAll(historicalAmendments);
        tr.principalLedgerEntry = principalLedgerEntry;
        tr.lastRisk = lastRisk;
        tr.lastRule = lastRule;
        tr.updatedAt = updatedAt;
        tr.version = version;
        tr.pullEvents();
        return tr;
    }

    // ── Typed inner exceptions ─────────────────────────────────────────────────

    public static class ScheduleNotFoundException extends DomainException {
        public ScheduleNotFoundException(UUID id) { super("Schedule not found: " + id); }
    }

    public static class LineItemNotFoundException extends DomainException {
        public LineItemNotFoundException(UUID id) { super("LineItem not found: " + id); }
    }

    public static class IterationNotFoundException extends DomainException {
        public IterationNotFoundException(UUID id) { super("CalculationIteration not found: " + id); }
    }
}
