package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.application.port.CaseManagementPort.CaseHandle;

import java.util.UUID;

public record CaseHandleResponse(UUID caseId, String caseReferenceNumber, String trackingUrl) {
    public static CaseHandleResponse from(CaseHandle h) {
        return new CaseHandleResponse(h.caseId(), h.caseReferenceNumber(), h.trackingUrl());
    }
}
