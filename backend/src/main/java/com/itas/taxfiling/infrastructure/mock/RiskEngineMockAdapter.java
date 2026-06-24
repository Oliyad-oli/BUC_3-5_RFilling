package com.itas.taxfiling.infrastructure.mock;

import com.itas.taxfiling.application.port.RiskEnginePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock Risk Engine Adapter
 * 
 * Simulates risk assessment. In production, this would call a
 * machine-learning or rules-based fraud detection service.
 */
@Slf4j
@Component
public class RiskEngineMockAdapter implements RiskEnginePort {

    @Override
    public RiskAssessment assessRisk(String returnId, String ledgerEntry) {
        log.info("[MOCK-RISK] Assessing risk for return={}, ledger={}", returnId, ledgerEntry);
        // Mock: always low risk
        return new RiskAssessment(
                returnId,
                0.1,
                "LOW",
                List.of(),
                "Mock risk assessment — no anomalies detected"
        );
    }
}
