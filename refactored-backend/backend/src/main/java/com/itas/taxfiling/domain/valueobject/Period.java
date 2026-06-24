package com.itas.taxfiling.domain.valueobject;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Filing period window value object — start, end, frequency, and human-readable label.
 */
public record Period(LocalDate start, LocalDate end, PeriodFrequency frequency) {

    public Period {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(frequency, "frequency");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Period end '" + end + "' is before start '" + start + "'");
        }
    }

    public String label() {
        return switch (frequency) {
            case MONTHLY  -> start.getMonth().name() + "-" + start.getYear();
            case QUARTERLY -> "Q" + ((start.getMonthValue() - 1) / 3 + 1) + "-" + start.getYear();
            case ANNUALLY  -> String.valueOf(start.getYear());
        };
    }
}
