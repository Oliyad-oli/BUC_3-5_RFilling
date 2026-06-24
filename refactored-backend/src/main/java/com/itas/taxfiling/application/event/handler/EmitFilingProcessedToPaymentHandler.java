package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.port.PaymentInvoicePushPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.event.TaxReturnCompletedEvent;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * BUC-PAY-001 trigger: when a TaxReturn reaches COMPLETED, push the
 * FilingProcessed webhook to payment-service so it can mint the user-facing
 * invoice. Per the refined Phase 1 scope this happens AFTER risk + rule
 * validation pass — so payment never raises an invoice for a return that
 * later turned out to be flagged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmitFilingProcessedToPaymentHandler {

    private final TaxReturnRepositoryPort taxReturns;
    private final PaymentInvoicePushPort paymentPush;

    @EventListener
    public void onCompleted(TaxReturnCompletedEvent event) {
        TaxReturn t = taxReturns.findById(event.taxReturnId()).orElse(null);
        if (t == null) {
            log.warn("TaxReturnCompleted received for unknown taxReturnId={} — skip payment push",
                    event.taxReturnId());
            return;
        }
        // The accepted iteration carries the assessed netTax we bill the
        // taxpayer for. There must be exactly one (BUC-FIL-013 invariant).
        CalculationIteration accepted = t.getIterations().stream()
                .filter(CalculationIteration::isAccepted)
                .findFirst()
                .orElse(null);
        if (accepted == null || accepted.getOutcome() == null) {
            log.warn("TaxReturn {} completed without an accepted iteration — skip payment push",
                    t.getId());
            return;
        }
        Money assessed = accepted.getOutcome().netTax();
        BigDecimal amount = assessed.amount() == null ? BigDecimal.ZERO : assessed.amount();
        String currency = assessed.currency() == null ? "ETB" : assessed.currency();
        paymentPush.pushFilingProcessed(
                t.getTaxpayer().tin(),
                t.getTaxType().value(),
                t.getPeriod().label(),
                t.getPeriod().start(),
                t.getPeriod().end(),
                t.getPeriod().frequency(),
                amount,
                currency);
    }
}
