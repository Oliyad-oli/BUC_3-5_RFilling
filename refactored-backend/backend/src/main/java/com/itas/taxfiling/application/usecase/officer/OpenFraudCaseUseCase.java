package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.CaseManagementPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Internal — called by FraudConfirmedHandler when an officer confirms fraud
 * at BUC-FIL-051 (decision = CONFIRM_FRAUD). Hands off to case-management
 * (Rule 13 Flow B); case-management owns post-confirmation lifecycle.
 */
@Service
@RequiredArgsConstructor
public class OpenFraudCaseUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final CaseManagementPort caseManagement;

    @Transactional
    public UUID execute(UUID taxReturnId, String officerActorId, String narrative) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        return caseManagement.openFraudCase(taxReturnId, t.getTaxpayer().tin(), narrative, officerActorId);
    }
}
