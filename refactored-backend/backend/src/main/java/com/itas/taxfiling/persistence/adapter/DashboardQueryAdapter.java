package com.itas.taxfiling.persistence.adapter;

import com.itas.taxfiling.application.port.DashboardQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Native-SQL adapter for the lazy-period dashboard JOIN. Produces one row per
 * (obligation × calendar_period) in the window, with status derived live from
 * dates when no FilingPeriod row has been materialized yet.
 *
 * <p>The {@code derive_period_status()} stored function is the single source
 * of truth for date-driven status — both this SQL and the in-memory
 * {@link com.itas.taxfiling.domain.model.FilingPeriod#statusFor} must stay aligned.
 */
@Component
public class DashboardQueryAdapter implements DashboardQueryPort {

    @PersistenceContext
    private EntityManager em;

    private static final String SQL = """
        SELECT
          fp.id                                                         AS filing_period_id,
          cp.tax_type_code                                              AS tax_type_code,
          cp.period_label                                               AS period_label,
          GREATEST(o.effective_from, cp.starts_on)                      AS covers_from,
          cp.ends_on                                                    AS covers_to,
          cp.due_on                                                     AS due_date,
          COALESCE(
            fp.status,
            derive_period_status(GREATEST(o.effective_from, cp.starts_on),
                                 cp.ends_on, cp.due_on, CURRENT_DATE)
          )                                                             AS status,
          (o.effective_from > cp.starts_on)                             AS is_partial,
          fp.tax_return_id                                              AS tax_return_id
        FROM taxpayer_obligation o
        JOIN calendar_period cp
          ON cp.tax_type_code = o.tax_type_code
         AND cp.starts_on   <= COALESCE(o.effective_to, DATE '9999-12-31')
         AND cp.ends_on     >= o.effective_from
        LEFT JOIN filing_period fp
          ON fp.taxpayer_obligation_id = o.id
         AND fp.period_label           = cp.period_label
        WHERE o.tin = :tin
          AND cp.starts_on BETWEEN :from AND :to
        ORDER BY
          CASE COALESCE(
            fp.status,
            derive_period_status(GREATEST(o.effective_from, cp.starts_on),
                                 cp.ends_on, cp.due_on, CURRENT_DATE))
            WHEN 'OVERDUE' THEN 1
            WHEN 'DUE'     THEN 2
            WHEN 'OPEN'    THEN 3
            WHEN 'FUTURE'  THEN 4
            WHEN 'FILED'   THEN 5
            ELSE 6
          END,
          cp.starts_on ASC,
          cp.tax_type_code
        """;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<OutstandingRow> findOutstandingRows(String tin, LocalDate from, LocalDate to) {
        var rows = (List<Object[]>) em.createNativeQuery(SQL)
            .setParameter("tin", tin)
            .setParameter("from", Date.valueOf(from))
            .setParameter("to",   Date.valueOf(to))
            .getResultList();
        return rows.stream().map(r -> new OutstandingRow(
            (UUID)     r[0],
            (String)   r[1],
            (String)   r[2],
            ((Date)    r[3]).toLocalDate(),
            ((Date)    r[4]).toLocalDate(),
            ((Date)    r[5]).toLocalDate(),
            (String)   r[6],
            (Boolean)  r[7],
            (UUID)     r[8]
        )).toList();
    }
}
