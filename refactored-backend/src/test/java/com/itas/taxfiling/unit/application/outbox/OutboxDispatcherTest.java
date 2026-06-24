package com.itas.taxfiling.unit.application.outbox;

import com.itas.taxfiling.application.event.handler.LedgerPostingOutboxHandler;
import com.itas.taxfiling.application.outbox.OutboxDispatcher;
import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LedgerEnginePort;
import com.itas.taxfiling.application.port.OutboxPort;
import com.itas.taxfiling.application.port.TaxReturnRepositoryPort;
import com.itas.taxfiling.application.usecase.taxreturn.RecordAmendmentPostedUseCase;
import com.itas.taxfiling.application.usecase.taxreturn.RecordLedgerPostedUseCase;
import com.itas.taxfiling.domain.model.OutboxEntry;
import com.itas.taxfiling.domain.model.TaxReturn;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;
import com.itas.taxfiling.domain.valueobject.Priority;
import com.itas.taxfiling.domain.valueobject.TaxReturnStatus;
import com.itas.taxfiling.unit.test_support.TaxReturnTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests OutboxDispatcher with real RecordLedgerPosted/RecordAmendmentPosted use
 * cases against mocked ports — Mockito can't mock concrete classes on Java 25.
 */
class OutboxDispatcherTest {

    private OutboxPort outbox;
    private LedgerEnginePort ledger;
    private TaxReturnRepositoryPort taxReturns;
    private EventPublisherPort eventPublisher;
    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        outbox = mock(OutboxPort.class);
        ledger = mock(LedgerEnginePort.class);
        taxReturns = mock(TaxReturnRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        when(taxReturns.save(any(TaxReturn.class))).thenAnswer(inv -> inv.getArgument(0));

        var recordLedger = new RecordLedgerPostedUseCase(taxReturns, eventPublisher);
        var recordAmendment = new RecordAmendmentPostedUseCase(taxReturns, eventPublisher);
        dispatcher = new OutboxDispatcher(outbox, ledger, taxReturns, recordLedger, recordAmendment);
        ReflectionTestUtils.setField(dispatcher, "batchSize", 10);
    }

    @Test
    void postAssessment_path_calls_ledger_and_records_posted() {
        TaxReturn t = TaxReturnTestBuilder.accepted();
        t.pullEvents();
        when(taxReturns.findById(t.getId())).thenReturn(Optional.of(t));

        String payload = "{\"taxReturnId\":\"" + t.getId() + "\","
            + "\"tin\":\"1234567\","
            + "\"taxType\":\"VAT\","
            + "\"periodLabel\":\"2026-04\","
            + "\"periodStart\":\"2026-04-01\","
            + "\"periodEnd\":\"2026-04-30\","
            + "\"periodFrequency\":\"MONTHLY\","
            + "\"amount\":\"100.00\","
            + "\"currency\":\"ETB\","
            + "\"category\":\"PRINCIPAL\"}";
        OutboxEntry entry = OutboxEntry.pending(
            "TaxReturn", t.getId(),
            LedgerPostingOutboxHandler.TOPIC_POST_ASSESSMENT, payload, Priority.HIGH);

        when(outbox.claimReady(anyInt(), any())).thenReturn(List.of(entry));
        LedgerEntryReference ref = new LedgerEntryReference(
            UUID.randomUUID(), AccountCategory.PRINCIPAL, Instant.now());
        when(ledger.postAssessment(eq("1234567"), any(), any(), any(), eq(AccountCategory.PRINCIPAL)))
            .thenReturn(ref);

        dispatcher.drain();

        verify(ledger).postAssessment(eq("1234567"), any(), any(), any(), eq(AccountCategory.PRINCIPAL));
        verify(outbox).markSent(entry);
        assertThat(t.getStatus()).isEqualTo(TaxReturnStatus.POSTED_TO_LEDGER);
    }

    @Test
    void unknown_topic_marks_failed() {
        OutboxEntry entry = OutboxEntry.pending(
            "X", UUID.randomUUID(), "unknown.topic", "{}", Priority.LOW);
        when(outbox.claimReady(anyInt(), any())).thenReturn(List.of(entry));

        dispatcher.drain();

        verify(outbox).markFailed(eq(entry), anyString());
        verify(ledger, never()).postAssessment(any(), any(), any(), any(), any());
    }

    @Test
    void exception_below_max_attempts_marks_retry() {
        TaxReturn t = TaxReturnTestBuilder.accepted();
        t.pullEvents();

        String payload = "{\"taxReturnId\":\"" + t.getId() + "\","
            + "\"tin\":\"1234567\","
            + "\"taxType\":\"VAT\","
            + "\"periodLabel\":\"2026-04\","
            + "\"periodStart\":\"2026-04-01\","
            + "\"periodEnd\":\"2026-04-30\","
            + "\"periodFrequency\":\"MONTHLY\","
            + "\"amount\":\"100.00\","
            + "\"currency\":\"ETB\","
            + "\"category\":\"PRINCIPAL\"}";
        OutboxEntry entry = OutboxEntry.pending(
            "TaxReturn", t.getId(),
            LedgerPostingOutboxHandler.TOPIC_POST_ASSESSMENT, payload, Priority.HIGH);

        when(outbox.claimReady(anyInt(), any())).thenReturn(List.of(entry));
        when(ledger.postAssessment(any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("upstream timeout"));

        dispatcher.drain();

        verify(outbox).markRetry(eq(entry), anyString(), any());
        verify(outbox, never()).markSent(any());
    }
}
