package com.itas.taxfiling.infrastructure.mock;

import com.itas.taxfiling.application.port.EInvoiceServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock E-Invoice Service Adapter
 * 
 * Simulates fetching e-invoice data for pre-population.
 */
@Slf4j
@Component
public class EInvoiceServiceMockAdapter implements EInvoiceServicePort {

    @Override
    public List<Object> fetchEInvoicesForTaxpayer(String tin, String periodCode) {
        log.info("[MOCK-EINVOICE] Fetching e-invoices for tin={}, period={}", tin, periodCode);
        // Return empty list — no pre-populated invoices in mock mode
        return List.of();
    }
}
