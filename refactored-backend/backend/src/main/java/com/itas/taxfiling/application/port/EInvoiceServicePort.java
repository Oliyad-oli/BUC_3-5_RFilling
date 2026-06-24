package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * E-invoice-service integration. Filing pulls e-invoice line items into the
 * SALES / PURCHASES schedules during return drafting (BUC-FIL-005 — auto-pull).
 *
 * <p>The pull is always scoped to the filing period — no separate from/to
 * date range. Schedule kind drives the counterparty role (customer vs.
 * supplier) so the mock can populate entry-type-specific fields with the
 * right keys.
 */
public interface EInvoiceServicePort {

    /**
     * Pull e-invoices for {@code tin} issued within {@code period}. The
     * returned lines carry entry-type-shaped data in {@link EInvoiceLine#extra}
     * so the use case can spread them into the line item's entryData without
     * any per-key translation.
     */
    List<EInvoiceLine> pullForTaxpayer(String tin, Period period, ScheduleKind kind);

    record EInvoiceLine(
        String externalInvoiceId,
        LocalDate issueDate,
        Money amount,
        Money taxAmount,
        String counterpartyTin,
        Map<String, Object> extra
    ) {
        public LineItemSource source() { return LineItemSource.E_INVOICE; }
    }
}
