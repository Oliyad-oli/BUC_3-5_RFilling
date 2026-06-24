package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.request.RegisterEntryTypeRequest;
import com.itas.taxfiling.api.dto.request.RetireEntryTypeRequest;
import com.itas.taxfiling.api.dto.response.LineItemEntryTypeResponse;
import com.itas.taxfiling.application.usecase.entrytype.QueryLineItemEntryTypeUseCase;
import com.itas.taxfiling.application.usecase.entrytype.ListLineItemEntryTypesUseCase;
import com.itas.taxfiling.application.usecase.entrytype.RegisterLineItemEntryTypeUseCase;
import com.itas.taxfiling.application.usecase.entrytype.RetireLineItemEntryTypeUseCase;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin-config catalog endpoints for LineItemEntryType (BUC-FIL-CONFIG-01).
 * Immutable-rows pattern: every register call inserts a new row; retire flips
 * status. Existing line items keep their original definition.
 */
@RestController
@RequestMapping("/line-item-entry-types")
@RequiredArgsConstructor
@Tag(name = "Line Item Entry Type Catalog",
     description = "BUC-FIL-CONFIG-01 — admin-configurable line-item shapes (Rule 11)")
public class LineItemEntryTypeController {

    private final RegisterLineItemEntryTypeUseCase registerUseCase;
    private final RetireLineItemEntryTypeUseCase retireUseCase;
    private final ListLineItemEntryTypesUseCase listUseCase;
    private final QueryLineItemEntryTypeUseCase getUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new entry type version (BUC-FIL-CONFIG-01)")
    public LineItemEntryTypeResponse register(@Valid @RequestBody RegisterEntryTypeRequest req) {
        return LineItemEntryTypeResponse.from(registerUseCase.execute(
            req.code(), req.toTaxTypeCode(), req.scheduleKind(),
            req.toFieldDefinitions(), req.adminActorId()));
    }

    @PostMapping("/{id}/retire")
    @Operation(summary = "Retire an entry type version")
    public LineItemEntryTypeResponse retire(@PathVariable UUID id,
                                            @Valid @RequestBody RetireEntryTypeRequest req) {
        return LineItemEntryTypeResponse.from(retireUseCase.execute(id, req.adminActorId()));
    }

    @GetMapping
    @Operation(summary = "List ACTIVE entry types for a tax type and schedule kind")
    public List<LineItemEntryTypeResponse> list(@RequestParam String taxType,
                                                @RequestParam ScheduleKind scheduleKind) {
        return listUseCase.execute(new TaxTypeCode(taxType), scheduleKind).stream()
            .map(LineItemEntryTypeResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one entry type by id")
    public LineItemEntryTypeResponse get(@PathVariable UUID id) {
        return LineItemEntryTypeResponse.from(getUseCase.execute(id));
    }
}
