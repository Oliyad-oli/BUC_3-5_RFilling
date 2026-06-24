package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.port.RiskEnginePort;
import com.itas.taxfiling.application.port.RuleEnginePort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.RecordRiskOutcomeUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RecordRuleOutcomeUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.StartPostLedgerValidationUseCase;
import com.itas.taxfiling.domain.event.PostedToLedgerEvent;
import com.itas.taxfiling.domain.event.PostLedgerValidationStartedEvent;
import com.itas.taxfiling.domain.model.TaxReturn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Implements the post-ledger validation fan-out (Rule 7, BUC-FIL-020/021):
 *   - PostedToLedgerEvent → start validation (transitions UNDER_VALIDATION).
 *   - PostLedgerValidationStartedEvent → call risk + rule arms in parallel and
 *     record their outcomes back on the aggregate.
 *
 * Both arms run inside the post-commit phase so they can each open new
 * transactions without competing for the original write lock.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostLedgerValidationHandler {

    private final StartPostLedgerValidationUseCase startValidation;
    private final RiskEnginePort riskEngine;
    private final RuleEnginePort ruleEngine;
    private final RecordRiskOutcomeUseCase recordRisk;
    private final RecordRuleOutcomeUseCase recordRule;
    private final TaxReturnRepositoryPort taxReturns;

    @EventListener
    public void onPostedToLedger(PostedToLedgerEvent event) {
        startValidation.execute(event.taxReturnId());
    }

    @EventListener
    public void onValidationStarted(PostLedgerValidationStartedEvent event) {
        TaxReturn t = taxReturns.findById(event.taxReturnId()).orElse(null);
        if (t == null) {
            log.warn("validation fan-out: tax return {} not found", event.taxReturnId());
            return;
        }
        try {
            recordRisk.execute(t.getId(), riskEngine.evaluate(t));
        } catch (Exception ex) {
            log.error("risk-engine arm failed for taxReturnId={}", t.getId(), ex);
        }
        try {
            recordRule.execute(t.getId(), ruleEngine.postLedgerCheck(t, t.getRulePackage()));
        } catch (Exception ex) {
            log.error("rule-engine arm failed for taxReturnId={}", t.getId(), ex);
        }
    }
}
