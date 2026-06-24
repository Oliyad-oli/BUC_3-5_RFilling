package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.application.port.TaxTypeEnginePort.UpcomingPeriod;

import java.time.LocalDate;

public record UpcomingPeriodResponse(
    String taxType,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDate dueDate,
    String frequency
) {
    public static UpcomingPeriodResponse from(UpcomingPeriod p) {
        return new UpcomingPeriodResponse(
            p.taxType().value(), p.periodStart(), p.periodEnd(), p.dueDate(), p.frequency().name());
    }
}
