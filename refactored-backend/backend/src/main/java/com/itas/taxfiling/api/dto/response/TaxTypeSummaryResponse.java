package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.application.port.TaxTypeEnginePort.TaxTypeSummary;

public record TaxTypeSummaryResponse(String code, String label, String frequency, boolean active) {
    public static TaxTypeSummaryResponse from(TaxTypeSummary s) {
        return new TaxTypeSummaryResponse(s.code(), s.label(), s.frequency().name(), s.active());
    }
}
