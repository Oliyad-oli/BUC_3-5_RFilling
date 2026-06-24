package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.RecordLedgerPostedUseCase;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordLedgerPostedUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks RecordLedgerPostedUseCase useCase;

    @Test
    void transitions_to_POSTED_TO_LEDGER() {
        TaxReturn t = TaxReturnTestBuilder.accepted();
        t.pullEvents();
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        ArgumentCaptor<TaxReturn> savedArg = ArgumentCaptor.forClass(TaxReturn.class);
        when(taxReturns.save(savedArg.capture())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(t.getId(), new LedgerEntryReference(
            UUID.randomUUID(), AccountCategory.PRINCIPAL, Instant.now()));

        assertThat(savedArg.getValue().getStatus()).isEqualTo(TaxReturnStatus.POSTED_TO_LEDGER);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publish(org.mockito.ArgumentMatchers.any());
    }
}
