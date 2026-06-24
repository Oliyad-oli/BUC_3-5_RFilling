package com.itas.taxfiling.application.outbox;

import com.itas.taxfiling.application.event.handler.LedgerPostingOutboxHandler;
import com.itas.taxfiling.application.port.LedgerEnginePort;
import com.itas.taxfiling.application.port.OutboxPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.RecordAmendmentPostedUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RecordLedgerPostedUseCase;
import com.itas.taxfiling.domain.exception.ResourceNotFoundException;
import com.itas.taxfiling.domain.model.OutboxEntry;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Polls outbox PENDING rows and dispatches them. Two topics today:
 *   - ledger.postAssessment → LedgerEnginePort.postAssessment + RecordLedgerPostedUseCase
 *   - ledger.postAdjustment → LedgerEnginePort.postAdjustment + RecordAmendmentPostedUseCase
 *
 * Failed rows get incremented attempts + exponential next-attempt backoff.
 * After 5 attempts the row is permanently FAILED — operator intervention required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxPort outbox;
    private final LedgerEnginePort ledgerEngine;
    private final TaxReturnRepositoryPort taxReturns;
    private final RecordLedgerPostedUseCase recordLedgerPosted;
    private final RecordAmendmentPostedUseCase recordAmendmentPosted;

    @Value("${itas.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${itas.outbox.poll-interval-ms:5000}")
    public void drain() {
        List<OutboxEntry> batch = outbox.claimReady(batchSize, Instant.now());
        if (batch.isEmpty()) return;
        log.debug("Outbox drain claimed {} pending rows", batch.size());
        for (OutboxEntry entry : batch) {
            dispatchOne(entry);
        }
    }

    private void dispatchOne(OutboxEntry entry) {
        try {
            switch (entry.getTopic()) {
                case LedgerPostingOutboxHandler.TOPIC_POST_ASSESSMENT -> dispatchPostAssessment(entry);
                case LedgerPostingOutboxHandler.TOPIC_POST_ADJUSTMENT -> dispatchPostAdjustment(entry);
                default -> {
                    log.warn("Unknown outbox topic: {}", entry.getTopic());
                    outbox.markFailed(entry, "unknown topic: " + entry.getTopic());
                    return;
                }
            }
            outbox.markSent(entry);
        } catch (Exception ex) {
            log.error("Outbox dispatch failed for entry {} topic={}", entry.getId(), entry.getTopic(), ex);
            handleFailure(entry, ex);
        }
    }

    private void handleFailure(OutboxEntry entry, Exception ex) {
        if (entry.getAttempts() + 1 >= MAX_ATTEMPTS) {
            outbox.markFailed(entry, ex.getMessage());
        } else {
            long backoffSec = (long) Math.pow(2, entry.getAttempts() + 1);
            outbox.markRetry(entry, ex.getMessage(),
                Instant.now().plus(backoffSec, ChronoUnit.SECONDS));
        }
    }

    private void dispatchPostAssessment(OutboxEntry entry) {
        Map<String, Object> p = parse(entry.getPayload());
        UUID taxReturnId = UUID.fromString((String) p.get("taxReturnId"));
        String tin = (String) p.get("tin");
        TaxTypeCode taxType = new TaxTypeCode((String) p.get("taxType"));
        Period period = new Period(
            LocalDate.parse((String) p.get("periodStart")),
            LocalDate.parse((String) p.get("periodEnd")),
            PeriodFrequency.valueOf((String) p.get("periodFrequency")));
        Money amount = new Money(new BigDecimal((String) p.get("amount")), (String) p.get("currency"));
        AccountCategory category = AccountCategory.valueOf((String) p.get("category"));

        LedgerEntryReference ref = ledgerEngine.postAssessment(tin, taxType, period, amount, category);
        recordLedgerPosted.execute(taxReturnId, ref);
    }

    private void dispatchPostAdjustment(OutboxEntry entry) {
        Map<String, Object> p = parse(entry.getPayload());
        UUID taxReturnId = UUID.fromString((String) p.get("taxReturnId"));
        Money delta = new Money(new BigDecimal((String) p.get("delta")), (String) p.get("currency"));
        UUID originalEntryId = UUID.fromString((String) p.get("originalEntryId"));

        TaxReturn t = taxReturns.findById(taxReturnId)
            .orElseThrow(() -> new ResourceNotFoundException("tax return not found: " + taxReturnId));
        LedgerEntryReference ref = ledgerEngine.postAdjustment(
            t.getTaxpayer().tin(), t.getTaxType(), t.getPeriod(),
            delta, AccountCategory.PRINCIPAL, originalEntryId);
        recordAmendmentPosted.execute(taxReturnId, ref);
    }

    private Map<String, Object> parse(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse outbox payload: " + e.getMessage(), e);
        }
    }
}
