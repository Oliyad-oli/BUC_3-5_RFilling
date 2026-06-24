package com.itas.taxfiling.application.usecase.dashboard;

import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.TaxTypeSummary;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.UpcomingPeriod;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * BUC-FIL-044 — portal dashboard. Aggregates per-taxpayer counts and upcoming
 * deadlines across all tax types the taxpayer is registered for.
 */
@Service
@RequiredArgsConstructor
public class QueryFilingDashboardUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final TaxTypeEnginePort taxTypeEngine;

    @Transactional(readOnly = true)
    public DashboardResult execute(String tin) {
        List<TaxReturn> all = taxReturns.findByTin(tin);

        Map<TaxReturnStatus, Long> countsByStatus = all.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                TaxReturn::getStatus, java.util.stream.Collectors.counting()));

        long openDrafts = countsByStatus.getOrDefault(TaxReturnStatus.DRAFT, 0L)
            + countsByStatus.getOrDefault(TaxReturnStatus.AMENDMENT_DRAFT, 0L);
        long awaitingValidation = countsByStatus.getOrDefault(TaxReturnStatus.UNDER_VALIDATION, 0L);
        long manualReview = countsByStatus.getOrDefault(TaxReturnStatus.MANUAL_REVIEW, 0L);
        long completed = countsByStatus.getOrDefault(TaxReturnStatus.COMPLETED, 0L);

        List<UpcomingPeriod> upcoming = taxTypeEngine.listAvailableTaxTypes().stream()
            .filter(TaxTypeSummary::active)
            .map(t -> taxTypeEngine.nextPeriod(new TaxTypeCode(t.code()), LocalDate.now()))
            .toList();

        return new DashboardResult(
            tin, all.size(),
            openDrafts, awaitingValidation, manualReview, completed,
            upcoming);
    }

    public record DashboardResult(
        String tin,
        int totalReturns,
        long openDrafts,
        long awaitingValidation,
        long manualReview,
        long completed,
        List<UpcomingPeriod> upcomingDeadlines
    ) {}
}
