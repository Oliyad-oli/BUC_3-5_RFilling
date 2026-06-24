package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.AcceptCalculationUseCase;
import com.itas.taxfiling.domain.event.CalculationAcceptedEvent;
import com.itas.taxfiling.domain.event.PostingToLedgerEvent;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptCalculationUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks AcceptCalculationUseCase useCase;

    @Test
    void emits_PostingToLedgerEvent_with_principal_amount() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        CalculationIteration iter = t.requestCalculation(QuestionnaireAnswers.empty());
        Money net = Money.of("100.00", "ETB");
        t.completeCalculation(iter.getId(), new CalculationOutcome(
            net, Money.zero("ETB"), net, List.of(),
            new RulePackageVersion("VAT", "1.0.0")));
        t.pullEvents();

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxReturn result = useCase.execute(t.getId(), iter.getId());

        assertThat(result.getStatus()).isEqualTo(TaxReturnStatus.ACCEPTED);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeast(2)).publish((com.itas.taxfiling.domain.event.DomainEvent) captor.capture());
        assertThat(captor.getAllValues())
            .anyMatch(e -> e instanceof CalculationAcceptedEvent)
            .anyMatch(e -> e instanceof PostingToLedgerEvent);
    }
}
