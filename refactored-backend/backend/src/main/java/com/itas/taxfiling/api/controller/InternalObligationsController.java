package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.application.usecase.obligation.GenerateInitialFilingPeriodsUseCase;
import com.itas.taxfiling.application.usecase.obligation.RefreshCalendarProjectionUseCase;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Internal — endpoints invoked by ops + scheduled jobs that aren't part of
 * the public API. In production these are gated by network policy. For the
 * demo this is how we trigger obligation seeding manually since the
 * registration-service event bus integration isn't connected locally.
 *
 * <p>Note: the old {@code run-status-job} endpoint was removed when the lazy
 * filing-period model landed — there are no periods to flip statuses on
 * anymore. Use {@code refresh-calendar} if the local calendar projection is
 * stale.
 */
@RestController
@RequestMapping("/internal/obligations")
@RequiredArgsConstructor
@Tag(name = "Internal — Obligations & Calendar")
public class InternalObligationsController {

    private final GenerateInitialFilingPeriodsUseCase generate;
    private final RefreshCalendarProjectionUseCase refreshCalendar;

    @PostMapping("/seed")
    @Operation(summary = "Seed an obligation for a (TIN × tax type)",
               description = "Idempotent — creates the TaxpayerObligation row if absent. Filing periods themselves are now virtual and derived from the calendar projection at dashboard read time.")
    public SeedResult seed(@RequestBody SeedRequest req) {
        var r = generate.execute(req.tin(), req.partyId(),
            new TaxTypeCode(req.taxTypeCode()), req.effectiveFrom());
        return new SeedResult(r.obligationId(), r.newlyCreated());
    }

    @PostMapping("/refresh-calendar")
    @Operation(summary = "Refresh the local calendar_period projection from tax-type-engine",
               description = "Same as the weekly CalendarRefreshJob; force-run for demos / when the upstream calendar changed.")
    public RefreshResult refreshCalendar() {
        var r = refreshCalendar.execute();
        return new RefreshResult(r.taxTypes(), r.rowsUpserted());
    }

    public record SeedRequest(
        @NotBlank String tin,
        @NotBlank String partyId,
        @NotBlank String taxTypeCode,
        @NotNull LocalDate effectiveFrom
    ) {}

    public record SeedResult(UUID obligationId, boolean newlyCreated) {}

    public record RefreshResult(int taxTypes, int rowsUpserted) {}
}
