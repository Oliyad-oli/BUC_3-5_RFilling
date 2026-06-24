package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.request.AddLineItemRequest;
import com.itas.taxfiling.api.dto.request.AddScheduleRequest;
import com.itas.taxfiling.api.dto.request.DraftTaxReturnRequest;
import com.itas.taxfiling.api.dto.request.PullEInvoiceRequest;
import com.itas.taxfiling.api.dto.request.RequestAmendmentRequest;
import com.itas.taxfiling.api.dto.request.RequestCalculationRequest;
import com.itas.taxfiling.api.dto.request.UpdateLineItemRequest;
import com.itas.taxfiling.api.dto.response.CalculationIterationResponse;
import com.itas.taxfiling.api.dto.response.LineItemResponse;
import com.itas.taxfiling.api.dto.response.ScheduleResponse;
import com.itas.taxfiling.api.dto.response.TaxReturnResponse;
import com.itas.taxfiling.application.usecase.taxreturn.AcceptAmendmentDeltaUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.AcceptCalculationUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.AddLineItemUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.AddScheduleUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.DeleteLineItemUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.DraftTaxReturnUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.GetTaxReturnUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.ListTaxReturnsForTaxpayerUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.PullEInvoiceLineItemsUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RequestAmendmentUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RequestCalculationUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.UpdateLineItemUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * TaxReturn endpoints. No auth annotations — security is fully owned by the
 * API Gateway + Keycloak.
 */
@RestController
@RequestMapping("/tax-returns")
@RequiredArgsConstructor
@Tag(name = "Tax Returns", description = "BUC-FIL-001..033 — draft, calculate, post, amend tax returns")
public class TaxReturnController {

    private final DraftTaxReturnUseCase draftUseCase;
    private final GetTaxReturnUseCase getUseCase;
    private final ListTaxReturnsForTaxpayerUseCase listUseCase;
    private final AddScheduleUseCase addScheduleUseCase;
    private final AddLineItemUseCase addLineItemUseCase;
    private final UpdateLineItemUseCase updateLineItemUseCase;
    private final DeleteLineItemUseCase deleteLineItemUseCase;
    private final PullEInvoiceLineItemsUseCase pullEInvoiceUseCase;
    private final RequestCalculationUseCase requestCalculationUseCase;
    private final AcceptCalculationUseCase acceptCalculationUseCase;
    private final RequestAmendmentUseCase requestAmendmentUseCase;
    private final AcceptAmendmentDeltaUseCase acceptAmendmentDeltaUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Draft a new tax return (BUC-FIL-001/002)")
    public TaxReturnResponse draft(@Valid @RequestBody DraftTaxReturnRequest request) {
        return TaxReturnResponse.from(draftUseCase.execute(
            request.tin(), request.toTaxTypeCode(), request.toPeriod(), request.method()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tax return by id")
    public TaxReturnResponse get(@PathVariable UUID id) {
        return TaxReturnResponse.from(getUseCase.execute(id));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Get a tax return with its schedules, line items, and iterations — for the resume / details view")
    public com.itas.taxfiling.api.dto.response.TaxReturnDetailsResponse details(@PathVariable UUID id) {
        return com.itas.taxfiling.api.dto.response.TaxReturnDetailsResponse.from(getUseCase.execute(id));
    }

    @GetMapping
    @Operation(summary = "List tax returns for a taxpayer")
    public List<TaxReturnResponse> list(@RequestParam String tin) {
        return listUseCase.execute(tin).stream().map(TaxReturnResponse::from).toList();
    }

    @PostMapping("/{id}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a schedule to a tax return (BUC-FIL-003)")
    public ScheduleResponse addSchedule(@PathVariable UUID id, @Valid @RequestBody AddScheduleRequest req) {
        return ScheduleResponse.from(addScheduleUseCase.execute(id, req.kind(), req.label()));
    }

    @PostMapping("/{id}/schedules/{scheduleId}/line-items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a line item to a schedule (BUC-FIL-004)")
    public LineItemResponse addLineItem(@PathVariable UUID id, @PathVariable UUID scheduleId,
                                        @Valid @RequestBody AddLineItemRequest req) {
        return LineItemResponse.from(addLineItemUseCase.execute(
            id, scheduleId, req.entryTypeId(), req.toMoney(), req.source(), req.toEntryData()));
    }

    @PutMapping("/{id}/schedules/{scheduleId}/line-items/{lineItemId}")
    @Operation(summary = "Update a line item (BUC-FIL-006)")
    public LineItemResponse updateLineItem(@PathVariable UUID id, @PathVariable UUID scheduleId,
                                           @PathVariable UUID lineItemId,
                                           @Valid @RequestBody UpdateLineItemRequest req) {
        return LineItemResponse.from(updateLineItemUseCase.execute(
            id, scheduleId, lineItemId, req.toMoneyOrNull(), req.toEntryDataOrNull()));
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}/line-items/{lineItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a line item (BUC-FIL-006)")
    public void deleteLineItem(@PathVariable UUID id, @PathVariable UUID scheduleId,
                               @PathVariable UUID lineItemId) {
        deleteLineItemUseCase.execute(id, scheduleId, lineItemId);
    }

    @PostMapping("/{id}/schedules/{scheduleId}/pull-einvoice")
    @Operation(summary = "Pull e-invoice line items into a schedule, scoped to the tax return's filing period (BUC-FIL-005). Existing line items with the same externalInvoiceId are skipped.")
    public PullResult pullEInvoice(@PathVariable UUID id, @PathVariable UUID scheduleId,
                                   @Valid @RequestBody PullEInvoiceRequest req) {
        var outcome = pullEInvoiceUseCase.execute(id, scheduleId, req.entryTypeId());
        return new PullResult(outcome.added(), outcome.skipped());
    }

    @PostMapping("/{id}/calculate")
    @Operation(summary = "Run a calculation iteration (BUC-FIL-010..012)")
    public CalculationIterationResponse calculate(@PathVariable UUID id,
                                                  @Valid @RequestBody RequestCalculationRequest req) {
        return CalculationIterationResponse.from(
            requestCalculationUseCase.execute(id, req.toAnswers()));
    }

    @PostMapping("/{id}/iterations/{iterationId}/accept")
    @Operation(summary = "Accept a calculation iteration and queue ledger posting (BUC-FIL-013)")
    public TaxReturnResponse acceptCalculation(@PathVariable UUID id, @PathVariable UUID iterationId) {
        return TaxReturnResponse.from(acceptCalculationUseCase.execute(id, iterationId));
    }

    @PostMapping("/{id}/amendments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open an amendment cycle (BUC-FIL-030)")
    public TaxReturnResponse requestAmendment(@PathVariable UUID id,
                                              @Valid @RequestBody RequestAmendmentRequest req) {
        return TaxReturnResponse.from(
            requestAmendmentUseCase.execute(id, req.reason(), req.requestedByActorId()));
    }

    @PostMapping("/{id}/amendments/iterations/{iterationId}/accept")
    @Operation(summary = "Accept the amendment delta and queue PRINCIPAL adjustment (BUC-FIL-032)")
    public TaxReturnResponse acceptAmendmentDelta(@PathVariable UUID id,
                                                  @PathVariable UUID iterationId) {
        return TaxReturnResponse.from(acceptAmendmentDeltaUseCase.execute(id, iterationId));
    }

    /**
     * Outcome of a pull. {@code linesAdded} is new line items appended to the
     * schedule; {@code linesSkipped} is incoming e-invoices dropped because
     * their {@code externalInvoiceId} was already present (idempotent replay).
     */
    public record PullResult(int linesAdded, int linesSkipped) {}
}
