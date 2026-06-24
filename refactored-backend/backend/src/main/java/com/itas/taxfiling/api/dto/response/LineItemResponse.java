package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.LineItem;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record LineItemResponse(
    UUID id,
    UUID entryTypeId,
    int entryTypeVersion,
    BigDecimal amount,
    String currency,
    String source,
    Map<String, Object> entryData,
    String validationState,
    java.util.List<ValidationMessageDto> messages
) {
    public static LineItemResponse from(LineItem li) {
        return new LineItemResponse(
            li.getId(), li.getEntryTypeId(), li.getEntryTypeVersion(),
            li.getAmount().amount(), li.getAmount().currency(),
            li.getSource().name(), li.getEntryData().values(),
            li.getValidationState().name(),
            li.getMessages().stream().map(ValidationMessageDto::from).toList());
    }

    public record ValidationMessageDto(String level, String severity, String code,
                                       String fieldPath, String message) {
        public static ValidationMessageDto from(com.itas.taxfiling.domain.valueobject.ValidationMessage v) {
            return new ValidationMessageDto(v.level().name(), v.severity().name(),
                v.code(), v.fieldPath(), v.message());
        }
    }
}
