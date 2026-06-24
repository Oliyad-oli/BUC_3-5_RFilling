package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.QuestionnaireAnswers;

import java.util.Map;

public record RequestCalculationRequest(Map<String, Object> answers) {
    public QuestionnaireAnswers toAnswers() {
        return new QuestionnaireAnswers(answers == null ? Map.of() : answers);
    }
}
