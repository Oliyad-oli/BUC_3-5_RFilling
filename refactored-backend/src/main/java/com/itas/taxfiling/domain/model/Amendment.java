package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.AmendmentDelta;
import com.itas.taxfiling.domain.valueobject.AmendmentReason;
import com.itas.taxfiling.domain.valueobject.LedgerEntryReference;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An amendment cycle on a COMPLETED TaxReturn (Rule 8, BUC-FIL-030..033).
 * Embedded entity on TaxReturn — only one amendment can be open at a time.
 * On finalisation the delta is posted to PRINCIPAL only.
 */
public final class Amendment {

    private final UUID id;
    private final AmendmentReason reason;
    private final String requestedByActorId;
    private final Instant requestedAt;
    private AmendmentDelta delta;
    private LedgerEntryReference postedDelta;
    private Instant finalisedAt;

    private Amendment(UUID id, AmendmentReason reason, String requestedByActorId, Instant requestedAt) {
        this.id = id;
        this.reason = reason;
        this.requestedByActorId = requestedByActorId;
        this.requestedAt = requestedAt;
    }

    public static Amendment open(AmendmentReason reason, String requestedByActorId) {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(requestedByActorId, "requestedByActorId");
        return new Amendment(UUID.randomUUID(), reason, requestedByActorId, Instant.now());
    }

    public void recordDelta(AmendmentDelta delta) {
        this.delta = Objects.requireNonNull(delta, "delta");
    }

    public void recordPosted(LedgerEntryReference reference) {
        this.postedDelta = Objects.requireNonNull(reference, "reference");
        this.finalisedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public AmendmentReason getReason() { return reason; }
    public String getRequestedByActorId() { return requestedByActorId; }
    public Instant getRequestedAt() { return requestedAt; }
    public AmendmentDelta getDelta() { return delta; }
    public LedgerEntryReference getPostedDelta() { return postedDelta; }
    public Instant getFinalisedAt() { return finalisedAt; }

    public static Amendment rehydrate(UUID id, AmendmentReason reason, String requestedBy,
                                      Instant requestedAt, AmendmentDelta delta,
                                      LedgerEntryReference postedDelta, Instant finalisedAt) {
        Amendment a = new Amendment(id, reason, requestedBy, requestedAt);
        a.delta = delta;
        a.postedDelta = postedDelta;
        a.finalisedAt = finalisedAt;
        return a;
    }
}
