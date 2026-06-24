package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.application.usecase.dashboard.GetFilingDashboardUseCase.DashboardResult;

import java.util.List;

public record FilingDashboardResponse(
    String tin,
    int totalReturns,
    long openDrafts,
    long awaitingValidation,
    long manualReview,
    long completed,
    List<UpcomingPeriodResponse> upcomingDeadlines
) {
    public static FilingDashboardResponse from(DashboardResult r) {
        return new FilingDashboardResponse(
            r.tin(), r.totalReturns(), r.openDrafts(), r.awaitingValidation(),
            r.manualReview(), r.completed(),
            r.upcomingDeadlines().stream().map(UpcomingPeriodResponse::from).toList());
    }
}
