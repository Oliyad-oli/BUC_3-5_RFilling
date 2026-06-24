package com.itas.taxfiling.api.webhook;

import com.itas.taxfiling.application.port.RulePackageCachePort;
import com.itas.taxfiling.domain.valueobject.RulePackageVersion;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * tax-type-engine push webhook (Rule 9). Notifies filing-service when a new
 * FilingRulePackage is published — invalidates / refreshes the local cache.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/tax-type-engine")
@RequiredArgsConstructor
@Tag(name = "Webhook — tax-type-engine", description = "Rule 9 — rule package change push")
public class RulePackageWebhookController {

    private static final long CACHE_TTL_HOURS = 6;

    private final RulePackageCachePort cache;

    @PostMapping("/rule-package-published")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Refresh local cache after a new rule package is published")
    public Ack onPublished(@RequestBody RulePackagePublishedPayload payload) {
        log.info("rule-package-published taxType={} version={} effectiveOn={}",
            payload.taxType(), payload.version(), payload.effectiveOn());
        TaxTypeCode taxType = new TaxTypeCode(payload.taxType());
        Instant now = Instant.now();
        cache.upsert(
            taxType,
            payload.effectiveOn(),
            new RulePackageVersion(payload.taxType(), payload.version()),
            now,
            now.plus(CACHE_TTL_HOURS, ChronoUnit.HOURS));
        return new Ack("refreshed");
    }

    public record RulePackagePublishedPayload(
        @NotBlank String taxType,
        @NotBlank String version,
        @NotNull LocalDate effectiveOn
    ) {}

    public record Ack(String status) {}
}
