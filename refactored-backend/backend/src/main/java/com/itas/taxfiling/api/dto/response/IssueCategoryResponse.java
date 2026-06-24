package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.application.port.CaseManagementPort.IssueCategory;

public record IssueCategoryResponse(String code, String label, boolean active) {
    public static IssueCategoryResponse from(IssueCategory c) {
        return new IssueCategoryResponse(c.code(), c.label(), c.active());
    }
}
