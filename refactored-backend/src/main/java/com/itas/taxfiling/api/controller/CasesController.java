package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.request.OpenCaseFromErrorRequestDto;
import com.itas.taxfiling.api.dto.response.CaseHandleResponse;
import com.itas.taxfiling.api.dto.response.IssueCategoryResponse;
import com.itas.taxfiling.application.usecase.cases.ListIssueCategoriesUseCase;
import com.itas.taxfiling.application.usecase.cases.OpenCaseFromErrorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rule 13 — case-from-error endpoints. Mounted under both /portal and
 * /backoffice paths so the same handler serves both Flow B (taxpayer) and
 * Flow A (officer-on-behalf).
 *
 * Per spec: the gateway authenticates and supplies party_id; the controller
 * forwards request as-is — categorisation + payload validation happens here.
 */
@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Tag(name = "Cases", description = "Rule 13 — open a support case after engine failure")
public class CasesController {

    private final OpenCaseFromErrorUseCase openCase;
    private final ListIssueCategoriesUseCase listCategories;

    @PostMapping({"/portal/cases/from-error", "/backoffice/cases/from-error"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a support case from a failed engine call (Rule 13)")
    public CaseHandleResponse open(@Valid @RequestBody OpenCaseFromErrorRequestDto dto) {
        return CaseHandleResponse.from(openCase.execute(dto.toDomain()));
    }

    @GetMapping({"/portal/cases/issue-categories", "/backoffice/cases/issue-categories"})
    @Operation(summary = "List the case categories for a given source BUC")
    public List<IssueCategoryResponse> categories(@RequestParam String sourceBuc) {
        return listCategories.execute(sourceBuc).stream()
            .map(IssueCategoryResponse::from).toList();
    }
}
