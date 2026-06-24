package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.request.AssignReviewerRequest;
import com.itas.taxfiling.api.dto.request.OfficerDecisionRequest;
import com.itas.taxfiling.api.dto.response.OfficerReviewItemResponse;
import com.itas.taxfiling.application.usecase.officer.AssignOfficerReviewItemUseCase;
import com.itas.taxfiling.application.usecase.officer.GetOfficerReviewItemUseCase;
import com.itas.taxfiling.application.usecase.officer.ListOfficerReviewQueueUseCase;
import com.itas.taxfiling.application.usecase.officer.SubmitOfficerDecisionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * In-house officer review queue endpoints (BUC-FIL-050/051). No auth annotations
 * — gateway-owned. The actor id is read from the X-Actor-Id header in the
 * MdcContextFilter and forwarded to the use case via the request body.
 */
@RestController
@RequestMapping("/officer-review-items")
@RequiredArgsConstructor
@Tag(name = "Officer Review Queue",
     description = "BUC-FIL-050/051 — in-house fraud review queue (Phase 1)")
public class OfficerReviewController {

    private final ListOfficerReviewQueueUseCase listUseCase;
    private final GetOfficerReviewItemUseCase getUseCase;
    private final AssignOfficerReviewItemUseCase assignUseCase;
    private final SubmitOfficerDecisionUseCase decideUseCase;

    @GetMapping
    @Operation(summary = "List open review queue items, sorted by priority")
    public List<OfficerReviewItemResponse> list() {
        return listUseCase.execute().stream().map(OfficerReviewItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one review item")
    public OfficerReviewItemResponse get(@PathVariable UUID id) {
        return OfficerReviewItemResponse.from(getUseCase.execute(id));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign an officer to a review item")
    public OfficerReviewItemResponse assign(@PathVariable UUID id,
                                            @Valid @RequestBody AssignReviewerRequest req) {
        return OfficerReviewItemResponse.from(assignUseCase.execute(id, req.officerActorId()));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Submit officer decision (BUC-FIL-051)")
    public OfficerReviewItemResponse decide(@PathVariable UUID id,
                                            @Valid @RequestBody OfficerDecisionRequest req) {
        return OfficerReviewItemResponse.from(
            decideUseCase.execute(id, req.decision(), req.officerActorId(), req.narrative()));
    }
}
