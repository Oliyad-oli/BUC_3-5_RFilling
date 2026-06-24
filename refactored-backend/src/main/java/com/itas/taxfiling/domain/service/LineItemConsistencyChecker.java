package com.itas.taxfiling.domain.service;

import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.ValidationLevel;
import com.itas.taxfiling.domain.valueobject.ValidationMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Runs Levels 1 + 2 of the validation cascade (Rule 12) against a single
 * LineItem given its LineItemEntryType definition.
 *
 * Level 1 — field shape:   required, type, regex, allowed values
 * Level 2 — cross-field:   simple intra-entry checks (e.g. required-when)
 *
 * Levels 3–5 (schedule-wide, return-wide, post-ledger risk/rule) live
 * elsewhere — post-ledger in the parallel risk + rule arms (Rule 7).
 *
 * Required by BUC-EFR-003 (daily return entry validation) and
 * BUC-EFR-005 (monthly return entry validation).
 */
public class LineItemConsistencyChecker {

    public List<ValidationMessage> check(LineItem item, LineItemEntryType definition) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(definition, "definition");
        List<ValidationMessage> findings = new ArrayList<>();
        for (EntryFieldDefinition field : definition.getFields()) {
            checkField(field, item, findings);
        }
        return findings;
    }

    private void checkField(EntryFieldDefinition field, LineItem item,
                             List<ValidationMessage> out) {
        Object value = item.getEntryData().get(field.key());

        if (field.required() && (value == null || (value instanceof String s && s.isBlank()))) {
            out.add(new ValidationMessage(
                ValidationLevel.LEVEL_1_FIELD,
                ValidationMessage.Severity.ERROR,
                "FIELD_REQUIRED",
                field.key(),
                "field is required"));
            return;
        }
        if (value == null) return;

        if (!matchesType(field.type(), value)) {
            out.add(new ValidationMessage(
                ValidationLevel.LEVEL_1_FIELD,
                ValidationMessage.Severity.ERROR,
                "FIELD_TYPE",
                field.key(),
                "expected " + field.type() + " but got " + value.getClass().getSimpleName()));
            return;
        }

        if (!field.allowedValues().isEmpty()
                && !field.allowedValues().contains(String.valueOf(value))) {
            out.add(new ValidationMessage(
                ValidationLevel.LEVEL_1_FIELD,
                ValidationMessage.Severity.ERROR,
                "FIELD_NOT_ALLOWED",
                field.key(),
                "value not in allowed set"));
        }

        if (field.validationRegex() != null && !field.validationRegex().isBlank()
                && !Pattern.matches(field.validationRegex(), String.valueOf(value))) {
            out.add(new ValidationMessage(
                ValidationLevel.LEVEL_1_FIELD,
                ValidationMessage.Severity.ERROR,
                "FIELD_REGEX",
                field.key(),
                "value does not match expected pattern"));
        }
    }

    private boolean matchesType(EntryFieldType type, Object value) {
        return switch (type) {
            case STRING, ENUM, REFERENCE -> value instanceof String;
            case INTEGER -> value instanceof Integer || value instanceof Long;
            case DECIMAL -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case DATE    -> value instanceof String s && s.matches("\\d{4}-\\d{2}-\\d{2}");
        };
    }
}
