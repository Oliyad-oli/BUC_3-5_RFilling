package com.itas.taxfiling.application.usecase.obligation;

import com.itas.taxfiling.application.port.DashboardQueryPort;
import com.itas.taxfiling.application.port.DashboardQueryPort.OutstandingRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Returns the outstanding-filing list for the dashboard. Lazy-period model:
 * derived live from the calendar projection, joined to materialized
 * {@code filing_period} rows when they exist. Excludes FILED.
 *
 * <p>Window: 24 months back, 12 months forward.
 */
@Service
@RequiredArgsConstructor
public class QueryOutstandingObligationsUseCase {

    private static final int LOOKBACK_MONTHS = 24;
    private static final int LOOKAHEAD_MONTHS = 12;

    private final DashboardQueryPort query;

    @Transactional(readOnly = true)
    public List<OutstandingRow> execute(String tin) {
        LocalDate today = LocalDate.now();
        return query.findOutstandingRows(tin,
                today.minusMonths(LOOKBACK_MONTHS),
                today.plusMonths(LOOKAHEAD_MONTHS))
            .stream()
            .filter(r -> !"FILED".equals(r.status()))
            .toList();
    }

    /** Convenience for callers that want days-late. */
    public static long daysLate(OutstandingRow r, LocalDate today) {
        if (!"OVERDUE".equals(r.status())) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(r.dueDate(), today);
    }
}
