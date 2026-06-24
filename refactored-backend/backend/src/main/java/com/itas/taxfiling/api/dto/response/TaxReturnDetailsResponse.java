package com.itas.taxfiling.api.dto.response;

import com.itas.taxfiling.domain.model.CalculationIteration;
import com.itas.taxfiling.domain.model.LineItem;
import com.itas.taxfiling.domain.model.Schedule;
import com.itas.taxfiling.domain.model.TaxReturn;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Full read-only projection of a TaxReturn for the resume / details view —
 * carries schedules + their line items + calculation iterations so the portal
 * can re-render the entire wizard state without extra round-trips.
 */
public record TaxReturnDetailsResponse(
    UUID id,
    String tin,
    String taxType,
    String periodLabel,
    String periodStart,
    String periodEnd,
    String periodFrequency,
    String filingMethod,
    String status,
    Instant createdAt,
    Instant updatedAt,
    List<ScheduleDto> schedules,
    List<IterationDto> iterations
) {
    public static TaxReturnDetailsResponse from(TaxReturn t) {
        return new TaxReturnDetailsResponse(
            t.getId(),
            t.getTaxpayer().tin(),
            t.getTaxType().value(),
            t.getPeriod().label(),
            t.getPeriod().start().toString(),
            t.getPeriod().end().toString(),
            t.getPeriod().frequency().name(),
            t.getMethod().name(),
            t.getStatus().name(),
            t.getCreatedAt(),
            t.getUpdatedAt(),
            t.getSchedules().stream().map(ScheduleDto::from).toList(),
            t.getIterations().stream().map(IterationDto::from).toList()
        );
    }

    public record ScheduleDto(UUID id, String kind, String label, List<LineItemDto> lineItems) {
        public static ScheduleDto from(Schedule s) {
            return new ScheduleDto(
                s.getId(), s.getKind().name(), s.getLabel(),
                s.getLineItems().stream().map(LineItemDto::from).toList());
        }
    }

    public record LineItemDto(
        UUID id,
        UUID entryTypeId,
        BigDecimal amount,
        String currency,
        String source,
        Map<String, Object> entryData
    ) {
        public static LineItemDto from(LineItem li) {
            return new LineItemDto(
                li.getId(), li.getEntryTypeId(),
                li.getAmount().amount(), li.getAmount().currency(),
                li.getSource().name(),
                li.getEntryData() == null ? Map.of() : li.getEntryData().values());
        }
    }

    public record IterationDto(
        UUID id, int sequence, boolean accepted,
        BigDecimal grossTax, BigDecimal credits, BigDecimal netTax,
        String currency, String rulePackageVersion
    ) {
        public static IterationDto from(CalculationIteration it) {
            var o = it.getOutcome();
            if (o == null) {
                return new IterationDto(it.getId(), it.getSequence(), it.isAccepted(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "ETB", null);
            }
            return new IterationDto(
                it.getId(), it.getSequence(), it.isAccepted(),
                o.grossTax().amount(), o.credits().amount(), o.netTax().amount(),
                o.netTax().currency(), o.rulePackage().version());
        }
    }
}
