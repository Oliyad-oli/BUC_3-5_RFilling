package com.itas.taxfiling.unit.application.usecase.officer;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.officer.SubmitOfficerDecisionUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.OfficerClearTaxReturnUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.OfficerConfirmFraudUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RequestAmendmentUseCase;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind;
import com.itas.taxfiling.domain.valueobject.Priority;
import com.itas.taxfiling.domain.valueobject.RiskLevel;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import com.itas.taxfiling.domain.valueobject.RuleOutcome;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests SubmitOfficerDecision orchestration with real inner use cases — Mockito
 * can't reliably mock concrete classes under Java 25, so we wire real
 * collaborators against mocked ports.
 */
class SubmitOfficerDecisionUseCaseTest {

    private OfficerReviewItemRepositoryPort items;
    private TaxReturnRepositoryPort taxReturns;
    private EventPublisherPort eventPublisher;
    private SubmitOfficerDecisionUseCase useCase;

    @BeforeEach
    void setUp() {
        items = mock(OfficerReviewItemRepositoryPort.class);
        taxReturns = mock(TaxReturnRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        var clear = new OfficerClearTaxReturnUseCase(taxReturns, eventPublisher);
        var amend = new RequestAmendmentUseCase(taxReturns, eventPublisher);
        var confirm = new OfficerConfirmFraudUseCase(taxReturns, eventPublisher);
        useCase = new SubmitOfficerDecisionUseCase(items, clear, amend, confirm, eventPublisher);
    }

    private TaxReturn manualReviewReturn() {
        TaxReturn t = TaxReturnTestBuilder.posted();
        t.startPostLedgerValidation();
        t.recordRiskOutcome(new RiskOutcome(RiskLevel.HIGH, "0.95", List.of(), "high-risk-test"));
        t.recordRuleOutcome(new RuleOutcome(true, List.of()));
        t.pullEvents();
        return t;
    }

    private OfficerReviewItem queue(java.util.UUID taxReturnId) {
        OfficerReviewItem item = OfficerReviewItem.queue(
            taxReturnId, OfficerReviewItemKind.FRAUD_FLAGGED, Priority.HIGH,
            "test risk justification", List.of("test-indicator"));
        item.pullEvents();
        when(items.findById(item.getId())).thenReturn(Optional.of(item));
        when(items.save(any(OfficerReviewItem.class))).thenAnswer(inv -> inv.getArgument(0));
        return item;
    }

    @Test
    void CLEAR_transitions_tax_return_to_COMPLETED() {
        TaxReturn t = manualReviewReturn();
        OfficerReviewItem item = queue(t.getId());
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));

        useCase.execute(item.getId(), OfficerReviewDecision.CLEAR, "officer-1", "false positive");

        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.COMPLETED);
    }

    @Test
    void REQUEST_AMENDMENT_opens_amendment_cycle() {
        TaxReturn t = manualReviewReturn();
        OfficerReviewItem item = queue(t.getId());
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));

        useCase.execute(item.getId(), OfficerReviewDecision.REQUEST_AMENDMENT,
            "officer-1", "needs fix");

        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.AMENDMENT_DRAFT);
        assertThat(t.getOpenAmendment()).isPresent();
    }

    @Test
    void CONFIRM_FRAUD_transitions_to_FRAUD_CONFIRMED() {
        TaxReturn t = manualReviewReturn();
        OfficerReviewItem item = queue(t.getId());
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));

        useCase.execute(item.getId(), OfficerReviewDecision.CONFIRM_FRAUD,
            "officer-1", "evidence");

        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.FRAUD_CONFIRMED);
    }
}
