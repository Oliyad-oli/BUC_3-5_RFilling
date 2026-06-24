package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.Schedule;

import java.util.List;
import java.util.UUID;

public record ScheduleResponse(
    UUID id,
    String kind,
    String label,
    List<LineItemResponse> lineItems
) {
    public static ScheduleResponse from(Schedule s) {
        return new ScheduleResponse(
            s.getId(), s.getKind().name(), s.getLabel(),
            s.getLineItems().stream().map(LineItemResponse::from).toList());
    }
}
