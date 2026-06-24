package com.itas.taxfiling.unit.application.usecase.dashboard;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.TaxTypeSummary;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.UpcomingPeriod;
import com.itas.taxfiling.application.usecase.dashboard.QueryFilingDashboardUseCase;
import com.itas.taxfiling.application.usecase.dashboard.QueryFilingDashboardUseCase.DashboardResult;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryFilingDashboardUseCaseTest {

    @Mock TaxReturnRepositoryPort taxReturns;
    @Mock TaxTypeEnginePort taxTypeEngine;
    @InjectMocks QueryFilingDashboardUseCase useCase;

    @Test
    void aggregates_counts_and_returns_upcoming_periods() {
        when(taxReturns.findByTin("1234567")).thenReturn(List.of(
            TaxReturnTestBuilder.newDraft(),
            TaxReturnTestBuilder.newDraft(),
            TaxReturnTestBuilder.posted()));
        when(taxTypeEngine.listAvailableTaxTypes()).thenReturn(List.of(
            new TaxTypeSummary("VAT", "Value Added Tax", PeriodFrequency.MONTHLY, true)));
        when(taxTypeEngine.nextPeriod(any(TaxTypeCode.class), any(LocalDate.class)))
            .thenReturn(new UpcomingPeriod(new TaxTypeCode("VAT"),
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 6, 15), PeriodFrequency.MONTHLY));

        DashboardResult result = useCase.execute("1234567");

        assertThat(result.tin()).isEqualTo("1234567");
        assertThat(result.totalReturns()).isEqualTo(3);
        assertThat(result.openDrafts()).isEqualTo(2);
        assertThat(result.upcomingDeadlines()).hasSize(1);
    }
}
