package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/** Reference to a posted ledger entry returned by the ledger engine. */
public record LedgerEntryReference(String entryId, String journalId, AccountCategory category) {

    public LedgerEntryReference {
        Objects.requireNonNull(entryId, "entryId");
        Objects.requireNonNull(category, "category");
    }
}
