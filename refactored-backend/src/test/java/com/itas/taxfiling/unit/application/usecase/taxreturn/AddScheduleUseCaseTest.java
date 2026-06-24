package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.AddScheduleUseCase;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddScheduleUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks AddScheduleUseCase useCase;

    @Test
    void adds_schedule_and_emits_ScheduleAddedEvent() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        t.pullEvents();
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        Schedule schedule = useCase.execute(t.getId(), ScheduleKind.SALES, "Sales 2026-04");

        assertThat(schedule).isNotNull();
        assertThat(schedule.getKind()).isEqualTo(ScheduleKind.SALES);
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void rejects_unknown_tax_return() {
        UUID id = UUID.randomUUID();
        when(taxReturns.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(id, ScheduleKind.SALES, "X"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
