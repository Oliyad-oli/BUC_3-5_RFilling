package com.itas.taxfiling.application.usecase.dashboard;

import com.itas.taxfiling.application.port.RegistrationProjectionPort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.TaxTypeSummary;
import com.itas.taxfiling.domain.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BUC-FIL-043 — list tax types the taxpayer is registered for, suitable for the
 * portal "File a return" dropdown. Filtered by the taxpayer's active
 * registration (Rule 10 — read from registration projection).
 */
@Service
@RequiredArgsConstructor
public class ListAvailableTaxTypesUseCase {

    private final TaxTypeEnginePort taxTypeEngine;
    private final RegistrationProjectionPort registration;

    @Transactional(readOnly = true)
    public List<TaxTypeSummary> execute(String tin) {
        var snapshot = registration.findByTin(tin)
            .orElseThrow(() -> new DomainException("taxpayer not found: " + tin));
        if (!snapshot.isActive()) {
            throw new DomainException("taxpayer not active: " + tin);
        }
        // Phase 1: registration projection doesn't yet expose per-tax-type registration —
        // return all active tax types from tax-type-engine. Phase 2 will filter by
        // taxpayer's per-tax-type registration status.
        return taxTypeEngine.listAvailableTaxTypes().stream()
            .filter(TaxTypeSummary::active)
            .toList();
    }
}
