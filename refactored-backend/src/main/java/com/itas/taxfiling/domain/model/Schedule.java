package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.*;

import java.time.Instant;
import java.util.*;

/**
 * A schedule under a TaxReturn (Rule 5). Owns its LineItems.
 * Schedule IDs are stable across amendment iterations.
 */
public final class Schedule {

    private final UUID id;
    private final ScheduleKind kind;
    private final String label;
    private final List<LineItem> lineItems;
    private final Instant createdAt;

    private Schedule(UUID id, ScheduleKind kind, String label,
                     List<LineItem> lineItems, Instant createdAt) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.lineItems = lineItems;
        this.createdAt = createdAt;
    }

    public static Schedule open(ScheduleKind kind, String label) {
        Objects.requireNonNull(kind, "kind");
        return new Schedule(UUID.randomUUID(), kind, label, new ArrayList<>(), Instant.now());
    }

    public LineItem addLineItem(UUID entryTypeId, int entryTypeVersion, Money amount,
                                LineItemSource source, EntrySpecificData entryData) {
        LineItem item = LineItem.create(entryTypeId, entryTypeVersion, amount, source, entryData);
        lineItems.add(item);
        return item;
    }

    public Optional<LineItem> findLineItem(UUID lineItemId) {
        return lineItems.stream().filter(li -> li.getId().equals(lineItemId)).findFirst();
    }

    public void removeLineItem(UUID lineItemId) {
        lineItems.removeIf(li -> li.getId().equals(lineItemId));
    }

    public Money totalAmount(String currency) {
        return lineItems.stream()
            .map(LineItem::getAmount)
            .reduce(Money.zero(currency), Money::add);
    }

    public UUID getId() { return id; }
    public ScheduleKind getKind() { return kind; }
    public String getLabel() { return label; }
    public List<LineItem> getLineItems() { return List.copyOf(lineItems); }
    public Instant getCreatedAt() { return createdAt; }

    public static Schedule rehydrate(UUID id, ScheduleKind kind, String label,
                                     List<LineItem> lineItems, Instant createdAt) {
        return new Schedule(id, kind, label, new ArrayList<>(lineItems), createdAt);
    }
}
