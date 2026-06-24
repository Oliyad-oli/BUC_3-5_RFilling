package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.*;

import java.time.Instant;
import java.util.*;

/**
 * Polymorphic line item under a Schedule (Rule 5).
 * Universal-spine columns are typed fields; tax-type-specific shape lives in
 * {@link EntrySpecificData} stored as JSONB.
 */
public final class LineItem {

    private final UUID id;
    private final UUID entryTypeId;
    private final int entryTypeVersion;
    private Money amount;
    private final LineItemSource source;
    private EntrySpecificData entryData;
    private LineItemValidationState validationState;
    private final List<ValidationMessage> messages;
    private final Instant createdAt;
    private Instant updatedAt;

    private LineItem(UUID id, UUID entryTypeId, int entryTypeVersion, Money amount,
                     LineItemSource source, EntrySpecificData entryData,
                     LineItemValidationState validationState,
                     List<ValidationMessage> messages, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.entryTypeId = entryTypeId;
        this.entryTypeVersion = entryTypeVersion;
        this.amount = amount;
        this.source = source;
        this.entryData = entryData;
        this.validationState = validationState;
        this.messages = messages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static LineItem create(UUID entryTypeId, int entryTypeVersion, Money amount,
                                  LineItemSource source, EntrySpecificData entryData) {
        Objects.requireNonNull(entryTypeId, "entryTypeId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(entryData, "entryData");
        Instant now = Instant.now();
        return new LineItem(UUID.randomUUID(), entryTypeId, entryTypeVersion, amount, source, entryData,
            LineItemValidationState.CLEAN, new ArrayList<>(), now, now);
    }

    public void replaceAmount(Money newAmount) {
        this.amount = Objects.requireNonNull(newAmount, "newAmount");
        this.updatedAt = Instant.now();
    }

    public void replaceEntryData(EntrySpecificData newData) {
        this.entryData = Objects.requireNonNull(newData, "newData");
        this.updatedAt = Instant.now();
    }

    public void flag(List<ValidationMessage> findings) {
        this.validationState = LineItemValidationState.FLAGGED;
        this.messages.clear();
        this.messages.addAll(findings);
        this.updatedAt = Instant.now();
    }

    public void clearFindings() {
        this.validationState = LineItemValidationState.CLEAN;
        this.messages.clear();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEntryTypeId() { return entryTypeId; }
    public int getEntryTypeVersion() { return entryTypeVersion; }
    public Money getAmount() { return amount; }
    public LineItemSource getSource() { return source; }
    public EntrySpecificData getEntryData() { return entryData; }
    public LineItemValidationState getValidationState() { return validationState; }
    public List<ValidationMessage> getMessages() { return List.copyOf(messages); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static LineItem rehydrate(UUID id, UUID entryTypeId, int entryTypeVersion, Money amount,
                                     LineItemSource source, EntrySpecificData entryData,
                                     LineItemValidationState state, List<ValidationMessage> messages,
                                     Instant createdAt, Instant updatedAt) {
        return new LineItem(id, entryTypeId, entryTypeVersion, amount, source, entryData,
            state, new ArrayList<>(messages), createdAt, updatedAt);
    }
}
