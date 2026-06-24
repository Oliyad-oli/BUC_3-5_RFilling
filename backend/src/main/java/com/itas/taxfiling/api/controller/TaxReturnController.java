package com.itas.taxfiling.api.controller;

import com.itas.taxfiling.api.dto.request.AddLineItemRequest;
import com.itas.taxfiling.api.dto.request.DraftTaxReturnRequest;
import com.itas.taxfiling.api.dto.response.CalculationResultResponse;
import com.itas.taxfiling.api.dto.response.TaxReturnDetailResponse;
import com.itas.taxfiling.api.dto.response.TaxReturnResponse;
import com.itas.taxfiling.application.usecase.taxreturn.*;
import com.itas.taxfiling.domain.model.*;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tax-returns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaxReturnController {

    private final DraftTaxReturnUseCase draftTaxReturnUseCase;
    private final AddLineItemUseCase addLineItemUseCase;
    private final RequestCalculationUseCase requestCalculationUseCase;
    private final AcceptCalculationUseCase acceptCalculationUseCase;
    private final GetTaxReturnUseCase getTaxReturnUseCase;
    private final ListTaxReturnsUseCase listTaxReturnsUseCase;

    @PostMapping
    public ResponseEntity<TaxReturnResponse> draft(@Valid @RequestBody DraftTaxReturnRequest request) {
        TaxReturn result = draftTaxReturnUseCase.execute(new DraftTaxReturnUseCase.Command(
                request.tin(),
                TaxTypeCode.valueOf(request.taxType()),
                request.filingPeriodId(),
                request.createdBy() != null ? request.createdBy() : request.tin()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxReturnDetailResponse> getById(@PathVariable String id) {
        TaxReturn result = getTaxReturnUseCase.execute(id);
        return ResponseEntity.ok(toDetailResponse(result));
    }

    @GetMapping
    public ResponseEntity<List<TaxReturnResponse>> list(@RequestParam String tin) {
        List<TaxReturn> results = listTaxReturnsUseCase.execute(tin);
        return ResponseEntity.ok(results.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/line-items")
    public ResponseEntity<TaxReturnResponse> addLineItem(
            @PathVariable String id,
            @Valid @RequestBody AddLineItemRequest request
    ) {
        String currency = request.currency() != null ? request.currency() : "ETB";
        TaxReturn result = addLineItemUseCase.execute(new AddLineItemUseCase.Command(
                id,
                request.scheduleCode(),
                request.scheduleName(),
                request.lineCode(),
                request.description(),
                new Money(request.amount(), currency)
        ));
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{id}/calculate")
    public ResponseEntity<CalculationResultResponse> calculate(@PathVariable String id) {
        RequestCalculationUseCase.CalculationResult result =
                requestCalculationUseCase.execute(new RequestCalculationUseCase.Command(id));
        return ResponseEntity.ok(toCalcResponse(result));
    }

    @PostMapping("/{id}/iterations/{iterationNo}/accept")
    public ResponseEntity<Void> accept(
            @PathVariable String id,
            @PathVariable int iterationNo
    ) {
        // Find the iteration ID by iteration number
        TaxReturn taxReturn = getTaxReturnUseCase.execute(id);
        String iterationId = taxReturn.getIterations().stream()
                .filter(it -> it.getIterationNumber() == iterationNo)
                .findFirst()
                .map(CalculationIteration::getId)
                .orElse(taxReturn.getCurrentIterationId());

        acceptCalculationUseCase.execute(new AcceptCalculationUseCase.Command(id, iterationId));
        return ResponseEntity.ok().build();
    }

    // --- Mapping helpers ---

    private TaxReturnResponse toResponse(TaxReturn r) {
        Money netTax = r.getTotalTax();
        return new TaxReturnResponse(
                r.getId(),
                r.getTin(),
                r.getTaxType() != null ? r.getTaxType().name() : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getPeriod() != null ? r.getPeriod().getStartDate() : null,
                r.getPeriod() != null ? r.getPeriod().getEndDate() : null,
                r.getCurrentIterationId(),
                netTax.getAmount(),
                netTax.getCurrency(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCreatedBy()
        );
    }

    private TaxReturnDetailResponse toDetailResponse(TaxReturn r) {
        Money netTax = r.getTotalTax();
        List<TaxReturnDetailResponse.ScheduleResponse> schedules = r.getSchedules().stream()
                .map(s -> new TaxReturnDetailResponse.ScheduleResponse(
                        s.getId(), s.getCode(), s.getName(),
                        s.getLineItems() != null ? s.getLineItems().stream()
                                .map(li -> new TaxReturnDetailResponse.LineItemResponse(
                                        li.getId(), li.getLineCode(), li.getDescription(),
                                        li.getAmount() != null ? li.getAmount().getAmount() : BigDecimal.ZERO,
                                        li.getAmount() != null ? li.getAmount().getCurrency() : "ETB",
                                        li.getSource() != null ? li.getSource().name() : null,
                                        li.getReferenceId()
                                )).collect(Collectors.toList()) : List.of()
                )).collect(Collectors.toList());

        List<TaxReturnDetailResponse.CalculationIterationResponse> iterations = r.getIterations().stream()
                .map(it -> new TaxReturnDetailResponse.CalculationIterationResponse(
                        it.getId(), it.getIterationNumber(),
                        it.getGrossTax() != null ? it.getGrossTax().getAmount() : BigDecimal.ZERO,
                        it.getInputCredit() != null ? it.getInputCredit().getAmount() : BigDecimal.ZERO,
                        it.getNetTax() != null ? it.getNetTax().getAmount() : BigDecimal.ZERO,
                        "ETB",
                        it.getCalculatedAt(),
                        it.isAccepted()
                )).collect(Collectors.toList());

        return new TaxReturnDetailResponse(
                r.getId(), r.getTin(),
                r.getTaxType() != null ? r.getTaxType().name() : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getPeriod() != null ? r.getPeriod().getStartDate() : null,
                r.getPeriod() != null ? r.getPeriod().getEndDate() : null,
                r.getCurrentIterationId(),
                netTax.getAmount(), netTax.getCurrency(),
                r.getCreatedAt(), r.getUpdatedAt(), r.getCreatedBy(),
                schedules, iterations
        );
    }

    private CalculationResultResponse toCalcResponse(RequestCalculationUseCase.CalculationResult r) {
        List<CalculationResultResponse.ComputedLineItemResponse> items = r.computedLineItems().stream()
                .map(li -> new CalculationResultResponse.ComputedLineItemResponse(
                        li.getLineCode(), li.getDescription(),
                        li.getAmount() != null ? li.getAmount().getAmount() : BigDecimal.ZERO,
                        li.getAmount() != null ? li.getAmount().getCurrency() : "ETB"
                )).collect(Collectors.toList());

        return new CalculationResultResponse(
                r.iterationId(), r.iterationNumber(),
                r.grossTax() != null ? r.grossTax().getAmount() : BigDecimal.ZERO,
                r.inputCredit() != null ? r.inputCredit().getAmount() : BigDecimal.ZERO,
                r.netTax() != null ? r.netTax().getAmount() : BigDecimal.ZERO,
                "ETB", items
        );
    }
}
