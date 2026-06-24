package com.itas.taxfiling.domain.valueobject;

import java.util.Map;
import java.util.Objects;

/** Questionnaire answers supplied by the taxpayer for the calculation loop. */
public record QuestionnaireAnswers(Map<String, Object> answers) {

    public QuestionnaireAnswers {
        Objects.requireNonNull(answers, "answers");
        answers = Map.copyOf(answers);
    }

    public static QuestionnaireAnswers empty() {
        return new QuestionnaireAnswers(Map.of());
    }

    public static QuestionnaireAnswers of(Map<String, Object> answers) {
        return new QuestionnaireAnswers(answers);
    }
}
