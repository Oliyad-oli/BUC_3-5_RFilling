package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.request.SubmitOfficerDecisionRequest;
import com.itas.taxfiling.api.dto.response.OfficerReviewItemResponse;
import com.itas.taxfiling.application.usecase.officer.GetOfficerReviewItemUseCase;
import com.itas.taxfiling.application.usecase.officer.ListOfficerReviewQueueUseCase;
import com.itas.taxfiling.application.usecase.officer.SubmitOfficerDecisionUseCase;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.domain.valueobject.OfficerReviewDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfficerReviewController {

    private final ListOfficerReviewQueueUseCase listReviewQueueUseCase;
    private final GetOfficerReviewItemUseCase getReviewItemUseCase;
    private final SubmitOfficerDecisionUseCase submitDecisionUseCase;

    // --- /cases endpoints (frontend expects /api/v1/cases) ---

    @GetMapping("/cases")
    public ResponseEntity<List<OfficerReviewItemResponse>> listCases(
            @RequestParam(required = false, defaultValue = "OPEN") String status
    ) {
        List<OfficerReviewItem> items = listReviewQueueUseCase.execute(status);
        return ResponseEntity.ok(items.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<OfficerReviewItemResponse> getCaseById(@PathVariable String id) {
        OfficerReviewItem item = getReviewItemUseCase.execute(id);
        return ResponseEntity.ok(toResponse(item));
    }

    @PostMapping("/cases/{id}/decision")
    public ResponseEntity<Void> submitCaseDecision(
            @PathVariable String id,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @Valid @RequestBody SubmitOfficerDecisionRequest request
    ) {
        String officerId = actorId != null ? actorId : request.officerActorId();
        submitDecisionUseCase.execute(new SubmitOfficerDecisionUseCase.Command(
                id,
                officerId,
                OfficerReviewDecision.valueOf(request.decision()),
                request.narrative()
        ));
        return ResponseEntity.ok().build();
    }

    // --- /officer-review-items endpoints (also expected by frontend) ---

    @GetMapping("/officer-review-items")
    public ResponseEntity<List<OfficerReviewItemResponse>> listReviewItems(
            @RequestParam(required = false, defaultValue = "OPEN") String status
    ) {
        List<OfficerReviewItem> items = listReviewQueueUseCase.execute(status);
        return ResponseEntity.ok(items.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/officer-review-items/{id}")
    public ResponseEntity<OfficerReviewItemResponse> getReviewItemById(@PathVariable String id) {
        OfficerReviewItem item = getReviewItemUseCase.execute(id);
        return ResponseEntity.ok(toResponse(item));
    }

    @PostMapping("/officer-review-items/{caseId}/decision")
    public ResponseEntity<Void> submitReviewDecision(
            @PathVariable String caseId,
            @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
            @Valid @RequestBody SubmitOfficerDecisionRequest request
    ) {
        String officerId = actorId != null ? actorId : request.officerActorId();
        submitDecisionUseCase.execute(new SubmitOfficerDecisionUseCase.Command(
                caseId,
                officerId,
                OfficerReviewDecision.valueOf(request.decision()),
                request.narrative()
        ));
        return ResponseEntity.ok().build();
    }

    private OfficerReviewItemResponse toResponse(OfficerReviewItem item) {
        return new OfficerReviewItemResponse(
                item.getId(),
                item.getReturnId(),
                item.getKind() != null ? item.getKind().name() : null,
                item.getPriority(),
                item.getStatus(),
                item.getAssignedOfficer(),
                item.getDecision() != null ? item.getDecision().name() : null,
                item.getDecisionNotes(),
                item.getEvidencePayload(),
                item.getCreatedAt(),
                item.getAssignedAt(),
                item.getDecidedAt()
        );
    }
}
