package com.itas.taxfiling.api.webhook;

import com.itas.taxfiling.application.usecase.taxreturn.PullEInvoiceLineItemsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * e-service push webhook (BUC-FIL-002). e-service notifies filing-service
 * when a new invoice is published. Triggers a one-off pull for the affected
 * (TIN × period) tuple — only if there's an open DRAFT.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/e-service")
@RequiredArgsConstructor
@Tag(name = "Webhook — e-service", description = "BUC-FIL-002 — e-invoice push")
public class EInvoiceWebhookController {

    private final PullEInvoiceLineItemsUseCase pullUseCase;

    @PostMapping("/invoice-published")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Notify filing-service that a new e-invoice was published")
    public AckResponse onInvoicePublished(@RequestBody InvoicePublishedPayload payload) {
        log.info("e-service webhook invoice-published tin={} taxReturnId={} scheduleId={} entryTypeId={}",
            payload.tin(), payload.taxReturnId(), payload.scheduleId(), payload.entryTypeId());
        if (payload.taxReturnId() != null && payload.scheduleId() != null && payload.entryTypeId() != null) {
            var outcome = pullUseCase.execute(payload.taxReturnId(), payload.scheduleId(), payload.entryTypeId());
            return new AckResponse("accepted", outcome.added(), outcome.skipped());
        }
        return new AckResponse("queued", 0, 0);
    }

    public record InvoicePublishedPayload(
        @NotNull String tin,
        UUID taxReturnId,
        UUID scheduleId,
        UUID entryTypeId
    ) {}

    public record AckResponse(String status, int linesPulled, int linesSkipped) {}
}
