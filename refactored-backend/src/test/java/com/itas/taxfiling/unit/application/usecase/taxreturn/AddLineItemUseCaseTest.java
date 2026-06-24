package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.AddLineItemUseCase;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.LineItemValidationState;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddLineItemUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock LineItemEntryTypeRepositoryPort entryTypes;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks AddLineItemUseCase useCase;

    private LineItemEntryType activeType(boolean requireKey) {
        return LineItemEntryType.register(
            "VAT_SALES", new TaxTypeCode("VAT"), ScheduleKind.SALES, 1,
            List.of(new EntryFieldDefinition("invoiceNumber", "Invoice #",
                EntryFieldType.STRING, requireKey, List.of(), null)),
            "admin");
    }

    private LineItemEntryType retiredType() {
        LineItemEntryType t = activeType(false);
        t.retire("admin");
        return t;
    }

    @Test
    void adds_line_item_when_validation_passes() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        Schedule s = t.addSchedule(ScheduleKind.SALES, "Sales");
        t.pullEvents();
        LineItemEntryType type = activeType(true);

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        LineItem li = useCase.execute(t.getId(), s.getId(), type.getId(),
            Money.of("100.00", "ETB"), LineItemSource.MANUAL,
            new EntrySpecificData(Map.of("invoiceNumber", "INV-1")));

        assertThat(li.getValidationState()).isEqualTo(LineItemValidationState.CLEAN);
    }

    @Test
    void rejects_when_level1_validation_fails() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        Schedule s = t.addSchedule(ScheduleKind.SALES, "Sales");
        t.pullEvents();
        LineItemEntryType type = activeType(true);

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> useCase.execute(t.getId(), s.getId(), type.getId(),
            Money.of("100.00", "ETB"), LineItemSource.MANUAL, EntrySpecificData.empty()))
            .isInstanceOf(DomainException.class);
    }

    @Test
    void rejects_retired_entry_type() {
        TaxReturn t = TaxReturnTestBuilder.newDraft();
        Schedule s = t.addSchedule(ScheduleKind.SALES, "Sales");
        t.pullEvents();
        LineItemEntryType type = retiredType();
        assertThat(type.getStatus()).isEqualTo(EntryTypeStatus.RETIRED);

        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));
        when(entryTypes.findById(type.getId())).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> useCase.execute(t.getId(), s.getId(), type.getId(),
            Money.of("100.00", "ETB"), LineItemSource.MANUAL, EntrySpecificData.empty()))
            .isInstanceOf(DomainException.class);
    }
}
