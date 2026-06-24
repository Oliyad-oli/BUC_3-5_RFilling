package com.itas.taxfiling.unit.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.usecase.taxreturn.DraftTaxReturnUseCase;
import com.itas.taxfiling.domain.exception.DomainException;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftTaxReturnUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock RegistrationProjectionPort registration;
    @Mock TaxTypeEnginePort taxTypeEngine;
    @Mock EventPublisherPort eventPublisher;

    @InjectMocks DraftTaxReturnUseCase useCase;

    @Test
    void drafts_a_new_return_and_publishes_drafted_event() {
        when(registration.findByTin("1234567"))
            .thenReturn(Optional.of(new RegistrationProjectionPort.TaxpayerSnapshot(
                "1234567", "party-1", "Acme", "ACTIVE", true)));
        when(taxReturns.findByTinAndTaxTypeAndPeriod(anyString(), any(), any()))
            .thenReturn(Optional.empty());
        when(taxTypeEngine.currentRulePackage(any(), any()))
            .thenReturn(new RulePackageVersion("VAT", "1.0.0"));
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxReturn result = useCase.execute(
            "1234567", new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                com.itas.taxfiling.domain.valueobject.PeriodFrequency.MONTHLY),
            FilingMethod.PORTAL);

        assertThat(result).isNotNull();
        verify(eventPublisher, times(1)).publish(any());
    }

    @Test
    void rejects_unknown_taxpayer() {
        when(registration.findByTin("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute("nope", new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                com.itas.taxfiling.domain.valueobject.PeriodFrequency.MONTHLY),
            FilingMethod.PORTAL))
            .isInstanceOf(DomainException.class);
        verify(taxReturns, never()).save(any());
    }

    @Test
    void rejects_inactive_taxpayer() {
        when(registration.findByTin("1234567"))
            .thenReturn(Optional.of(new RegistrationProjectionPort.TaxpayerSnapshot(
                "1234567", "party-1", "Acme", "SUSPENDED", false)));
        assertThatThrownBy(() -> useCase.execute("1234567", new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                com.itas.taxfiling.domain.valueobject.PeriodFrequency.MONTHLY),
            FilingMethod.PORTAL))
            .isInstanceOf(DomainException.class);
        verify(taxReturns, never()).save(any());
    }

    @Test
    void rejects_duplicate_return_for_same_tin_taxtype_period() {
        when(registration.findByTin("1234567"))
            .thenReturn(Optional.of(new RegistrationProjectionPort.TaxpayerSnapshot(
                "1234567", "party-1", "Acme", "ACTIVE", true)));
        TaxReturn existing = TaxReturn.draft(
            new com.itas.taxfiling.domain.valueobject.TaxpayerReference("1234567", "party-1"),
            new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                com.itas.taxfiling.domain.valueobject.PeriodFrequency.MONTHLY),
            FilingMethod.PORTAL,
            new RulePackageVersion("VAT", "1.0.0"));
        when(taxReturns.findByTinAndTaxTypeAndPeriod(anyString(), any(), any()))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute("1234567", new TaxTypeCode("VAT"),
            new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
                com.itas.taxfiling.domain.valueobject.PeriodFrequency.MONTHLY),
            FilingMethod.PORTAL))
            .isInstanceOf(DomainException.class);
        verify(taxReturns, never()).save(any());
    }
}
