package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.valueobject.Money;

/**
 * Ledger Engine Port
 * 
 * Port interface for ledger posting integration
 */
public interface LedgerEnginePort {
    
    /**
     * Post tax liability to ledger
     * 
     * @return Ledger entry reference ID
     */
    String postToLedger(
        String returnId,
        String tin,
        Money netTax,
        String taxType,
        String period
    );
}
