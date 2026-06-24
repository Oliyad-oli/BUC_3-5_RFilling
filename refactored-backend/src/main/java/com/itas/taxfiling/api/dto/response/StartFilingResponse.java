package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.application.usecase.obligation.StartFilingFromPeriodUseCase;

import java.util.UUID;

public record StartFilingResponse(UUID filingPeriodId, UUID taxReturnId, boolean newlyCreated) {
    public static StartFilingResponse from(StartFilingFromPeriodUseCase.Result r) {
        return new StartFilingResponse(r.filingPeriodId(), r.taxReturnId(), r.newlyCreated());
    }
}
