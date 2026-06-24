package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.response.FilingDashboardResponse;
import com.itas.taxfiling.api.dto.response.OutstandingObligationResponse;
import com.itas.taxfiling.api.dto.response.TaxTypeSummaryResponse;
import com.itas.taxfiling.application.usecase.dashboard.GetFilingDashboardUseCase;
import com.itas.taxfiling.application.usecase.dashboard.ListAvailableTaxTypesUseCase;
import com.itas.taxfiling.application.usecase.obligation.GetOutstandingObligationsUseCase;
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
 * Portal read endpoints for the filing dashboard. No auth annotations —
 * gateway-owned. The gateway extracts TIN from the JWT and forwards as the
 * X-Actor-Id header; the tin query parameter here is the resolved subject.
 */
@RestController
@RequestMapping("/portal/filing")
@RequiredArgsConstructor
@Tag(name = "Portal Filing Dashboard", description = "BUC-FIL-043/044 — taxpayer dashboard reads")
public class PortalDashboardController {

    private final ListAvailableTaxTypesUseCase listUseCase;
    private final GetFilingDashboardUseCase dashboardUseCase;
    private final GetOutstandingObligationsUseCase outstandingUseCase;

    @GetMapping("/available-tax-types")
    @Operation(summary = "List tax types available for filing (BUC-FIL-043)")
    public List<TaxTypeSummaryResponse> availableTaxTypes(@RequestParam String tin) {
        return listUseCase.execute(tin).stream().map(TaxTypeSummaryResponse::from).toList();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Filing dashboard with deadlines (BUC-FIL-044)")
    public FilingDashboardResponse dashboard(@RequestParam String tin) {
        return FilingDashboardResponse.from(dashboardUseCase.execute(tin));
    }

    @GetMapping("/outstanding-obligations")
    @Operation(summary = "Outstanding filing obligations for the dashboard cards (Layer 2)")
    public List<OutstandingObligationResponse> outstanding(@RequestParam String tin) {
        LocalDate today = LocalDate.now();
        return outstandingUseCase.execute(tin).stream()
            .map(p -> OutstandingObligationResponse.from(p, today))
            .toList();
    }
}
