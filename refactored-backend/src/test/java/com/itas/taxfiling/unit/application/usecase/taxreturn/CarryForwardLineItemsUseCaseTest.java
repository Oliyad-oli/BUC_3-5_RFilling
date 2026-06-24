package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.ScheduleSpec;
import com.itas.taxfiling.application.usecase.taxreturn.CarryForwardLineItemsUseCase;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarryForwardLineItemsUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock TaxTypeEnginePort taxTypeEngine;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks CarryForwardLineItemsUseCase useCase;

    private LineItemEntryType type() {
        return LineItemEntryType.register(
            "WHT_EMP", new TaxTypeCode("WHT"), ScheduleKind.WITHHOLDING, 1,
            List.of(new EntryFieldDefinition("employeeNid", "Employee NID",
                EntryFieldType.STRING, true, List.of(), null)),
            "admin");
    }

    @Test
    void copies_carry_forward_eligible_items_with_amount_zeroed() {
        TaxReturn prior = TaxReturnTestBuilder.newDraft();
        Schedule priorSchedule = prior.addSchedule(ScheduleKind.WITHHOLDING, "Employee Withholding");
        var typeEntity = type();
        prior.addLineItem(priorSchedule.getId(), typeEntity.getId(), typeEntity.getVersion(),
            Money.of("500.00", "ETB"), LineItemSource.MANUAL,
            new EntrySpecificData(Map.of("employeeNid", "NID-001")));
        prior.pullEvents();

        TaxReturn target = TaxReturnTestBuilder.newDraft();
        target.addSchedule(ScheduleKind.WITHHOLDING, "Employee Withholding");
        target.pullEvents();

        when(taxReturns.findById(target.getId())).thenReturn(Optional.of(target));
        when(taxTypeEngine.schedulesFor(any(), any())).thenReturn(List.of(
            new ScheduleSpec(ScheduleKind.WITHHOLDING, "Employee Withholding", true)));
        when(taxReturns.findPriorCompleted(any(), any(), any())).thenReturn(Optional.of(prior));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        int copied = useCase.execute(target.getId());

        assertThat(copied).isEqualTo(1);
        var newLi = target.getSchedules().get(0).getLineItems().get(0);
        assertThat(newLi.getSource()).isEqualTo(LineItemSource.CARRY_FORWARD);
        assertThat(newLi.getAmount().amount()).isEqualByComparingTo("0.00");
        assertThat(newLi.getEntryData().get("employeeNid")).isEqualTo("NID-001");
    }

    @Test
    void no_op_when_no_prior_period() {
        TaxReturn target = TaxReturnTestBuilder.newDraft();
        target.pullEvents();
        when(taxReturns.findById(target.getId())).thenReturn(Optional.of(target));
        when(taxTypeEngine.schedulesFor(any(), any())).thenReturn(List.of(
            new ScheduleSpec(ScheduleKind.WITHHOLDING, "WHT", true)));
        when(taxReturns.findPriorCompleted(any(), any(), any())).thenReturn(Optional.empty());

        int copied = useCase.execute(target.getId());

        assertThat(copied).isZero();
    }
}
