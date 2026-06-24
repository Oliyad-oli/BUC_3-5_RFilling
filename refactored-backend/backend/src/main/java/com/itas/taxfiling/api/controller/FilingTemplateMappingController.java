package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.application.usecase.templates.FilingTemplateMappingUseCases;
import com.itas.taxfiling.domain.model.FilingTemplateMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Back-office CRUD for the filing-service ledger template mapping.
 * No auth annotations — security is gateway-owned.
 */
@RestController
@RequestMapping("/back-office/filing-templates")
@RequiredArgsConstructor
@Tag(name = "Filing Template Mapping",
     description = "BUC-FIL-CONFIG-LDG-01 — admin assigns ledger templateRefs per tax type")
public class FilingTemplateMappingController {

    private final FilingTemplateMappingUseCases useCases;

    @GetMapping
    @Operation(summary = "List all tax-type → filing template mappings")
    public List<FilingTemplateResponse> list() {
        return useCases.list().stream().map(FilingTemplateResponse::from).toList();
    }

    @GetMapping("/{taxTypeCode}")
    @Operation(summary = "Get the mapping for one tax type")
    public FilingTemplateResponse get(@PathVariable String taxTypeCode) {
        return FilingTemplateResponse.from(useCases.get(taxTypeCode));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a mapping for a tax type")
    public FilingTemplateResponse create(@Valid @RequestBody UpsertRequest req) {
        return FilingTemplateResponse.from(useCases.create(
            req.taxTypeCode(),
            req.filingTemplateRef(),
            req.lateFilingPenaltyTemplateRef(),
            req.actorId()));
    }

    @PutMapping("/{taxTypeCode}")
    @Operation(summary = "Replace the mapping for a tax type")
    public FilingTemplateResponse update(
            @PathVariable String taxTypeCode,
            @Valid @RequestBody UpdateRequest req) {
        return FilingTemplateResponse.from(useCases.update(
            taxTypeCode,
            req.filingTemplateRef(),
            req.lateFilingPenaltyTemplateRef(),
            req.actorId()));
    }

    @DeleteMapping("/{taxTypeCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a mapping (filing-service can no longer post for this tax type)")
    public void delete(@PathVariable String taxTypeCode) {
        useCases.delete(taxTypeCode);
    }

    public record UpsertRequest(
        @NotBlank String taxTypeCode,
        @NotBlank String filingTemplateRef,
        String lateFilingPenaltyTemplateRef,
        @NotBlank String actorId
    ) {}

    public record UpdateRequest(
        @NotBlank String filingTemplateRef,
        String lateFilingPenaltyTemplateRef,
        @NotBlank String actorId
    ) {}

    public record FilingTemplateResponse(
        String taxTypeCode,
        String filingTemplateRef,
        String lateFilingPenaltyTemplateRef,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt
    ) {
        public static FilingTemplateResponse from(FilingTemplateMapping m) {
            return new FilingTemplateResponse(
                m.getTaxTypeCode(),
                m.getFilingTemplateRef(),
                m.getLateFilingPenaltyTemplateRef(),
                m.getCreatedBy(),
                m.getCreatedAt(),
                m.getUpdatedBy(),
                m.getUpdatedAt());
        }
    }
}
