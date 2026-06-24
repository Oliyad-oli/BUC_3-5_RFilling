package com.itas.taxfiling.unit.domain.model;

import com.itas.taxfiling.domain.event.TaxReturnDraftedEvent;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AmendmentReason;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RiskLevel;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import com.itas.taxfiling.domain.valueobject.RuleOutcome;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.domain.valueobject.TaxpayerReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxReturnTest {

    private TaxReturn newDraft() {
        return TaxReturn.draft(
            new TaxpayerReference("1234567", "party-1"),
            new TaxTypeCode("VAT"),
            Period.monthly(YearMonth.of(2026, 4)),
            FilingMethod.PORTAL,
            new RulePackageVersion("VAT", "1.0.0"));
    }

    @Test
    void draft_starts_in_DRAFT_and_emits_TaxReturnDraftedEvent() {
        TaxReturn t = newDraft();
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
        var events = t.pullEvents();
        assertThat(events).hasSize(1).first().isInstanceOf(TaxReturnDraftedEvent.class);
    }

    @Test
    void calculation_loop_then_accept_transitions_to_ACCEPTED() {
        TaxReturn t = newDraft();
        t.pullEvents();

        var iter = t.requestCalculation(QuestionnaireAnswers.empty());
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.CALCULATING);

        Money zero = Money.zero("ETB");
        Money net  = Money.of("100.00", "ETB");
        t.completeCalculation(iter.getId(), new CalculationOutcome(net, zero, net,
            List.of(), new RulePackageVersion("VAT", "1.0.0")));
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);

        t.acceptCalculation(iter.getId());
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.ACCEPTED);
    }

    @Test
    void cannot_accept_calculation_before_completion() {
        TaxReturn t = newDraft();
        var iter = t.requestCalculation(QuestionnaireAnswers.empty());
        // CALCULATING — accept not allowed
        assertThatThrownBy(() -> t.acceptCalculation(iter.getId()))
            .isInstanceOf(TaxReturn.InvalidTransitionException.class);
    }

    @Test
    void low_risk_and_passed_rule_finalises_to_COMPLETED() {
        TaxReturn t = postedReturn();
        t.startPostLedgerValidation();
        t.recordRiskOutcome(new RiskOutcome(RiskLevel.LOW, "0.05", List.of(), "no risk"));
        t.recordRuleOutcome(new RuleOutcome(true, List.of()));
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.COMPLETED);
    }

    @Test
    void high_risk_routes_to_MANUAL_REVIEW_and_emits_fraud_flagged_event() {
        TaxReturn t = postedReturn();
        t.startPostLedgerValidation();
        t.recordRiskOutcome(new RiskOutcome(RiskLevel.HIGH, "0.95",
            List.of("indicator-1", "indicator-2"), "Two anomalies vs. taxpayer history."));
        t.recordRuleOutcome(new RuleOutcome(true, List.of()));
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.MANUAL_REVIEW);
        assertThat(t.pullEvents())
            .anyMatch(e -> e.getClass().getSimpleName().equals("TaxReturnFraudFlaggedEvent"));
    }

    @Test
    void officer_request_amendment_then_post_delta_then_revalidate_clears_to_COMPLETED() {
        TaxReturn t = postedReturn();
        t.startPostLedgerValidation();
        t.recordRiskOutcome(new RiskOutcome(RiskLevel.HIGH, "0.95", List.of(), "high-risk-test"));
        t.recordRuleOutcome(new RuleOutcome(true, List.of()));
        t.requestAmendment(AmendmentReason.OFFICER_REQUESTED, "officer-1");
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.AMENDMENT_DRAFT);

        var iter = t.requestCalculation(QuestionnaireAnswers.empty());
        Money delta = Money.of("50.00", "ETB");
        t.completeCalculation(iter.getId(), new CalculationOutcome(delta, Money.zero("ETB"), delta,
            List.of(), new RulePackageVersion("VAT", "1.0.0")));
        t.acceptAmendmentDelta(new com.itas.taxfiling.domain.valueobject.AmendmentDelta(
            delta, t.getPrincipalLedgerEntry().orElseThrow().entryId()));
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.AMENDMENT_ACCEPTED);

        t.recordAmendmentPosted(new LedgerEntryReference(
            UUID.randomUUID(), AccountCategory.PRINCIPAL, Instant.now()));
        // Per BUC-FIL-033 the return stays in AMENDMENT_POSTED until the
        // AmendmentRevalidationHandler kicks off re-validation.
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.AMENDMENT_POSTED);

        t.startAmendmentRevalidation();
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.UNDER_VALIDATION);

        t.recordRiskOutcome(new RiskOutcome(RiskLevel.LOW, "0.05", List.of(), "no risk"));
        t.recordRuleOutcome(new RuleOutcome(true, List.of()));
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.COMPLETED);
        // Open amendment moved to history after re-validation passes.
        assertThat(t.getOpenAmendment()).isEmpty();
        assertThat(t.getHistoricalAmendments()).hasSize(1);
    }

    private TaxReturn postedReturn() {
        TaxReturn t = newDraft();
        var iter = t.requestCalculation(QuestionnaireAnswers.empty());
        Money net = Money.of("100.00", "ETB");
        t.completeCalculation(iter.getId(), new CalculationOutcome(net, Money.zero("ETB"), net,
            List.of(), new RulePackageVersion("VAT", "1.0.0")));
        t.acceptCalculation(iter.getId());
        t.recordLedgerPosted(new LedgerEntryReference(
            UUID.randomUUID(), AccountCategory.PRINCIPAL, Instant.now()));
        t.pullEvents();
        return t;
    }
}
