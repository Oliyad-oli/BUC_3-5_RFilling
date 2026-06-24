package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.response.FilingPeriodResponse;
import com.itas.taxfiling.application.usecase.filingperiod.QueryFilingPeriodsUseCase;
import com.itas.taxfiling.domain.model.FilingPeriod;
import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/filing-periods")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FilingPeriodController {

    private final QueryFilingPeriodsUseCase queryFilingPeriodsUseCase;

    @GetMapping
    public ResponseEntity<List<FilingPeriodResponse>> list(
            @RequestParam String tin,
            @RequestParam(required = false) String status
    ) {
        FilingPeriodStatus filingStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filingStatus = FilingPeriodStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                // Invalid status — return all
            }
        }

        List<FilingPeriod> periods = queryFilingPeriodsUseCase.execute(tin, filingStatus);
        return ResponseEntity.ok(periods.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    private FilingPeriodResponse toResponse(FilingPeriod p) {
        return new FilingPeriodResponse(
                p.getId(),
                p.getTin(),
                p.getTaxType() != null ? p.getTaxType().name() : null,
                p.getPeriod() != null ? p.getPeriod().getStartDate() : null,
                p.getPeriod() != null ? p.getPeriod().getEndDate() : null,
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getDueDate(),
                p.getFiledDate(),
                p.getReturnId(),
                p.getCreatedAt()
        );
    }
}
