package com.itas.taxfiling.domain.service;

import com.itas.taxfiling.domain.exception.BusinessRuleViolationException;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;

/**
 * Domain service to centralize business rule validation for TaxReturn operations.
 */
public class TaxReturnValidator {

    public void validateForCalculation(TaxReturn taxReturn) {
        if (taxReturn.getStatus() != TaxReturnStatus.DRAFT && taxReturn.getStatus() != TaxReturnStatus.AMENDMENT_DRAFT) {
            throw new BusinessRuleViolationException("Tax return must be in draft to request calculation.");
        }
        if (taxReturn.getSchedules().isEmpty()) {
            throw new BusinessRuleViolationException("Cannot calculate a tax return with no schedules.");
        }
        // Additional business rules...
    }

    public void validateForSubmission(TaxReturn taxReturn) {
        if (taxReturn.getStatus() != TaxReturnStatus.DRAFT) {
             throw new BusinessRuleViolationException("Only DRAFT returns can be submitted.");
        }
    }
}
