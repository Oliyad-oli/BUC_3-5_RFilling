package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.response.FilingCertificateResponse;
import com.itas.taxfiling.application.usecase.certificate.GetFilingCertificateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Filing certificate read endpoints (BUC-FIL-040). The certificate is
 * generated automatically on TaxReturn completion via the
 * TaxReturnCompletedHandler — there is no POST endpoint.
 */
@RestController
@RequestMapping("/filing-certificates")
@RequiredArgsConstructor
@Tag(name = "Filing Certificates", description = "BUC-FIL-040 — issued automatically on completion")
public class FilingCertificateController {

    private final GetFilingCertificateUseCase getUseCase;

    @GetMapping("/by-tax-return/{taxReturnId}")
    @Operation(summary = "Get the filing certificate for a tax return")
    public FilingCertificateResponse getByTaxReturn(@PathVariable UUID taxReturnId) {
        return FilingCertificateResponse.from(getUseCase.executeByTaxReturnId(taxReturnId));
    }
}
