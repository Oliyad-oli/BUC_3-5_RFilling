package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.response.StartFilingResponse;
import com.itas.taxfiling.application.usecase.obligation.StartFilingFromPeriodUseCase;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for actions on a filing period. Used by the dashboard's Start /
 * Resume button.
 *
 * <p>Lazy-period model: there's no FilingPeriod UUID until the user clicks
 * Start (it's derived from the calendar on the dashboard side). So the
 * endpoint takes {@code (tin, taxTypeCode, periodLabel)} as a body and
 * materializes the row on first use.
 */
@RestController
@RequestMapping("/filing-periods")
@RequiredArgsConstructor
@Tag(name = "Filing Periods")
public class FilingPeriodController {

    private final StartFilingFromPeriodUseCase startUseCase;

    @PostMapping("/start")
    @Operation(summary = "Start (or resume) the wizard for a (TIN × taxType × period) tuple",
               description = "Idempotent — materializes the FilingPeriod row from the calendar, drafts a TaxReturn aligned to its window, and links the two. Re-calling returns the same taxReturnId.")
    public StartFilingResponse start(@Valid @RequestBody StartRequest req) {
        return StartFilingResponse.from(
            startUseCase.execute(req.tin(), new TaxTypeCode(req.taxTypeCode()), req.periodLabel()));
    }

    public record StartRequest(
        @NotBlank String tin,
        @NotBlank String taxTypeCode,
        @NotBlank String periodLabel
    ) {}
}
