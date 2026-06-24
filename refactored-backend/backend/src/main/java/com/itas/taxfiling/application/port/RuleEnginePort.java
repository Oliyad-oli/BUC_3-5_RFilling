package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import com.itas.taxfiling.domain.valueobject.RuleOutcome;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;

/**
 * Rule-engine integration. Two distinct invocations:
 *   - calculate: the iterative tax computation loop (Rule 6, BUC-FIL-010..013).
 *   - postLedgerCheck: post-ledger rule arm running in parallel with risk-engine
 *     (Rule 7, BUC-FIL-021).
 */
public interface RuleEnginePort {

    CalculationOutcome calculate(TaxReturn taxReturn, QuestionnaireAnswers answers,
                                 RulePackageVersion rulePackage);

    RuleOutcome postLedgerCheck(TaxReturn taxReturn, RulePackageVersion rulePackage);
}
