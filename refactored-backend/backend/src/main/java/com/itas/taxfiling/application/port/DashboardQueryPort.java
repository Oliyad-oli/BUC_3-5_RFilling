package com.itas.taxfiling.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-side query port for the dashboard. The outstanding-obligations list
 * is a JOIN across the obligation × global-calendar × materialized-period —
 * it doesn't fit any single aggregate repository, so it lives in its own
 * port backed by a native SQL adapter.
 */
public interface DashboardQueryPort {

    List<OutstandingRow> findOutstandingRows(String tin, LocalDate from, LocalDate to);

    /**
     * One dashboard row. {@code filingPeriodId} is null when the period is
     * virtual (no row has been materialized yet); {@code taxReturnId} is null
     * when no return has been drafted.
     */
    record OutstandingRow(
        UUID filingPeriodId,
        String taxTypeCode,
        String periodLabel,
        LocalDate coversFrom,
        LocalDate coversTo,
        LocalDate dueDate,
        String status,
        boolean isPartial,
        UUID taxReturnId
    ) {}
}
