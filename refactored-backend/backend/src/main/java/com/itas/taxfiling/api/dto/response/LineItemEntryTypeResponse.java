package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LineItemEntryTypeResponse(
    UUID id,
    String code,
    String taxType,
    String scheduleKind,
    int version,
    List<FieldDto> fields,
    String status,
    Instant createdAt,
    String createdByActorId
) {
    public static LineItemEntryTypeResponse from(LineItemEntryType t) {
        return new LineItemEntryTypeResponse(
            t.getId(), t.getCode(), t.getTaxType().value(), t.getScheduleKind().name(),
            t.getVersion(),
            t.getFields().stream().map(FieldDto::from).toList(),
            t.getStatus().name(), t.getCreatedAt(), t.getCreatedByActorId());
    }

    public record FieldDto(String key, String label, String type, boolean required,
                           List<String> allowedValues, String validationRegex) {
        public static FieldDto from(EntryFieldDefinition f) {
            return new FieldDto(f.key(), f.label(), f.type().name(), f.required(),
                f.allowedValues(), f.validationRegex());
        }
    }
}
