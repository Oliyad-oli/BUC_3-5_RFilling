package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.ScheduleSpec;
import com.itas.taxfiling.application.usecase.taxreturn.OpenFilingPeriodUseCase;
import com.itas.taxfiling.domain.event.TaxReturnPeriodOpenedEvent;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenFilingPeriodUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock RegistrationProjectionPort registration;
    @Mock TaxTypeEnginePort taxTypeEngine;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks OpenFilingPeriodUseCase useCase;

    @Test
    void opens_with_pre_populated_schedules_and_emits_PeriodOpenedEvent() {
        when(taxReturns.findByTinAndTaxTypeAndPeriod(any(), any(), any())).thenReturn(Optional.empty());
        when(registration.findByTin("1234567")).thenReturn(Optional.of(
            new RegistrationProjectionPort.TaxpayerSnapshot("1234567", "party-1", "Acme", "ACTIVE", true)));
        when(taxTypeEngine.currentRulePackage(any(), any()))
            .thenReturn(new RulePackageVersion("VAT", "1.0.0"));
        when(taxTypeEngine.schedulesFor(any(), any())).thenReturn(List.of(
            new ScheduleSpec(ScheduleKind.SALES, "Sales", false),
            new ScheduleSpec(ScheduleKind.PURCHASES, "Purchases", false)));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxReturn result = useCase.execute("1234567", new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), PeriodFrequency.MONTHLY));

        assertThat(result.getSchedules()).hasSize(2);
        verify(eventPublisher, atLeastOnce()).publish(any(TaxReturnPeriodOpenedEvent.class));
    }

    @Test
    void idempotent_when_return_already_exists() {
        TaxReturn existing = com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder.newDraft();
        when(taxReturns.findByTinAndTaxTypeAndPeriod(any(), any(), any()))
            .thenReturn(Optional.of(existing));

        TaxReturn result = useCase.execute("1234567", new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), PeriodFrequency.MONTHLY));

        assertThat(result).isSameAs(existing);
    }

    @Test
    void rejects_inactive_taxpayer() {
        when(taxReturns.findByTinAndTaxTypeAndPeriod(any(), any(), any())).thenReturn(Optional.empty());
        when(registration.findByTin("nope")).thenReturn(Optional.of(
            new RegistrationProjectionPort.TaxpayerSnapshot("nope", "party-x", "X", "SUSPENDED", false)));
        assertThatThrownBy(() -> useCase.execute("nope", new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), PeriodFrequency.MONTHLY)))
            .isInstanceOf(DomainException.class);
    }
}
