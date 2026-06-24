package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RegisterEntryTypeRequest(
    @NotBlank String code,
    @NotBlank String taxType,
    @NotNull ScheduleKind scheduleKind,
    @NotNull List<FieldDefinitionDto> fields,
    @NotBlank String adminActorId
) {
    public TaxTypeCode toTaxTypeCode() { return new TaxTypeCode(taxType); }
    public List<EntryFieldDefinition> toFieldDefinitions() {
        return fields.stream().map(FieldDefinitionDto::toDomain).toList();
    }

    public record FieldDefinitionDto(
        @NotBlank String key,
        @NotBlank String label,
        @NotNull EntryFieldType type,
        boolean required,
        List<String> allowedValues,
        String validationRegex
    ) {
        public EntryFieldDefinition toDomain() {
            return new EntryFieldDefinition(
                key, label, type, required,
                allowedValues == null ? List.of() : allowedValues,
                validationRegex);
        }
    }
}
