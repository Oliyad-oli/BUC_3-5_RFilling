package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;

/**
 * Risk-engine integration (Rule 7, BUC-FIL-020). HIGH outcome triggers in-house
 * officer review (BUC-FIL-022 → BUC-FIL-050).
 */
public interface RiskEnginePort {
    RiskOutcome evaluate(TaxReturn taxReturn);
}
