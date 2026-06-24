package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.PeriodFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pushes the FilingProcessed webhook to payment-service after a TaxReturn
 * reaches COMPLETED. payment-service consumes this to mint the user-facing
 * invoice (BUC-PAY-001).
 *
 * <p>Phase-1 wiring — direct HTTP. Phase-2 will move to event-bus dispatch.
 * Failures here log + return; the filing's own COMPLETED state is unaffected.
 */
public interface PaymentInvoicePushPort {

    void pushFilingProcessed(
            String tin,
            String taxType,
            String periodLabel,
            LocalDate periodStart,
            LocalDate periodEnd,
            PeriodFrequency periodFrequency,
            BigDecimal principalAmount,
            String currency);
}
