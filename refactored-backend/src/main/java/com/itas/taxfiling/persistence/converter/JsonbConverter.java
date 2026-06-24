package com.itas.taxfiling.persistence.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic JSONB converter for free-form Map columns. The Postgres column type
 * is jsonb; Hibernate sees String here but the dialect is configured to store
 * it as jsonb in the migration.
 */
@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        // Return null (SQL NULL) for null input — NOT "{}". Otherwise nullable JSONB
        // columns (e.g. open_amendment_json) end up storing an empty object that the
        // toDomain mapper then mistakes for a populated value and tries to parse.
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise jsonb", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new LinkedHashMap<>();
        try {
            return MAPPER.readValue(dbData, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialise jsonb", e);
        }
    }
}
