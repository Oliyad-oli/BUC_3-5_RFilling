package com.itas.taxfiling.domain.service;

import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;

import java.util.List;
import java.util.Objects;

/**
 * Derives a dynamic questionnaire form from a versioned rule package fetched
 * from the tax-type-engine SDK (Rule 9). The compiled form is what the portal
 * renders for the taxpayer at filing time and what feeds back into the
 * rule-engine calculation iteration (Rule 6).
 *
 * Stateless domain service — no dependencies on infrastructure. The actual
 * rule package fetch + cache live behind TaxTypeEnginePort.
 *
 * Required by BUC-EFR-003 (Register Daily Return) and BUC-EFR-005 (Submit Monthly Return).
 */
public class FilingFormCompiler {

    public CompiledForm compile(RulePackageVersion rulePackage,
                                List<EntryFieldDefinition> fields) {
        Objects.requireNonNull(rulePackage, "rulePackage");
        Objects.requireNonNull(fields, "fields");
        return new CompiledForm(rulePackage, List.copyOf(fields));
    }

    public record CompiledForm(RulePackageVersion rulePackage, List<EntryFieldDefinition> fields) {

        public boolean hasField(String key) {
            return fields.stream().anyMatch(f -> f.key().equals(key));
        }

        public EntryFieldDefinition fieldOrThrow(String key) {
            return fields.stream()
                .filter(f -> f.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown field: " + key));
        }

        public List<EntryFieldDefinition> requiredFields() {
            return fields.stream().filter(EntryFieldDefinition::required).toList();
        }

        public List<EntryFieldDefinition> fieldsOfType(EntryFieldType type) {
            return fields.stream().filter(f -> f.type() == type).toList();
        }
    }
}
