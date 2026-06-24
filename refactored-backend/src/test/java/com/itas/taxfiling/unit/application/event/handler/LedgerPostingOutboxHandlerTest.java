package com.itas.taxfiling.unit.application.event.handler;

import com.itas.taxfiling.application.event.handler.LedgerPostingOutboxHandler;
import com.itas.taxfiling.application.port.OutboxPort;
import com.itas.taxfiling.domain.event.AmendmentAcceptedEvent;
import com.itas.taxfiling.domain.event.PostingToLedgerEvent;
import com.itas.taxfiling.domain.model.OutboxEntry;
import com.itas.taxfiling.domain.valueobject.AccountCategory;
import com.itas.taxfiling.domain.valueobject.AmendmentDelta;
import com.itas.taxfiling.domain.valueobject.Money;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerPostingOutboxHandlerTest {

    @Mock OutboxPort outbox;
    @InjectMocks LedgerPostingOutboxHandler handler;

    @Test
    void onPostingToLedger_enqueues_postAssessment_outbox_row() {
        UUID taxReturnId = UUID.randomUUID();
        when(outbox.enqueue(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        handler.onPostingToLedger(new PostingToLedgerEvent(
            UUID.randomUUID(), Instant.now(), taxReturnId,
            "1234567", new TaxTypeCode("VAT"), Period.monthly(YearMonth.of(2026, 4)),
            Money.of("100.00", "ETB"), AccountCategory.PRINCIPAL));

        ArgumentCaptor<OutboxEntry> entryCaptor = ArgumentCaptor.forClass(OutboxEntry.class);
        org.mockito.Mockito.verify(outbox).enqueue(entryCaptor.capture());
        OutboxEntry entry = entryCaptor.getValue();
        assertThat(entry.getTopic()).isEqualTo(LedgerPostingOutboxHandler.TOPIC_POST_ASSESSMENT);
        assertThat(entry.getPayload()).contains(taxReturnId.toString()).contains("PRINCIPAL");
    }

    @Test
    void onAmendmentAccepted_enqueues_postAdjustment_outbox_row() {
        UUID taxReturnId = UUID.randomUUID();
        UUID origEntry = UUID.randomUUID();
        when(outbox.enqueue(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        handler.onAmendmentAccepted(new AmendmentAcceptedEvent(
            UUID.randomUUID(), Instant.now(), taxReturnId,
            new AmendmentDelta(Money.of("50.00", "ETB"), origEntry)));

        ArgumentCaptor<OutboxEntry> entryCaptor = ArgumentCaptor.forClass(OutboxEntry.class);
        org.mockito.Mockito.verify(outbox).enqueue(entryCaptor.capture());
        OutboxEntry entry = entryCaptor.getValue();
        assertThat(entry.getTopic()).isEqualTo(LedgerPostingOutboxHandler.TOPIC_POST_ADJUSTMENT);
        assertThat(entry.getPayload()).contains(origEntry.toString());
    }
}
