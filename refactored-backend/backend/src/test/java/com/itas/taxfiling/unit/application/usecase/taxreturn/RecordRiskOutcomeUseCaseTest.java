package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.RecordRiskOutcomeUseCase;
import com.itas.taxfiling.domain.event.TaxReturnFraudFlaggedEvent;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.RiskLevel;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordRiskOutcomeUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks RecordRiskOutcomeUseCase useCase;

    @Test
    void HIGH_risk_emits_TaxReturnFraudFlaggedEvent() {
        TaxReturn t = TaxReturnTestBuilder.posted();
        t.startPostLedgerValidation();
        t.pullEvents();

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(t.getId(), new RiskOutcome(RiskLevel.HIGH, "0.95",
            List.of("indicator-1", "indicator-2", "indicator-3", "indicator-4"),
            "Multiple anomalies vs. taxpayer history; manual review required."));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publish((com.itas.taxfiling.domain.event.DomainEvent) captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof TaxReturnFraudFlaggedEvent);
    }
}
