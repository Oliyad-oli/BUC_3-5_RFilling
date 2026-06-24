package com.itas.taxfiling.engineadapter.risk;

import com.itas.taxfiling.application.port.RiskEnginePort;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.RiskLevel;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import com.itas.taxfiling.engineadapter.shared.BaseEngineAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** [MOCK] risk-engine adapter. Replace by adding the risk-engine client library. */
@Slf4j
@Component
public class RiskEngineMockAdapter extends BaseEngineAdapter implements RiskEnginePort {

    public RiskEngineMockAdapter() { super("risk-engine"); }

    @Override
    @CircuitBreaker(name = "risk-engine", fallbackMethod = "evaluateFallback")
    @Retry(name = "risk-engine")
    public RiskOutcome evaluate(TaxReturn taxReturn) {
        log.info("[MOCK] risk-engine evaluate taxReturn={}", taxReturn.getId());
        return new RiskOutcome(
            RiskLevel.LOW,
            "0.05",
            List.of(),
            "No risk indicators detected for this return. Within tolerance against historical patterns for the taxpayer.");
    }

    private RiskOutcome evaluateFallback(TaxReturn taxReturn, Exception ex) {
        throw wrapException("evaluate", ex);
    }
}
