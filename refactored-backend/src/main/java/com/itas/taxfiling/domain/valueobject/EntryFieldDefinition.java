package com.itas.taxfiling.domain.valueobject;

import java.util.List;
import java.util.Objects;

/**
 * Definition of a single field within a {@code LineItemEntryType}.
 * Drives front-end field rendering and server-side entry validation.
 */
public record EntryFieldDefinition(
        String key,
        String label,
        EntryFieldType type,
        boolean required,
        List<String> allowedValues,
        String regex
) {
    public EntryFieldDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(type, "type");
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
    }
}
