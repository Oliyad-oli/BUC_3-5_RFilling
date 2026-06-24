package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/** A single validation message from the Rule 12 validation cascade. */
public record ValidationMessage(ValidationLevel level, String code, String message) {

    public ValidationMessage {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
