package com.itas.taxfiling.infrastructure.mock;

import com.itas.taxfiling.application.port.LedgerEnginePort;
import com.itas.taxfiling.domain.valueobject.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock Ledger Engine Adapter
 * 
 * Simulates posting to the general ledger. In production, this would
 * integrate with a real ledger/accounting system.
 */
@Slf4j
@Component
public class LedgerEngineMockAdapter implements LedgerEnginePort {

    @Override
    public String postToLedger(String returnId, String tin, Money netTax, String taxType, String period) {
        String ledgerRef = "LEDGER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MOCK-LEDGER] Posted to ledger: return={}, tin={}, amount={}, ref={}",
                returnId, tin, netTax, ledgerRef);
        return ledgerRef;
    }
}
