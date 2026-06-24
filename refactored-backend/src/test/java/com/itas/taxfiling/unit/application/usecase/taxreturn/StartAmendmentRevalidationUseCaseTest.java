package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.StartAmendmentRevalidationUseCase;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.AmendmentDelta;
import com.itas.taxfiling.domain.valueobject.AmendmentReason;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RiskLevel;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import com.itas.taxfiling.domain.valueobject.RuleOutcome;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartAmendmentRevalidationUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks StartAmendmentRevalidationUseCase useCase;

    @Test
    void transitions_AMENDMENT_POSTED_to_UNDER_VALIDATION() {
        TaxReturn t = amendmentPosted();
        t.pullEvents();
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(t.getId());

        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.UNDER_VALIDATION);
        assertThat(t.getLastRisk()).isEmpty();
        assertThat(t.getLastRule()).isEmpty();
    }

    private TaxReturn amendmentPosted() {
        TaxReturn t = TaxReturnTestBuilder.posted();
        t.startPostLedgerValidation();
        t.recordRiskOutcome(new RiskOutcome(RiskLevel.HIGH, "0.95", List.of(), "high-risk-test"));
        t.recordRuleOutcome(new RuleOutcome(true, List.of()));
        t.requestAmendment(AmendmentReason.OFFICER_REQUESTED, "officer-1");
        var iter = t.requestCalculation(QuestionnaireAnswers.empty());
        Money delta = Money.of("50.00", "ETB");
        t.completeCalculation(iter.getId(), new CalculationOutcome(
            delta, Money.zero("ETB"), delta, List.of(),
            new RulePackageVersion("VAT", "1.0.0")));
        t.acceptAmendmentDelta(new AmendmentDelta(
            delta, t.getPrincipalLedgerEntry().orElseThrow().entryId()));
        t.recordAmendmentPosted(new LedgerEntryReference(
            UUID.randomUUID(), AccountCategory.PRINCIPAL, Instant.now()));
        return t;
    }
}
