package com.itas.taxfiling.domain.valueobject;

import java.util.Map;
import java.util.Objects;

/**
 * Tax-type-specific data for a line item, stored as JSONB (Rule 5).
 * The shape is defined by the {@link EntryFieldDefinition}s of the entry type.
 */
public record EntrySpecificData(Map<String, Object> fields) {

    public EntrySpecificData {
        Objects.requireNonNull(fields, "fields");
        fields = Map.copyOf(fields);
    }

    public static EntrySpecificData of(Map<String, Object> fields) {
        return new EntrySpecificData(fields);
    }

    public static EntrySpecificData empty() {
        return new EntrySpecificData(Map.of());
    }

    public Object get(String key) { return fields.get(key); }
    public boolean isEmpty() { return fields.isEmpty(); }
}
