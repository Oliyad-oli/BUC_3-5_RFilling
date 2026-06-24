package com.itas.taxfiling.api.webhook;

import com.itas.taxfiling.application.usecase.obligation.GenerateInitialFilingPeriodsUseCase;
import com.itas.taxfiling.application.usecase.projection.UpdateTaxpayerProjectionUseCase;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * registration-service push webhook (Rule 10). Updates the local
 * taxpayer_projection + subledger_projection read-models AND auto-generates
 * the obligation + filing periods for the (TIN × tax type) so the taxpayer
 * lands in the portal with everything ready to file — no manual seed step.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/registration-service")
@RequiredArgsConstructor
@Tag(name = "Webhook — registration-service",
     description = "Rule 10 — projection sync + obligation seeding from registration-service")
public class RegistrationWebhookController {

    private final UpdateTaxpayerProjectionUseCase projection;
    private final GenerateInitialFilingPeriodsUseCase generatePeriods;

    @PostMapping("/tax-type-status-changed")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Refresh local projection + auto-generate obligation/filing periods")
    public Ack onChanged(@RequestBody StatusChangedPayload payload) {
        log.info("registration webhook tax-type-status-changed tin={} taxType={} status={}",
            payload.tin(), payload.taxType(), payload.status());
        projection.upsertTaxpayer(payload.tin(), payload.partyId(), payload.legalName(),
            payload.status(), "ACTIVE".equalsIgnoreCase(payload.status()));
        if (payload.principalSubledgerId() != null) {
            projection.upsertSubledgers(payload.tin(), payload.taxType(),
                payload.principalSubledgerId(),
                payload.penaltySubledgerId(),
                payload.interestSubledgerId(),
                payload.refundSubledgerId());
        }

        // Auto-generate obligation + filing periods for ACTIVE tax types so the
        // taxpayer immediately sees a ready-to-file row in /efiling. The
        // effective_from defaults to first day of last calendar month — VAT is
        // filed AFTER month-end so this gives exactly one fileable period
        // (last month) without burying the demo in years of historical periods.
        if ("ACTIVE".equalsIgnoreCase(payload.status())) {
            try {
                LocalDate effectiveFrom = payload.effectiveFrom() != null
                    ? payload.effectiveFrom()
                    : LocalDate.now().minusMonths(1).withDayOfMonth(1);
                var result = generatePeriods.execute(
                    payload.tin(), payload.partyId(),
                    new TaxTypeCode(payload.taxType()),
                    effectiveFrom);
                log.info("ensured obligation tin={} taxType={} obligationId={} newlyCreated={}",
                    payload.tin(), payload.taxType(),
                    result.obligationId(), result.newlyCreated());
            } catch (Exception ex) {
                log.warn("obligation auto-generation failed tin={} taxType={}: {}",
                    payload.tin(), payload.taxType(), ex.getMessage());
            }
        }

        return new Ack("applied");
    }

    public record StatusChangedPayload(
        @NotBlank String tin,
        @NotBlank String partyId,
        String legalName,
        @NotBlank String status,
        @NotBlank String taxType,
        UUID principalSubledgerId,
        UUID penaltySubledgerId,
        UUID interestSubledgerId,
        UUID refundSubledgerId,
        // Optional — registration's tax-type effective date. When omitted the
        // handler defaults to the first day of last calendar month (demo
        // sweet-spot for monthly filings like VAT).
        LocalDate effectiveFrom
    ) {}

    public record Ack(String status) {}
}
