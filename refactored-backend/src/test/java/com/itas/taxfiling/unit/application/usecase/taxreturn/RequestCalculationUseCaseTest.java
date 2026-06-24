package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.RuleEnginePort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.RequestCalculationUseCase;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestCalculationUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock RuleEnginePort ruleEngine;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks RequestCalculationUseCase useCase;

    @Test
    void runs_iteration_and_persists_outcome() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        t.pullEvents();
        Money net = Money.of("250.00", "ETB");
        CalculationOutcome outcome = new CalculationOutcome(
            net, Money.zero("ETB"), net, List.of(), new RulePackageVersion("VAT", "1.0.0"));

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(ruleEngine.calculate(any(), any(), any())).thenReturn(outcome);
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        CalculationIteration iter = useCase.execute(t.getId(), QuestionnaireAnswers.empty());

        assertThat(iter.getOutcome()).isEqualTo(outcome);
        assertThat(iter.getOutcome().netTax()).isEqualTo(net);
    }
}
