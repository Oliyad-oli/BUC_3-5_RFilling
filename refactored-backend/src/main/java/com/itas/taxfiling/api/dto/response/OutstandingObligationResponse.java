package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.application.port.DashboardQueryPort.OutstandingRow;
import com.itas.taxfiling.application.usecase.obligation.QueryOutstandingObligationsUseCase;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One row on the portal dashboard. {@code filingPeriodId} and
 * {@code existingTaxReturnId} are null for virtual (un-materialized) periods —
 * the wizard composes its URL from {@code (taxTypeCode, periodLabel)} instead
 * of relying on these IDs.
 */
public record OutstandingObligationResponse(
    UUID filingPeriodId,
    String taxTypeCode,
    String periodLabel,
    LocalDate coversFrom,
    LocalDate coversTo,
    LocalDate dueDate,
    String status,
    boolean isPartial,
    UUID existingTaxReturnId,
    long daysUntilDue,
    long daysLate
) {
    public static OutstandingObligationResponse from(OutstandingRow r, LocalDate today) {
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, r.dueDate());
        long late      = QueryOutstandingObligationsUseCase.daysLate(r, today);
        return new OutstandingObligationResponse(
            r.filingPeriodId(), r.taxTypeCode(), r.periodLabel(),
            r.coversFrom(), r.coversTo(), r.dueDate(),
            r.status(),
            r.isPartial(),
            r.taxReturnId(),
            Math.max(0, daysUntil),
            late);
    }
}
