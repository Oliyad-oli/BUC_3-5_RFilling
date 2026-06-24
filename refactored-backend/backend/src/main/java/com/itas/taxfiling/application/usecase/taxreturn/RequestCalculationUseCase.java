package com.itas.taxfiling.application.usecase.taxreturn;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.RuleEnginePort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.CalculationOutcome;
import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * One pass of the calculation loop (Rule 6, BUC-FIL-010..012). Requests the
 * iteration, calls rule-engine synchronously, completes the iteration, and
 * returns it back to the caller for review.
 */
@Service
@RequiredArgsConstructor
public class RequestCalculationUseCase {

    private final TaxReturnRepositoryPort taxReturns;
    private final RuleEnginePort ruleEngine;
    private final EventPublisherPort eventPublisher;

    @Transactional
    public CalculationIteration execute(UUID taxReturnId, QuestionnaireAnswers answers) {
        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        CalculationIteration iter = t.requestCalculation(answers);
        CalculationOutcome outcome = ruleEngine.calculate(t, answers, t.getRulePackage());
        t.completeCalculation(iter.getId(), outcome);
        TaxReturn saved = taxReturns.save(t);
        saved.pullEvents().forEach(eventPublisher::publish);
        return saved.getIterations().stream()
            .filter(i -> i.getId().equals(iter.getId()))
            .findFirst().orElseThrow();
    }
}
