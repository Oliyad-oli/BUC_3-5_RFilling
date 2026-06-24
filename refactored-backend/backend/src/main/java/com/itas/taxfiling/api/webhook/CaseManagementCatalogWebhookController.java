package com.itas.taxfiling.api.webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * case-management push webhook (Rule 13). Notifies filing-service when the
 * issue-category catalog changes. Phase 1: filing always reads categories
 * fresh from case-management, so this is a no-op acknowledgement that keeps
 * the contract surface ready for Phase 2 (local issue-category cache).
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/case-management")
@Tag(name = "Webhook — case-management",
     description = "Rule 13 — issue-category catalog change push (Phase 2 readiness)")
public class CaseManagementCatalogWebhookController {

    @PostMapping("/issue-category-catalog-published")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Acknowledge a case-management catalog change")
    public Map<String, String> onPublished(@RequestBody Map<String, Object> payload) {
        log.info("case-management webhook issue-category-catalog-published payloadKeys={}", payload.keySet());
        return Map.of("status", "acknowledged");
    }
}
