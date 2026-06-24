package com.itas.taxfiling.application.port;

import java.util.List;

/**
 * Risk Engine Port
 * 
 * Port interface for risk assessment integration
 */
public interface RiskEnginePort {
    
    /**
     * Assess fraud risk for a return
     */
    RiskAssessment assessRisk(String returnId, String ledgerEntry);
    
    /**
     * Risk assessment result
     */
    record RiskAssessment(
        String returnId,
        double riskScore,  // 0.0 to 1.0
        String riskLevel,  // HIGH, MEDIUM, LOW
        List<String> indicators,
        String details
    ) {
        public boolean isHighRisk() {
            return "HIGH".equals(riskLevel);
        }
    }
}
