package com.itas.taxfiling.engineadapter.integration.payment;

import com.itas.taxfiling.application.port.PaymentInvoicePushPort;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Direct HTTP push to payment-service's FilingProcessed webhook. Fired when
 * a TaxReturn reaches COMPLETED so payment-service mints the invoice
 * (BUC-PAY-001). Phase-1 wiring — Phase-2 swaps for event-bus dispatch.
 */
@Slf4j
@Component
public class PaymentInvoicePushAdapter implements PaymentInvoicePushPort {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String webhookUrl;

    public PaymentInvoicePushAdapter(
            @Value("${itas.payment.filing-processed-url:http://localhost:8083/api/v1/webhooks/filing-service/filing-processed}")
            String webhookUrl) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper().findAndRegisterModules();
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void pushFilingProcessed(
            String tin, String taxType, String periodLabel,
            LocalDate periodStart, LocalDate periodEnd, PeriodFrequency periodFrequency,
            BigDecimal principalAmount, String currency) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("tin", tin);
            body.put("taxType", taxType);
            body.put("periodLabel", periodLabel);
            body.put("periodStart", periodStart.toString());
            body.put("periodEnd", periodEnd.toString());
            body.put("periodFrequency", periodFrequency.name());
            body.put("principalAmount", principalAmount.toPlainString());
            body.put("currency", currency);

            HttpRequest req = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                log.info("FilingProcessed pushed tin={} taxType={} period={} → {}",
                        tin, taxType, periodLabel, res.statusCode());
            } else {
                log.warn("FilingProcessed push non-2xx tin={} status={} body={}",
                        tin, res.statusCode(), res.body());
            }
        } catch (Exception ex) {
            // Swallow — the COMPLETED state is committed regardless of whether
            // payment-service receives the push (will be picked up by retry
            // job in Phase 2).
            log.warn("FilingProcessed push failed tin={} taxType={}: {}",
                    tin, taxType, ex.getMessage());
        }
    }
}
