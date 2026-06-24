package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.OutboxEntry;

import java.time.Instant;
import java.util.List;

/**
 * Outbox port — used by use cases to enqueue side-effects (ledger posts, event
 * fan-outs) within the same transaction as the domain change, and by the
 * dispatcher to drain pending rows.
 */
public interface OutboxPort {

    OutboxEntry enqueue(OutboxEntry entry);

    List<OutboxEntry> claimReady(int batchSize, Instant now);

    void markSent(OutboxEntry entry);

    void markRetry(OutboxEntry entry, String error, Instant nextAttemptAt);

    void markFailed(OutboxEntry entry, String error);
}
