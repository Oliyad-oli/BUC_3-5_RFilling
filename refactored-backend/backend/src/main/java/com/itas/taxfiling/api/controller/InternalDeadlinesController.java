package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.response.UpcomingPeriodResponse;
import com.itas.taxfiling.application.usecase.dashboard.GetUpcomingDeadlinesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Internal system-to-system endpoint (BUC-FIL-045). Phase-2 readiness — used by
 * scheduled-tasks to drive reminder dispatch. mTLS-authenticated via gateway,
 * not in any user-facing role. Phase 1: dormant but contract-stable.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Tag(name = "Internal — Upcoming Deadlines", description = "BUC-FIL-045 — Phase-2 readiness")
public class InternalDeadlinesController {

    private final GetUpcomingDeadlinesUseCase useCase;

    @GetMapping("/upcoming-deadlines")
    @Operation(summary = "Per-tax-type upcoming deadlines for the calendar (BUC-FIL-045)")
    public List<UpcomingPeriodResponse> upcoming(
            @RequestParam(required = false) LocalDate from) {
        LocalDate after = from != null ? from : LocalDate.now();
        return useCase.execute(after).stream().map(UpcomingPeriodResponse::from).toList();
    }
}
