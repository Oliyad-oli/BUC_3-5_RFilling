package com.itas.taxfiling.application.event.handler;

import com.itas.taxfiling.application.port.OutboxPort;
import com.itas.taxfiling.domain.event.AmendmentAcceptedEvent;
import com.itas.taxfiling.domain.event.PostingToLedgerEvent;
import com.itas.taxfiling.domain.model.OutboxEntry;
import com.itas.taxfiling.domain.valueobject.Priority;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Listens to events that require an external ledger-engine call and writes a
 * corresponding outbox row. Runs BEFORE_COMMIT so the outbox row is part of the
 * same transaction as the aggregate state change — at-least-once is preserved.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerPostingOutboxHandler {

    public static final String TOPIC_POST_ASSESSMENT = "ledger.postAssessment";
    public static final String TOPIC_POST_ADJUSTMENT = "ledger.postAdjustment";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OutboxPort outbox;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPostingToLedger(PostingToLedgerEvent e) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taxReturnId", e.taxReturnId().toString());
        payload.put("tin", e.tin());
        payload.put("taxType", e.taxType().value());
        payload.put("periodLabel", e.period().label());
        payload.put("periodStart", e.period().start().toString());
        payload.put("periodEnd", e.period().end().toString());
        payload.put("periodFrequency", e.period().frequency().name());
        payload.put("amount", e.amount().amount().toPlainString());
        payload.put("currency", e.amount().currency());
        payload.put("category", e.category().name());

        outbox.enqueue(OutboxEntry.pending(
            "TaxReturn", e.taxReturnId(), TOPIC_POST_ASSESSMENT, toJson(payload), Priority.HIGH));
        log.debug("Enqueued ledger.postAssessment outbox row for taxReturnId={}", e.taxReturnId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onAmendmentAccepted(AmendmentAcceptedEvent e) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taxReturnId", e.taxReturnId().toString());
        payload.put("delta", e.delta().delta().amount().toPlainString());
        payload.put("currency", e.delta().delta().currency());
        payload.put("originalEntryId", e.delta().originalLedgerEntryId().toString());

        outbox.enqueue(OutboxEntry.pending(
            "TaxReturn", e.taxReturnId(), TOPIC_POST_ADJUSTMENT, toJson(payload), Priority.HIGH));
        log.debug("Enqueued ledger.postAdjustment outbox row for taxReturnId={}", e.taxReturnId());
    }

    private static String toJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialise outbox payload", ex);
        }
    }
}
